package de.geolykt.stardust.cl;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;

public class ParticleClassLoader extends URLClassLoader {

    private final MasterClassLoader master;

    public ParticleClassLoader(MasterClassLoader master, String name, URL... urls) {
        super(name, urls, master);
        this.master = master;
        this.master.addChild(this);
    }

    @Override
    public void close() throws IOException {
        this.master.removeChild(this);
        super.close();
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        return master.findClass(name);
    }

    Class<?> findClass0(String name) throws ClassNotFoundException {
        return super.findClass(name);
    }
}
