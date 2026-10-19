package dev.trailpack.compat.ldl;

import dev.lambdaurora.lambdynlights.api.DynamicLightsContext;
import dev.lambdaurora.lambdynlights.api.DynamicLightsInitializer;
import dev.lambdaurora.lambdynlights.api.item.ItemLightSource;
import dev.lambdaurora.lambdynlights.api.item.ItemLightSourceManager;
import dev.trailpack.Registration;
import dev.trailpack.backpack.LanternType;
import net.minecraft.advancements.critereon.ItemPredicate;
import net.minecraft.core.component.DataComponentPredicate;

/**
 * LambDynamicLights 연동: 랜턴을 단 배낭 아이템을 광원 아이템으로 등록한다.
 * LDL 은 손·갑옷 칸(몸 칸에 멘 배낭)과 함께 Curios 칸의 아이템도 같은 방식으로 계산하므로,
 * 등에 멘 배낭이 손에 든 랜턴과 똑같은 밝기(랜턴 15, 영혼 랜턴 10)로 빛난다.
 * 배낭 아이템의 trailpack:lantern_light 값(BackpackInventory 가 갱신)으로 구분한다.
 * neoforge.mods.toml 의 "yumi:entrypoints" 로 LDL 이 있을 때만 불러온다.
 */
public class TrailPackLdlInit implements DynamicLightsInitializer {

    @Override
    public void onInitializeDynamicLights(DynamicLightsContext context) {
        context.itemLightSourceManager().onRegisterEvent().register(ctx -> {
            for (LanternType type : new LanternType[]{LanternType.LANTERN, LanternType.SOUL}) {
                ItemPredicate predicate = ItemPredicate.Builder.item()
                        .of(Registration.BACKPACK_ITEM.get())
                        .hasComponents(DataComponentPredicate.builder()
                                .expect(Registration.LANTERN_LIGHT.get(), type.light())
                                .build())
                        .build();
                ctx.register(new ItemLightSource(predicate, type.light()));
            }
        });
    }

    @Override
    @SuppressWarnings("removal") // 인터페이스가 아직 이 메서드 구현을 요구한다
    public void onInitializeDynamicLights(ItemLightSourceManager itemLightSourceManager) {
        // 쓰지 않음 (위의 context 버전을 쓴다)
    }
}
