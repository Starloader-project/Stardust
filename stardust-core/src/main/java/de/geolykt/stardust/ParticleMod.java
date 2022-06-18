package de.geolykt.stardust;

import java.util.Collection;

import org.objectweb.asm.tree.ClassNode;

public abstract class ParticleMod {

    @SuppressWarnings("unchecked")
    public static <T extends ParticleMod> T getInstance(Class<T> c) {
        if (c.getClassLoader() instanceof ParticleClassLoader loader) {
            return (T) loader.mod;
        } else {
            throw new IllegalArgumentException("Class not loaded through the ParticleClassloader");
        }
    }

    public ParticleMod() {
        if (getClass().getClassLoader() instanceof ParticleClassLoader loader) {
            loader.mod = this;
        } else {
            throw new IllegalStateException("Instances of this class need to be loaded by the particle classloader!");
        }
    }

    public ParticleClassLoader getClassloader() {
        return (ParticleClassLoader) getClass().getClassLoader();
    }

    public void lateStartup() {
        //
    }

    public abstract void onClassloadTransform(ClassNode node);

    /**
     * Provides the particle mod with all nodes that originate from the bootstrap classpath.
     * <b>Changing the names of the classes is not supported. They will be reset.</b>
     *
     * @param nodes A list of nodes to transform
     */
    public abstract void onReadTransform(Collection<ClassNode> nodes);
}
