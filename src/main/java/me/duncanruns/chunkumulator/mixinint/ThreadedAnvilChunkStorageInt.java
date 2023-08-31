package me.duncanruns.chunkumulator.mixinint;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.chunk.WorldChunk;

public interface ThreadedAnvilChunkStorageInt {
    void chunkumulator$actuallySendChunkDataPackets(ServerPlayerEntity player, WorldChunk chunk);
}
