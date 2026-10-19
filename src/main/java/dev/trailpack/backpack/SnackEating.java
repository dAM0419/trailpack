package dev.trailpack.backpack;

import dev.trailpack.backpack.BackpackLayout.Position;
import dev.trailpack.item.AttachmentItem;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 간식 주머니가 달린 쪽의 키(G / 웅크리기+G)를 누르고 있는 동안 주머니 속 음식을 먹는다 (서버).
 * 손에 든 것과 상관없고, 손은 바뀌지 않는다. 우클릭을 누르고 있는 것처럼 음식의 원래 섭취 시간이
 * 차면 하나를 먹고, 계속 누르고 있으면 다음 것을 이어서 먹는다. 키를 떼면 취소.
 */
public final class SnackEating {
    private SnackEating() {}

    private static final class State {
        final Position position;
        int ticks;

        State(Position position) {
            this.position = position;
        }
    }

    private static final Map<UUID, State> EATING = new HashMap<>();

    public static void start(ServerPlayer player, Position position) {
        State current = EATING.get(player.getUUID());
        if (current != null && current.position == position) return; // 이미 먹는 중이면 진행을 이어 간다
        EATING.put(player.getUUID(), new State(position));
    }

    public static void stop(ServerPlayer player) {
        EATING.remove(player.getUUID());
    }

    public static void tick(ServerPlayer player) {
        State state = EATING.get(player.getUUID());
        if (state == null) return;
        ItemStack backpack = WornBackpack.find(player);
        if (backpack.isEmpty() || !player.isAlive() || player.isSpectator()) {
            stop(player);
            return;
        }
        // 매 틱 읽기만 하므로 캐시된 읽기 전용 사본을 본다. 실제로 먹을 때만 인벤토리를 연다.
        List<ItemStack> extra = BackpackVisuals.extraOf(backpack);
        ItemStack food = extra.get(state.position.content);
        FoodProperties props = food.isEmpty() ? null : food.getFoodProperties(player);
        if (BackpackLayout.kindOf(extra.get(state.position.attachment)) != AttachmentItem.Kind.POUCH
                || props == null) {
            stop(player);
            return;
        }
        if (!player.canEat(props.canAlwaysEat())) {
            state.ticks = 0; // 배부르면 먹지 않고 기다린다 (바닐라와 같음)
            return;
        }

        state.ticks++;
        if (state.ticks >= 7 && state.ticks % 4 == 0) effects(player, food, 5);
        if (state.ticks < food.getUseDuration(player)) return;

        effects(player, food, 16);
        boolean infinite = player.hasInfiniteMaterials();
        // 하나만 떼어 바닐라 방식으로 먹는다 (허기·포화·효과·통계·발전과제)
        ItemStack one = food.copyWithCount(1);
        ItemStack left = one.finishUsingItem(player.level(), player);
        if (!infinite) {
            BackpackInventory inv = BackpackInventory.access(backpack, player);
            ItemStack rest = inv.extra.getItem(state.position.content).copy();
            rest.shrink(1);
            inv.extra.setItem(state.position.content, rest);
            // 그릇 요리: 남은 그릇 등은 인벤토리 → 바닥
            if (!left.isEmpty() && !player.getInventory().add(left)) player.drop(left, false);
        }
        state.ticks = 0;
    }

    /** 바닐라 먹기와 같은 소리·부스러기. */
    private static void effects(ServerPlayer player, ItemStack food, int particles) {
        SoundEvent sound = food.getUseAnimation() == UseAnim.DRINK
                ? food.getItem().getDrinkingSound() : food.getItem().getEatingSound();
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), sound, SoundSource.PLAYERS,
                0.5F + 0.5F * player.getRandom().nextInt(2),
                (player.getRandom().nextFloat() - player.getRandom().nextFloat()) * 0.2F + 1.0F);
        if (food.getUseAnimation() == UseAnim.EAT && player.level() instanceof ServerLevel level) {
            Vec3 at = player.getEyePosition().add(player.getLookAngle().scale(0.4)).subtract(0, 0.15, 0);
            level.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, food.copyWithCount(1)),
                    at.x, at.y, at.z, particles, 0.08, 0.05, 0.08, 0.05);
        }
    }
}
