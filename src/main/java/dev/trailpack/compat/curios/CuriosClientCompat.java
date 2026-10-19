package dev.trailpack.compat.curios;

import dev.trailpack.Registration;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import top.theillusivec4.curios.api.client.CuriosRendererRegistry;

/** Curios 등 칸에 멘 배낭 그리기 등록. Mods.CURIOS 가 true 일 때만 부른다. */
public final class CuriosClientCompat {
    private CuriosClientCompat() {}

    public static void init(IEventBus modBus) {
        modBus.addListener((FMLClientSetupEvent event) -> event.enqueueWork(() ->
                CuriosRendererRegistry.register(Registration.BACKPACK_ITEM.get(), BackpackCurioRenderer::new)));
    }
}
