package org.ashin.chunkClaimPlugin2.data;

import java.util.Objects;

public class ChunkData {
    private final String world;
    private final int x;
    private final int z;
    private String claimName; // name of the claim group this chunk belongs to

    public ChunkData(String world, int x, int z) {
        this.world = world;
        this.x = x;
        this.z = z;
        this.claimName = null;
    }

    public ChunkData(String world, int x, int z, String claimName) {
        this.world = world;
        this.x = x;
        this.z = z;
        this.claimName = claimName;
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

    public String getClaimName() {
        return claimName;
    }

    public void setClaimName(String claimName) {
        this.claimName = claimName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ChunkData)) return false;
        ChunkData that = (ChunkData) o;
        return x == that.x && z == that.z && Objects.equals(world, that.world);
    }

    @Override
    public int hashCode() {
        return Objects.hash(world, x, z);
    }

    @Override
    public String toString() {
        return "World: " + world + ", X: " + x + ", Z: " + z
                + (claimName != null ? ", Name: " + claimName : "");
    }
}