package com.example.mcatowns.town;

import com.example.mcatowns.network.ModNetworking;
import com.example.mcatowns.network.TownManagerView;
import com.example.mcatowns.event.TownRemovalHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;

public final class TownManagerService {
    private TownManagerService() {
    }

    public static void open(ServerPlayerEntity player, TownContext town, BlockPos bellPos) {
        TownSavedData data = TownSavedData.get(player.getServerWorld(), town.townId());
        if (data.getTownCenter().equals(BlockPos.ORIGIN)) data.setTownCenter(town.center());
        TownBuildingService.refreshDerivedValues(data);
        TownRequest request = data.getActiveRequest();
        ModNetworking.openTownManager(player, new TownManagerView(
                data.getTownName(), data.getTownRank(), data.getProsperity(), data.getProsperityBase(),
                data.getTownTokens(), data.getFoodReserves(), data.getFoodCapacity(), data.getPopulation(),
                data.getPopulationCapacity(), data.getRegisteredBuildingCount(), data.getSpecialistCount(),
                TownManager.hasMayorAuthority(player, town), TownRemovalHandler.canRemove(player, bellPos),
                bellPos, TownProgressionService.checklist(data),
                data.getResidents().size(),
                request == null ? "" : request.name(), request == null ? "" : request.type().displayName(),
                request == null ? -1 : request.dueDay(), request == null ? 0 : request.prosperityReward(),
                request == null ? 0 : request.tokenReward(), TownRequestService.requirementLines(player.getServerWorld(), data),
                data.getBountyKills()
        ));
    }
}
