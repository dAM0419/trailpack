package dev.trailpack.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.trailpack.backpack.BackpackVisuals;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/**
 * 배낭 아이템 (인벤토리 아이콘·손에 든 모습·바닥에 떨어진 모습).
 * 따로 그린 그림 없이, 등에 멘 모습과 같은 모양을 지금 부착 상태 그대로 입체로 그린다.
 * 화면별 각도·크기는 models/item/backpack.json 의 display 에 있다 (GUI: 오른쪽 앞에서 비스듬히).
 */
public class BackpackItemRenderer extends BlockEntityWithoutLevelRenderer {

    // 기본 배낭(부착물 없음)의 중심 (블록 px). 크기도 기본 배낭이 칸을 채우도록 맞춘다 (backpack.json gui 0.72).
    // 부착물이 붙어도 크기·위치는 그대로라, 늘어난 부분은 칸 밖으로 조금 나갈 수 있다.
    private static final float CENTER_X = 8.0F / 16F;
    private static final float CENTER_Y = 8.75F / 16F;
    private static final float CENTER_Z = 8.15F / 16F;

    public BackpackItemRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack pose, MultiBufferSource buffers,
                             int light, int overlay) {
        pose.pushPose();
        // 아이템 렌더러가 (0.5, 0.5, 0.5) 를 중심으로 display 변환을 적용하므로 배낭 중심을 거기에 맞춘다
        pose.translate(0.5F - CENTER_X, 0.5F - CENTER_Y, 0.5F - CENTER_Z);
        BackpackRenderer.renderAll(BackpackVisuals.of(stack), Direction.NORTH, pose, buffers, light, overlay,
                Minecraft.getInstance().level);
        pose.popPose();
    }
}
