package dev.trailpack.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.trailpack.Registration;
import dev.trailpack.backpack.BackpackLayout;
import dev.trailpack.backpack.BackpackVisuals;
import dev.trailpack.block.BackpackBlock;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * 배낭 그리기 공용 코드. 설치된 블록, 등에 멘 모습, 인벤토리 아이콘이 모두 이것을 쓴다.
 * 좌표는 블록 모델 공간(0~1, 북쪽 = 등쪽 면). 착용자 오른쪽 = +x.
 *
 * <ul>
 *   <li>몸통·간식 주머니·갑옷주머니·침낭 롤: 블록 상태 모델</li>
 *   <li>도구걸이에 건 도구: 손에 든 것과 같은 아이템 모델·같은 크기, 손잡이가 위, 옆면에서 살짝 띄워 앞쪽으로 35° 비스듬히</li>
 *   <li>도구걸이에 건 랜턴: 바닐라 랜턴 블록 모델을 옆면에 그대로 붙임</li>
 * </ul>
 */
public final class BackpackRenderer {
    private BackpackRenderer() {}

    /** 등에 멜 때 배낭 전체를 줄이는 비율. */
    public static final float WORN_SCALE = 0.8F;

    // 도구: 손잡이 선(그림의 대각선)이 지나는 위치(블록 단위)와 회전 — tools/preview_icon.py 와 같은 값.
    // 손잡이 선이 옆면(x=13)에서 2px 떨어진다 (칼날·자루가 몸통·덮개에 닿지 않는 최소 간격).
    // 손잡이 끝이 침낭 윗면(y=19.2)보다 아래에 오게 낮춰, 침낭과 겹친 부분이 밖으로 보이지 않게 한다.
    private static final float TOOL_X = 15.0F / 16F;
    private static final float TOOL_Y = 7.0F / 16F;
    private static final float TOOL_Z = 8.2F / 16F;
    private static final float TOOL_TURN = 55F;      // = 90° - 기울기 35°
    private static final float TOOL_SPIN = -135F;    // 손잡이(그림 왼쪽 아래)를 위로
    // 랜턴: 블록 모델을 옆으로 8px, 위로 3px 옮긴다
    private static final float LANTERN_X = 8F / 16F;
    private static final float LANTERN_Y = 3F / 16F;

    /**
     * 등에 멘 모습 (몸 장비 칸·Curios 등 칸 공용). 몸통을 따라 움직이고, 등쪽 면이 플레이어 등에 닿는다.
     */
    public static void renderOnBack(ItemStack stack, LivingEntity entity, EntityModel<?> model, PoseStack pose,
                                    MultiBufferSource buffers, int light) {
        pose.pushPose();
        if (model instanceof HumanoidModel<?> humanoid) {
            humanoid.body.translateAndRotate(pose); // 웅크리기 등 몸통 기울기를 따라감
        }
        // 모델 공간: y 아래, +z 등쪽. 몸통 중앙 높이(6px), 등 표면(2px)으로 이동
        pose.translate(0.0F, 6.0F / 16.0F, 2.0F / 16.0F);
        // 블록 공간으로 뒤집기: y 위, z 반전 (행렬식 양수 유지)
        pose.scale(WORN_SCALE, -WORN_SCALE, -WORN_SCALE);
        // 블록(0~1)을 가운데로. 등쪽 면(z=0.75)이 원점에 오게 → 배낭은 등 뒤(-z)로 나감
        pose.translate(-0.5F, -0.5F, -0.75F);
        // 등쪽 면이 플레이어 등에 닿도록 남쪽을 보게 그린다 (위 좌표 뒤집기와 짝)
        renderAll(BackpackVisuals.of(stack), Direction.SOUTH, pose, buffers, light, OverlayTexture.NO_OVERLAY,
                entity.level());
        pose.popPose();
    }

