package me.duncanruns.chunkumulator;

import me.duncanruns.chunkumulator.mixin.ThreadedAnvilChunkStorageAccessor;
import net.minecraft.network.Packet;
import net.minecraft.network.packet.s2c.play.KeepAliveS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerChunkManager;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.WorldChunk;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class PlayerChunkAccumulator {
    private final ServerPlayerEntity player;
    private final List<Courier> queuedPackages = new ArrayList<>();
    private boolean readyForMore = true;
    private long lastSendTime;
    private boolean lastSendWasBatchSize;

    private static final int MIN_BATCH_SIZE = 16;
    private static final int MAX_BATCH_SIZE = 256;
    private static final int TARGET_RTT_LOWER = 200;
    private static final int TARGET_RTT_UPPER = 550;
    private int batchSize = 32;

    public PlayerChunkAccumulator(ServerPlayerEntity player) {
        this.player = player;
    }

    public void addChunk(ChunkPos pos, World world) {
        Courier courier = new Courier(pos, world);
        courier.updateDistance();
        queuedPackages.add(courier);
    }

    public synchronized void tick() {
        if (!readyForMore) return;
        if (queuedPackages.isEmpty()) return;
        readyForMore = false;

        queuedPackages.removeIf(courier -> courier.world != player.world);
        queuedPackages.forEach(Courier::updateDistance);
        queuedPackages.stream().sorted(Comparator.comparingInt(o -> o.distance)).limit(batchSize).collect(Collectors.toList()).forEach(courier -> {
            queuedPackages.remove(courier);
            if (courier.isChunkLoaded()) courier.sendToPlayer();
        });

        lastSendWasBatchSize = !queuedPackages.isEmpty();
        lastSendTime = System.currentTimeMillis();

        // Send a keep alive packet with id -1, the client will respond with a keep alive packet with the given ID
        player.networkHandler.sendPacket(new KeepAliveS2CPacket(-1));
    }

    private synchronized void updateBatchSize(long rtt) {
        int startSpeed = batchSize;
        if (rtt < TARGET_RTT_LOWER) {
            // The purpose of the booster value is to significantly jump the batch size if the connection is very good.
            // The calculation blindly assumes the rtt (round trip time) involves 0 ping and is purely the time spent
            // transferring chunk data, for example if a connected player has 50 ping and the round trip time was 100
            // (meaning 50 for purely transferring chunks), it assumes all 100 were spent purely on transferring chunk
            // data. It will then calculate how much the batch size should be multiplied by to achieve the lower rtt
            // target (200 / 100 = 2). Since the actual chunk transfer time is lower, the booster speed almost certainly
            // won't cause the rtt to exceed the lower target (in the example, it would only get it to 150).
            // Additionally, it will only go halfway towards the calculated booster value, so it will definitely need
            // a few cycles before either maxing out or hitting target rtt.
            int booster = (int) (batchSize * (TARGET_RTT_LOWER / (double) rtt));
            // Final decision: We should at least increase by a couple for when getting close to the target, and don't
            // increase by more than max.
            batchSize = Math.min(MAX_BATCH_SIZE, Math.max(batchSize + 4, (booster + batchSize) / 2));
        } else if (rtt > TARGET_RTT_UPPER) {
            // Starting batch size (32) is low enough that we don't need the opposite of a booster. In case of a spike
            // for better connections, it's better not to overreact, so only cut by 20% if over threshold.
            // Final decision: don't decrease more than minimum. Minimum (16) should be low enough to provide playability
            // to very poor connections, while ensuring that chunks keep continuously sending
            batchSize = Math.max(MIN_BATCH_SIZE, (int) (batchSize * 0.8));
        }
        if (startSpeed != batchSize) {
            Chunkumulator.LOGGER.info("Updated speed for {}: rtt={}, speed={}", player.getEntityName(), rtt, batchSize);
        }
    }

    public void removeChunk(ChunkPos chunkPos) {
        queuedPackages.removeIf(courier -> courier.chunkPos.equals(chunkPos));
    }

    public synchronized void onFinishBatch() {
        readyForMore = true;
        if (lastSendWasBatchSize) updateBatchSize(System.currentTimeMillis() - lastSendTime);
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
            BlockPos.Mutable pos = player.getBlockPos().mutableCopy();
            pos.setY(0);
            distance = (int) (chunkPos.getCenterBlockPos().getSquaredDistance(pos));
        }

        public void sendToPlayer() {
            WorldChunk chunk = world.getChunk(chunkPos.x, chunkPos.z);
            ((ThreadedAnvilChunkStorageAccessor) ((ServerChunkManager) world.getChunkManager()).threadedAnvilChunkStorage).invokeSendChunkDataPackets(player, new Packet[2], chunk);
        }

        public boolean isChunkLoaded() {
            return world.isChunkLoaded(chunkPos.x, chunkPos.z);
        }
    }
}
