# How do I import it into my project?

## Repositories:
```gradle
repositories {
    mavenCentral()
    maven {
        url = "https://jitpack.io"
        credentials { username authToken }
    }
}
```

## Dependencies:

### Client With Fabric:

```gradle
compileOnly include("com.github.feeldev12.NetworkingMessages:common:588f547bc8")
modImplementation include("com.github.feeldev12.NetworkingMessages:client:acf7dc404f")
```

### Server With SpigotMC/PaperMC or forks:

```gradle
compileOnly "com.github.feeldev12.NetworkingMessages:common:588f547bc8"
implementation("com.github.feeldev12.NetworkingMessages:server:588f547bc8")
```

If the common library does not work for some reason, add this as it is necessary to use the netty libraries.
```gradle
compileOnly("io.netty:netty-all:4.1.68.Final")
```
