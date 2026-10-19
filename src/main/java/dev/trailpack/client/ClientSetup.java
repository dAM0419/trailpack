package dev.trailpack.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.trailpack.Registration;
import dev.trailpack.TrailPack;
import dev.trailpack.network.BackpackKeyPayload;
import dev.trailpack.network.BackpackKeyPayload.Action;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;
import dev.trailpack.backpack.BackpackLayout;
import dev.trailpack.backpack.WornBackpack;
import dev.trailpack.compat.Mods;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import dev.trailpack.compat.curios.CuriosClientCompat;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.resources.PlayerSkin;

public final class ClientSetup {
    private ClientSetup() {}

    private static final String CATEGORY = "key.categories.trailpack";

    /** B: 열기. 갑옷 교환은 웅크리기 + 바닐라 손 바꾸기 키(기본 F) — clientTickPre 참고. */
    public static final KeyMapping BACKPACK_KEY = key("key.trailpack.backpack", GLFW.GLFW_KEY_B);
    /** G: 오른쪽 부착물 / 웅크리기+G: 왼쪽 부착물 */
    public static final KeyMapping SIDE_KEY = key("key.trailpack.side", GLFW.GLFW_KEY_G);
    /** V: 위 부착물 (침낭 꺼내기·넣기) */
    public static final KeyMapping TOP_KEY = key("key.trailpack.top", GLFW.GLFW_KEY_V);

    private static KeyMapping key(String name, int code) {
        return new KeyMapping(name, KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, code, CATEGORY);
    }

    public static void init(IEventBus modBus) {
        modBus.addListener(ClientSetup::registerKeys);
        modBus.addListener(ClientSetup::registerScreens);
        modBus.addListener(ClientSetup::registerRenderers);
        modBus.addListener(ClientSetup::registerItemExtensions);
        modBus.addListener(ClientSetup::registerGuiLayers);
        modBus.addListener(ClientSetup::addLayers);
        if (Mods.CURIOS) CuriosClientCompat.init(modBus);
        NeoForge.EVENT_BUS.addListener(ClientSetup::clientTick);
        NeoForge.EVENT_BUS.addListener(ClientSetup::clientTickPre);
    }

    private static void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(BACKPACK_KEY);
        event.register(SIDE_KEY);
        event.register(TOP_KEY);
    }

    private static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(Registration.BACKPACK_MENU.get(), BackpackScreen::new);
    }

    private static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(Registration.BACKPACK_BE.get(), BackpackBlockRenderer::new);
    }

    /** 배낭 아이템은 입체로 그린다 (models/item/backpack.json 이 builtin/entity). */
    private static void registerItemExtensions(RegisterClientExtensionsEvent event) {
        event.registerItem(new IClientItemExtensions() {
            private BackpackItemRenderer renderer;

            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (renderer == null) renderer = new BackpackItemRenderer();
                return renderer;
            }
        }, Registration.BACKPACK_ITEM.get());
    }

    private static void registerGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.HOTBAR, TrailPack.rl("backpack_slots"), BackpackHud::render);
    }

    /** 몸 장비 칸에 멘 배낭을 플레이어 등에 그린다. */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void addLayers(EntityRenderersEvent.AddLayers event) {
        for (PlayerSkin.Model skin : event.getSkins()) {
            if (event.getSkin(skin) instanceof LivingEntityRenderer renderer
                    && renderer.getModel() instanceof HumanoidModel<?>) {
                renderer.addLayer(new BackpackBodyLayer(renderer));
            }
        }
    }

    private static boolean backpackHeld, sideHeld, topHeld, swapHeld;

    /**
     * 갑옷 교환: 웅크린 채 손 바꾸기 키(기본 F). 바닐라가 키를 처리하기 전(틱 시작)에 가로채서,
     * 멘 배낭에 갑옷주머니가 있을 때만 바닐라 손 바꾸기 대신 갑옷 교환을 보낸다. 그 밖에는 바닐라 그대로.
     * (예전 웅크리기+B 는 Ctrl+B 내레이터 단축키와 겹쳐 웅크리기를 Ctrl 로 둔 플레이어가 쓸 수 없었다.)
     */
    private static void clientTickPre(ClientTickEvent.Pre event) {
        Minecraft mc = Minecraft.getInstance();
        KeyMapping swap = mc.options.keySwapOffhand;
        boolean held = swap.isDown();
        if (mc.player != null && mc.screen == null && mc.player.isShiftKeyDown() && hasArmorPouch(mc.player)) {
            boolean fresh = freshPress(swap, swapHeld); // 클릭을 모두 소비 → 바닐라 손 바꾸기가 일어나지 않는다
            if (fresh) send(Action.ARMOR);
        }
        swapHeld = held;
    }

    private static boolean hasArmorPouch(net.minecraft.world.entity.player.Player player) {
        ItemStack backpack = WornBackpack.find(player);
        if (backpack.isEmpty()) return false;
        NonNullList<ItemStack> extra = NonNullList.withSize(BackpackLayout.EXTRA_SIZE, ItemStack.EMPTY);
        backpack.getOrDefault(Registration.EXTRA.get(), ItemContainerContents.EMPTY).copyInto(extra);
        return !extra.get(BackpackLayout.ATT_ARMOR).isEmpty();
    }

    /**
     * 키를 누르고 있으면 OS 키 반복 때문에 consumeClick 이 계속 들어온다 (0.5초 뒤부터 초당 수십 번).
     * 그래서 모든 키는 "새로 눌렀을 때" 한 번만 보낸다. G 는 뗄 때도 알린다 (간식 먹기 취소).
     */
    private static void clientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        boolean ready = mc.player != null && mc.screen == null;

        boolean backpackNew = freshPress(BACKPACK_KEY, backpackHeld);
        backpackHeld = BACKPACK_KEY.isDown();
        if (ready && backpackNew) send(Action.OPEN);

        boolean sideNew = freshPress(SIDE_KEY, sideHeld);
        boolean sideDown = ready && SIDE_KEY.isDown();
        boolean sideSent = ready && sideNew;
        if (sideSent) send(mc.player.isShiftKeyDown() ? Action.LEFT : Action.RIGHT);
        // 뗐거나 화면이 열렸거나, 한 틱 안에 눌렀다 뗐으면 취소를 알린다
        if (mc.player != null && !sideDown && (sideHeld || sideSent)) send(Action.SIDE_RELEASED);
        sideHeld = sideDown;

        boolean topNew = freshPress(TOP_KEY, topHeld);
        topHeld = TOP_KEY.isDown();
        if (ready && topNew) send(Action.TOP);
    }

    /** 이번 틱에 새로 눌렸는가 (지난 틱에 이미 눌려 있었으면 키 반복이므로 무시). */
    private static boolean freshPress(KeyMapping key, boolean heldLastTick) {
        boolean clicked = false;
        while (key.consumeClick()) clicked = true;
        return clicked && !heldLastTick;
    }

    private static void send(Action action) {
        PacketDistributor.sendToServer(new BackpackKeyPayload(action));
    }
}
