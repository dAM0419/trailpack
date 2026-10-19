package dev.trailpack.compat.curios;

import dev.trailpack.Registration;
import dev.trailpack.item.BackpackItem;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;
import top.theillusivec4.curios.api.type.inventory.IDynamicStackHandler;

/** Curios 가 깔려 있을 때만: 배낭을 등(back) 칸에도 멜 수 있게 한다. Mods.CURIOS 가 true 일 때만 부른다. */
public final class CuriosCompat {
    private CuriosCompat() {}

    public static final String SLOT = "back";

    public static void init(IEventBus modBus) {
        modBus.addListener(CuriosCompat::commonSetup);
    }

    private static void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> CuriosApi.registerCurio(Registration.BACKPACK_ITEM.get(), new BackpackCurio()));
    }

    /** 등 칸의 배낭 스택(살아 있는 참조). 없으면 EMPTY. */
    public static ItemStack findBack(LivingEntity entity) {
        return CuriosApi.getCuriosInventory(entity)
                .flatMap(handler -> handler.getStacksHandler(SLOT))
                .map(stacksHandler -> {
                    IDynamicStackHandler stacks = stacksHandler.getStacks();
                    for (int i = 0; i < stacks.getSlots(); i++) {
                        ItemStack stack = stacks.getStackInSlot(i);
                        if (stack.getItem() instanceof BackpackItem) return stack;
                    }
                    return ItemStack.EMPTY;
                })
                .orElse(ItemStack.EMPTY);
    }

    private static final class BackpackCurio implements ICurioItem {
        @Override
        public boolean canEquipFromUse(SlotContext slotContext, ItemStack stack) {
            return false; // 우클릭은 설치에 쓴다
        }

        /** 배낭은 한 번에 하나만 멘다: 몸 장비 칸에 이미 배낭이 있으면 등 칸에는 못 멘다. */
        @Override
        public boolean canEquip(SlotContext slotContext, ItemStack stack) {
            return !(slotContext.entity().getItemBySlot(EquipmentSlot.CHEST).getItem() instanceof BackpackItem);
        }
    }
}
