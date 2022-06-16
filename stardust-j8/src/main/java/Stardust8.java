import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLConnection;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import de.geolykt.stardust.Stardust17;
import de.geolykt.stardust.j8.CompletionAnnounceInputStream;
import de.geolykt.stardust.j8.SystemOS;

/**
 * Launcher class for Stardust, compiled using Java 8.
 * The only job of this class is to get & launch a more recent version of java.
 *
 * @author Emeric Werner
 * @since 0.0.1
 */
public class Stardust8 {

    private static void moveJRE(Path path) throws IOException {

        long fileCount = Files.list(path).count();
        Optional<Path> subfolder = Files.list(path).findFirst();

        if (fileCount == 1) {
            Files.walkFileTree(subfolder.get(), new FileVisitor<Path>() {

                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                    if (dir.equals(subfolder.get())) {
                        return FileVisitResult.CONTINUE;
                    }
                    Files.createDirectory(path.resolve(subfolder.get().relativize(dir)));
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Files.move(file, path.resolve(subfolder.get().relativize(file)));
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    Files.delete(dir);
                    return FileVisitResult.CONTINUE;
                }
            });
        } else {
            System.out.println("JRE already installed");
        }
    }

    private static void downloadJRE(Path path) {
        String downloadLoc = SystemOS.getCurrentOS().getJDKDownloadPath();
        URI uri = URI.create(downloadLoc);
        System.out.println("Downloading JDK... please wait");
        try {
            URLConnection connection = uri.toURL().openConnection();
            connection.connect();
            long total = connection.getContentLengthLong();
            System.out.println("Download is " + total + " bytes long");
            Path packed = Files.createTempFile("jdkdownload", "tmp");
            Files.copy(new CompletionAnnounceInputStream(connection.getInputStream(), total), packed, StandardCopyOption.REPLACE_EXISTING);
            System.out.println("Actually transfered " + Files.size(packed) + " bytes. Starting to unpack now");

            if (SystemOS.getCurrentOS() == SystemOS.LINUX) {
                // We assume that they have tar installed.
                new ProcessBuilder("tar", "-xvzf", packed.toAbsolutePath().toString(), "-C", path.toAbsolutePath().toString())
                    .inheritIO()
                    .start()
                    .waitFor();
            } else if (SystemOS.getCurrentOS() == SystemOS.WINDOWS) {
                try (ZipInputStream zip = new ZipInputStream(Files.newInputStream(packed))) {
                    for (ZipEntry entry = zip.getNextEntry(); entry != null; entry = zip.getNextEntry()) {
                        if (entry.isDirectory()) {
                            zip.closeEntry();
                            continue;
                        }
                        String pathname = entry.getName();
                        if (pathname.charAt(0) == '/') {
                            pathname = pathname.substring(1);
                        }
                        Files.copy(zip, path.resolve(entry.getName()));
                        zip.closeEntry();
                    }
                }
            } else {
                throw new UnsupportedOperationException("Unsupported os: " + SystemOS.getCurrentOS());
            }

            moveJRE(path);
            System.out.println("Unpacked");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static String getClasspath() {
        String classpath = System.getProperty("java.class.path");

        if (!classpath.isEmpty()) {
            return classpath;
        }

        ClassLoader loader = Stardust8.class.getClassLoader();

        if (loader.getClass() != URLClassLoader.class) {
            throw new IllegalStateException("Unable to get classpath");
        }

        @SuppressWarnings("resource") // Java moment
        URLClassLoader urlClasslaoder = (URLClassLoader) loader;

        StringBuilder path = new StringBuilder();

        for (URL url : urlClasslaoder.getURLs()) {
            if (path.length() != 0) {
                path.append(File.pathSeparator);
            }
            path.append(url.getPath());
        }
        return path.toString();
    }

    /**
     * Entrypoint method for launching Stardust using Java 8.
     *
     * @param args The arguments
     */
    public static void main(String[] args) {
        boolean isJ17 = false;
        try {
            Class.forName("java.util.random.RandomGenerator");
            isJ17 = true;
        } catch (ClassNotFoundException ignore) {
        }
        if (isJ17) {
            try {
                Stardust17.main(args);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return;
        }
        if (SystemOS.getCurrentOS() == SystemOS.MAC) {
            System.err.println("You are running an unsupported system (MacOS/OSX). Consult a developer if you want to know workarounds");
            return;
        }
        if (SystemOS.getCurrentOS() == SystemOS.WINDOWS) {
            System.err.println("Please be aware that the windows integration is only partially supported.");
        }

        Path jre17path = Paths.get("jre17");

        if (!Files.exists(jre17path)) {
            try {
                Files.createDirectory(jre17path);
            } catch (IOException e) {
                e.printStackTrace();
            }
            downloadJRE(jre17path);
        } else {
            try {
                moveJRE(jre17path);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        String javaloc;

        if (SystemOS.getCurrentOS() == SystemOS.WINDOWS) {
            javaloc = jre17path.resolve("bin").resolve("java.exe").toAbsolutePath().toString();
        } else {
            javaloc = jre17path.resolve("bin").resolve("java").toAbsolutePath().toString();
        }

        String[] startArguments = {javaloc, "-cp", getClasspath(), "de.geolykt.stardust.Stardust17"};
        String[] fullArgs = new String[args.length + startArguments.length];

        System.arraycopy(startArguments, 0, fullArgs, 0, startArguments.length);
        System.arraycopy(args, 0, fullArgs, startArguments.length, args.length);

        try {
            if (new ProcessBuilder(fullArgs)
                .inheritIO()
                .start()
                .waitFor() != 0) {
                System.err.println("Exited with non-zero exit code. Used command argument vector: " + Arrays.toString(fullArgs));
            }
        } catch (InterruptedException | IOException e) {
            e.printStackTrace();
            System.err.println("Used command: " + Arrays.toString(fullArgs));
        }
    }
}
