package com.example.mcatowns.blockentity;

import com.example.mcatowns.registry.ModBlockEntities;
import com.example.mcatowns.screen.SiloScreenHandler;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.screen.ArrayPropertyDelegate;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;

public class SiloBlockEntity extends BlockEntity implements ExtendedScreenHandlerFactory {
    public SiloBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SILO, pos, state);
    }

    @Override
    public void writeScreenOpeningData(ServerPlayerEntity player, PacketByteBuf buf) {
        buf.writeBlockPos(getPos());
    }

    @Override
    public Text getDisplayName() {
        return Text.translatable("screen.mcatowns.silo");
    }

    @Nullable
    @Override
    public ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) {
        return new SiloScreenHandler(syncId, playerInventory, getPos(), new ArrayPropertyDelegate(3));
    }
}
