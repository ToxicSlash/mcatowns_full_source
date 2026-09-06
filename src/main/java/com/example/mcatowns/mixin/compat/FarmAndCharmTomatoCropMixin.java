package com.example.mcatowns.mixin.compat;

import com.example.mcatowns.integration.UnloadedActivityCompat;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;

/** Farm & Charm tomatoes use a custom crop base rather than vanilla CropBlock. */
@Pseudo
@Mixin(targets = "net.satisfy.farm_and_charm.core.block.crops.TomatoCropBlock", remap = false)
public abstract class FarmAndCharmTomatoCropMixin {
    public boolean implementsSimulateRandTicks() {
        return true;
    }

    public boolean canSimulateRandTicks(BlockState state, ServerWorld world, BlockPos pos) {
        return UnloadedActivityCompat.canSimulateCustomCrop();
    }

    public void simulateRandTicks(BlockState state, ServerWorld world, BlockPos pos, Random random,
                                  long elapsedTicks, int randomTickSpeed) {
        UnloadedActivityCompat.simulateCustomCrop((Block) (Object) this, state, world, pos,
                random, elapsedTicks, randomTickSpeed);
    }
}
