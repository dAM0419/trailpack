package dev.trailpack.compat.curios;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.trailpack.client.BackpackRenderer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.client.ICurioRenderer;

/** Curios 등 칸에 멘 배낭. 몸 장비 칸에 멘 것과 같은 모습으로 그린다. */
public class BackpackCurioRenderer implements ICurioRenderer {
    @Override
    public <T extends LivingEntity, M extends EntityModel<T>> void render(ItemStack stack, SlotContext slotContext,
            PoseStack pose, RenderLayerParent<T, M> parent, MultiBufferSource buffers, int light,
            float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
        BackpackRenderer.renderOnBack(stack, slotContext.entity(), parent.getModel(), pose, buffers, light);
    }
}
