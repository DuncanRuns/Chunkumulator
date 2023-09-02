package me.duncanruns.chunkumulator;

import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import me.duncanruns.chunkumulator.mixin.ThreadedAnvilChunkStorageAccessor;
import net.minecraft.network.Packet;
import net.minecraft.network.packet.s2c.play.StatisticsS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerChunkManager;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.WorldChunk;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

public class PlayerChunkAccumulator {
    private static final Packet<?> EMPTY_PACKET = new StatisticsS2CPacket(new Object2IntOpenHashMap<>());
    private final ServerPlayerEntity player;
    private final List<Courier> queuedPackages = new ArrayList<>();
    private final AtomicBoolean readyForMore = new AtomicBoolean(true);

    public PlayerChunkAccumulator(ServerPlayerEntity player) {
        this.player = player;
    }

    public void addChunk(ChunkPos pos, World world) {
        Courier courier = new Courier(pos, world);
        courier.updateDistance();
        queuedPackages.add(courier);
    }

    public void tick() {
        if (!readyForMore.get()) return;
        if (queuedPackages.isEmpty()) return;
        readyForMore.set(false);

        queuedPackages.removeIf(courier -> courier.world != player.world);
        queuedPackages.forEach(Courier::updateDistance);
        queuedPackages.stream().sorted(Comparator.comparingInt(o -> o.distance)).collect(Collectors.toList()).subList(0, Math.min(100, queuedPackages.size())).forEach(courier -> {
            queuedPackages.remove(courier);
            courier.sendToPlayer();
        });
        player.networkHandler.sendPacket(EMPTY_PACKET, future -> readyForMore.set(true));
    }

    public void removeChunk(ChunkPos chunkPos) {
        queuedPackages.removeIf(courier -> courier.chunkPos.equals(chunkPos));
    }

    /**
     * cour·i·er
     * 1. a company or employee (or object) of a company (or fabric mod) that transports commercial packages and documents (or packets). (real definition)
     */
    private class Courier {
        private final ChunkPos chunkPos;
        private int distance;
        private final World world;

        Courier(ChunkPos pos, World world) {
            this.chunkPos = pos;
            this.world = world;
        }

        void updateDistance() {
            BlockPos.Mutable pos = new BlockPos.Mutable(player.getBlockPos());
            pos.setY(0);
            distance = (int) (chunkPos.getCenterBlockPos().getSquaredDistance(pos));
        }

        public void sendToPlayer() {
            World world = player.world;
            WorldChunk chunk = world.getChunk(chunkPos.x, chunkPos.z);
            ((ThreadedAnvilChunkStorageAccessor) ((ServerChunkManager) world.getChunkManager()).threadedAnvilChunkStorage).invokeSendChunkDataPackets(player, new Packet[2], chunk);
        }
    }
}
