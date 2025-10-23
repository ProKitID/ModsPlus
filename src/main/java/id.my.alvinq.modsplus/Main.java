package id.my.alvinq.modsplus;

import android.content.Context;
import java.io.*;

public class Main {

    public static void onLoad(Context ctx) {
        clearCache(ctx);
        copyAllLibs(ctx);
        loadAllLibs(ctx);
    }

    public static void clearCache(Context ctx) {
        File dir = ctx.getDir("modsplus", Context.MODE_PRIVATE);
        File cacheDir = new File(dir, "cache");
        Logger.get().info("Clearing Cache...");
        Utils.deleteFolder(cacheDir.getAbsolutePath());
        Logger.get().info("Clearing Cache Done!");
    }

    public static void copyAllLibs(Context ctx) {
        try {
            File dir = ctx.getDir("modsplus", Context.MODE_PRIVATE);
            File internalLibs = new File(dir, "cache/libs");
            File externalLibs = new File("/storage/emulated/0/alvinqid/libs");

            if (!externalLibs.exists() || !externalLibs.isDirectory()) {
                externalLibs.delete();
                externalLibs.mkdirs();
                Logger.get().info("Libs Folder Created!");
            }

            if (!internalLibs.exists() || !internalLibs.isDirectory()) {
                internalLibs.delete();
                internalLibs.mkdirs();
                Logger.get().info("Internal Libs Folder Created!");
            } else {
                internalLibs.delete();
                internalLibs.mkdirs();
            }

            File[] jars = externalLibs.listFiles();
            if (jars != null) {
                for (File jar : jars) {
                    if (jar.getName().endsWith(".modplus")) {
                        File dest = new File(internalLibs, jar.getName() + ".jar");
                        Utils.copyFile(jar, dest);
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void loadAllLibs(Context ctx) {
        try {
            File dir = ctx.getDir("modsplus", Context.MODE_PRIVATE);
            File libsDir = new File(dir, "cache/libs");
            File[] jars = libsDir.listFiles();

            if (jars != null) {
                for (File jar : jars) {
                    if (!jar.getName().endsWith(".modplus.jar")) continue;
                    Logger.get().info("Loading -> " + jar.getName());
                    LibsManager.get(ctx).loadLib(jar);
                    Logger.get().info("Loaded -> " + jar.getName() + " Done!");
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
