# NetworkingMessages

Library for typed server↔client communication across multiple Minecraft platforms — Bukkit/Paper plugins and Fabric/Forge/NeoForge mods — with a shared registration model (`TypesManager` + `MessagesManager`) so the same channel/message concepts work regardless of platform.

## Modules

| Module | Platform | Status |
|---|---|---|
| `java-common` | Plain Java | Shared core: `FriendlyByteBuf`, `TypesManager`, exceptions, serialization registry. No Minecraft dependency. |
| `common` | Fabric/Forge/NeoForge shared code | Compiled against vanilla Minecraft only. |
| `bukkit` | Bukkit/Spigot | Full implementation (`ServerAPI`, `MessagesManager` over plugin messaging channels). |
| `paper` | Paper | Currently an empty placeholder — Paper servers should depend on `bukkit` instead, since Paper implements the Bukkit API. |
| `fabric` | Fabric | Full implementation, built on `net.minecraft.network.codec.StreamCodec` + `PayloadTypeRegistry`. |
| `forge` | Forge | Full implementation, built on `StreamCodec` + Forge `Channel`. |
| `neoforge` | NeoForge | Full implementation, built on `StreamCodec` + `PayloadRegistrar`. |

Targets Minecraft 1.21.

## Installation

```groovy
repositories {
    maven { url 'https://jitpack.io' }
}

dependencies {
    // pick the module(s) for the platform(s) you target
    implementation 'com.github.feeldev12.NetworkingMessages:bukkit:1.0.0'
    // implementation 'com.github.feeldev12.NetworkingMessages:fabric:1.0.0'
    // implementation 'com.github.feeldev12.NetworkingMessages:forge:1.0.0'
    // implementation 'com.github.feeldev12.NetworkingMessages:neoforge:1.0.0'
}
```

Each platform module already pulls in `java-common` (and `common` for Minecraft-based loaders) transitively.

## Quick start (Bukkit/Paper)

Bukkit has no access to Minecraft's internal networking classes, so messages here implement `PluginMessageListener` directly and (de)serialize to `byte[]` via `FriendlyByteBuf`, this library's own Netty `ByteBuf` wrapper (varint/UTF/UUID helpers, plus optional gzip compression above a size threshold).

```java
public class HelloMessage extends AbstractMessage<HelloMessage> implements PluginMessageListener {
    private String text;

    public HelloMessage() {}
    public HelloMessage(String text) { this.text = text; }

    @Override
    public byte[] sendMessage(HelloMessage message) {
        FriendlyByteBuf buf = new FriendlyByteBuf();
        buf.writeUtf(message.text);
        return buf.readOnlyNecessaryBytes();
    }

    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] message) {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.wrappedBuffer(message));
        String text = buf.readUtf();
        player.sendMessage("Server says: " + text);
    }
}
```

```java
public class MyPlugin extends JavaPlugin {
    private ServerAPI serverAPI;

    @Override
    public void onEnable() {
        serverAPI = new ServerAPI(this, "myplugin");
        MessageType helloType = serverAPI.getTypesManager().registerMessageType("hello", true);
        serverAPI.getMessagesManager().registerMessage(helloType, new HelloMessage());
    }

    public void greet(Player player) {
        serverAPI.getMessagesManager().sendMessageToClient(player, new HelloMessage("hi from the server!"));
    }
}
```

`registerMessageType(channelId, serverListener)` reserves the channel and packet id; `serverListener = true` means the server also listens for this message coming from the client (registers an incoming channel), not just sends it.

## Quick start (Fabric / Forge / NeoForge)

On Minecraft-loader platforms, messages implement `IModMessage<T>`, which extends `net.minecraft.network.codec.StreamCodec<ByteBuf, T>` directly — you write `encode`/`decode` the same way you would for a vanilla `CustomPacketPayload`, rather than going through `FriendlyByteBuf`. The registered instance acts as both the message's "prototype" (its `encode`/`decode` are the codec) and the id holder — `encode`/`decode` must read/write through their `value`/return value, not `this`.

This message class is loader-agnostic — write it once in your mod's shared code and it works on Fabric, Forge and NeoForge unchanged:

```java
public class HelloMessage extends AbstractMessage<HelloMessage> {
    private String text;

    public HelloMessage(MessageType type) { super(type); }
    public HelloMessage(MessageType type, String text) { super(type); this.text = text; }

    @Override
    public void encode(ByteBuf buf, HelloMessage value) {
        ByteBufCodecs.STRING_UTF8.encode(buf, value.text);
    }

    @Override
    public HelloMessage decode(ByteBuf buf) {
        return new HelloMessage(getMessageType(), ByteBufCodecs.STRING_UTF8.decode(buf));
    }

    @Override
    public void handleOnServer(ServerPlayer sender) {
        System.out.println(sender.getName().getString() + " says: " + text);
    }
}
```

`registerMessage` still goes through `TypesManager`/`MessagesManager` on all three, but each platform's `MessagesManager` wires the message into that loader's native payload registry underneath (`PayloadTypeRegistry` on Fabric, `Channel` on Forge, `PayloadRegistrar` on NeoForge) — you never touch those directly.

### Fabric

```java
public class MyModFabric implements ModInitializer {
    public static ServerAPI serverAPI;
    public static MessageType helloType;

    @Override
    public void onInitialize() {
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            serverAPI = new ServerAPI(server, "mymod");
            helloType = serverAPI.getTypesManager().registerMessageType("hello", true);
            serverAPI.getMessagesManager().registerMessage(helloType, new HelloMessage(helloType));
        });
    }

    public static void greet(ServerPlayer player) {
        serverAPI.getMessagesManager().sendMessageToClient(player, new HelloMessage(helloType, "hi from Fabric!"));
    }
}
```

Client side mirrors it with `new ClientAPI("mymod")` instead of `ServerAPI`, and `clientAPI.getMessagesManager().sendMessageToServer(...)` to talk back (only works if `helloType` was registered with `serverListener = true`).

### Forge

Channel registration happens immediately inside `registerMessage`, so it can run straight from the mod constructor — but a live `MinecraftServer` reference for *sending* only exists once the server starts, so `ServerAPI` is built without one and wired up later:

```java
@Mod("mymod")
public class MyModForge {
    public static ServerAPI serverAPI;

    public MyModForge() {
        serverAPI = new ServerAPI("mymod");
        MessageType helloType = serverAPI.getTypesManager().registerMessageType("hello", true);
        serverAPI.getMessagesManager().registerMessage(helloType, new HelloMessage(helloType));

        MinecraftForge.EVENT_BUS.addListener((ServerStartingEvent event) -> serverAPI.setServer(event.getServer()));
    }
}
```

Configuration-phase payloads don't need any extra wiring here: Forge's `ServerAPI` constructor already hooks `MinecraftForge.EVENT_BUS` itself to register those during login.

### NeoForge

`ServerAPI(String, IEventBus)` registers this instance's payload/config-task handlers on your mod event bus for you:

```java
@Mod("mymod")
public class MyModNeoForge {
    public static ServerAPI serverAPI;

    public MyModNeoForge(IEventBus modEventBus) {
        serverAPI = new ServerAPI("mymod", modEventBus);
        MessageType helloType = serverAPI.getTypesManager().registerMessageType("hello", true);
        serverAPI.getMessagesManager().registerMessage(helloType, new HelloMessage(helloType));
    }
}
```

(The single-arg `ServerAPI(String)` constructor still exists for when you'd rather call `onRegisterPayloads`/`onRegisterConfigTasks` from your own event listeners.)

## License

CC0-1.0.
