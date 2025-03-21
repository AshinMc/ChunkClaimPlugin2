package org.ashin.chunkClaimPlugin2.data;

public class ChunkData {
    private final String world;
    private final int x;
    private final int z;

    public ChunkData(String world, int x, int z) {
        this.world = world;
        this.x = x;
        this.z = z;
    }

    public String getWorld() {
        return world;
    }

    public int getX() {
        return x;
    }

    public int getZ() {
        return z;
    }

    @Override
    public String toString() {
        return "World: " + world + ", X: " + x + ", Z: " + z;
    }
}