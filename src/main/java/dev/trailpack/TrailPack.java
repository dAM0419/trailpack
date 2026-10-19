package dev.trailpack;

import dev.trailpack.client.ClientSetup;
import dev.trailpack.compat.Mods;
import dev.trailpack.compat.curios.CuriosCompat;
import dev.trailpack.event.ServerEvents;
import dev.trailpack.network.BackpackKeyPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.items.wrapper.InvWrapper;
import net.neoforged.neoforge.items.wrapper.SidedInvWrapper;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(TrailPack.MODID)
public final class TrailPack {
    public static final String MODID = "trailpack";
    public static final Logger LOGGER = LoggerFactory.getLogger(MODID);

    public TrailPack(IEventBus modBus, ModContainer container) {
        Registration.register(modBus);
        modBus.addListener(TrailPack::registerCapabilities);
        modBus.addListener(TrailPack::registerPayloads);
        modBus.addListener(TrailPack::buildCreativeTabs);
        NeoForge.EVENT_BUS.register(ServerEvents.class);
        if (Mods.CURIOS) CuriosCompat.init(modBus);
        if (FMLEnvironment.dist == Dist.CLIENT) {
            ClientSetup.init(modBus);
        }
    }

    public static ResourceLocation rl(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }

    private static void registerCapabilities(RegisterCapabilitiesEvent event) {
        // Create 깔때기·슈트 등 아이템 핸들러만 보는 모드용. 주 인벤토리만 노출된다.
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, Registration.BACKPACK_BE.get(),
                (be, side) -> side == null ? new InvWrapper(be) : new SidedInvWrapper(be, side));
    }

    private static void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToServer(BackpackKeyPayload.TYPE, BackpackKeyPayload.STREAM_CODEC, BackpackKeyPayload::handle);
    }

    private static void buildCreativeTabs(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(Registration.BACKPACK_ITEM.get());
            event.accept(Registration.SNACK_POUCH.get());
            event.accept(Registration.TOOL_HOOK.get());
            event.accept(Registration.BEDROLL_STRAP.get());
            event.accept(Registration.ARMOR_POUCH.get());
            Registration.SLEEPING_BAGS.values().forEach(bag -> event.accept(bag.get()));
        }
    }
}
