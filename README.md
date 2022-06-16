# Stardust

Stardust is a mod loader for galimulator.
It is designed to be usable by the common plebian, at the cost of being
complicated for mod devs.

## Design philosophy

The main philosophy of this application is that it isn't actually meant to load
mods. Instead a "Particle" (just a fancy name for Stardust mods) is used that
loads mods themselves.

As such Stardust has a really sparse modding API that is behaving on a low
level. Mods are also loaded in an arbitrary order due to the extreme mini-
malism present.

## Usage

Unlike 1st generation mod loaders, the 2nd generation (and therefore
Stardust too) exploits the config.json file in order to be loaded by the JVM
and as such does not rely on the CLI. It furthermore downloads a Java 17
JDK in order to allow the usage of the vast quantities of features introduced
in newer versions of java.

In order to run this mod loader, you'll want to edit the config.json
file to something like
```json
{
  "classPath": [
    "jar/galimulator-desktop.jar",
    "stardust-j8-0.0.1-SNAPSHOT.jar"
  ],
  "mainClass": "Stardust8",
  "vmArgs": [
    "-Dsun.java2d.dpiaware=true"
  ]
}
```

Mods/particles are put in the "mods" folder (it may need to be created manually)
that is within the galimulator directory.

## OS Compatibility

As with every other galimulator mod loader that saw the light of day,
OS compatibility is an issue as it was only debugged on a linux system.
However unlike SLL, Stardust will outright not work under MacOS/OSX
unless launched in very finicky ways. Windows compatibility is also
questionable as it was not tested.
