package dev.trailpack.compat.jei;

import dev.trailpack.Registration;
import dev.trailpack.TrailPack;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Unit;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapedRecipePattern;
import net.minecraft.world.level.ItemLike;

import java.util.List;
import java.util.Map;

/**
 * JEI 연동: 직접 판정하는 조합법(셜커 강화, 부착물 4종)을 JEI 조합 화면에 보이게 한다.
 * 여기 등록하는 조합법은 보기 전용이다. 실제 조합은 recipe/ 의 CustomRecipe 가 처리한다.
 * JEI 가 있을 때만 JEI 가 이 클래스를 불러온다.
 */
@JeiPlugin
public class TrailPackJeiPlugin implements IModPlugin {
    private static final ResourceLocation UID = TrailPack.rl("jei_plugin");

    @Override
    public ResourceLocation getPluginUid() {
        return UID;
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        ItemStack upgraded = new ItemStack(Registration.BACKPACK_ITEM.get());
        upgraded.set(Registration.UPGRADED.get(), Unit.INSTANCE);

        List<RecipeHolder<CraftingRecipe>> recipes = List.of(
                // 배낭 위아래 셜커 껍질 → 9×3 배낭
                shaped("backpack_upgrade", upgraded, Map.of(
                                'S', Ingredient.of(Items.SHULKER_SHELL),
                                'B', Ingredient.of(Registration.BACKPACK_ITEM.get())),
                        "S", "B", "S"),
                // 가죽 기준 실의 위치로 부착물이 정해진다
                attachment("snack_pouch", Registration.SNACK_POUCH.get(), "SL"),
                attachment("tool_hook", Registration.TOOL_HOOK.get(), "LS"),
                attachment("bedroll_strap", Registration.BEDROLL_STRAP.get(), "S", "L"),
                attachment("armor_pouch", Registration.ARMOR_POUCH.get(), "L", "S"));
        registration.addRecipes(RecipeTypes.CRAFTING, recipes);
    }

    private static RecipeHolder<CraftingRecipe> attachment(String name, ItemLike result, String... pattern) {
        return shaped(name, new ItemStack(result), Map.of(
                'S', Ingredient.of(Items.STRING),
                'L', Ingredient.of(Items.LEATHER)), pattern);
    }

    private static RecipeHolder<CraftingRecipe> shaped(String name, ItemStack result, Map<Character, Ingredient> key,
                                                       String... pattern) {
        ShapedRecipe recipe = new ShapedRecipe("", CraftingBookCategory.MISC, ShapedRecipePattern.of(key, pattern), result);
        return new RecipeHolder<>(TrailPack.rl("jei/" + name), recipe);
    }
}
