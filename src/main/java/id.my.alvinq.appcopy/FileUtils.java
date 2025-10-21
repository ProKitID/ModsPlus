package id.my.alvinq.appcopy;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import android.widget.Toast;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;

public class FileUtils {
    public static String getStackTraceAsString(Throwable th) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        th.printStackTrace(pw);
        return sw.toString();
    }
    public static void copyFile(String from, String to) {
        try {
          Files.copy(Paths.get(from), Paths.get(to), StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception e) {
            Throwable real = (e instanceof InvocationTargetException) ? ((InvocationTargetException) e).getCause() : e;
            String errorLog = getStackTraceAsString(real);
        }
    }

    public static void copyFolder(String source, String target) {
        try {
        final Path from = Paths.get(source);
        final Path to = Paths.get(target);

        Files.walkFileTree(from, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                Path targetDir = to.resolve(from.relativize(dir));
                if (!Files.exists(targetDir)) {
                    Files.createDirectories(targetDir);
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.copy(file, to.resolve(from.relativize(file)), StandardCopyOption.REPLACE_EXISTING);
                return FileVisitResult.CONTINUE;
            }
        });
    } catch (Exception e) {
            Throwable real = (e instanceof InvocationTargetException) ? ((InvocationTargetException) e).getCause() : e;
            String errorLog = getStackTraceAsString(real);
       }
    }
}
