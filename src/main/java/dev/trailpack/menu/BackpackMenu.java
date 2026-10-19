package dev.trailpack.menu;

import dev.trailpack.Registration;
import dev.trailpack.backpack.BackpackInventory;
import dev.trailpack.backpack.BackpackLayout;
import dev.trailpack.item.AttachmentItem;
import com.mojang.datafixers.util.Pair;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.function.Predicate;

/**
 * 배낭 화면. 세 경우에 쓴다: 멘 배낭(본인), 설치된 배낭, 남이 멘 배낭(주 인벤토리만).
 *
 * <pre>
 * 슬롯 순서: [주 인벤토리 rows*9] [부착물 관련 11칸 (full 일 때만)] [플레이어 인벤토리 27] [단축바 9]
 * 화면 배치(x): 부착물 패널 0~104 | 주 패널 108~284
 *
 * 부착물 패널: 배낭을 등 뒤에서 본 모양 그대로 배치한다 (위 = 침낭, 양옆 = 좌우, 아래 = 갑옷).
 *              ↑ [위 부착물][침낭]
 *   ← [내용물][왼쪽 부착물]  [오른쪽 부착물][내용물] →
 *   [갑옷주머니]  [머리][몸통][다리][발]
 * </pre>
 */
public class BackpackMenu extends AbstractContainerMenu {
    public static final int MAIN_X = 108;
    public static final int IMAGE_WIDTH = MAIN_X + 176;
    public static final int PANEL_WIDTH = 104;
    public static final int PANEL_HEIGHT = 102;

    /** extra 인덱스별 화면 좌표 */
    public static final int[][] EXTRA_POS = {
            {56, 44},                                     // 0 오른쪽 부착물 (가운데 줄 오른쪽 안)
            {30, 44},                                     // 1 왼쪽 부착물 (가운데 줄 왼쪽 안)
            {34, 18},                                     // 2 위 부착물 (윗줄)
            {6, 76},                                      // 3 갑옷주머니 (아랫줄 맨 왼쪽)
            {74, 44},                                     // 4 오른쪽 내용물 (바깥쪽)
            {12, 44},                                     // 5 왼쪽 내용물 (바깥쪽)
            {52, 18},                                     // 6 위 내용물 (침낭)
            {28, 76}, {46, 76}, {64, 76}, {82, 76}        // 7~10 갑옷 머리·몸통·다리·발 (가로)
    };

    private final BackpackInventory inventory;
    private final boolean full;
    private final int mainSize;
    private final Predicate<Player> validator;
    @Nullable
    private final Runnable onClose;

