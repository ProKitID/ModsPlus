package id.my.alvinq.modsplus;

import android.os.Build;
import org.jetbrains.annotations.NotNull;
import java.io.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.jar.*;

public class Utils {

    public static void deleteFolder(String path) {
        File f = new File(path);
        if (!f.exists()) return;
        if (f.isDirectory()) {
            File[] files = f.listFiles();
            if (files != null) {
                for (File c : files) deleteFolder(c.getAbsolutePath());
            }
        }
        boolean deleted = f.delete();
        Logger.get().i("Folder " + (deleted ? "berhasil dihapus" : "gagal dihapus") + ": " + f.getAbsolutePath());
    }

    public static Object getPathList(@NotNull ClassLoader loader) throws ReflectiveOperationException {
        Class<?> superCls = loader.getClass().getSuperclass();
        if (superCls == null) throw new ReflectiveOperationException("Superclass dari loader tidak ditemukan");
        Field f = superCls.getDeclaredField("pathList");
        f.setAccessible(true);
        return f.get(loader);
    }

    public static void injectNativeLibraries(String path, Object list) throws ReflectiveOperationException {
        try {
            File dir = new File(path);
            Class<?> cls = list.getClass();

            Field fDirs = cls.getDeclaredField("nativeLibraryDirectories");
            fDirs.setAccessible(true);
            List<File> dirs = new ArrayList<>((Collection<File>) fDirs.get(list));
            dirs.removeIf(dir::equals);
            dirs.add(0, dir);
            fDirs.set(list, dirs);

            Field fSys = cls.getDeclaredField("systemNativeLibraryDirectories");
            fSys.setAccessible(true);
            List<File> sys = (List<File>) fSys.get(list);
            if (sys != null) dirs.addAll(sys);

            Field fElem = cls.getDeclaredField("nativeLibraryPathElements");
            fElem.setAccessible(true);

            Method make;
            Object[] elem;
            if (Build.VERSION.SDK_INT >= 25) {
                make = cls.getDeclaredMethod("makePathElements", List.class);
                make.setAccessible(true);
                elem = (Object[]) make.invoke(list, dirs);
            } else {
                make = cls.getDeclaredMethod("makePathElements", List.class, File.class, List.class);
                make.setAccessible(true);
                elem = (Object[]) make.invoke(list, dirs, null, new ArrayList<>());
            }

            fElem.set(list, elem);
        } catch (Exception e) {
            throw new ReflectiveOperationException("Inject native libraries gagal", e);
        }
    }

    public static void copyFileFromJar(String jar, String src, File dst) throws IOException {
        try (JarFile j = new JarFile(jar)) {
            JarEntry entry = j.getJarEntry(src);
            if (entry == null) return;
            try (InputStream in = j.getInputStream(entry);
                 OutputStream out = new FileOutputStream(dst)) {
                copyStream(in, out);
            }
        }
    }

    public static void copyFolderFromJar(String jar, String src, File dst) throws IOException {
        try (JarFile j = new JarFile(jar)) {
            Enumeration<JarEntry> e = j.entries();
            while (e.hasMoreElements()) {
                JarEntry en = e.nextElement();
                if (!en.getName().startsWith(src + "/")) continue;
                if (en.isDirectory()) continue;
                File outFile = new File(dst, en.getName().substring(src.length() + 1));
                File parent = outFile.getParentFile();
                if (parent != null && !parent.exists()) parent.mkdirs();
                try (InputStream in = j.getInputStream(en);
                     OutputStream out = new FileOutputStream(outFile)) {
                    copyStream(in, out);
                }
            }
        }
    }

    public static void copyFile(File src, File dst) throws IOException {
        if (!dst.getParentFile().exists()) dst.getParentFile().mkdirs();
        try (InputStream in = new FileInputStream(src);
             OutputStream out = new FileOutputStream(dst)) {
            copyStream(in, out);
        }
    }

    public static void copyStream(InputStream in, OutputStream out) throws IOException {
        byte[] buf = new byte[8192];
        int len;
        while ((len = in.read(buf)) != -1) {
            out.write(buf, 0, len);
        }
    }
}
