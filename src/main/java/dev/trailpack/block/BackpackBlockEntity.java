package dev.trailpack.block;

import dev.trailpack.Registration;
import dev.trailpack.backpack.BackpackInventory;
import dev.trailpack.backpack.BackpackLayout;
import dev.trailpack.menu.BackpackMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Unit;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Containers;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.stream.IntStream;

public class BackpackBlockEntity extends BlockEntity implements WorldlyContainer {

    private final BackpackInventory.Host host = new BackpackInventory.Host() {
        @Override
        public void onChanged(BackpackInventory inventory) {
            setChanged();
            updateVisualState();
            // 매단 도구·랜턴은 블록 엔티티 렌더러가 그리므로 부착물 칸을 클라이언트에 보낸다
            if (level != null && !level.isClientSide) {
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            }
        }

        @Override
        public void drop(ItemStack stack) {
            if (level != null && !level.isClientSide) {
                Containers.dropItemStack(level, worldPosition.getX() + 0.5, worldPosition.getY() + 0.5,
                        worldPosition.getZ() + 0.5, stack);
            }
        }
    };

    private BackpackInventory inventory = new BackpackInventory(false, host);
    private int[] faceSlots = slotRange(inventory);
    @Nullable
    private Component customName;

    public BackpackBlockEntity(BlockPos pos, BlockState state) {
        super(Registration.BACKPACK_BE.get(), pos, state);
    }

    public BackpackInventory inventory() {
        return inventory;
    }

    private static int[] slotRange(BackpackInventory inv) {
        return IntStream.range(0, inv.main.getContainerSize()).toArray();
    }

    private void rebuild(boolean upgraded) {
        if (upgraded != inventory.upgraded()) {
            inventory = new BackpackInventory(upgraded, host);
            faceSlots = slotRange(inventory);
        }
    }

    private void updateVisualState() {
        if (level == null || level.isClientSide) return;
        BlockState current = getBlockState();
        if (!(current.getBlock() instanceof BackpackBlock)) return;
        BlockState wanted = inventory.visuals().apply(current);
        if (wanted != current) {
            level.setBlock(worldPosition, wanted, 3);
        }
    }

    public void openFor(ServerPlayer player) {
        Component title = customName != null ? customName : Component.translatable("container.trailpack.backpack");
        BackpackMenu.open(player, inventory, true, title, this::stillValid, null);
    }

    // ---- 저장 ----

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putBoolean("Upgraded", inventory.upgraded());
        ContainerHelper.saveAllItems(tag, inventory.main.getItems(), registries);
        CompoundTag extraTag = new CompoundTag();
        ContainerHelper.saveAllItems(extraTag, inventory.extra.getItems(), registries);
        tag.put("Extra", extraTag);
        if (customName != null) {
            tag.putString("CustomName", Component.Serializer.toJson(customName, registries));
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        rebuild(tag.getBoolean("Upgraded"));
        // 빈 칸은 저장되지 않으므로 먼저 비운다 (클라이언트 동기화 때 빠진 부착물이 남지 않게)
        java.util.Collections.fill(inventory.main.getItems(), ItemStack.EMPTY);
        java.util.Collections.fill(inventory.extra.getItems(), ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag, inventory.main.getItems(), registries);
        ContainerHelper.loadAllItems(tag.getCompound("Extra"), inventory.extra.getItems(), registries);
        if (tag.contains("CustomName", 8)) {
            customName = parseCustomNameSafe(tag.getString("CustomName"), registries);
        }
    }

    // ---- 클라이언트 동기화: 부착물 칸만 보낸다 (주 인벤토리는 열 때만 보임) ----

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        tag.putBoolean("Upgraded", inventory.upgraded());
        CompoundTag extraTag = new CompoundTag();
        ContainerHelper.saveAllItems(extraTag, inventory.extra.getItems(), true, registries);
        tag.put("Extra", extraTag);
        return tag;
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    public boolean hasSoulLantern() {
        return inventory.visuals().hasSoulLantern();
    }

    // ---- 아이템 컴포넌트 <-> 블록 엔티티 (설치할 때, 부술 때) ----

    @Override
    protected void applyImplicitComponents(DataComponentInput input) {
        super.applyImplicitComponents(input);
        rebuild(input.get(Registration.UPGRADED.get()) != null);
        input.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY).copyInto(inventory.main.getItems());
        input.getOrDefault(Registration.EXTRA.get(), ItemContainerContents.EMPTY).copyInto(inventory.extra.getItems());
        customName = input.get(DataComponents.CUSTOM_NAME);
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder builder) {
        super.collectImplicitComponents(builder);
        if (inventory.upgraded()) builder.set(Registration.UPGRADED.get(), Unit.INSTANCE);
        builder.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(inventory.main.getItems()));
        builder.set(Registration.EXTRA.get(), ItemContainerContents.fromItems(inventory.extra.getItems()));
        int light = inventory.visuals().lantern().light();
        if (light > 0) builder.set(Registration.LANTERN_LIGHT.get(), light);
        if (customName != null) builder.set(DataComponents.CUSTOM_NAME, customName);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void removeComponentsFromTag(CompoundTag tag) {
        tag.remove("Upgraded");
        tag.remove("Items");
        tag.remove("Extra");
        tag.remove("CustomName");
    }

    // ---- Container / WorldlyContainer: 주 인벤토리만 노출 (깔때기·비교기) ----

    @Override
    public int[] getSlotsForFace(Direction side) {
        return faceSlots;
    }

    @Override
    public boolean canPlaceItemThroughFace(int index, ItemStack stack, @Nullable Direction direction) {
        return BackpackLayout.canStoreInMain(stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int index, ItemStack stack, Direction direction) {
        return true;
    }

    @Override
    public boolean canPlaceItem(int index, ItemStack stack) {
        return BackpackLayout.canStoreInMain(stack);
    }

    @Override
    public int getContainerSize() {
        return inventory.main.getContainerSize();
    }

    @Override
    public boolean isEmpty() {
        return inventory.main.isEmpty();
    }

    @Override
    public ItemStack getItem(int slot) {
        return inventory.main.getItem(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        return inventory.main.removeItem(slot, amount);
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return inventory.main.removeItemNoUpdate(slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        inventory.main.setItem(slot, stack);
    }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

    @Override
    public void clearContent() {
        inventory.main.clearContent();
    }
}
