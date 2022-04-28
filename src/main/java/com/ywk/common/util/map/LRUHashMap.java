package com.ywk.common.util.map;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @date 2022年2月8日15:37:56
 * @author yanwenkai
 */
public class LRUHashMap<K, V> {

	public static enum ExpireStrategy {
		Idle, Expire;
	}

	private final static ScheduledExecutorService CHECK_EXPIRE_EXEC = new ScheduledThreadPoolExecutor(1,
			new ThreadFactoryBuilder().setNameFormat("lru-schedule-pool-%d").setDaemon(true).build());

	private static final Executor EVICT_EXEC = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors(),
			new ThreadFactoryBuilder().setDaemon(true).setNameFormat("lru-action-thread-%d").build());

	static class TimestampEntryValue<V> {
		V value;
		long timestamp;
	}

	private static class LRUContainerMap<K, V extends TimestampEntryValue<?>> extends LinkedHashMap<K, V> {

		private static final long serialVersionUID = -3826014966717299610L;

		private ReentrantLock lock = new ReentrantLock();

		private int maxCapacity;

		private F.Action2<K, V> onEvict;

		public LRUContainerMap(int maxCapacity, F.Action2<K, V> onEvict) {
			super(16, 0.75f, true);
			this.maxCapacity = maxCapacity;
			this.onEvict = onEvict;
		}

		int getMaxCapacity() {
			return maxCapacity;
		}

		void lock() {
			lock.lock();
		}

		public void unlock() {
			lock.unlock();
		}

		@Override
		public V put(K key, V value) {
			lock();
			try {
				return super.put(key, value);
			} finally {
				unlock();
			}
		}

		@Override
		public V putIfAbsent(K key, V value) {
			lock();
			try {
				V result = super.get(key);
				if (result != null) {
					return result;
				} else {
					super.put(key, value);
					return null;
				}
			} finally {
				unlock();
			}
		}

		@Override
		public V get(Object key) {
			lock.lock();
			try {
				return super.get(key);
			} finally {
				lock.unlock();
			}
		}

		public V getAndPut(K key, V value) {
			lock.lock();
			try {
				V v = super.get(key);
				super.put(key, value);
				return v;
			} finally {
				lock.unlock();
			}
		}

		@SuppressWarnings("unchecked")
		@Override
		public V remove(final Object key) {
			lock();
			try {
				final V ret = super.remove(key);
				if (onEvict != null) {
					EVICT_EXEC.execute(() -> {
						try {
							onEvict.invoke((K) key, ret);
						} catch (Exception ignore) {
						}
					});
				}
				return ret;
			} finally {
				unlock();
			}
		}

		V removeUnEvict(final Object key) {
			lock();
			try {
				final V ret = super.remove(key);

				return ret;
			} finally {
				unlock();
			}
		}

		void clearWithEvict() {
			lock();
			try {
				if (onEvict != null) {

					List<Entry<K, V>> ls = new ArrayList<Entry<K, V>>();

					for (final Entry<K, V> e : this.entrySet()) {
						ls.add(e);
					}

					this.clear();

					for (final Entry<K, V> e : ls) {
						EVICT_EXEC.execute(() -> {
							try {
								onEvict.invoke(e.getKey(), e.getValue());
							} catch (Exception ignore) {
							}
						});
					}
				} else {
					this.clear();
				}

			} finally {
				unlock();
			}
		}

		void clearWithoutEvict() {
			lock();
			try {
				super.clear();
			} finally {
				unlock();
			}
		}

		@Override
		protected boolean removeEldestEntry(final Entry<K, V> eldest) {
			final boolean ret = size() > maxCapacity;
			if (onEvict != null && ret) {
				EVICT_EXEC.execute(() -> onEvict.invoke(eldest.getKey(), eldest.getValue()));
			}
			return ret;
		}
	}

	private AtomicBoolean isCleanerRunning = new AtomicBoolean(false);

	private LRUContainerMap<K, TimestampEntryValue<V>> container;

	private Runnable expireRunnable = new Runnable() {

		@Override
		public void run() {
			long nextInterval = 1000;
			container.lock();
			try {

				boolean shouldStopCleaner = true;

				if (container.size() > 0) {
					long now = System.currentTimeMillis();

					List<K> toBeRemoved = new ArrayList<>();

					for (Entry<K, TimestampEntryValue<V>> e : container.entrySet()) {
						K key = e.getKey();
						TimestampEntryValue<V> tValue = e.getValue();
						long timeLapsed = now - tValue.timestamp;

						if (timeLapsed >= duration) {
							toBeRemoved.add(key);
						} else {
							long delta = duration - timeLapsed;
							if (delta > 1000L) {
								nextInterval = delta;
							}
							break;
						}
					}

					if (toBeRemoved.size() > 0) {
						for (K key : toBeRemoved) {
							container.remove(key);
						}
					}

					if (container.size() > 0) {
						shouldStopCleaner = false;
					}
				}

				if (shouldStopCleaner) {
					isCleanerRunning.compareAndSet(true, false);
				} else {
					CHECK_EXPIRE_EXEC.schedule(this, nextInterval, TimeUnit.MILLISECONDS);
				}

			} finally {
				container.unlock();
			}

		}
	};

	private long duration = -1;

	private ExpireStrategy expireStrategy;

	public LRUHashMap(int maxCapacity, final F.Action2<K, V> onEvict, ExpireStrategy expireStrategy, long duration) {

		F.Action2<K, TimestampEntryValue<V>> doOnEvict = null;

		if (onEvict != null) {
			doOnEvict = (key, value) -> {
				if (value != null) {
					onEvict.invoke(key, value.value);
				}
			};
		}
		this.duration = duration;
		this.expireStrategy = expireStrategy;
		container = new LRUContainerMap<>(maxCapacity, doOnEvict);
	}

	public int size() {
		return container.size();
	}

	int getMaxCapacity() {
		return container.getMaxCapacity();
	}

	public Set<K> getKeys() {
		return container.keySet();
	}

	public long getDuration() {
		return duration;
	}

	public V put(K key, V value) {
		TimestampEntryValue<V> v = new TimestampEntryValue<>();
		v.timestamp = System.currentTimeMillis();
		v.value = value;
		TimestampEntryValue<V> old = container.put(key, v);

		if (duration > 0) {
			if (isCleanerRunning.compareAndSet(false, true)) {

				switch (this.expireStrategy) {
				case Idle:
					CHECK_EXPIRE_EXEC.schedule(expireRunnable, duration, TimeUnit.MILLISECONDS);
					break;
				case Expire:
					long l = (duration / 2);
					if (l <= 0) {
						l = 1000L;
					}
					CHECK_EXPIRE_EXEC.schedule(expireRunnable, l, TimeUnit.MILLISECONDS);
				default:
					break;
				}

			}
		}

		return old == null ? null : old.value;
	}

	public V putIfAbsent(K key, V value) {
		TimestampEntryValue<V> v = new TimestampEntryValue<>();
		v.timestamp = System.currentTimeMillis();
		v.value = value;
		TimestampEntryValue<V> old = container.putIfAbsent(key, v);

		if (old == null) {
			if (duration > 0) {
				if (isCleanerRunning.compareAndSet(false, true)) {
					switch (this.expireStrategy) {
					case Idle:
						CHECK_EXPIRE_EXEC.schedule(expireRunnable, duration, TimeUnit.MILLISECONDS);
						break;
					case Expire:
						long l = (duration / 2);
						if (l <= 0) {
							l = 1000L;
						}
						CHECK_EXPIRE_EXEC.schedule(expireRunnable, l, TimeUnit.MILLISECONDS);
						break;
					}
				}
			}
		}

		return old == null ? null : old.value;
	}

	public boolean containsKey(Object key) {
		TimestampEntryValue<V> got = container.get(key);
		boolean ret = false;
		if (got != null) {
			if (this.expireStrategy == ExpireStrategy.Expire
					&& (System.currentTimeMillis() - got.timestamp < this.duration)) {
				ret = true;
			}
		}

		return ret;
	}

	public V get(K key) {

		TimestampEntryValue<V> got = container.get(key);
		V ret = null;
		if (got != null) {
			switch (this.expireStrategy) {
			case Idle:
				got.timestamp = System.currentTimeMillis();
				return got.value;
			case Expire:
				if (System.currentTimeMillis() - got.timestamp < this.duration) {
					ret = got.value;
				}
			}
		}
		return ret;
	}

	public V getAndPut(K key, V value) {

		TimestampEntryValue<V> v = new TimestampEntryValue<>();
		v.timestamp = System.currentTimeMillis();
		v.value = value;

		TimestampEntryValue<V> got = container.getAndPut(key, v);

		V ret = null;
		if (got != null) {
			switch (this.expireStrategy) {
			case Idle:
				got.timestamp = System.currentTimeMillis();
				return got.value;
			case Expire:
				if (System.currentTimeMillis() - got.timestamp < this.duration) {
					ret = got.value;
				}
			}
		}
		return ret;
	}

	public V remove(K key, boolean doEvict) {
		TimestampEntryValue<V> removed;
		if (doEvict) {
			removed = container.remove(key);
		} else {
			removed = container.removeUnEvict(key);
		}

		V ret = null;
		if (removed != null) {
			ret = removed.value;
		}
		return ret;
	}

	public V remove(K key) {
		return remove(key, true);
	}

	public void clearWithEvict() {
		this.container.clearWithEvict();
	}

	public void clearWithoutEvict() {
		this.container.clearWithoutEvict();
	}

	public List<V> values() {
		Collection<TimestampEntryValue<V>> c = this.container.values();

		List<V> result = new ArrayList<>(c.size());

		for (TimestampEntryValue<V> e : c) {
			result.add(e.value);
		}

		return result;
	}
}