package id.my.alvinq.modsplus;

import org.jetbrains.annotations.NotNull;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.widget.Toast;

import dalvik.system.DexClassLoader;
import android.content.Context;
import java.io.*;
import java.util.*;
import java.lang.reflect.*;
import java.util.jar.JarFile;
import java.util.jar.JarEntry;
import org.json.JSONObject;

public class Utils {

  public Object getPathList(@NotNull ClassLoader loader) throws ReflectiveOperationException {
        Field field = Objects.requireNonNull(loader.getClass().getSuperclass()).getDeclaredField("pathList");
        field.setAccessible(true);
        return field.get(loader);
  }

  public void injectNativeLibraries(String nld, Object pathList) throws ReflectiveOperationException {
        try {
            final File newLibDir = new File(nld);

            Field nativeLibraryDirectoriesField = pathList.getClass().getDeclaredField("nativeLibraryDirectories");
            nativeLibraryDirectoriesField.setAccessible(true);

            Collection<File> currentDirs = (Collection<File>) nativeLibraryDirectoriesField.get(pathList);
            if (currentDirs == null) {
                currentDirs = new ArrayList<>();
            }

            List<File> libDirs = new ArrayList<>(currentDirs);

            Iterator<File> it = libDirs.iterator();
            while (it.hasNext()) {
                File libDir = it.next();
                if (newLibDir.equals(libDir)) {
                    it.remove();
                    break;
                }
            }
            libDirs.add(0, newLibDir);
            nativeLibraryDirectoriesField.set(pathList, libDirs);

            Field nativeLibraryPathElementsField = pathList.getClass().getDeclaredField("nativeLibraryPathElements");
            nativeLibraryPathElementsField.setAccessible(true);

            Object[] elements;

            if (Build.VERSION.SDK_INT >= 25) {
                Method makePathElements = pathList.getClass().getDeclaredMethod("makePathElements", List.class);
                makePathElements.setAccessible(true);

                Field systemNativeLibDirsField = pathList.getClass().getDeclaredField("systemNativeLibraryDirectories");
                systemNativeLibDirsField.setAccessible(true);
                List<File> systemLibDirs = (List<File>) systemNativeLibDirsField.get(pathList);
                if (systemLibDirs != null) {
                    libDirs.addAll(systemLibDirs);
                }

                elements = (Object[]) makePathElements.invoke(pathList, libDirs);
            } else {
                Method makePathElements = pathList.getClass().getDeclaredMethod("makePathElements", List.class, File.class, List.class);
                makePathElements.setAccessible(true);

                Field systemNativeLibDirsField = pathList.getClass().getDeclaredField("systemNativeLibraryDirectories");
                systemNativeLibDirsField.setAccessible(true);
                List<File> systemLibDirs = (List<File>) systemNativeLibDirsField.get(pathList);
                if (systemLibDirs != null) {
                    libDirs.addAll(systemLibDirs);
                }
                ArrayList<Throwable> suppressedExceptions = new ArrayList<>();
                elements = (Object[]) makePathElements.invoke(pathList, libDirs, null, suppressedExceptions);
            }
            nativeLibraryPathElementsField.set(pathList, elements);


        } catch (NoSuchFieldException | NoSuchMethodException e) {
            throw new ReflectiveOperationException("Unable to inject native libraries", e);
        }
  }

  public void copyFileFromJar(String jarFilePath, String sourceFileName, File destFile) throws IOException {
    try (JarFile jarFile = new JarFile(jarFilePath)) {
        JarEntry jarEntry = jarFile.getJarEntry(sourceFileName);
        try (InputStream in = jarFile.getInputStream(jarEntry);
             FileOutputStream out = new FileOutputStream(destFile)) {
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
        }
    }
  }

  public void copyFolderFromJar(String jarFilePath, String sourceFolderName, File destFolder) throws IOException {
    try (JarFile jarFile = new JarFile(jarFilePath)) {
        Enumeration<JarEntry> entries = jarFile.entries();
        while (entries.hasMoreElements()) {
            JarEntry entry = entries.nextElement();
            if (entry.getName().startsWith(sourceFolderName + "/")) {
                String fileName = entry.getName().substring(sourceFolderName.length() + 1);
                File destFile = new File(destFolder, fileName);
                destFile.getParentFile().mkdirs();
                try (InputStream in = jarFile.getInputStream(entry);
                     FileOutputStream out = new FileOutputStream(destFile)) {
                    byte[] buffer = new byte[1024];
                    int bytesRead;
                    while ((bytesRead = in.read(buffer)) != -1) {
                        out.write(buffer, 0, bytesRead);
                    }
                }
            }
        }
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
}
