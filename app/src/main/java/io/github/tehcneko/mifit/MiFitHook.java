package io.github.tehcneko.mifit;

import android.util.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;

import dalvik.system.BaseDexClassLoader;
import dalvik.system.DexFile;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class MiFitHook implements IXposedHookLoadPackage {
    private static final String TAG = "MiFitHook";

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam loadPackageParam) {
        if (loadPackageParam.packageName.equals("com.xiaomi.hm.health")) {
            Log.d(TAG, "handleLoadPackage");
            var packageMapClass = findPackageMapClass(loadPackageParam.classLoader);
            if (packageMapClass != null) {
                try {
                    XposedHelpers.findAndHookConstructor(packageMapClass, new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            //noinspection unchecked
                            var hashMap = (HashMap<String, Object>) param.thisObject;
                            var telegramIcon = hashMap.get("org.telegram.messenger");
                            if (telegramIcon != null) {
                                hashMap.put("tw.nekomimi.nekogram", telegramIcon);
                                hashMap.put("tw.nekomimi.nekogram.beta", telegramIcon);
                                hashMap.put("ua.itaysonlab.messenger", telegramIcon);
                                Log.d(TAG, "map content: " + hashMap.toString());
                            }
                        }
                    });
                } catch (Throwable t) {
                    XposedBridge.log(t);
                }
            }
        }
    }

    private Class<?> findPackageMapClass(ClassLoader classLoader) {
        try {
            var classNames = getAllClassNamesList(classLoader);
            for (String className : classNames) {
                if (className.startsWith("com.xiaomi.hm.health.ui.smartplay.") &&
                        className.length() - className.replace(".", "").length() == 6 &&
                        className.contains("$") &&
                        !className.contains("Activity") &&
                        !className.contains("Service")) {
                    var clazz = classLoader.loadClass(className);
                    if (clazz.getSuperclass() != null && clazz.getSuperclass().equals(HashMap.class)) {
                        var hashMap = (HashMap<?, ?>) XposedHelpers.newInstance(clazz);
                        if (hashMap.containsKey("org.telegram.messenger")) {
                            Log.d(TAG, "found map class: " + className);
                            return clazz;
                        }
                    }
                }
            }
        } catch (Throwable t) {
            XposedBridge.log(t);
        }
        XposedBridge.log("map class not found");
        return null;
    }

    private List<String> getAllClassNamesList(ClassLoader classLoader) {
        var baseDexClassLoader = classLoader;
        while (!(baseDexClassLoader instanceof BaseDexClassLoader)) {
            if (baseDexClassLoader.getParent() != null) {
                baseDexClassLoader = baseDexClassLoader.getParent();
            } else {
                return Collections.emptyList();
            }
        }
        var pathList = XposedHelpers.getObjectField(baseDexClassLoader, "pathList");
        if (pathList == null) {
            return Collections.emptyList();
        }
        var dexElements = (Object[]) XposedHelpers.getObjectField(pathList, "dexElements");
        if (dexElements == null) {
            return Collections.emptyList();
        }
        List<String> classes = new ArrayList<>();
        for (Object element : dexElements) {
            DexFile dexFile = (DexFile) XposedHelpers.getObjectField(element, "dexFile");
            if (dexFile != null) {
                //noinspection unchecked
                var enumeration = (Enumeration<String>) XposedHelpers.callMethod(dexFile, "entries");
                if (enumeration != null) {
                    classes.addAll(Collections.list(enumeration));
                }
            }
        }
        return classes;
    }
}
