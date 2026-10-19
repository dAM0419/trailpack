package dev.trailpack.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.trailpack.item.BackpackItem;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/** 바닐라 몸 장비 칸에 멘 배낭을 등에 그린다 (Curios 등 칸과 같은 모습). */
public class BackpackBodyLayer<T extends LivingEntity, M extends HumanoidModel<T>> extends RenderLayer<T, M> {

    public BackpackBodyLayer(RenderLayerParent<T, M> parent) {
        super(parent);
    }

    @Override
    public void render(PoseStack pose, MultiBufferSource buffers, int light, T entity, float limbSwing,
                       float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        ItemStack stack = entity.getItemBySlot(EquipmentSlot.CHEST);
        if (stack.getItem() instanceof BackpackItem) {
            BackpackRenderer.renderOnBack(stack, entity, getParentModel(), pose, buffers, light);
        }
    }
}
