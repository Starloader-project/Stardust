package de.geolykt.stardust;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;

import de.geolykt.starloader.deobf.ClassWrapperPool;

public class MasterClassLoader extends ClassLoader {

    private final Set<ParticleClassLoader> children = ConcurrentHashMap.newKeySet();

    private final ParticleLoader loader;
    private final Map<String, ClassNode> nodes = new ConcurrentHashMap<>();
    private final ClassWrapperPool wrapperPool = new ClassWrapperPool(nodes, this);

    public MasterClassLoader(ParticleLoader loader, Collection<ClassNode> nodes) {
        super("StardustCore", null);
        this.loader = loader;
        nodes.forEach(node -> this.nodes.put(node.name, node));
    }

    public void addChild(ParticleClassLoader child) {
        children.add(child);
    }

    @Override
    public Class<?> findClass(String name) throws ClassNotFoundException {
        ClassNode node = nodes.get(name.replace('.', '/'));
        if (node != null) {
            byte[] bytes = transform(node);
            return defineClass(name, bytes, 0, bytes.length);
        }

        ClassNotFoundException throwing = new ClassNotFoundException("Unable to find class: " + name);

        for (ParticleClassLoader child : children) {
            try {
                Class<?> cl = child.findClass0(name);
                return cl;
            } catch (ClassNotFoundException e) {
                throwing.addSuppressed(e);
            }
        }
        throw throwing;
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        if (name.startsWith("de.geolykt.stardust") || name.startsWith("org.objectweb.asm")) {
            try {
                Class<?> c = getClass().getClassLoader().loadClass(name);
                if (resolve) {
                    resolveClass(c);
                }
                return c;
            } catch (ClassNotFoundException ignore) {
            }
        }
        return super.loadClass(name, resolve);
    }

    // Increased visibility because of modularisation
    @Override
    public String findLibrary(String libname) {
        return super.findLibrary(libname);
    }

    Collection<ClassNode> getNodes() {
        return nodes.values();
    }

    @Override
    public URL getResource(String name) {
        URL resource = super.getResource(name);
        if (resource != null) {
            return resource;
        }
        for (URLClassLoader cl : children) {
            resource = cl.findResource(name);
            if (resource != null) {
                return resource;
            }
        }
        return getClass().getClassLoader().getResource(name);
    }

    @Override
    public InputStream getResourceAsStream(String name) {
        InputStream resource = super.getResourceAsStream(name);
        if (resource != null) {
            return resource;
        }
        for (URLClassLoader cl : children) {
            URL url = cl.findResource(name);
            if (url == null) {
                continue;
            }
            try {
                return resource = url.openStream();
            } catch (IOException e) {
            }
        }
        return getClass().getClassLoader().getResourceAsStream(name);
    }

    @Override
    public Enumeration<URL> getResources(String name) throws IOException {
        Enumeration<URL> resources = super.getResources(name);
        if (resources != null) {
            return resources;
        }
        for (URLClassLoader cl : children) {
            resources = cl.findResources(name);
            if (resources != null) {
                return resources;
            }
        }
        return getClass().getClassLoader().getResources(name);
    }

    public void removeChild(ParticleClassLoader c) {
        this.children.remove(c);
    }

    private byte[] transform(ClassNode node) {
        loader.getParticles().forEach((part) -> part.onClassloadTransform(node));
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES) {
            @Override
            protected String getCommonSuperClass(String type1, String type2) {
                return wrapperPool.getCommonSuperClass(wrapperPool.get(type1), wrapperPool.get(type2)).getName();
            }
        };
        node.accept(writer);
        return writer.toByteArray();
    }
}
