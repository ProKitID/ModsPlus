package id.my.alvinq.modsplus;

import dalvik.system.DexClassLoader;
import android.content.Context;
import java.io.*;
import java.util.*;
import java.lang.reflect.*;
import java.util.jar.*;
import org.json.JSONObject;

public class LibsManager {
    private final Context context;
    private final File cacheDir;

    public LibsManager(Context ctx) {
        this.context = ctx;
        this.cacheDir = new File(ctx.getCacheDir(), "dexout");
        if (!cacheDir.exists()) cacheDir.mkdirs();
    }

    public static LibsManager get(Context ctx) {
        return new LibsManager(ctx);
    }

    public void loadLib(File jarFile) {
        JSONObject manifest = readManifest(jarFile);
        if (manifest == null) {
            Logger.get().error("manifest.json tidak ditemukan di " + jarFile.getName());
            return;
        }

        boolean hasNative = manifest.optBoolean("native", false);
        String mainClass = manifest.optString("main", null);

        if (mainClass == null) {
            Logger.get().error("Field 'main' tidak ditemukan di manifest.json pada " + jarFile.getName());
            return;
        }

        try {
            File nativeDir = null;
            if (hasNative) {
                nativeDir = new File(cacheDir, jarFile.getName().replace(".jar", "") + "/native");
                if (!nativeDir.exists() && !nativeDir.mkdirs()) {
                    Logger.get().error("Gagal membuat direktori native: " + nativeDir);
                    return;
                }
                copyFolderFromJar(jarFile.getAbsolutePath(), "native", nativeDir);
            }

            DexClassLoader dcl = new DexClassLoader(
                jarFile.getAbsolutePath(),
                cacheDir.getAbsolutePath(),
                nativeDir != null ? nativeDir.getAbsolutePath() : null,
                context.getClassLoader()
            );

            invokeMain(dcl, mainClass);

        } catch (Exception e) {
            Logger.get().error("Gagal memuat library: " + e);
        }
    }

    private void invokeMain(DexClassLoader dcl, String className) {
        Thread.currentThread().setContextClassLoader(dcl);
        try {
            Class<?> clazz = dcl.loadClass(className);
            Method onLoad = clazz.getDeclaredMethod("onLoad", Context.class);
            onLoad.invoke(null, context);
            Logger.get().info("Loaded Class -> " + className + " Done!");
        } catch (Exception e) {
            Throwable real = e instanceof InvocationTargetException ? e.getCause() : e;
            Logger.get().error("Error saat load class: " + getStackTraceAsString(real));
        }
    }

    private JSONObject readManifest(File jarFile) {
        try (JarFile jar = new JarFile(jarFile)) {
            JarEntry entry = jar.getJarEntry("manifest.json");
            if (entry == null) return null;

            try (InputStream in = jar.getInputStream(entry);
                 BufferedReader reader = new BufferedReader(new InputStreamReader(in))) {

                StringBuilder jsonBuilder = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) jsonBuilder.append(line);
                return new JSONObject(jsonBuilder.toString());
            }
        } catch (Exception e) {
            Logger.get().error("Gagal membaca manifest: " + e);
            return null;
        }
    }

    private void copyFolderFromJar(String jarPath, String folderName, File destDir) throws IOException {
        try (JarFile jar = new JarFile(jarPath)) {
            Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                if (!entry.getName().startsWith(folderName + "/") || entry.isDirectory()) continue;

                String relativePath = entry.getName().substring(folderName.length() + 1);
                File destFile = new File(destDir, relativePath);
                destFile.getParentFile().mkdirs();

                try (InputStream in = jar.getInputStream(entry);
                     FileOutputStream out = new FileOutputStream(destFile)) {
                    copyStream(in, out);
                }
            }
        }
    }

    private void copyStream(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[4096];
        int bytesRead;
        while ((bytesRead = in.read(buffer)) != -1) out.write(buffer, 0, bytesRead);
    }

    private String getStackTraceAsString(Throwable t) {
        StringWriter sw = new StringWriter();
        t.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }
}
