package com.example.mcatowns.block;

import com.example.mcatowns.town.PlayerTownRegistry;
import com.example.mcatowns.town.TownContext;
import com.example.mcatowns.town.TownManager;
import com.example.mcatowns.town.TownSavedData;
import com.example.mcatowns.town.TownManagerService;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class TownBellBlock extends Block {
    public TownBellBlock(Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player,
                              Hand hand, BlockHitResult hit) {
        if (world.isClient) return ActionResult.SUCCESS;
        if (!(world instanceof ServerWorld serverWorld) || !(player instanceof ServerPlayerEntity serverPlayer)) {
            return ActionResult.PASS;
        }

        TownContext town = PlayerTownRegistry.get(serverWorld).getTownAt(pos)
                .orElseGet(() -> TownManager.findExistingTown(serverWorld, pos, 16).orElse(null));
        if (town == null) return ActionResult.PASS;

        TownManagerService.open(serverPlayer, town, pos);
        return ActionResult.CONSUME;
    }
}
