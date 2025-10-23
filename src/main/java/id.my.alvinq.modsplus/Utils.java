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
        if (f.isDirectory()) for (File c : Objects.requireNonNull(f.listFiles())) deleteFolder(c.getPath());
        f.delete();
        Logger.get().i("Folder " + (f.exists() ? "gagal dihapus" : "berhasil dihapus"));
    }

    public static Object getPathList(@NotNull ClassLoader loader) throws ReflectiveOperationException {
        Field f = Objects.requireNonNull(loader.getClass().getSuperclass()).getDeclaredField("pathList");
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

            Method make = Build.VERSION.SDK_INT >= 25
                    ? cls.getDeclaredMethod("makePathElements", List.class)
                    : cls.getDeclaredMethod("makePathElements", List.class, File.class, List.class);
            make.setAccessible(true);

            Object[] elem = Build.VERSION.SDK_INT >= 25
                    ? (Object[]) make.invoke(list, dirs)
                    : (Object[]) make.invoke(list, dirs, null, new ArrayList<>());

            fElem.set(list, elem);
        } catch (Exception e) {
            throw new ReflectiveOperationException("Inject native libraries gagal", e);
        }
    }

    public static void copyFileFromJar(String jar, String src, File dst) throws IOException {
        try (JarFile j = new JarFile(jar);
             InputStream in = j.getInputStream(j.getJarEntry(src));
             OutputStream out = new FileOutputStream(dst)) {
            in.transferTo(out);
        }
    }

    public static void copyFolderFromJar(String jar, String src, File dst) throws IOException {
        try (JarFile j = new JarFile(jar)) {
            Enumeration<JarEntry> e = j.entries();
            while (e.hasMoreElements()) {
                JarEntry en = e.nextElement();
                if (!en.getName().startsWith(src + "/")) continue;
                File outFile = new File(dst, en.getName().substring(src.length() + 1));
                if (en.isDirectory()) continue;
                outFile.getParentFile().mkdirs();
                try (InputStream in = j.getInputStream(en);
                     OutputStream out = new FileOutputStream(outFile)) {
                    in.transferTo(out);
                }
            }
        }
    }

    public static void copyFile(File src, File dst) throws IOException {
        dst.getParentFile().mkdirs();
        try (InputStream in = new FileInputStream(src);
             OutputStream out = new FileOutputStream(dst)) {
            in.transferTo(out);
        }
    }
}
