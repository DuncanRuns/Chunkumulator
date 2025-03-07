package me.duncanruns.chunkumulator;

import com.bawnorton.mixinsquared.api.MixinCanceller;

import java.util.List;

public class KryptonCanceller implements MixinCanceller {
    @Override
    public boolean shouldCancel(List<String> targetClassNames, String mixinClassName) {
        return mixinClassName.equals("me.steinborn.krypton.mixin.network.shared.flushconsolidation.ThreadedAnvilChunkStorageMixin");
    }
}