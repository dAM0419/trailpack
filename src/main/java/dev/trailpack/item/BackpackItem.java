package dev.trailpack.item;

import dev.trailpack.Registration;
import dev.trailpack.backpack.BackpackLayout;
import net.minecraft.ChatFormatting;
import dev.trailpack.backpack.WornBackpack;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Equipable;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.block.Block;

import java.util.List;

/**
 * 배낭 아이템. 들고 우클릭하면 설치되고(BlockItem), 바닐라 몸 장비 칸에 멘다 (Curios 가 있으면 등 칸에도).
 * 한 번에 하나만 멜 수 있다.
 * 주 인벤토리는 바닐라 minecraft:container 컴포넌트에 저장되므로, 아이템 엔티티가 파괴되면
 * 셜커 상자처럼 내용물을 쏟는다.
 */
public class BackpackItem extends BlockItem implements Equipable {

    public BackpackItem(Block block, Properties properties) {
        super(block, properties);
    }

    /** 셜커 상자·꾸러미 등 다른 수납 아이템 안에 들어가지 않는다. */
    @Override
    public boolean canFitInsideContainerItems() {
        return false;
    }

    @Override
    public void onDestroyed(ItemEntity itemEntity) {
        super.onDestroyed(itemEntity); // 주 인벤토리 쏟기 (바닐라 BlockItem 처리)
        ItemContainerContents extra = itemEntity.getItem().set(Registration.EXTRA.get(), ItemContainerContents.EMPTY);
        if (extra != null) {
            ItemUtils.onContainerDestroyed(itemEntity, extra.nonEmptyItemsCopy());
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        int rows = BackpackLayout.rows(BackpackLayout.isUpgraded(stack));
        tooltip.add(Component.translatable("tooltip.trailpack.capacity", 9, rows).withStyle(ChatFormatting.GRAY));
        long used = stack.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY)
                .nonEmptyStream().count();
        tooltip.add(Component.translatable("tooltip.trailpack.used", used, rows * 9).withStyle(ChatFormatting.DARK_GRAY));
    }

    // ---- 몸 장비 칸 ----

    @Override
    public EquipmentSlot getEquipmentSlot() {
        return EquipmentSlot.CHEST;
    }

    @Override
    public Holder<SoundEvent> getEquipSound() {
        return SoundEvents.ARMOR_EQUIP_LEATHER;
    }

    /** 플레이어만, 몸 칸에만, 그리고 Curios 등 칸에 이미 배낭이 없을 때만 멜 수 있다. */
    @Override
    public boolean canEquip(ItemStack stack, EquipmentSlot armorType, LivingEntity entity) {
        return armorType == EquipmentSlot.CHEST && entity instanceof Player
                && WornBackpack.findInCurios(entity).isEmpty();
    }
}