    /** 블록 상태의 y 회전(북 0, 동 90, 남 180, 서 270)과 같은 각도. */
    public static float facingDegrees(Direction facing) {
        return switch (facing) {
            case EAST -> 90F;
            case SOUTH -> 180F;
            case WEST -> 270F;
            default -> 0F;
        };
    }

    /** 블록 모델 + 매단 것 전부 (착용 모습, 아이콘). */
    public static void renderAll(BackpackVisuals look, Direction facing, PoseStack pose, MultiBufferSource buffers,
                                 int light, int overlay, @Nullable Level level) {
        BlockState state = look.apply(Registration.BACKPACK_BLOCK.get().defaultBlockState()
                .setValue(BackpackBlock.FACING, facing));
        Minecraft.getInstance().getBlockRenderer().renderSingleBlock(state, pose, buffers, light, overlay);
        renderHung(look, facing, pose, buffers, light, overlay, level);
    }

    /** 도구걸이에 건 도구·랜턴만 (설치된 블록은 블록 모델을 월드가 그리므로 이것만 쓴다). */
    public static void renderHung(BackpackVisuals look, Direction facing, PoseStack pose, MultiBufferSource buffers,
                                  int light, int overlay, @Nullable Level level) {
        if (look.rightHung().isEmpty() && look.leftHung().isEmpty()) return;
        pose.pushPose();
        pose.translate(0.5F, 0F, 0.5F);
        pose.mulPose(Axis.YP.rotationDegrees(-facingDegrees(facing)));
        pose.translate(-0.5F, 0F, -0.5F);
        renderSide(look.rightHung(), true, pose, buffers, light, overlay, level);
        renderSide(look.leftHung(), false, pose, buffers, light, overlay, level);
        pose.popPose();
    }

    /**
     * 3인칭으로 손에 들었을 때 그 아이템 모델이 쓰는 크기 (바닐라 도구는 0.85).
     * 배낭은 등에서 WORN_SCALE 로 줄어 그려지므로, 그만큼 나눠 주면 등에 멘 모습에서 손에 든 것과 같은 크기가 된다.
     */
    private static float heldScale(ItemStack stack, @Nullable Level level) {
        BakedModel model = Minecraft.getInstance().getItemRenderer().getModel(stack, level, null, 0);
        @SuppressWarnings("deprecation")
        float scale = model.getTransforms().getTransform(ItemDisplayContext.THIRD_PERSON_RIGHT_HAND).scale.x();
        return scale > 0F ? scale : 0.85F;
    }

    private static void renderSide(ItemStack stack, boolean right, PoseStack pose, MultiBufferSource buffers,
                                   int light, int overlay, @Nullable Level level) {
        if (stack.isEmpty()) return;
        pose.pushPose();
        if (BackpackLayout.isLantern(stack) && stack.getItem() instanceof BlockItem blockItem) {
            pose.translate(right ? LANTERN_X : -LANTERN_X, LANTERN_Y, 0F);
            Minecraft.getInstance().getBlockRenderer()
                    .renderSingleBlock(blockItem.getBlock().defaultBlockState(), pose, buffers, light, overlay);
        } else {
            pose.translate(right ? TOOL_X : 1F - TOOL_X, TOOL_Y, TOOL_Z);
            pose.mulPose(Axis.YP.rotationDegrees(right ? TOOL_TURN : -TOOL_TURN));
            pose.mulPose(Axis.ZP.rotationDegrees(TOOL_SPIN));
            float scale = heldScale(stack, level) / WORN_SCALE;
            pose.scale(scale, scale, scale);
            // NONE: 아이템 모델을 회전 없이 그대로 (손에 들 때와 같은 모델). 중심은 렌더러가 맞춘다.
            Minecraft.getInstance().getItemRenderer().renderStatic(stack, ItemDisplayContext.NONE, light, overlay,
                    pose, buffers, level, 0);
        }
        pose.popPose();
    }
}
