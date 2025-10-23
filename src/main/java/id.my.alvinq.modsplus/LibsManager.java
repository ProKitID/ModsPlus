package id.my.alvinq.modsplus;

import android.content.Context;
import android.content.res.AssetManager;
import dalvik.system.DexClassLoader;
import org.json.JSONObject;
import java.io.*;
import java.lang.reflect.Method;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class LibsManager {
    private final Context context;
    private final File cacheDir;
    private static Context ctxv;

    public LibsManager(Context ctx) {
        context = ctx;
        ctxv = ctx;
        File dir = ctx.getDir("modsplus", Context.MODE_PRIVATE);
        cacheDir = new File(dir, "cache");
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
        boolean hasAssets = manifest.optBoolean("assets", false);
        String mainClass = manifest.optString("main", null);
        if (mainClass == null) {
            Logger.get().error("Field 'main' tidak ditemukan di manifest.json pada " + jarFile.getName());
            return;
        }

        try {
            if (hasAssets) extractToApk(jarFile);

            File nativeDir = null;
            if (hasNative) {
                nativeDir = new File(cacheDir, "libs/" + jarFile.getName().replace(".jar", "") + "/native");
                if (!nativeDir.exists() && !nativeDir.mkdirs()) {
                    Logger.get().error("Gagal membuat direktori native: " + nativeDir);
                    return;
                }
                Utils.copyFolderFromJar(jarFile.getAbsolutePath(), "native", nativeDir);
            }

            DexClassLoader dcl = new DexClassLoader(
                    jarFile.getAbsolutePath(),
                    cacheDir.getAbsolutePath(),
                    null,
                    context.getClassLoader()
            );

            invokeMain(dcl, mainClass, nativeDir);
        } catch (Exception e) {
            Logger.get().error("Gagal memuat library: " + e);
        }
    }

    private void invokeMain(DexClassLoader dcl, String className, File nativeDir) {
        try {
            if (nativeDir != null) {
                Object pathList = Utils.getPathList(dcl);
                Utils.injectNativeLibraries(nativeDir.getAbsolutePath(), pathList);
            }
            Class<?> clazz = dcl.loadClass(className);
            clazz.getDeclaredMethod("onLoad", Context.class).invoke(null, context);
            Logger.get().info("Loaded Class -> " + className + " Done!");
        } catch (Exception e) {
            Throwable real = (e instanceof java.lang.reflect.InvocationTargetException) ? e.getCause() : e;
            Logger.get().error("Error saat load class: " + getStackTraceAsString(real));
        }
    }

    private JSONObject readManifest(File jarFile) {
        try (JarFile jar = new JarFile(jarFile)) {
            JarEntry entry = jar.getJarEntry("manifest.json");
            if (entry == null) return null;
            try (InputStream in = jar.getInputStream(entry);
                 BufferedReader reader = new BufferedReader(new InputStreamReader(in))) {
                StringBuilder json = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) json.append(line);
                return new JSONObject(json.toString());
            }
        } catch (Exception e) {
            Logger.get().error("Gagal membaca manifest: " + e);
            return null;
        }
    }

    public static void extractToApk(File jarFile) throws IOException {
        if (!jarFile.exists()) throw new FileNotFoundException("File JAR tidak ditemukan: " + jarFile.getAbsolutePath());

        String baseName = jarFile.getName().replace(".jar", "");
        File apkFile = new File(jarFile.getParentFile(), baseName + ".apk");
        File tempDir = new File(jarFile.getParentFile(), baseName + "_temp");

        if (tempDir.exists()) Utils.deleteFolder(tempDir.getAbsolutePath());
        tempDir.mkdirs();
        File assetsDir = new File(tempDir, "assets");
        assetsDir.mkdirs();

        boolean foundAssets = false;
        try (JarFile jar = new JarFile(jarFile)) {
            Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                if (!entry.getName().startsWith("assets/")) continue;
                foundAssets = true;
                File outFile = new File(tempDir, entry.getName());
                if (entry.isDirectory()) {
                    outFile.mkdirs();
                    continue;
                }
                outFile.getParentFile().mkdirs();
                try (InputStream is = jar.getInputStream(entry);
                     OutputStream os = new FileOutputStream(outFile)) {
                    Utils.copyStream(is, os);
                }
            }
        }

        if (!foundAssets) {
            Logger.get().i("[JarAssetsToApk] Tidak ada folder 'assets/' di dalam " + jarFile.getName());
            Utils.deleteFolder(tempDir.getAbsolutePath());
            return;
        }

        zipFolder(tempDir, apkFile);
        Utils.deleteFolder(tempDir.getAbsolutePath());
        Logger.get().i("[JarAssetsToApk] Berhasil membuat: " + apkFile.getAbsolutePath());
        addAssetOverride(ctxv.getAssets(), apkFile.getAbsolutePath());
    }

    private static void zipFolder(File source, File zipFile) throws IOException {
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipFile))) {
            zipFileRecursive(source, source, zos);
        }
    }

    private static void zipFileRecursive(File rootDir, File source, ZipOutputStream zos) throws IOException {
        File[] files = source.listFiles();
        if (files == null) return;
        for (File file : files) {
            String entryName = rootDir.toPath().relativize(file.toPath()).toString().replace("\\", "/");
            if (file.isDirectory()) {
                if (!entryName.endsWith("/")) entryName += "/";
                zos.putNextEntry(new ZipEntry(entryName));
                zos.closeEntry();
                zipFileRecursive(rootDir, file, zos);
            } else {
                try (InputStream fis = new FileInputStream(file)) {
                    zos.putNextEntry(new ZipEntry(entryName));
                    Utils.copyStream(fis, zos);
                    zos.closeEntry();
                }
            }
        }
    }

    private String getStackTraceAsString(Throwable t) {
        StringWriter sw = new StringWriter();
        t.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }

    public static void addAssetOverride(AssetManager mgr, String packagePath) {
        try {
            Method m = AssetManager.class.getMethod("addAssetPath", String.class);
            m.invoke(mgr, packagePath);
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }
}
