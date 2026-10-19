package dev.trailpack;

import com.mojang.serialization.Codec;
import dev.trailpack.block.BackpackBlock;
import dev.trailpack.block.BackpackBlockEntity;
import dev.trailpack.block.SleepingBagBlock;
import dev.trailpack.item.AttachmentItem;
import dev.trailpack.item.BackpackItem;
import dev.trailpack.menu.BackpackMenu;
import dev.trailpack.recipe.AttachmentRecipe;
import dev.trailpack.recipe.BackpackUpgradeRecipe;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.Unit;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BedItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.EnumMap;
import java.util.Map;

public final class Registration {
    private Registration() {}

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(TrailPack.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(TrailPack.MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, TrailPack.MODID);
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, TrailPack.MODID);
    public static final DeferredRegister<DataComponentType<?>> COMPONENTS =
            DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, TrailPack.MODID);
    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS =
            DeferredRegister.create(Registries.RECIPE_SERIALIZER, TrailPack.MODID);

    // ---- 데이터 컴포넌트 (주 인벤토리는 바닐라 minecraft:container 를 그대로 쓴다) ----
    /** 셜커 껍질로 강화됨 (있으면 9×3, 없으면 9×1) */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Unit>> UPGRADED =
            COMPONENTS.register("upgraded", () -> DataComponentType.<Unit>builder()
                    .persistent(Unit.CODEC)
                    .networkSynchronized(StreamCodec.unit(Unit.INSTANCE))
                    .build());
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<ItemContainerContents>> EXTRA =
            COMPONENTS.register("extra", () -> DataComponentType.<ItemContainerContents>builder()
                    .persistent(ItemContainerContents.CODEC)
                    .networkSynchronized(ItemContainerContents.STREAM_CODEC)
                    .cacheEncoding()
                    .build());
    /** 걸린 랜턴의 밝기 (0·10·15). 내용물에서 계산되는 값을 동적 조명 모드가 읽기 쉽도록 따로 둔다. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> LANTERN_LIGHT =
            COMPONENTS.register("lantern_light", () -> DataComponentType.<Integer>builder()
                    .persistent(Codec.intRange(0, 15))
                    .networkSynchronized(ByteBufCodecs.VAR_INT)
                    .build());

    // ---- 블록 / 아이템 ----
    public static final DeferredBlock<BackpackBlock> BACKPACK_BLOCK = BLOCKS.register("backpack",
            () -> new BackpackBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BROWN)
                    .strength(0.5F, 2.0F) // 맨손 0.75초 (양털 1.2초보다 빠름). 폭발 저항은 그대로
                    .sound(SoundType.WOOL)
                    .noOcclusion()
                    .pushReaction(PushReaction.DESTROY)
                    .lightLevel(state -> state.getValue(BackpackBlock.LANTERN).light())));

    public static final DeferredItem<BackpackItem> BACKPACK_ITEM = ITEMS.register("backpack",
            () -> new BackpackItem(BACKPACK_BLOCK.get(), new Item.Properties().stacksTo(1)));

    public static final DeferredItem<AttachmentItem> SNACK_POUCH = ITEMS.register("snack_pouch",
            () -> new AttachmentItem(AttachmentItem.Kind.POUCH, new Item.Properties().stacksTo(16)));
    public static final DeferredItem<AttachmentItem> TOOL_HOOK = ITEMS.register("tool_hook",
            () -> new AttachmentItem(AttachmentItem.Kind.HOOK, new Item.Properties().stacksTo(16)));
    public static final DeferredItem<AttachmentItem> BEDROLL_STRAP = ITEMS.register("bedroll_strap",
            () -> new AttachmentItem(AttachmentItem.Kind.STRAP, new Item.Properties().stacksTo(16)));
    public static final DeferredItem<AttachmentItem> ARMOR_POUCH = ITEMS.register("armor_pouch",
            () -> new AttachmentItem(AttachmentItem.Kind.ARMOR, new Item.Properties().stacksTo(16)));

    @SuppressWarnings("DataFlowIssue")
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BackpackBlockEntity>> BACKPACK_BE =
            BLOCK_ENTITIES.register("backpack",
                    () -> BlockEntityType.Builder.of(BackpackBlockEntity::new, BACKPACK_BLOCK.get()).build(null));

    public static final DeferredHolder<MenuType<?>, MenuType<BackpackMenu>> BACKPACK_MENU =
            MENUS.register("backpack", () -> IMenuTypeExtension.create(BackpackMenu::fromNetwork));

    public static final DeferredHolder<RecipeSerializer<?>, SimpleCraftingRecipeSerializer<BackpackUpgradeRecipe>> UPGRADE_SERIALIZER =
            RECIPE_SERIALIZERS.register("backpack_upgrade",
                    () -> new SimpleCraftingRecipeSerializer<>(BackpackUpgradeRecipe::new));
    public static final DeferredHolder<RecipeSerializer<?>, SimpleCraftingRecipeSerializer<AttachmentRecipe>> ATTACHMENT_SERIALIZER =
            RECIPE_SERIALIZERS.register("attachment",
                    () -> new SimpleCraftingRecipeSerializer<>(AttachmentRecipe::new));

    // ---- 태그 ----
    /** 도구걸이에 걸 수 있는 랜턴 (기본: 랜턴, 영혼 랜턴). 걸면 빛을 낸다. */
    public static final TagKey<Item> HANGABLE_LANTERNS = ItemTags.create(TrailPack.rl("hangable_lanterns"));
    /** 도구걸이에 걸 수 있는 도구 (바닐라 도구 태그 + c:tools). 손에 든 것과 맞바꾸는 기준이기도 하다. */
    public static final TagKey<Item> HOOK_TOOLS = ItemTags.create(TrailPack.rl("hook_tools"));

    // ---- 침낭 16색 ----
    public static final Map<DyeColor, DeferredBlock<SleepingBagBlock>> SLEEPING_BAG_BLOCKS = new EnumMap<>(DyeColor.class);
    public static final Map<DyeColor, DeferredItem<BedItem>> SLEEPING_BAGS = new EnumMap<>(DyeColor.class);

    static {
        for (DyeColor color : DyeColor.values()) {
            String name = color.getSerializedName() + "_sleeping_bag";
            DeferredBlock<SleepingBagBlock> block = BLOCKS.register(name, () -> new SleepingBagBlock(color,
                    BlockBehaviour.Properties.of()
                            .mapColor(color.getMapColor())
                            .sound(SoundType.WOOL)
                            .strength(0.2F)
                            .noOcclusion()
                            .ignitedByLava()
                            .pushReaction(PushReaction.DESTROY)));
            SLEEPING_BAG_BLOCKS.put(color, block);
            SLEEPING_BAGS.put(color, ITEMS.register(name, () -> new BedItem(block.get(), new Item.Properties().stacksTo(1))));
        }
    }

    static void register(IEventBus modBus) {
        COMPONENTS.register(modBus);
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
        MENUS.register(modBus);
        RECIPE_SERIALIZERS.register(modBus);
    }
}
