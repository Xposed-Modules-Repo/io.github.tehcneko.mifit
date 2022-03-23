package io.github.tehcneko.mifit;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.content.res.XModuleResources;
import android.util.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;

import dalvik.system.BaseDexClassLoader;
import dalvik.system.DexFile;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class MiFitHook implements IXposedHookLoadPackage, IXposedHookZygoteInit {
    private static final String TAG = "MiFitHook";
    private static final long MIN_VERSION_CODE = 50542;
    private static final String MIN_VERSION_NAME = "6.0.0";
    private static final String[] PACKAGES = new String[]{
            "org.telegram.messenger.web",
            "org.telegram.messenger.beta",
            "tw.nekomimi.nekogram",
            "tw.nekomimi.nekogram.beta",
            "com.cool2645.nekolite",
            "ua.itaysonlab.messenger",
            "org.forkclient.messenger.beta",
            "it.owlgram.android",
            "nekox.messenger"
    };

    private static Resources moduleResources;

    @Override
    public void initZygote(StartupParam startupParam) {
        moduleResources = XModuleResources.createInstance(startupParam.modulePath, null);
    }

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam loadPackageParam) {
        if (loadPackageParam.packageName.equals("com.xiaomi.hm.health")) {
            Log.d(TAG, "handleLoadPackage");

            var packageMap = findPackageMap(loadPackageParam.classLoader);
            if (packageMap != null) {
                //noinspection unchecked
                var hashMap = (HashMap<String, Object>) packageMap;
                var telegramIcon = hashMap.get("org.telegram.messenger");
                if (telegramIcon != null) {
                    for (var packageName : PACKAGES) {
                        hashMap.put(packageName, telegramIcon);
                    }
                    Log.d(TAG, "map content: " + hashMap);
                }
            } else {
                hookDialog();
            }
        }
    }

    private int getAppVersionCode(Context context) {
        try {
            PackageInfo packageInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            return packageInfo.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            return 0;
        }
    }

    private void hookDialog() {
        XposedBridge.hookAllMethods(Activity.class, "onCreate", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) {
                var context = (Context) param.thisObject;
                new AlertDialog.Builder(context)
                        .setTitle(moduleResources.getString(R.string.unsupported_title))
                        .setMessage(getAppVersionCode(context) < MIN_VERSION_CODE ?
                                moduleResources.getString(R.string.unsupported_message, MIN_VERSION_NAME, MIN_VERSION_CODE) :
                                moduleResources.getString(R.string.unsupported_message_notfound))
                        .setPositiveButton(android.R.string.ok, null)
                        .setCancelable(false)
                        .show();
            }
        });
    }

    private HashMap<?, ?> findPackageMap(ClassLoader classLoader) {
        try {
            var classNames = getAllClassNamesList(classLoader);
            for (String className : classNames) {
                if (className.startsWith("com.huami.device.feature.notification.appnotify.") && !className.contains("$")) {
                    var clazz = classLoader.loadClass(className);
                    var fields = clazz.getDeclaredFields();
                    for (var field : fields) {
                        if (HashMap.class.equals(field.getType())) {
                            field.setAccessible(true);
                            var hashMap = (HashMap<?, ?>) field.get(null);
                            if (hashMap != null && hashMap.containsKey("org.telegram.messenger")) {
                                Log.d(TAG, "found map field: " + className + " " + field.getName());
                                return hashMap;
                            }
                        }
                    }
                }
            }
        } catch (Throwable t) {
            XposedBridge.log(t);
        }
        XposedBridge.log("map field not found");
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
