package me.duncanruns.chunkumulator;

import me.duncanruns.chunkumulator.mixinint.ServerPlayerEntityInt;
import net.minecraft.server.network.ServerPlayerEntity;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Chunkumulator {
    public static final Logger LOGGER = LogManager.getLogger("chunkumulator");
    public static final long CHUNKUMULATOR_KEEPALIVE_ID = -1457264210 /*-Math.abs("Chunkumulator".hashCode())*/;

    public static PlayerChunkAccumulator getChunkQueueFromPlayer(ServerPlayerEntity player) {
        return ((ServerPlayerEntityInt) player).chunkumulator$getChunkQueue();
    }
}