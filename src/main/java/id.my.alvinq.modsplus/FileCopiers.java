package id.my.alvinq.modsplus;

import java.io.*;
import java.nio.file.*;

public class FileCopiers {

    /**
     * Menyalin satu file dari sumber ke tujuan.
     * Jika file tujuan sudah ada, maka akan ditimpa.
     *
     * @param sourceFile  file sumber
     * @param targetFile  file tujuan
     * @throws IOException jika terjadi kesalahan IO
     */
    public static void copyFile(File sourceFile, File targetFile) throws IOException {
        if (!sourceFile.exists()) {
            throw new FileNotFoundException("File sumber tidak ditemukan: " + sourceFile.getAbsolutePath());
        }

        // Pastikan folder tujuan ada
        File parentDir = targetFile.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            parentDir.mkdirs();
        }

        // Gunakan NIO untuk efisiensi
        Files.copy(sourceFile.toPath(), targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
    }

    /**
     * Menyalin seluruh folder secara rekursif (termasuk semua subfolder dan file).
     *
     * @param sourceDir folder sumber
     * @param targetDir folder tujuan
     * @throws IOException jika terjadi kesalahan IO
     */
    public static void copyFolder(File sourceDir, File targetDir) throws IOException {
        if (!sourceDir.exists()) {
            throw new FileNotFoundException("Folder sumber tidak ditemukan: " + sourceDir.getAbsolutePath());
        }
        if (!sourceDir.isDirectory()) {
            throw new IllegalArgumentException("Path sumber bukan folder: " + sourceDir.getAbsolutePath());
        }

        if (!targetDir.exists()) {
            targetDir.mkdirs();
        }

        File[] files = sourceDir.listFiles();
        if (files == null) return;

        for (File file : files) {
            File targetFile = new File(targetDir, file.getName());
            if (file.isDirectory()) {
                copyFolder(file, targetFile);
            } else {
                copyFile(file, targetFile);
            }
        }
    }
}
