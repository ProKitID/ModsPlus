package id.my.alvinq.modsplus;

import android.content.Context;
import java.io.*;
//import org.levimc.launcher.util.Logger;
import org.levimc.launcher.core.minecraft.pesdk.utils.AssetOverrideManager;

public class Main {
  public static void onLoad(Context ctx) {
	 // try {
	/*File bs = new File(ctx.getCacheDir().getAbsolutePath());
	File bd = new File("/sdcard/alvinqid/before");
	FileCopiers.copyFolder(bs,bd);*/
    clearCache(ctx);
    copyAllLibs(ctx);
    loadAllLibs(ctx);/*
	File as = new File(ctx.getCacheDir().getAbsolutePath());
	File ad = new File("/sdcard/alvinqid/after");
	FileCopiers.copyFolder(as,ad);*/
					 //  } catch(Exception e) {};
  }
  public static void clearCache(Context ctx) {/*
	File amod = new File("/sdcard/alvinqid/mod.apk");
	AssetOverrideManager.addAssetOverride(ctx.getAssets(), amod.getAbsolutePath());*/
	File dir = ctx.getDir("modsplus", Context.MODE_PRIVATE);
    String path = dir.getAbsolutePath();
	File cacheDir = new File(path, "cache");
	//if(cacheDir.exists()) {
	   Logger.get().info("Clearing Cache...");
	   //cacheDir.delete();
	  Utils.deleteFolder(cacheDir.getAbsolutePath());
	   Logger.get().info("Clearing Cache Done!");
	//}
  }
  public static void copyAllLibs(Context ctx) {
      try {
        File dir = ctx.getDir("modsplus", Context.MODE_PRIVATE);
        String path = dir.getAbsolutePath();
        File dirsPath = new File(path, "cache/libs");
        File libsDir = new File("/storage/emulated/0/alvinqid/libs");
        if (!libsDir.exists()) {
            libsDir.mkdirs();
            Logger.get().info("Libs Folder Created!");
        } else {
            if(!libsDir.isDirectory()) {
                libsDir.delete();
                libsDir.mkdirs();
            }
        }
        if (!dirsPath.exists()) {
            dirsPath.mkdirs();
            Logger.get().info("Folder Internal Libs");
        } else {
            if(!dirsPath.isDirectory()) {
                dirsPath.delete();
                dirsPath.mkdirs();
            } else {
	        dirsPath.delete();
                dirsPath.mkdirs();
	    }
        }
	      
        File[] jars = libsDir.listFiles();
        if (jars != null) {
            for (File jar : jars) {
                if(!jar.getName().endsWith(".jar")) continue;
                File resf = new File(dirsPath, jar.getName());
	            	copyFile(jar, resf);
	          }  
        }
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
  }
  public static void copyFile(File source, File dest) throws IOException {
    try (InputStream in = new FileInputStream(source);
         OutputStream out = new FileOutputStream(dest)) {

        byte[] buffer = new byte[4096];
        int length;
        while ((length = in.read(buffer)) > 0) {
            out.write(buffer, 0, length);
        }
    }
  }
  public static void loadAllLibs(Context ctx) {
        try {
        File dir = ctx.getDir("modsplus", Context.MODE_PRIVATE);
        String path = dir.getAbsolutePath();
        File libsDir = new File(path, "cache/libs");
        
        File[] jars = libsDir.listFiles();
        if (jars != null) {
            for (File jar : jars) {
                if(!jar.getName().endsWith(".jar")) continue;
                Logger.get().info("Loaded -> " + jar.getName());
                LibsManager.get(ctx).loadLib(jar);
	         	Logger.get().info("Loaded -> " + jar.getName() + " Done!");
            }
        }
      } catch (Exception e) {
        throw new RuntimeException(e);
        }
    }
  }
