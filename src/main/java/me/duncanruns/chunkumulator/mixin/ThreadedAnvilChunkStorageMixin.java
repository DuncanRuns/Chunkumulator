package me.duncanruns.chunkumulator.mixin;

import me.duncanruns.chunkumulator.Chunkumulator;
import me.duncanruns.chunkumulator.mixinint.ThreadedAnvilChunkStorageInt;
import net.minecraft.network.packet.s2c.play.ChunkDataS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.server.world.ThreadedAnvilChunkStorage;
import net.minecraft.world.chunk.WorldChunk;
import org.apache.commons.lang3.mutable.MutableObject;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ThreadedAnvilChunkStorage.class)
public abstract class ThreadedAnvilChunkStorageMixin implements ThreadedAnvilChunkStorageInt {
    @Shadow
    @Final
    ServerWorld world;
    @Unique
    private ServerPlayerEntity hostPlayer = null;

    @Shadow
    protected abstract void sendChunkDataPackets(ServerPlayerEntity player, MutableObject<ChunkDataS2CPacket> cachedDataPacket, WorldChunk chunk);

    // method_17243 is a lambda expression
    @Redirect(method = "method_17243", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/world/ThreadedAnvilChunkStorage;sendChunkDataPackets(Lnet/minecraft/server/network/ServerPlayerEntity;Lorg/apache/commons/lang3/mutable/MutableObject;Lnet/minecraft/world/chunk/WorldChunk;)V"))
    private void redirectChunkSend1(ThreadedAnvilChunkStorage instance, ServerPlayerEntity player, MutableObject<ChunkDataS2CPacket> cachedDataPacket, WorldChunk chunk) {
        redirectChunkSend(player, cachedDataPacket, chunk);
    }

    @Redirect(method = "sendWatchPackets", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/world/ThreadedAnvilChunkStorage;sendChunkDataPackets(Lnet/minecraft/server/network/ServerPlayerEntity;Lorg/apache/commons/lang3/mutable/MutableObject;Lnet/minecraft/world/chunk/WorldChunk;)V"))
    private void redirectChunkSend2(ThreadedAnvilChunkStorage instance, ServerPlayerEntity player, MutableObject<ChunkDataS2CPacket> cachedDataPacket, WorldChunk chunk) {
        redirectChunkSend(player, cachedDataPacket, chunk);
    }

    @Unique
    private void redirectChunkSend(ServerPlayerEntity player, MutableObject<ChunkDataS2CPacket> cachedDataPacket, WorldChunk chunk) {
        // assert packets[0] == null;
        if (!world.getServer().isDedicated()) {
            if (hostPlayer == null) {
                if ((hostPlayer = world.getServer().getPlayerManager().getPlayer(((IntegratedServerAccessor) world.getServer()).getLocalPlayerUuid())) == null) {
                    sendChunkDataPackets(player, cachedDataPacket, chunk);
                    return;
                }
            }
            if (player.equals(hostPlayer)) {
                sendChunkDataPackets(player, cachedDataPacket, chunk);
                return;
            }
        }

        Chunkumulator.getChunkQueueFromPlayer(player).addChunk(chunk.getPos(), world);
    }

    @Override
    public void chunkumulator$actuallySendChunkDataPackets(ServerPlayerEntity player, WorldChunk chunk) {
        sendChunkDataPackets(player, new MutableObject<>(), chunk);
    }
}