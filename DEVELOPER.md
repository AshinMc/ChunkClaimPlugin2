# 💻 ChunkClaimPlugin2 (CCP) — Developer API Guide

Welcome to the developer documentation for **ChunkClaimPlugin2**. This guide covers adding CCP as a dependency to your plugin, interacting with the `ChunkClaimAPI` singleton, and listening to custom cancellable Bukkit events.

---

## 📋 Table of Contents
1. [Adding CCP as a Dependency](#1-adding-ccp-as-a-dependency)
2. [Accessing `ChunkClaimAPI`](#2-accessing-chunkclaimapi)
3. [Custom Bukkit Events](#3-custom-bukkit-events)
   - [`ChunkClaimEvent`](#chunkclaimevent)
   - [`ChunkUnclaimEvent`](#chunkunclaimevent)
   - [`ChunkRenameEvent`](#chunkrenameevent)
   - [`ChunkTransferEvent`](#chunktransferevent)
4. [Code Examples](#4-code-examples)

---

## 1. Adding CCP as a Dependency

Add `CCP` to your `plugin.yml` as a `depend` or `softdepend`:

```yaml
name: MyPlugin
version: 1.0.0
main: com.example.myplugin.MyPlugin
softdepend: [CCP]
```

### Maven (`pom.xml`)
If adding as a local system dependency or JAR reference:
```xml
<dependency>
    <groupId>org.ashin</groupId>
    <artifactId>ChunkClaimPlugin</artifactId>
    <version>0.7.0</version>
    <scope>provided</scope>
</dependency>
```

### Gradle (`build.gradle`)
```groovy
dependencies {
    compileOnly files('libs/ChunkClaimPlugin.jar')
}
```

---

## 2. Accessing `ChunkManager` & `ChunkClaimAPI`

You can access chunk data and operations either directly via `ChunkClaimPlugin2.getInstance().getChunkManager()` or via the `ChunkClaimAPI` singleton.

### Option A: Via Main Plugin Instance
```java
import org.ashin.chunkClaimPlugin2.ChunkClaimPlugin2;
import org.ashin.chunkClaimPlugin2.managers.ChunkManager;

// Grab the manager directly from the main plugin class instance
ChunkManager chunkManager = ChunkClaimPlugin2.getInstance().getChunkManager();

// Check if a chunk is claimed
boolean isClaimed = chunkManager.isChunkClaimed(chunk);
```

### Option B: Via `ChunkClaimAPI` Singleton
```java
import org.ashin.chunkClaimPlugin2.api.ChunkClaimAPI;
import org.bukkit.Chunk;
import org.bukkit.entity.Player;
import java.util.UUID;

ChunkClaimAPI api = ChunkClaimAPI.getInstance();

// Check if a chunk is claimed
boolean isClaimed = api.isChunkClaimed(chunk);

// Get chunk owner UUID (returns null if unclaimed)
UUID ownerUUID = api.getChunkOwner(chunk);

// Get chunk claim group name (returns null if unclaimed)
String claimName = api.getChunkClaimName(chunk);

// Check if a player can claim a chunk (evaluates ownership and WorldGuard regions)
boolean canClaim = api.canClaimChunk(chunk, player);

// Get player claim stats
int claimCount = api.getPlayerChunkCount(playerUUID);

// Check if a player is trusted in a claim group
boolean trusted = api.isTrusted(ownerUUID, "Base", friendUUID);

// Get a claim flag setting (e.g. "pvp", "explosions", "fire-spread")
boolean pvpBlocked = api.getClaimFlag(ownerUUID, "Base", "pvp");
```

---

## 3. Custom Bukkit Events

All CCP events extend `org.bukkit.event.Event` and implement `org.bukkit.event.Cancellable`.

### `ChunkClaimEvent`
Fired when a player claims a chunk (either via command or right-click item).
```java
import org.ashin.chunkClaimPlugin2.api.events.ChunkClaimEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class MyClaimListener implements Listener {
    @EventHandler
    public void onChunkClaim(ChunkClaimEvent event) {
        Player player = event.getPlayer();
        Chunk chunk = event.getChunk();
        String claimName = event.getClaimName();

        // Prevent claiming in Nether
        if (chunk.getWorld().getEnvironment() == World.Environment.NETHER) {
            event.setCancelled(true);
            player.sendMessage("Claiming in the Nether is disabled!");
        }
    }
}
```

### `ChunkUnclaimEvent`
Fired when a chunk or claim group is unclaimed.
```java
import org.ashin.chunkClaimPlugin2.api.events.ChunkUnclaimEvent;

@EventHandler
public void onChunkUnclaim(ChunkUnclaimEvent event) {
    UUID ownerId = event.getOwnerId();
    String claimName = event.getClaimName();
    Chunk chunk = event.getChunk(); // null if unclaiming an entire group by name

    plugin.getLogger().info("Owner " + ownerId + " unclaimed " + claimName);
}
```

### `ChunkRenameEvent`
Fired when a claim group is renamed.
```java
import org.ashin.chunkClaimPlugin2.api.events.ChunkRenameEvent;

@EventHandler
public void onChunkRename(ChunkRenameEvent event) {
    String oldName = event.getOldName();
    String newName = event.getNewName();

    // Enforce name formatting
    if (newName.contains("Illegal")) {
        event.setCancelled(true);
    }
}
```

### `ChunkTransferEvent`
Fired when claim group ownership is transferred to another player.
```java
import org.ashin.chunkClaimPlugin2.api.events.ChunkTransferEvent;

@EventHandler
public void onChunkTransfer(ChunkTransferEvent event) {
    UUID oldOwner = event.getOldOwnerId();
    UUID newOwner = event.getNewOwnerId();
    String claimName = event.getClaimName();
}
```

---

## 4. Code Examples

### Prevent PVP outside claimed land
```java
@EventHandler
public void onEntityDamage(EntityDamageByEntityEvent event) {
    if (event.getEntity() instanceof Player && event.getDamager() instanceof Player) {
        Chunk chunk = event.getEntity().getLocation().getChunk();
        if (!ChunkClaimAPI.getInstance().isChunkClaimed(chunk)) {
            event.setCancelled(true);
            event.getDamager().sendMessage("PvP is only allowed inside claimed arenas!");
        }
    }
}
```
