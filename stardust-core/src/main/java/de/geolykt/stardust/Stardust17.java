package de.geolykt.stardust;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;

/**
 * Core class for launching Stardust, written using Java 17.
 *
 * @author Emeric Werner
 * @since 0.0.1
 */
public class Stardust17 {

    public static final String STARDUST_DELEGATE_CLASS_KEY = "stardust-delegate-class";
    public static final String STARDUST_DELEGATE_PATHS_KEY = "stardust-delegate-paths";

    private static List<ClassNode> getNodes() throws IOException {
        List<ClassNode> nodes = new ArrayList<>();
        for (String s : System.getProperty("java.class.path").split(File.pathSeparator)) {
            Path path = Path.of(s);
            if (Files.isDirectory(path)) {
                Files.walkFileTree(path, new FileVisitor<Path>() {

                    @Override
                    public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                        try {
                            ClassReader reader = new ClassReader(Files.newInputStream(file));
                            ClassNode node = new ClassNode();
                            reader.accept(node, 0);
                            nodes.add(node);
                        } catch (Exception e) {
                            // Discard
                        }
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                        return FileVisitResult.CONTINUE;
                    }
                });
            } else {
                try (ZipInputStream zipIn = new ZipInputStream(Files.newInputStream(path))) {
                    for (ZipEntry entry = zipIn.getNextEntry(); entry != null; entry = zipIn.getNextEntry()) {
                        if (!entry.getName().endsWith(".class")) {
                            zipIn.closeEntry();
                        } else {
                            ClassReader reader = new ClassReader(zipIn);
                            ClassNode node = new ClassNode();
                            reader.accept(node, 0);
                            nodes.add(node);
                        }
                    }
                } catch (IOException e) {
                    ClassReader reader = new ClassReader(Files.newInputStream(path));
                    ClassNode node = new ClassNode();
                    reader.accept(node, 0);
                    nodes.add(node);
                }
            }
        }
        return nodes;
    }

    /**
     * Main entrypoint.
     *
     * @param args The arguments to pass
     * @throws Exception All the exceptions that could arise during runtime.
     */
    public static void main(String[] args) throws Exception {
        System.setProperty(STARDUST_DELEGATE_CLASS_KEY, System.getProperty(STARDUST_DELEGATE_CLASS_KEY, "com.example.Main"));

        ParticleLoader loader = new ParticleLoader(getNodes());
        loader.discoverParticles(Path.of("mods"));
        loader.loadParticles();
        loader.getClassloader().loadClass(System.getProperty(STARDUST_DELEGATE_CLASS_KEY)).getMethod("main", String[].class).invoke(null, (Object) args);
    }
}
