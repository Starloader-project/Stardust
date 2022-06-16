package de.geolykt.stardust.particle;

import org.objectweb.asm.tree.ClassNode;

public abstract class ParticleMod {

    public void lateStartup() {
        //
    }

    public abstract void onClassloadTransform(ClassNode node);

    public abstract void onReadTransform(ClassNode node);

}
