package dev.trailpack.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.trailpack.backpack.WornBackpack;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Iris 셰이더의 "손에 든 광원" 조명에 배낭 랜턴을 넣는다.
 * 셰이더는 손에 든 아이템 정보(heldItemId)로 플레이어 주변을 밝히는데, LambDynamicLights 보다 훨씬 밝다.
 * 왼손이 비어 있으면 배낭에 걸린 랜턴을 왼손에 든 것처럼 알려 준다 (Lamb Lanterns 와 같은 방식).
 *
 * - @Pseudo: Iris 가 없으면 대상 클래스가 없으므로 조용히 건너뛴다.
 * - @WrapOperation: Lamb Lanterns 의 @Redirect 와 같은 호출을 건드려도 서로 이어서 동작한다.
 */
@Pseudo
@Mixin(targets = "net.irisshaders.iris.uniforms.IdMapUniforms$HeldItemSupplier", remap = false)
public abstract class IrisHeldItemMixin {

    @WrapOperation(
            method = "update",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/player/LocalPlayer;getItemInHand(Lnet/minecraft/world/InteractionHand;)Lnet/minecraft/world/item/ItemStack;"),
            require = 0)
    private ItemStack trailpack$backpackLantern(LocalPlayer player, InteractionHand hand, Operation<ItemStack> original) {
        ItemStack held = original.call(player, hand);
        if (hand != InteractionHand.OFF_HAND || !held.isEmpty()) return held;
        ItemStack lantern = WornBackpack.hangingLantern(player);
        return lantern.isEmpty() ? held : lantern;
    }
}