    public BackpackMenu(int id, Inventory playerInventory, BackpackInventory inventory, boolean full,
                        Predicate<Player> validator, @Nullable Runnable onClose) {
        super(Registration.BACKPACK_MENU.get(), id);
        this.inventory = inventory;
        this.full = full;
        this.validator = validator;
        this.onClose = onClose;
        this.mainSize = inventory.main.getContainerSize();
        int rows = inventory.rows();

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < 9; c++) {
                addSlot(new MainSlot(inventory.main, c + r * 9, MAIN_X + 8 + c * 18, 18 + r * 18));
            }
        }
        if (full) {
            for (int i = 0; i < BackpackLayout.EXTRA_SIZE; i++) {
                addSlot(new ExtraSlot(inventory.extra, i, EXTRA_POS[i][0], EXTRA_POS[i][1]));
            }
        }
        int invY = 31 + rows * 18;
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 9; c++) {
                addSlot(new Slot(playerInventory, c + r * 9 + 9, MAIN_X + 8 + c * 18, invY + r * 18));
            }
        }
        for (int c = 0; c < 9; c++) {
            addSlot(new Slot(playerInventory, c, MAIN_X + 8 + c * 18, invY + 58));
        }

    }

    /**
     * 배낭 화면을 연다 (설치형·착용형 공통). 열렸으면 true.
     * onClose 는 화면이 닫힐 때 서버에서 한 번 불린다.
     */
    public static boolean open(ServerPlayer player, BackpackInventory inventory, boolean full, Component title,
                               Predicate<Player> validator, @Nullable Runnable onClose) {
        boolean upgraded = inventory.upgraded();
        return player.openMenu(new SimpleMenuProvider(
                        (id, playerInv, p) -> new BackpackMenu(id, playerInv, inventory, full, validator, onClose), title),
                buf -> {
                    buf.writeBoolean(upgraded);
                    buf.writeBoolean(full);
                }).isPresent();
    }

    public static BackpackMenu fromNetwork(int id, Inventory playerInventory, RegistryFriendlyByteBuf buf) {
        boolean upgraded = buf.readBoolean();
        boolean full = buf.readBoolean();
        return new BackpackMenu(id, playerInventory, new BackpackInventory(upgraded, null), full, p -> true, null);
    }

    public int rows() {
        return inventory.rows();
    }

    public boolean isFull() {
        return full;
    }

    public BackpackInventory inventory() {
        return inventory;
    }


    @Override
    public boolean stillValid(Player player) {
        return validator.test(player);
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (onClose != null) onClose.run();
    }


    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        int extraEnd = full ? mainSize + BackpackLayout.EXTRA_SIZE : mainSize;
        int invEnd = extraEnd + 36;

        if (index < extraEnd) {
            // 배낭 → 플레이어
            if (!moveItemStackTo(stack, extraEnd, invEnd, true)) return ItemStack.EMPTY;
        } else {
            // 플레이어 → 배낭. 부착물은 비어 있는 맞는 부착물 칸으로, 나머지는 주 인벤토리로.
            boolean moved = false;
            if (full && stack.getItem() instanceof AttachmentItem) {
                for (int i = 0; i < BackpackLayout.ATTACHMENT_COUNT && !moved; i++) {
                    Slot target = slots.get(mainSize + i);
                    if (!target.hasItem() && target.mayPlace(stack)) {
                        moved = moveItemStackTo(stack, mainSize + i, mainSize + i + 1, false);
                    }
                }
            }
            if (!moved && !moveItemStackTo(stack, 0, mainSize, false)) return ItemStack.EMPTY;
        }

        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        if (stack.getCount() == original.getCount()) return ItemStack.EMPTY;
        slot.onTake(player, stack);
        return original;
    }

    // ------------------------------------------------------------------

    private static class MainSlot extends Slot {
        MainSlot(Container container, int index, int x, int y) {
            super(container, index, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return BackpackLayout.canStoreInMain(stack);
        }
    }

    /** 부착물 칸과, 부착물이 있어야 쓸 수 있는 칸. */
    /** BackpackLayout.ARMOR_SLOTS 순서 (머리, 몸통, 다리, 발). */
    private static final ResourceLocation[] ARMOR_ICONS = {
            InventoryMenu.EMPTY_ARMOR_SLOT_HELMET, InventoryMenu.EMPTY_ARMOR_SLOT_CHESTPLATE,
            InventoryMenu.EMPTY_ARMOR_SLOT_LEGGINGS, InventoryMenu.EMPTY_ARMOR_SLOT_BOOTS
    };

    public static class ExtraSlot extends Slot {
        private final int extraIndex;

        ExtraSlot(Container container, int index, int x, int y) {
            super(container, index, x, y);
            this.extraIndex = index;
        }

        public int extraIndex() {
            return extraIndex;
        }

        private boolean attachmentPresent() {
            int att = BackpackLayout.attachmentOf(extraIndex);
            return att < 0 || !container.getItem(att).isEmpty();
        }

        @Override
        public boolean isActive() {
            return attachmentPresent();
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return attachmentPresent() && container.canPlaceItem(extraIndex, stack);
        }

        @Override
        public int getMaxStackSize() {
            return BackpackLayout.maxStack(extraIndex);
        }

        /** 빈 갑옷 칸에 바닐라 인벤토리와 같은 부위 무늬 (머리·몸통·다리·발). 잠긴 칸은 그려지지 않는다. */
        @Override
        @Nullable
        public Pair<ResourceLocation, ResourceLocation> getNoItemIcon() {
            int armor = extraIndex - BackpackLayout.ARMOR_FIRST;
            if (armor < 0 || armor >= ARMOR_ICONS.length) return null;
            return Pair.of(InventoryMenu.BLOCK_ATLAS, ARMOR_ICONS[armor]);
        }
    }
}
