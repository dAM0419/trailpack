package dev.trailpack.recipe;

import dev.trailpack.Registration;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

/**
 * 부착물 4종. 가죽을 기준으로 실의 위치가 결과를 정한다.
 * 바닐라 모양 조합법은 좌우 반전도 받아들이므로(실 왼쪽 = 실 오른쪽) 직접 판정한다.
 * <pre>
 * 실 가죽 → 간식 주머니     가죽 실 → 도구걸이
 * 실 / 가죽(세로) → 침낭끈   가죽 / 실(세로) → 갑옷주머니
 * </pre>
 */
public class AttachmentRecipe extends CustomRecipe {

    public AttachmentRecipe(CraftingBookCategory category) {
        super(category);
    }

    private static ItemStack result(CraftingInput input) {
        if (input.ingredientCount() != 2 || input.size() != 2) return ItemStack.EMPTY;
        ItemStack first = input.getItem(0);
        ItemStack second = input.getItem(1);
        boolean stringFirst = first.is(Items.STRING) && second.is(Items.LEATHER);
        boolean leatherFirst = first.is(Items.LEATHER) && second.is(Items.STRING);
        if (!stringFirst && !leatherFirst) return ItemStack.EMPTY;
        if (input.width() == 2 && input.height() == 1) {
            return new ItemStack(stringFirst ? Registration.SNACK_POUCH.get() : Registration.TOOL_HOOK.get());
        }
        if (input.width() == 1 && input.height() == 2) {
            return new ItemStack(stringFirst ? Registration.BEDROLL_STRAP.get() : Registration.ARMOR_POUCH.get());
        }
        return ItemStack.EMPTY;
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        return !result(input).isEmpty();
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        return result(input);
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 2;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return Registration.ATTACHMENT_SERIALIZER.get();
    }
}
