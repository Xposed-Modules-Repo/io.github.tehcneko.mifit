package io.github.tehcneko.mifit;

import android.util.Pair;

import java.util.HashMap;

public class ClassMaps {
    public static HashMap<Integer, Pair<String, String>> languageClassMap = new HashMap<>();
    public static int maxSupportedVersion = 50472;

    static {
        languageClassMap.put(50426, new Pair<>("com.xiaomi.hm.health.o0000O00.OooOo", "OooOOoo"));
        languageClassMap.put(50429, new Pair<>("com.xiaomi.hm.health.o0000O00.OooOo", "OooOOoo"));
        languageClassMap.put(50433, new Pair<>("com.xiaomi.hm.health.o0000O00.OooOo", "OooOOoo"));
        languageClassMap.put(50472, new Pair<>("com.xiaomi.hm.health.o0000O00.OooOo", "OooOo00"));
    }
}
