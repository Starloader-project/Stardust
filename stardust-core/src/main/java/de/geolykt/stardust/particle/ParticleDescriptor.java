package de.geolykt.stardust.particle;

import java.nio.file.Path;

public record ParticleDescriptor(Path path, String name, String version, String entrypoint) {
}
