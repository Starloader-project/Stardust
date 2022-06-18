package de.geolykt.stardust;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.objectweb.asm.tree.ClassNode;

public class ParticleLoader {

    private final MasterClassLoader cl;

    private final Set<ParticleDescriptor> descriptors = new HashSet<>();

    private final List<ParticleMod> particles = new ArrayList<>();

    public ParticleLoader(List<ClassNode> masterClassnodes) {
        cl = new MasterClassLoader(this, masterClassnodes);
    }

    public void discoverParticles(Path modDirectory) throws IOException {
        Files.list(modDirectory)
            .filter(this::isJar)
            .map(this::getDescriptor)
            .filter(Objects::nonNull)
            .forEach(descriptors::add);
    }

    public MasterClassLoader getClassloader() {
        return cl;
    }

    private ParticleDescriptor getDescriptor(Path path) {
        Properties particleProperties = null;
        try (ZipInputStream zipIn = new ZipInputStream(Files.newInputStream(path))) {
            for (ZipEntry entry = zipIn.getNextEntry(); entry != null; entry = zipIn.getNextEntry()) {
                if (!entry.getName().equals("particle.properties")) {
                    zipIn.closeEntry();
                    continue;
                }
                particleProperties = new Properties();
                particleProperties.load(zipIn);
                break;
            }
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

        if (particleProperties == null) {
            return null;
        }

        String entrypoint = particleProperties.getProperty("entrypoint");
        String name = particleProperties.getProperty("name");
        String version = particleProperties.getProperty("version");

        if (name == null) {
            System.out.println("\u001b[31mParticle Mod at location " + path.toAbsolutePath().toString() + " does not have a name set");
            System.out.println("\u001b[0m");
            return null;
        }
        if (entrypoint == null) {
            System.out.println("\u001b[31mParticle Mod \"" + name + "\" at location " + path.toAbsolutePath().toString() + " does not have the entrypoint set");
            System.out.println("\u001b[0m");
            return null;
        }
        if (version == null) {
            version = "0.0.1";
        }
        return new ParticleDescriptor(path, name, version, entrypoint);
    }

    private ParticleDescriptor getNewest(List<ParticleDescriptor> descriptor) {
        List<ParticleDescriptor> sorted = new ArrayList<>(descriptor);
        sorted.sort((c1, c2) -> {
            String c1Ver = c1.version();
            String c2Ver = c2.version();
            try {
                return Integer.compare(Integer.parseInt(c1Ver), Integer.parseInt(c2Ver));
            } catch (NumberFormatException nfe) {
                try {
                    return Runtime.Version.parse(c1Ver).compareTo(Runtime.Version.parse(c2Ver));
                } catch (Exception e) {
                    return c1Ver.compareTo(c2Ver);
                }
            }
        });
        return sorted.get(sorted.size() - 1);
    }

    public List<ParticleMod> getParticles() {
        return Collections.unmodifiableList(particles);
    }

    private boolean isJar(Path jar) {
        return jar.getFileName().toString().endsWith(".jar") && !jar.getFileName().toString().startsWith(".");
    }

    private void loadIntoMemory(ParticleDescriptor desc) {
        try {
            ParticleClassLoader particleCl = new ParticleClassLoader(getClassloader(), desc.name(), desc.path().toUri().toURL());
            @SuppressWarnings("unchecked")
            Class<? extends ParticleMod> entrypoint = (Class<? extends ParticleMod>) particleCl.loadClass(desc.entrypoint());
            particles.add(entrypoint.getConstructor().newInstance());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void loadParticles() {
        Map<String, List<ParticleDescriptor>> name2Desc = new HashMap<>();

        for (ParticleDescriptor desc : descriptors) {
            name2Desc.compute(desc.name(), (name, list) -> {
                if (list == null) {
                    list = new ArrayList<>();
                }
                list.add(desc);
                return list;
            });
        }

        Set<ParticleDescriptor> loadingMods = new HashSet<>();

        name2Desc.forEach((name, candidates) -> {
            if (candidates.size() > 1) {
                System.out.println("There are multiple particle mods that have the name " + name + ". Only one will get loaded!");
                loadingMods.add(getNewest(candidates));
            } else {
                loadingMods.add(candidates.get(0));
            }
        });

        for (ParticleDescriptor desc : loadingMods) {
            loadIntoMemory(desc);
        }

        Map<ClassNode, String> originNames = new HashMap<>();
        getClassloader().getNodes().forEach(node -> originNames.put(node, node.name));
        for (ParticleMod mod : particles) {
            mod.onReadTransform(Collections.unmodifiableCollection(getClassloader().getNodes()));
            originNames.forEach((node, name) -> node.name = name);
        }
        particles.forEach(ParticleMod::lateStartup);
        originNames.forEach((node, name) -> node.name = name);
    }
}
