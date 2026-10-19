package dev.trailpack.backpack;

import dev.trailpack.item.AttachmentItem;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

/**
 * 부착물 키 동작 (서버).
 * G = 오른쪽, 웅크리기+G = 왼쪽, V = 위. 세 위치 모두 같은 규칙이다:
 * 손에 든 것이 그 칸에 들어가는 것이면 맞바꾸고(빈 칸이면 넣고), 아니면 칸의 것을 주 손으로 꺼낸다.
 * 들고 있던 것은 인벤토리 → 배낭 → 바닥 순서로 보낸다.
 * 꺼낸 침낭은 바닐라 침대처럼 우클릭해서 설치한다.
 * 예외: 간식 주머니가 달린 쪽은 키를 누르고 있는 동안 주머니의 음식을 먹는다 ({@link SnackEating}).
 */
public final class AttachmentActions {
    private AttachmentActions() {}

    public static void use(ServerPlayer player, BackpackLayout.Position position) {
        ItemStack backpack = WornBackpack.find(player);
        if (backpack.isEmpty()) return;
        BackpackInventory inv = BackpackInventory.access(backpack, player);
        ItemStack attachment = inv.extra.getItem(position.attachment);
        if (attachment.isEmpty()) {
            player.displayClientMessage(Component.translatable("message.trailpack.no_attachment." + key(position)), true);
            return;
        }
        int slot = position.content;
        ItemStack content = inv.extra.getItem(slot);
        if (BackpackLayout.kindOf(attachment) == AttachmentItem.Kind.POUCH) {
            if (content.isEmpty()) {
                player.displayClientMessage(Component.translatable("message.trailpack.pouch_empty"), true);
            } else {
                SnackEating.start(player, position);
            }
            return;
        }
        ItemStack held = player.getMainHandItem();

        if (!held.isEmpty() && BackpackLayout.isValidContent(attachment, slot, held)) {
            if (!content.isEmpty() && ItemStack.isSameItemSameComponents(content, held)) {
                // 같은 것 (랜턴·음식): 칸에 채워 넣는다
                int room = Math.min(BackpackLayout.maxStack(slot), content.getMaxStackSize()) - content.getCount();
                if (room <= 0) return;
                int move = Math.min(room, held.getCount());
                ItemStack merged = content.copyWithCount(content.getCount() + move);
                held.shrink(move);
                inv.extra.setItem(slot, merged);
                player.setItemInHand(InteractionHand.MAIN_HAND, held);
            } else {
                // 맞바꾸기 (칸이 비어 있으면 넣기만)
                inv.extra.setItem(slot, held.copy());
                player.setItemInHand(InteractionHand.MAIN_HAND, content);
            }
        } else if (!content.isEmpty()) {
            // 꺼내기: 칸의 것을 주 손으로, 들고 있던 것은 다른 곳으로
            inv.extra.setItem(slot, ItemStack.EMPTY);
            player.setItemInHand(InteractionHand.MAIN_HAND, content);
            if (!held.isEmpty()) stash(player, inv, held);
        } else {
            return;
        }
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ARMOR_EQUIP_LEATHER.value(), SoundSource.PLAYERS, 0.5F, 1.2F);
    }

    /** 인벤토리 → 배낭 주 인벤토리 → 바닥. */
    private static void stash(ServerPlayer player, BackpackInventory inv, ItemStack stack) {
        player.getInventory().add(stack);
        if (!stack.isEmpty() && BackpackLayout.canStoreInMain(stack)) {
            stack = inv.main.addItem(stack);
        }
        if (!stack.isEmpty()) player.drop(stack, false);
    }

    private static String key(BackpackLayout.Position position) {
        return switch (position) {
            case RIGHT -> "right";
            case LEFT -> "left";
            case TOP -> "top";
        };
    }
}
