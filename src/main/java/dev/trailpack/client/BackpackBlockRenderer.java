package dev.trailpack.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.trailpack.backpack.BackpackVisuals;
import dev.trailpack.block.BackpackBlock;
import dev.trailpack.block.BackpackBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.phys.AABB;

/** 설치된 배낭의 도구걸이에 건 도구·랜턴. 나머지는 블록 모델이 그린다. */
public class BackpackBlockRenderer implements BlockEntityRenderer<BackpackBlockEntity> {

    public BackpackBlockRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(BackpackBlockEntity be, float partialTick, PoseStack pose, MultiBufferSource buffers,
                       int light, int overlay) {
        BackpackVisuals look = be.inventory().visuals();
        BackpackRenderer.renderHung(look, be.getBlockState().getValue(BackpackBlock.FACING), pose, buffers, light,
                overlay, be.getLevel());
    }

    @Override
    public AABB getRenderBoundingBox(BackpackBlockEntity be) {
        return new AABB(be.getBlockPos()).inflate(1.0); // 옆으로 튀어나온 도구까지
    }
}
