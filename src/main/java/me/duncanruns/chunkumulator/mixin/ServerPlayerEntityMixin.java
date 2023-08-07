package me.duncanruns.chunkumulator.mixin;

import me.duncanruns.chunkumulator.PlayerChunkAccumulator;
import me.duncanruns.chunkumulator.mixinint.ServerPlayerEntityInt;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.ChunkPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayerEntity.class)
public abstract class ServerPlayerEntityMixin implements ServerPlayerEntityInt {
    @Unique
    private final PlayerChunkAccumulator accumulator = new PlayerChunkAccumulator((ServerPlayerEntity) (Object) this);

    @Override
    public PlayerChunkAccumulator chunkumulator$getChunkQueue() {
        return accumulator;
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void chunkQueueTick(CallbackInfo ci) {
        accumulator.tick();
    }

    @Inject(method = "sendUnloadChunkPacket", at = @At("HEAD"))
    private void cancelQueuedChunk(ChunkPos chunkPos, CallbackInfo ci) {
        accumulator.removeChunk(chunkPos);
    }
}
