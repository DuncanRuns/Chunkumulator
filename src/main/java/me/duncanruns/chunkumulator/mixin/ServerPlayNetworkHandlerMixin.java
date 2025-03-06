package me.duncanruns.chunkumulator.mixin;

import me.duncanruns.chunkumulator.mixinint.ServerPlayerEntityInt;
import net.minecraft.network.packet.c2s.play.KeepAliveC2SPacket;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayNetworkHandler.class)
public abstract class ServerPlayNetworkHandlerMixin {
    @Shadow
    public ServerPlayerEntity player;

    @Inject(method = "onKeepAlive", at = @At("HEAD"), cancellable = true)
    private void interceptChunkumulatorReply(KeepAliveC2SPacket packet, CallbackInfo ci) {
        // Normally the id of the keep alive packet is of a timestamp, but if it's -1 that means it's Chunkumulator
        // looking for a reply to determine rtt, so redirect those to the PlayerChunkAccumulator.
        if (packet.getId() == -1) {
            ci.cancel();
            ((ServerPlayerEntityInt) player).chunkumulator$getChunkQueue().onFinishBatch();
        }
    }
}
