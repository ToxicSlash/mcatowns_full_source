package com.example.mcatowns.mixin.compat;

import com.example.mcatowns.integration.UnloadedActivityCompat;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;

/** Vinery's grape crops use custom randomTick implementations outside vanilla CropBlock. */
@Pseudo
@Mixin(targets = {
        "net.satisfy.vinery.core.block.GrapeBush",
        "net.satisfy.vinery.core.block.LatticeBlock",
        "net.satisfy.vinery.core.block.PaleStemBlock",
        "net.satisfy.vinery.core.block.GrapeVineBlock"
}, remap = false)
public abstract class VineryCropMixin {
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
