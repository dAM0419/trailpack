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
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.world.item.ItemDisplayContext;
import org.joml.Matrix3f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
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

    /** 평평한 바닐라식 손 도구(handheld: 손 변환 회전 0,-90,55)인가. 이것들은 확인된 기존 방식으로 건다. */
    private static boolean isHandheldRotation(BakedModel model) {
        @SuppressWarnings("deprecation")
        Vector3f r = model.getTransforms().getTransform(ItemDisplayContext.THIRD_PERSON_RIGHT_HAND).rotation;
        return Math.abs(r.x()) < 1F && Math.abs(r.y() + 90F) < 1F && Math.abs(r.z() - 55F) < 1F;
    }

    /** 표시 컨텍스트별로 모델이 갈리는 경우(손엔 3D, 인벤토리엔 그림 등) 그 컨텍스트가 실제로 쓰는 모델. */
    private static BakedModel perspective(BakedModel model, ItemDisplayContext context) {
        return model.applyTransform(context, new PoseStack(), false);
    }

    /**
     * 걸이에 그릴 방법을 정해 그린다.
     * <ol>
     *   <li>평평한 바닐라식 손 도구(회전이 handheld 이고, 실제로 그려질 모델이 1px 두께 그림): 기존 방식 (false 반환)</li>
     *   <li>전용 렌더러인데 면이 있는 것(Create 렌치처럼 기본 모델 + 움직이는 부품): 면으로 형태를 재고, 그리기는 전용 렌더러에 맡김</li>
     *   <li>전용 렌더러인데 면이 없는 것(삼지창): 인벤토리용 일반 모델을 재서 직접 그림</li>
     *   <li>일반 모델: 인벤토리(GUI)에서 쓰는 모델을 재서 직접 그림 — 손에는 긴 3D 모델, 인벤토리엔 그림을 쓰는 모드 무기도 그림으로 걸림</li>
     * </ol>
     */
    private static boolean renderMeasured(ItemStack stack, BakedModel model, boolean right, PoseStack pose,
                                          MultiBufferSource buffers, int light, int overlay, @Nullable Level level) {
        ItemRenderer items = Minecraft.getInstance().getItemRenderer();
        BakedModel measured;
        boolean viaRenderer = false;
        if (model.isCustomRenderer()) {
            HungShape own = HungShape.of(model, stack);
            if (own != null) {
                measured = model;
                viaRenderer = true;
            } else {
                measured = items.getItemModelShaper().getItemModel(stack);
                if (measured == null || measured.isCustomRenderer()) return false;
                measured = perspective(measured, ItemDisplayContext.GUI);
                if (measured.isCustomRenderer()) return false;
            }
        } else {
            BakedModel drawn = perspective(model, ItemDisplayContext.NONE);
            HungShape drawnShape = HungShape.of(drawn, stack);
            if (isHandheldRotation(model) && drawnShape != null && drawnShape.thickness() < 0.1F
                    && drawnShape.length() < 1.6F) {
                return false; // 1번: 기존 방식
            }
            measured = perspective(model, ItemDisplayContext.GUI);
            if (measured.isCustomRenderer()) measured = drawn;
        }
        HungShape shape = HungShape.of(measured, stack);
        if (shape == null) return false;

        float scale = heldScale(stack, level) / WORN_SCALE;
        float turn = (float) Math.toRadians(TOOL_TURN);
        Vector3f down = new Vector3f(0F, -1F, 0F);
        Vector3f out = new Vector3f(right ? (float) Math.sin(turn) : -(float) Math.sin(turn), 0F, (float) Math.cos(turn));
        Vector3f head = new Vector3f(shape.longAxis());
        if (stack.is(Registration.HOOK_FLIPPED)) head.negate();
        Vector3f thin = shape.thinAxis();
        // 모델 축 (head, thin, head×thin) → 배낭 축 (down, out, down×out)
        Matrix3f from = new Matrix3f(head, thin, new Vector3f(head).cross(thin));
        Matrix3f to = new Matrix3f(down, out, new Vector3f(down).cross(out));
        Matrix3f rot = to.mul(from.transpose());

        // 두께가 있는 모델은 그만큼 바깥으로 밀어 옆면을 파고들지 않게 (평평한 그림 1px 기준)
        float push = Math.max(0F, (shape.thickness() - 1F / 16F) / 2F * scale);
        pose.translate((right ? TOOL_X : 1F - TOOL_X) + out.x * push, TOOL_Y, TOOL_Z + out.z * push);
        pose.mulPose(new Quaternionf().setFromNormalized(rot));
        pose.scale(scale, scale, scale);
        Vector3f c = shape.center();

        if (viaRenderer) {
            // NONE 은 변환 없이 (-0.5) 만 옮기고 전용 렌더러를 부른다 → 면 좌표와 같은 자리에 그려진다
            pose.translate(0.5F - c.x, 0.5F - c.y, 0.5F - c.z);
            items.renderStatic(stack, ItemDisplayContext.NONE, light, overlay, pose, buffers, level, 0);
            return true;
        }
        pose.translate(-c.x, -c.y, -c.z);
        for (BakedModel pass : measured.getRenderPasses(stack, true)) {
            for (RenderType type : pass.getRenderTypes(stack, true)) {
                VertexConsumer consumer = ItemRenderer.getFoilBufferDirect(buffers, type, true, stack.hasFoil());
                items.renderModelLists(pass, stack, light, overlay, pose, consumer);
            }
        }
        return true;
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
            BakedModel model = Minecraft.getInstance().getItemRenderer().getModel(stack, level, null, 0);
            if (!renderMeasured(stack, model, right, pose, buffers, light, overlay, level)) {
                pose.translate(right ? TOOL_X : 1F - TOOL_X, TOOL_Y, TOOL_Z);
                pose.mulPose(Axis.YP.rotationDegrees(right ? TOOL_TURN : -TOOL_TURN));
                pose.mulPose(Axis.ZP.rotationDegrees(TOOL_SPIN));
                float scale = heldScale(stack, level) / WORN_SCALE;
                pose.scale(scale, scale, scale);
                // NONE: 아이템 모델을 회전 없이 그대로 (손에 들 때와 같은 모델). 중심은 렌더러가 맞춘다.
                Minecraft.getInstance().getItemRenderer().renderStatic(stack, ItemDisplayContext.NONE, light, overlay,
                        pose, buffers, level, 0);
            }
        }
        pose.popPose();
    }
}
