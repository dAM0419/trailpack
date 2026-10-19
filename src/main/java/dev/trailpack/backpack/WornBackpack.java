package dev.trailpack.backpack;

import dev.trailpack.Registration;
import dev.trailpack.compat.Mods;
import dev.trailpack.compat.curios.CuriosCompat;
import dev.trailpack.item.BackpackItem;
import dev.trailpack.menu.BackpackMenu;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentEffectComponents;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

/** 멘 배낭(몸 장비 칸, 또는 Curios 등 칸) 찾기, 열기, 갑옷 교환. 부착물 키 동작은 AttachmentActions. */
public final class WornBackpack {
    private WornBackpack() {}

    private static final double VIEW_DISTANCE_SQR = 8.0 * 8.0;

    /** 멘 배낭 스택(살아 있는 참조): 몸 장비 칸 → Curios 등 칸 순서. 없으면 EMPTY. */
    public static ItemStack find(LivingEntity entity) {
        ItemStack chest = entity.getItemBySlot(EquipmentSlot.CHEST);
        if (chest.getItem() instanceof BackpackItem) return chest;
        return findInCurios(entity);
    }

    /** Curios 등 칸의 배낭. Curios 가 없으면 항상 EMPTY. */
    public static ItemStack findInCurios(LivingEntity entity) {
        return Mods.CURIOS ? CuriosCompat.findBack(entity) : ItemStack.EMPTY;
    }

    /** 멘 배낭의 도구걸이에 걸린 랜턴 (오른쪽 우선, 없으면 EMPTY). 셰이더 손 조명용. */
    public static ItemStack hangingLantern(LivingEntity entity) {
        ItemStack backpack = find(entity);
        if (backpack.isEmpty() || !backpack.has(Registration.EXTRA.get())) return ItemStack.EMPTY;
        BackpackVisuals look = BackpackVisuals.of(backpack);
        if (BackpackLayout.isLantern(look.rightHung())) return look.rightHung();
        if (BackpackLayout.isLantern(look.leftHung())) return look.leftHung();
        return ItemStack.EMPTY;
    }

    /**
     * 멘 배낭 화면 열기. viewer == owner 이면 부착물까지, 아니면 주 인벤토리만 보인다.
     * 배낭이 등 칸에서 빠지거나(벗기·사망 등) 거리가 멀어지면 화면이 닫힌다.
     */
    public static void open(ServerPlayer viewer, LivingEntity owner) {
        ItemStack stack = find(owner);
        if (stack.isEmpty()) return;
        boolean full = viewer == owner;
        BackpackInventory inventory = BackpackInventory.acquire(stack, owner);
        boolean opened = BackpackMenu.open(viewer, inventory, full, stack.getHoverName(),
                player -> isStillValid(player, owner, stack), () -> BackpackInventory.release(stack));
        if (!opened) {
            BackpackInventory.release(stack); // 열리지 않았으면 되돌림
            return;
        }
        owner.level().playSound(null, owner.getX(), owner.getY(), owner.getZ(),
                SoundEvents.ARMOR_EQUIP_LEATHER.value(), SoundSource.PLAYERS, 0.6F, 1.1F);
    }

    private static boolean isStillValid(Player viewer, LivingEntity owner, ItemStack stack) {
        if (!owner.isAlive() || owner.isRemoved()) return false;
        if (find(owner) != stack) return false;
        return viewer == owner || viewer.distanceToSqr(owner) <= VIEW_DISTANCE_SQR;
    }

    /**
     * 웅크리기 + 손 바꾸기 키(기본 F). 갑옷주머니 4칸과 바닐라 장비 칸을 칸끼리 맞바꾼다. 빈 칸끼리도 교환한다.
     * 바닐라 칸에 귀속 저주 장비가 있으면 그 칸만 건너뛴다. 갑옷주머니가 없으면 아무 일도 없다.
     * 배낭을 몸 칸에 멨으면 몸 칸은 교환하지 않는다 (배낭은 벗겨지지 않고, 주머니의 몸통 갑옷은 그대로 남는다).
     */
    public static void swapArmor(ServerPlayer player) {
        ItemStack stack = find(player);
        if (stack.isEmpty()) return;
        BackpackInventory inventory = BackpackInventory.access(stack, player);
        if (!inventory.hasAttachment(BackpackLayout.ATT_ARMOR)) return;

        boolean changed = false;
        for (int i = 0; i < BackpackLayout.ARMOR_SLOTS.length; i++) {
            EquipmentSlot slot = BackpackLayout.ARMOR_SLOTS[i];
            int index = BackpackLayout.ARMOR_FIRST + i;
            ItemStack worn = player.getItemBySlot(slot);
            ItemStack stored = inventory.extra.getItem(index);
            if (worn.isEmpty() && stored.isEmpty()) continue;
            if (worn.getItem() instanceof BackpackItem) continue;
            if (!player.isCreative() && EnchantmentHelper.has(worn, EnchantmentEffectComponents.PREVENT_ARMOR_CHANGE)) {
                continue;
            }
            player.setItemSlot(slot, stored);
            inventory.extra.setItem(index, worn);
            changed = true;
        }
        if (changed) {
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.ARMOR_EQUIP_GENERIC.value(), SoundSource.PLAYERS, 1.0F, 1.0F);
        }
    }
}
