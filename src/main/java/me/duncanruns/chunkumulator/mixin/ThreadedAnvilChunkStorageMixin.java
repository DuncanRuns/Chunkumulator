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
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ThreadedAnvilChunkStorage.class)
public abstract class ThreadedAnvilChunkStorageMixin implements ThreadedAnvilChunkStorageInt {
    @Shadow
    @Final
    ServerWorld world;

    @Unique
    private ServerPlayerEntity hostPlayer = null;

    @Unique
    private final MutableObject<ChunkDataS2CPacket> actuallySendSignal = new MutableObject<>();

    @Shadow
    protected abstract void sendChunkDataPackets(ServerPlayerEntity player, MutableObject<ChunkDataS2CPacket> cachedDataPacket, WorldChunk chunk);

    @Inject(method = "sendChunkDataPackets", at = @At("HEAD"), cancellable = true)
    private void interceptChunkSend(ServerPlayerEntity player, MutableObject<ChunkDataS2CPacket> cachedDataPacket, WorldChunk chunk, CallbackInfo ci) {
        // cachedDataPacket == ACTUALLY_SEND_SIGNAL means go ahead with it
        if (cachedDataPacket == actuallySendSignal) {
            return;
        }

        // If the server is integrated and the host player has not yet joined (so open to lan isn't even on) or the player is the host player, do not intercept.
        if (!world.getServer().isDedicated()) {
            if (hostPlayer == null) {
                if ((hostPlayer = world.getServer().getPlayerManager().getPlayer(((IntegratedServerAccessor) world.getServer()).getLocalPlayerUuid())) == null) {
                    return;
                }
            }
            if (player.equals(hostPlayer)) {
                return;
            }
        }

        Chunkumulator.getChunkQueueFromPlayer(player).addChunk(chunk.getPos(), world);
        ci.cancel();
    }

    @Override
    public void chunkumulator$actuallySendChunkDataPackets(ServerPlayerEntity player, WorldChunk chunk) {
        actuallySendSignal.setValue(null);
        sendChunkDataPackets(player, actuallySendSignal, chunk);
    }
}