package me.duncanruns.chunkumulator.mixin;

import net.minecraft.server.integrated.IntegratedServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.UUID;

@Mixin(IntegratedServer.class)
public interface IntegratedServerAccessor {
    @Accessor(value = "localPlayerUuid")
    UUID getLocalPlayerUuid();
}
