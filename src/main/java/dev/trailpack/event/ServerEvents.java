package dev.trailpack.event;

import dev.trailpack.backpack.SnackEating;
import dev.trailpack.backpack.WornBackpack;
import dev.trailpack.block.SleepingBagBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerSetSpawnEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

public final class ServerEvents {
    private ServerEvents() {}

    /** 남이 멘 배낭을 빈손으로 우클릭하면 주 인벤토리가 열린다. */
    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (event.getHand() != InteractionHand.MAIN_HAND) return;
        if (!(event.getTarget() instanceof Player target)) return;
        Player player = event.getEntity();
        if (!player.getMainHandItem().isEmpty() || player.isSpectator()) return;
        if (event.getLevel().isClientSide) return;
        if (WornBackpack.find(target).isEmpty()) return;
        WornBackpack.open((ServerPlayer) player, target);
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
    }

    /** 침낭에서 자도 리스폰 지점은 바뀌지 않는다. */
    @SubscribeEvent
    public static void onSetSpawn(PlayerSetSpawnEvent event) {
        BlockPos pos = event.getNewSpawn();
        if (pos != null && event.getEntity().level().getBlockState(pos).getBlock() instanceof SleepingBagBlock) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player) SnackEating.tick(player);
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) SnackEating.stop(player);
    }
}
