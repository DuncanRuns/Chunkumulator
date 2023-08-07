package me.duncanruns.chunkumulator.mixin;

import me.duncanruns.chunkumulator.Chunkumulator;
import net.minecraft.network.Packet;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.server.world.ThreadedAnvilChunkStorage;
import net.minecraft.world.chunk.WorldChunk;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ThreadedAnvilChunkStorage.class)
public abstract class ThreadedAnvilChunkStorageMixin {
    @Shadow
    @Final
    ServerWorld world;

    @Inject(method = "sendChunkDataPackets", at = @At("HEAD"), cancellable = true)
    private void interceptChunkSend(ServerPlayerEntity player, Packet<?>[] packets, WorldChunk chunk, CallbackInfo ci) {
        // packets being a length of 3 indicates that we want to send now, so do not intercept.
        if (packets.length == 3) {
            return;
        }

        // If the server is integrated and the host player has not yet joined (so open to lan isn't even on) or the player is the host player, do not intercept.
        if (!world.getServer().isDedicated()) {
            ServerPlayerEntity hostPlayer = world.getServer().getPlayerManager().getPlayer(((IntegratedServerAccessor) world.getServer()).getLocalPlayerUuid());
            if (hostPlayer == null || player.equals(hostPlayer)) {
                return;
            }
        }

        Chunkumulator.getChunkQueueFromPlayer(player).addChunk(chunk.getPos(), world);
        ci.cancel();
    }
}