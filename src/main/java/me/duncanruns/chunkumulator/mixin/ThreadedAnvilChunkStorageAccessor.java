package me.duncanruns.chunkumulator.mixin;

import net.minecraft.network.Packet;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ThreadedAnvilChunkStorage;
import net.minecraft.world.chunk.WorldChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ThreadedAnvilChunkStorage.class)
public interface ThreadedAnvilChunkStorageAccessor {
    @Invoker("sendChunkDataPackets")
    void invokeSendChunkDataPackets(ServerPlayerEntity player, Packet<?>[] packets, WorldChunk chunk);
}
