package dev.trailpack.mixin;

import dev.trailpack.block.BackpackBlock;
import dev.trailpack.block.BackpackBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.sensing.PiglinSpecificSensor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** 도구걸이에 영혼 랜턴을 건 설치 배낭을 피글린 퇴치 블록으로 취급한다 (바닐라 영혼 랜턴과 같은 효과). */
@Mixin(PiglinSpecificSensor.class)
public abstract class PiglinSpecificSensorMixin {

    @Inject(method = "isValidRepellent", at = @At("HEAD"), cancellable = true, require = 0)
    private static void trailpack$soulLanternBackpack(ServerLevel level, BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        if (level.getBlockState(pos).getBlock() instanceof BackpackBlock
                && level.getBlockEntity(pos) instanceof BackpackBlockEntity be && be.hasSoulLantern()) {
            cir.setReturnValue(true);
        }
    }
}
