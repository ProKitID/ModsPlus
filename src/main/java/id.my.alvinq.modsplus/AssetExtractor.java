package id.my.alvinq.modsplus;

import java.io.*;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
import java.lang.reflect.Method;
import java.util.List;

public class AssetExtractor {
    private static Method addAssetPathMethod;

    private static void fun1(File jarFile) {
        if (jarFile == null || !jarFile.exists()) {
            throw new FileNotFoundException("File JAR tidak ditemukan: " + jarFile);
        }

        // nama output: namafile.apk
        String jarName = jarFile.getName();
        if (!jarName.endsWith(".jar")) {
            throw new IllegalArgumentException("File harus berekstensi .jar");
        }
        String apkName = jarName.substring(0, jarName.length() - 4) + ".apk";
        File apkDir = new File(jarFile.getParentFile(), apkName);

        // hapus folder lama jika ada (agar hanya berisi assets)
        if (apkDir.exists()) {
            deleteRecursive(apkDir);
        }
        apkDir.mkdirs();

        boolean foundAssets = false;
        File assetsDir = new File(apkDir, "assets");
        assetsDir.mkdirs();

        try (JarFile jar = new JarFile(jarFile)) {
            Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                String name = entry.getName();

                if (!name.startsWith("assets/")) continue;
                if (entry.isDirectory()) continue;
                foundAssets = true;

                File outFile = new File(apkDir, name);
                File parent = outFile.getParentFile();
                if (!parent.exists() && !parent.mkdirs()) {
                    throw new IOException("Gagal membuat folder: " + parent);
                }

                try (InputStream in = jar.getInputStream(entry);
                     OutputStream out = new FileOutputStream(outFile)) {
                    byte[] buffer = new byte[8192];
                    int len;
                    while ((len = in.read(buffer)) != -1) {
                        out.write(buffer, 0, len);
                    }
                }
            }
        }

        if (!foundAssets) {
            // tidak ada folder assets di dalam jar, hapus folder apk
            deleteRecursive(apkDir);
            throw new IOException("Tidak ditemukan folder 'assets/' di dalam: " + jarFile.getName());
        }

        Logger.get().i("✅ Assets berhasil diekstrak ke: " + apkDir.getAbsolutePath());
    }

    private static void deleteRecursive(File file) {
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                for (File f : children) deleteRecursive(f);
            }
        }
        file.delete();
    }

    private static void fun2(Context context, String path) throws Exception {
        if (path == null || path.isEmpty()) return;

        AssetManager am = context.getAssets();

        if (addAssetPathMethod == null) {
            addAssetPathMethod = AssetManager.class.getDeclaredMethod("addAssetPath", String.class);
            addAssetPathMethod.setAccessible(true);
        }

        int cookie = (int) addAssetPathMethod.invoke(am, path);
        if (cookie == 0) {
            throw new IllegalStateException("Gagal menambahkan asset path: " + path);
        }
                    }

    public static void extract(File jf) {
      try {
fun1(jf);
          } catch (Exception e) {
        Throwable real = e instanceof InvocationTargetException ? ((InvocationTargetException) e).getCause() : e;
        String errorLog = getStackTraceAsString(real);
        //System.err.println(errorLog); // atau kirim ke LeviLogger
        Logger.get().error("Error!: " + errorLog);
                              }
    }

    public static void addAssetPath(Context context, String path) {
      try {
fun2(context,path);
          } catch (Exception e) {
        Throwable real = e instanceof InvocationTargetException ? ((InvocationTargetException) e).getCause() : e;
        String errorLog = getStackTraceAsString(real);
        //System.err.println(errorLog); // atau kirim ke LeviLogger
        Logger.get().error("Error!: " + errorLog);
                              }
      }

  public static String getStackTraceAsString(Throwable th) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        th.printStackTrace(pw);
        return sw.toString();
  }

  
}
