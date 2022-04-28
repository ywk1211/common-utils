package com.ywk.common.util.token;

import com.alibaba.fastjson.JSON;
import com.google.common.collect.Sets;
import com.ywk.common.util.encryption.DesUtil;
import lombok.Getter;
import org.apache.commons.codec.binary.Base64;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.util.Set;

/**
 * 用户token des 加解密
 * @date 2021年10月18日13:52:46
 * @author yanwenkai
 */
@Getter
public class UserToken {

	private String userCd;

	private int isTemp;

	private long createDtLong;

	private String ct;

	private UserToken() {
	}

	public UserToken(String userCd, String ct) {
		this.userCd = userCd;
		this.isTemp = 0;
		this.ct = ct;
		this.createDtLong = System.currentTimeMillis();
	}

	public UserToken(String userCd, String ct, Integer isTemp) {
		this.userCd = userCd;
		this.ct = ct;
		this.isTemp = isTemp;
		this.createDtLong = System.currentTimeMillis();
	}

	public String toTokenString(byte[] secureKey) throws RuntimeException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		DataOutputStream dos = new DataOutputStream(out);
		try {
			String str = isTemp + "," + ct + "," + userCd;
			dos.writeUTF(str);
			dos.writeLong(createDtLong);

			dos.flush();

			return Base64.encodeBase64String(DesUtil.encode(secureKey, out.toByteArray()));
		} catch (Exception e) {
			throw new RuntimeException(userCd+".isTemp:"+isTemp+".desFail");
		} finally {
			try {
				dos.close();
			} catch (Exception e2) {
			}
		}
	}

	public static UserToken fromTokenString(byte[] secureKey, String tokenString) throws RuntimeException {

		ByteArrayInputStream in = null;
		DataInputStream dis = null;
		try {
			if (tokenString.contains(" ")) {
				tokenString = tokenString.replaceAll(" ", "+");
			}
			byte[] d = DesUtil.decode(secureKey, Base64.decodeBase64(tokenString));

			in = new ByteArrayInputStream(d);

			dis = new DataInputStream(in);

			UserToken token = new UserToken();

			String str = dis.readUTF();
			String[] split = str.split(",");
			token.isTemp = Integer.parseInt(split[0]);
			token.ct = split[1];
			token.userCd = split[2];
			token.createDtLong = dis.readLong();

			return token;

		} catch (Exception e) {
			throw new RuntimeException(tokenString+".decodeFail");
		} finally {
			try {
				if (dis != null) {
					dis.close();
				}

			} catch (Exception ignored) {
			}
		}

	}

	public static void main(String[] args) throws Exception {

		byte[] secureKey = DesUtil.generateKey();
//		byte[] secureKey = Base64.decodeBase64("PkzfMjJRHIo=");
		String secureKeyString = Base64.encodeBase64String(secureKey);
		System.out.println(secureKeyString);
//		String secureKeyString = Base64.encodeBase64String(secureKey);
//		System.out.println(secureKeyString);
		Set<String> set = Sets.newHashSet();
//		for (int i = 0; i < 1000; i++) {

//		UserToken ut = new UserToken("2062252", "android", 0);
//
//			String tokenString = ut.toTokenString(secureKey);
//
//			System.out.println(tokenString + "\t" + tokenString.length());
//
//			ut = UserToken.fromTokenString(secureKey, tokenString);
//
//			System.out.println(ut.getIsTemp());
//			System.out.println(ut.getUserCd());
//		System.out.println(ut.getCt());
//			System.out.println(ut.getCreateDtLong());

//		}

		String str = "VRUQZvmvElipwsZ 0UbVdvJ3xJFxxglx";
		UserToken userToken = UserToken.fromTokenString(Base64.decodeBase64("PkzfMjJRHIo="), str);
		System.out.println(JSON.toJSONString(userToken));

	}

}
