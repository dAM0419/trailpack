package dev.trailpack.recipe;

import dev.trailpack.Registration;
import dev.trailpack.backpack.BackpackLayout;
import dev.trailpack.item.BackpackItem;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import net.minecraft.util.Unit;

/**
 * 배낭 위아래에 셜커 껍질 → 곧바로 9×3 (셜커 상자와 같은 27칸). 한 번만 가능하다.
 * 내용물과 부착물은 그대로 옮긴다.
 */
public class BackpackUpgradeRecipe extends CustomRecipe {

    public BackpackUpgradeRecipe(CraftingBookCategory category) {
        super(category);
    }

    /** 맞으면 배낭 칸 번호, 아니면 -1. CraftingInput 은 내용물 영역으로 잘려 있다. */
    private static int findBackpack(CraftingInput input) {
        int width = input.width();
        int backpack = -1;
        int count = 0;
        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (stack.isEmpty()) continue;
            count++;
            if (stack.getItem() instanceof BackpackItem) {
                if (backpack != -1) return -1;
                backpack = i;
            } else if (!stack.is(Items.SHULKER_SHELL)) {
                return -1;
            }
        }
        if (backpack < 0 || count != 3) return -1;
        int x = backpack % width;
        int y = backpack / width;
        if (y < 1 || y + 1 >= input.height()) return -1;
        if (!input.getItem(x + (y - 1) * width).is(Items.SHULKER_SHELL)) return -1;
        if (!input.getItem(x + (y + 1) * width).is(Items.SHULKER_SHELL)) return -1;
        return BackpackLayout.isUpgraded(input.getItem(backpack)) ? -1 : backpack;
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        return findBackpack(input) >= 0;
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        int index = findBackpack(input);
        if (index < 0) return ItemStack.EMPTY;
        ItemStack result = input.getItem(index).copyWithCount(1);
        result.set(Registration.UPGRADED.get(), Unit.INSTANCE);
        return result;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return height >= 3;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return Registration.UPGRADE_SERIALIZER.get();
    }
}
