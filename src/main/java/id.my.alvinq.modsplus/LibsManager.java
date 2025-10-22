package id.my.alvinq.modsplus;

import dalvik.system.DexClassLoader;
import android.content.Context;
import java.io.*;
import java.util.*;
import java.lang.reflect.*;
import java.util.jar.*;
import org.json.JSONObject;

import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.levimc.launcher.core.minecraft.pesdk.utils.AssetOverrideManager;
//import com.mojang.minecraftpe.MainActivity;

public class LibsManager {
    private final Context context;
    private final File cacheDir;
    private static Context ctxv;

    public LibsManager(Context ctx) {
        this.context = ctx;
        ctxv = ctx;
        File dir = ctx.getDir("modsplus", Context.MODE_PRIVATE);
        String path = dir.getAbsolutePath();
        this.cacheDir = new File(path, "cache");
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
            if(hasAssets) {
            String jarName = jarFile.getName();
            String apkName = jarName.substring(0, jarName.length() - 4) + ".apk";
            String pathApk = context.getCacheDir().getAbsolutePath() + "libs/" + apkName;
            extractToApk(jarFile);
            }
            
            File nativeDir = null;
            if (hasNative) {
                nativeDir = new File(cacheDir, "libs/" + jarFile.getName().replace(".jar", "") + "/native");
                if (!nativeDir.exists() && !nativeDir.mkdirs()) {
                    Logger.get().error("Gagal membuat direktori native: " + nativeDir);
                    return;
                }
                copyFolderFromJar(jarFile.getAbsolutePath(), "native", nativeDir);
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

    private void invokeMain(DexClassLoader dcl, String className, File nd) {
        //Thread.currentThread().setContextClassLoader(dcl);
        try {
            if(nd != null) {
            String nld = nd.getAbsolutePath();
            Object pathList = Utils.getPathList(dcl);
            Utils.injectNativeLibraries(nld, pathList);
            }
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

    public static void extractToApk(File jarFile) throws IOException {
        if (!jarFile.exists()) {
            throw new FileNotFoundException("File JAR tidak ditemukan: " + jarFile.getAbsolutePath());
        }

        String baseName = jarFile.getName();
        if (baseName.endsWith(".jar")) {
            baseName = baseName.substring(0, baseName.length() - 4);
        }

        File apkFile = new File(jarFile.getParentFile(), baseName + ".apk");
        File tempDir = new File(jarFile.getParentFile(), baseName + "_temp");

        if (tempDir.exists()) deleteRecursively(tempDir);
        tempDir.mkdirs();

        File assetsDir = new File(tempDir, "assets");
        assetsDir.mkdirs();

        boolean foundAssets = false;

        try (JarFile jar = new JarFile(jarFile)) {
            Enumeration<JarEntry> entries = jar.entries();

            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                String entryName = entry.getName();

                // Hanya ekstrak "assets/"
                if (entryName.startsWith("assets/")) {
                    foundAssets = true;
                    File outFile = new File(tempDir, entryName);

                    if (entry.isDirectory()) {
                        outFile.mkdirs();
                        continue;
                    }

                    File parent = outFile.getParentFile();
                    if (!parent.exists()) parent.mkdirs();

                    try (InputStream is = jar.getInputStream(entry);
                         OutputStream os = new FileOutputStream(outFile)) {
                        byte[] buffer = new byte[4096];
                        int len;
                        while ((len = is.read(buffer)) != -1) {
                            os.write(buffer, 0, len);
                        }
                    }
                }
            }
        }

        if (!foundAssets) {
            Logger.get().i("[JarAssetsToApk] Tidak ada folder 'assets/' di dalam " + jarFile.getName());
            deleteRecursively(tempDir);
            return;
        }

        // Kompres jadi APK (zip)
        zipFolder(tempDir, apkFile);

        // Bersihkan folder sementara
        deleteRecursively(tempDir);

        Logger.get().i("[JarAssetsToApk] Berhasil membuat: " + apkFile.getAbsolutePath());
        AssetOverrideManager.addAssetOverride(ctxv.getAssets(), apkFile.getAbsolutePath());
    }

    /** Membuat ZIP dari folder */
    private static void zipFolder(File sourceFolder, File zipFile) throws IOException {
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipFile))) {
            zipFileRecursive(sourceFolder, sourceFolder, zos);
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
                try (FileInputStream fis = new FileInputStream(file)) {
                    zos.putNextEntry(new ZipEntry(entryName));
                    byte[] buffer = new byte[4096];
                    int len;
                    while ((len = fis.read(buffer)) != -1) {
                        zos.write(buffer, 0, len);
                    }
                    zos.closeEntry();
                }
            }
        }
    }

    /** Hapus folder/file rekursif */
    private static void deleteRecursively(File file) {
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (files != null) {
                for (File f : files) deleteRecursively(f);
            }
        }
        file.delete();
    }
}
