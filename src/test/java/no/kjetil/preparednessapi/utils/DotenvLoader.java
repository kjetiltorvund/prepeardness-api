package no.kjetil.preparednessapi.utils;

import io.github.cdimascio.dotenv.Dotenv;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DotenvLoader {
    private static final Map<String, String> CACHE = new ConcurrentHashMap<>();
    private static volatile boolean loaded = false;

    private DotenvLoader() {}

    private static void loadIfNeeded() {
        if(loaded) {
            return;
        }

        synchronized (DotenvLoader.class) {
            if(loaded) return;
            Dotenv dotenv = Dotenv.configure()
                    .filename(".env")
                    .ignoreIfMissing()
                    .ignoreIfMalformed()
                    .load();

            dotenv.entries().forEach(e -> CACHE.put(e.getKey(), e.getValue()));
            loaded = true;
        }
    }

    public static String get(String key) {
        loadIfNeeded();
        return CACHE.get(key);
    }

    public static String getOrDefault(String key, String defaultValue) {
        String v = get(key);
        return v != null ? v : defaultValue;
    }

}
