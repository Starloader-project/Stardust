package de.geolykt.stardust;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.concurrent.ConcurrentHashMap;

public class ParticleClassLoader extends URLClassLoader {

    private final MasterClassLoader master;
    ParticleMod mod;
    // Are you sure that this makes sense?
    private final ConcurrentHashMap<String, Class<?>> loadedClasses = new ConcurrentHashMap<>();

    public ParticleClassLoader(MasterClassLoader master, String name, URL... urls) {
        super(name, urls, master);
        this.master = master;
        this.master.addChild(this);
    }

    @Override
    public void close() throws IOException {
        this.master.removeChild(this);
        loadedClasses.clear();
        super.close();
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        return master.findClass(name);
    }

    Class<?> findClass0(String name) throws ClassNotFoundException {
        Class<?> c = loadedClasses.get(name);
        if (c != null) {
            return c;
        }
        c = super.findClass(name);
        loadedClasses.put(name, c);
        return c;
    }

    public MasterClassLoader getRootLoader() {
        return master;
    }
}
