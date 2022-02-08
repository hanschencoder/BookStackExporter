package site.hanschen;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class GsonUtils {

    private GsonUtils() {
    }

    private final static Gson gson = new GsonBuilder().create();

    public static String toJson(Object object) {
        return gson.toJson(object);
    }

    public static <T> T fromJson(String json, Class<T> clz) {
        return gson.fromJson(json, clz);
    }
}
