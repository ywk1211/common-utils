package com.ywk.common.util.serialize;

import com.google.gson.*;
import org.apache.commons.lang3.StringUtils;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * @author yanwenkai
 */
public class GsonProvider {

    public final static Charset UTF8 = StandardCharsets.UTF_8;

    public final static GsonProvider INSTANCE = new GsonProvider();

    private final static DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final static DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final static byte[] EMPTY = new byte[0];

    private Gson gson = null;

    private GsonProvider() {
        GsonBuilder builder = new GsonBuilder();
        builder.disableHtmlEscaping();

        builder.registerTypeAdapter(LocalDateTime.class, (JsonSerializer<LocalDateTime>) (src, typeOfSrc, context) -> new JsonPrimitive(src.format(DATE_TIME_FORMATTER)));

        builder.registerTypeAdapter(LocalDateTime.class, (JsonDeserializer<LocalDateTime>) (json, typeOfT, context) -> {
            String datetime = json.getAsJsonPrimitive().getAsString();
            return LocalDateTime.parse(datetime, DATE_TIME_FORMATTER);
        });

        builder.registerTypeAdapter(LocalDate.class, (JsonSerializer<LocalDate>) (src, typeOfSrc, context) -> new JsonPrimitive(src.format(DATE_FORMATTER)));

        builder.registerTypeAdapter(LocalDate.class, (JsonDeserializer<LocalDate>) (json, typeOfT, context) -> {
            String datetime = json.getAsJsonPrimitive().getAsString();
            return LocalDate.parse(datetime, DATE_FORMATTER);
        });

        this.gson = builder.create();
    }

    public <T> T parse(String json, Class<T> clazz) {

        if (StringUtils.isBlank(json)) {
            return null;
        }

        return gson.fromJson(json, clazz);
    }

    public <T> T parse(byte[] json, Class<T> clazz) {

        if (json == null || json.length <= 0) {
            return null;
        }

        return gson.fromJson(new String(json, UTF8), clazz);
    }

    public String toJsonString(Object obj) {

        if (obj == null) {
            return StringUtils.EMPTY;
        }

        return gson.toJson(obj);
    }

    public byte[] toJsonBytes(Object obj) {

        if (obj == null) {
            return EMPTY;
        }

        return gson.toJson(obj).getBytes(UTF8);
    }

}
