package dev.trailpack.backpack;

import dev.trailpack.Registration;
import dev.trailpack.block.SleepingBagBlock;
import dev.trailpack.item.AttachmentItem;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Equipable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ShulkerBoxBlock;

import java.util.List;

/**
 * 부착물 칸 배치와 각 칸에 넣을 수 있는 아이템 규칙.
 *
 * <pre>
 * extra 인덱스            위치 (착용자 기준)
 *  0 오른쪽 부착물  → 4  오른쪽 내용물   (G)          도구걸이 / 간식 주머니
 *  1 왼쪽 부착물    → 5  왼쪽 내용물     (웅크리기+G)  도구걸이 / 간식 주머니
 *  2 위 부착물      → 6  위 내용물       (V)          침낭끈
 *  3 앞 부착물      → 7~10 갑옷 4칸      (웅크리기+F)  갑옷주머니
 * </pre>
 * 도구걸이 = 도구(태그 trailpack:hook_tools) 또는 랜턴(trailpack:hangable_lanterns), 64개까지 쌓임.
 * 간식 주머니 = 먹을 수 있는 것 아무거나, 양옆 합쳐 하나만.
 */
public final class BackpackLayout {
    private BackpackLayout() {}

    public static final int BASE_ROWS = 1;
    public static final int UPGRADED_ROWS = 3; // 셜커 상자와 같은 27칸
    public static final int COLUMNS = 9;

    public static boolean isUpgraded(ItemStack backpack) {
        return backpack.has(Registration.UPGRADED.get());
    }

    public static int rows(boolean upgraded) {
        return upgraded ? UPGRADED_ROWS : BASE_ROWS;
    }

    public static final int EXTRA_SIZE = 11;

    public static final int ATT_RIGHT = 0;
    public static final int ATT_LEFT = 1;
    public static final int ATT_TOP = 2;
    public static final int ATT_ARMOR = 3;
    public static final int ATTACHMENT_COUNT = 4;

    public static final int RIGHT = 4;
    public static final int LEFT = 5;
    public static final int TOP = 6;
    public static final int ARMOR_FIRST = 7;

    public static final EquipmentSlot[] ARMOR_SLOTS = {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
    };

    /** 키로 쓰는 세 위치. */
    public enum Position {
        RIGHT(ATT_RIGHT, BackpackLayout.RIGHT),
        LEFT(ATT_LEFT, BackpackLayout.LEFT),
        TOP(ATT_TOP, BackpackLayout.TOP);

        public final int attachment;
        public final int content;

        Position(int attachment, int content) {
            this.attachment = attachment;
            this.content = content;
        }
    }

    /** 내용물 칸에 대응하는 부착물 칸. 부착물 칸 자신은 -1. */
    public static int attachmentOf(int extraIndex) {
        return switch (extraIndex) {
            case RIGHT -> ATT_RIGHT;
            case LEFT -> ATT_LEFT;
            case TOP -> ATT_TOP;
            case 7, 8, 9, 10 -> ATT_ARMOR;
            default -> -1;
        };
    }

    public static boolean isAttachmentSlot(int extraIndex) {
        return extraIndex >= 0 && extraIndex < ATTACHMENT_COUNT;
    }

    public static int maxStack(int extraIndex) {
        return extraIndex == RIGHT || extraIndex == LEFT ? 64 : 1;
    }

    /** 배낭 주 인벤토리에 넣을 수 있는가. 셜커 상자와 같은 규칙에 셜커 상자 자체를 추가로 막는다. */
    public static boolean canStoreInMain(ItemStack stack) {
        return stack.getItem().canFitInsideContainerItems()
                && !(Block.byItem(stack.getItem()) instanceof ShulkerBoxBlock);
    }

    public static AttachmentItem.Kind kindOf(ItemStack stack) {
        return stack.getItem() instanceof AttachmentItem att ? att.kind() : null;
    }

    /** 그 칸에 넣을 수 있는가. 내용물 칸은 같은 위치에 달린 부착물에 따라 정해진다. */
    public static boolean isValidExtra(int extraIndex, ItemStack stack, List<ItemStack> extra) {
        if (stack.isEmpty()) return true;
        if (isAttachmentSlot(extraIndex)) return isValidAttachment(extraIndex, stack, extra);
        int att = attachmentOf(extraIndex);
        return att >= 0 && isValidContent(extra.get(att), extraIndex, stack);
    }

    private static boolean isValidAttachment(int slot, ItemStack stack, List<ItemStack> extra) {
        AttachmentItem.Kind kind = kindOf(stack);
        if (kind == null) return false;
        return switch (slot) {
            case ATT_RIGHT, ATT_LEFT -> kind == AttachmentItem.Kind.HOOK
                    || (kind == AttachmentItem.Kind.POUCH
                    && kindOf(extra.get(slot == ATT_RIGHT ? ATT_LEFT : ATT_RIGHT)) != AttachmentItem.Kind.POUCH);
            case ATT_TOP -> kind == AttachmentItem.Kind.STRAP;
            case ATT_ARMOR -> kind == AttachmentItem.Kind.ARMOR;
            default -> false;
        };
    }

    /** attachment 가 달려 있을 때 contentIndex 칸에 stack 을 넣을 수 있는가. */
    public static boolean isValidContent(ItemStack attachment, int contentIndex, ItemStack stack) {
        AttachmentItem.Kind kind = kindOf(attachment);
        if (kind == null || stack.isEmpty()) return false;
        return switch (kind) {
            case HOOK -> isHookable(stack);
            case POUCH -> isFood(stack);
            case STRAP -> isSleepingBag(stack);
            case ARMOR -> contentIndex >= ARMOR_FIRST && canStoreInMain(stack)
                    && fitsArmorSlot(stack, ARMOR_SLOTS[contentIndex - ARMOR_FIRST]);
        };
    }

    public static boolean isHookable(ItemStack stack) {
        return (stack.is(Registration.HOOK_TOOLS) && !stack.is(Registration.HOOK_EXCLUDED)) || isLantern(stack);
    }

    public static boolean isLantern(ItemStack stack) {
        return stack.is(Registration.HANGABLE_LANTERNS);
    }

    public static boolean isFood(ItemStack stack) {
        return stack.getFoodProperties(null) != null;
    }

    public static boolean isSleepingBag(ItemStack stack) {
        return stack.getItem() instanceof BlockItem item && item.getBlock() instanceof SleepingBagBlock;
    }

    public static boolean fitsArmorSlot(ItemStack stack, EquipmentSlot slot) {
        EquipmentSlot target = stack.getEquipmentSlot();
        if (target == null) {
            Equipable equipable = Equipable.get(stack);
            if (equipable != null) target = equipable.getEquipmentSlot();
        }
        return target == slot;
    }
}
