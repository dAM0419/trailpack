package dev.trailpack.backpack;

import dev.trailpack.Registration;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * 배낭 하나의 내용물(주 인벤토리 + 부착물 칸).
 * 설치된 배낭은 블록 엔티티가, 멘 배낭은 아이템 스택이 저장소(Host)가 된다.
 */
public final class BackpackInventory {

    /** 내용이 바뀌었을 때 저장하고, 떨어뜨릴 아이템을 처리하는 쪽. */
    public interface Host {
        void onChanged(BackpackInventory inventory);

        void drop(ItemStack stack);
    }

    private final boolean upgraded;
    public final SimpleContainer main;
    public final SimpleContainer extra;
    @Nullable
    private final Host host;
    private boolean spilling;

    public BackpackInventory(boolean upgraded, @Nullable Host host) {
        this.upgraded = upgraded;
        this.host = host;
        this.main = new SimpleContainer(rows() * BackpackLayout.COLUMNS) {
            @Override
            public boolean canPlaceItem(int slot, ItemStack stack) {
                return BackpackLayout.canStoreInMain(stack);
            }
        };
        this.extra = new SimpleContainer(BackpackLayout.EXTRA_SIZE) {
            @Override
            public boolean canPlaceItem(int slot, ItemStack stack) {
                return BackpackLayout.isValidExtra(slot, stack, getItems());
            }
        };
        this.main.addListener(c -> changed());
        this.extra.addListener(c -> {
            spillOrphans();
            changed();
        });
    }

    public boolean upgraded() {
        return upgraded;
    }

    public int rows() {
        return BackpackLayout.rows(upgraded);
    }

    public boolean hasAttachment(int attachment) {
        return !extra.getItem(attachment).isEmpty();
    }

    public BackpackVisuals visuals() {
        return BackpackVisuals.of(extra.getItems());
    }

    public boolean isEmpty() {
        return main.isEmpty() && extra.isEmpty();
    }

    private void changed() {
        if (host != null) host.onChanged(this);
    }

    /** 부착물이 빠지거나 바뀌어 더는 들어갈 수 없는 내용물을 떨어뜨린다. 서버(host 있음)에서만. */
    private void spillOrphans() {
        if (host == null || spilling) return;
        spilling = true;
        try {
            for (int i = BackpackLayout.ATTACHMENT_COUNT; i < BackpackLayout.EXTRA_SIZE; i++) {
                ItemStack stack = extra.getItem(i);
                if (!stack.isEmpty() && !BackpackLayout.isValidExtra(i, stack, extra.getItems())) {
                    extra.getItems().set(i, ItemStack.EMPTY);
                    host.drop(stack);
                }
            }
        } finally {
            spilling = false;
        }
    }

    // ------------------------------------------------------------------
    // 아이템 스택 저장
    // ------------------------------------------------------------------

    public static BackpackInventory load(ItemStack stack, @Nullable Host host) {
        BackpackInventory inv = new BackpackInventory(BackpackLayout.isUpgraded(stack), host);
        stack.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY).copyInto(inv.main.getItems());
        stack.getOrDefault(Registration.EXTRA.get(), ItemContainerContents.EMPTY).copyInto(inv.extra.getItems());
        return inv;
    }

    public void writeTo(ItemStack stack) {
        stack.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(main.getItems()));
        stack.set(Registration.EXTRA.get(), ItemContainerContents.fromItems(extra.getItems()));
        int light = visuals().lantern().light();
        if (light > 0) stack.set(Registration.LANTERN_LIGHT.get(), light);
        else stack.remove(Registration.LANTERN_LIGHT.get());
    }

    private record StackHost(ItemStack stack, LivingEntity owner) implements Host {
        @Override
        public void onChanged(BackpackInventory inventory) {
            if (!stack.isEmpty()) inventory.writeTo(stack);
        }

        @Override
        public void drop(ItemStack dropped) {
            if (!owner.level().isClientSide) owner.spawnAtLocation(dropped);
        }
    }

    // ------------------------------------------------------------------
    // 멘 배낭 공유: 같은 스택을 여러 명이 동시에 열면 같은 인벤토리를 보게 한다 (복사 방지).
    // 서버 스레드에서만 쓴다. ItemStack 은 equals 를 재정의하지 않으므로 객체 동일성으로 구분된다.
    // ------------------------------------------------------------------

    private static final class Shared {
        final BackpackInventory inventory;
        int users;

        Shared(BackpackInventory inventory) {
            this.inventory = inventory;
        }
    }

    private static final Map<ItemStack, Shared> SHARED = new WeakHashMap<>();

    /** 화면을 열 때. 반드시 {@link #release(ItemStack)} 과 짝을 맞춘다. */
    public static BackpackInventory acquire(ItemStack stack, LivingEntity owner) {
        Shared shared = SHARED.computeIfAbsent(stack, s -> new Shared(load(s, new StackHost(s, owner))));
        shared.users++;
        return shared.inventory;
    }

    public static void release(ItemStack stack) {
        Shared shared = SHARED.get(stack);
        if (shared != null && --shared.users <= 0) {
            SHARED.remove(stack);
        }
    }

    /** 화면 없이 잠깐 다룰 때 (부착물 키, 갑옷 교환). 열려 있으면 그 인벤토리를 쓴다. */
    public static BackpackInventory access(ItemStack stack, LivingEntity owner) {
        Shared shared = SHARED.get(stack);
        return shared != null ? shared.inventory : load(stack, new StackHost(stack, owner));
    }
}
