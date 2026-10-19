package dev.trailpack.block;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * 바닥에 2칸으로 까는 침낭. 침대처럼 누워 밤을 넘기지만 리스폰 지점은 바꾸지 않는다
 * (ServerEvents 에서 리스폰 설정을 취소). 네더·엔드에서는 폭발하지 않고 쓸 수 없다고만 알린다.
 * 침대의 두 칸 설치·파괴·누워 있기 처리는 BedBlock 것을 그대로 쓰고, 모양·렌더·튕김만 바꾼다.
 */
public class SleepingBagBlock extends BedBlock {
    public static final MapCodec<SleepingBagBlock> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            DyeColor.CODEC.fieldOf("color").forGetter(BedBlock::getColor),
            propertiesCodec()).apply(i, SleepingBagBlock::new));

    private static final VoxelShape SHAPE = Block.box(1, 0, 0, 15, 2.5, 16);

    public SleepingBagBlock(DyeColor color, Properties properties) {
        super(color, properties);
    }

    @Override
    @SuppressWarnings("unchecked")
    public MapCodec<BedBlock> codec() {
        return (MapCodec<BedBlock>) (MapCodec<?>) CODEC;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL; // 침대와 달리 블록 엔티티 렌더러 없이 모델로 그린다
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return null;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.CONSUME;
        if (state.getValue(PART) != BedPart.HEAD) {
            pos = pos.relative(state.getValue(FACING));
            state = level.getBlockState(pos);
            if (!state.is(this)) return InteractionResult.CONSUME;
        }
        if (!canSetSpawn(level)) {
            player.displayClientMessage(Component.translatable("message.trailpack.cannot_sleep_here"), true);
            return InteractionResult.SUCCESS;
        }
        if (state.getValue(OCCUPIED)) {
            player.displayClientMessage(Component.translatable("block.minecraft.bed.occupied"), true);
            return InteractionResult.SUCCESS;
        }
        player.startSleepInBed(pos).ifLeft(problem -> {
            if (problem.getMessage() != null) player.displayClientMessage(problem.getMessage(), true);
        });
        return InteractionResult.SUCCESS;
    }

    // 침대의 튕김·낙하 피해 감소는 없앤다 (얇은 천이다)
    @Override
    public void fallOn(Level level, BlockState state, BlockPos pos, Entity entity, float fallDistance) {
        entity.causeFallDamage(fallDistance, 1.0F, entity.damageSources().fall());
    }

    @Override
    public void updateEntityAfterFallOn(BlockGetter level, Entity entity) {
        entity.setDeltaMovement(entity.getDeltaMovement().multiply(1.0, 0.0, 1.0));
    }
}
