package dev.trailpack.block;

import com.mojang.serialization.MapCodec;
import dev.trailpack.backpack.BackpackVisuals;
import dev.trailpack.backpack.LanternType;
import dev.trailpack.backpack.RollColor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * 설치된 배낭. FACING 은 배낭의 등쪽 면(멜 때 등에 닿는 면)이 향하는 방향이며,
 * 설치한 플레이어 쪽을 향한다. 몸통·간식 주머니·갑옷주머니·침낭 롤은 블록 모델로,
 * 도구걸이에 건 도구·랜턴은 블록 엔티티 렌더러(BackpackBlockRenderer)가 그린다.
 */
public class BackpackBlock extends BaseEntityBlock {
    public static final MapCodec<BackpackBlock> CODEC = simpleCodec(BackpackBlock::new);

    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final EnumProperty<LanternType> LANTERN = EnumProperty.create("lantern", LanternType.class);
    public static final EnumProperty<RollColor> ROLL = EnumProperty.create("roll", RollColor.class);
    public static final BooleanProperty RIGHT_POUCH = BooleanProperty.create("right_pouch");
    public static final BooleanProperty LEFT_POUCH = BooleanProperty.create("left_pouch");
    public static final BooleanProperty ARMOR = BooleanProperty.create("armor");

    // 몸통: 폭 12, 깊이 8, 높이 16 / 침낭·해먹: 위로 3.2 (총 1.2칸)
    private static final VoxelShape BODY_NS = Block.box(2, 0, 4, 14, 16, 12);
    private static final VoxelShape BODY_EW = Block.box(4, 0, 2, 12, 16, 14);
    private static final VoxelShape ROLL_NS = Block.box(1, 16, 5, 15, 19.2, 11);
    private static final VoxelShape ROLL_EW = Block.box(5, 16, 1, 11, 19.2, 15);
    private static final VoxelShape FULL_NS = Shapes.or(BODY_NS, ROLL_NS);
    private static final VoxelShape FULL_EW = Shapes.or(BODY_EW, ROLL_EW);

    public BackpackBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(LANTERN, LanternType.NONE)
                .setValue(ROLL, RollColor.NONE)
                .setValue(RIGHT_POUCH, false)
                .setValue(LEFT_POUCH, false)
                .setValue(ARMOR, false));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, LANTERN, ROLL, RIGHT_POUCH, LEFT_POUCH, ARMOR);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BackpackBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BackpackVisuals visuals = BackpackVisuals.of(context.getItemInHand());
        Level level = context.getLevel();
        BlockPos above = context.getClickedPos().above();
        if (visuals.hasRoll() && !level.getBlockState(above).canBeReplaced()) {
            Player player = context.getPlayer();
            if (player != null && !level.isClientSide) {
                player.displayClientMessage(Component.translatable("message.trailpack.no_room_above"), true);
            }
            return null;
        }
        BlockState state = defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
        return visuals.apply(state);
    }

    // ---- 모양 ----

    private static boolean northSouth(BlockState state) {
        return state.getValue(FACING).getAxis() == Direction.Axis.Z;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        boolean ns = northSouth(state);
        if (state.getValue(ROLL) != RollColor.NONE) return ns ? FULL_NS : FULL_EW;
        return ns ? BODY_NS : BODY_EW;
    }

    @Override
    protected boolean isPathfindable(BlockState state, PathComputationType type) {
        return false;
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    // ---- 열기 ----

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (player instanceof ServerPlayer sp && level.getBlockEntity(pos) instanceof BackpackBlockEntity be) {
            be.openFor(sp);
            level.playSound(null, pos, SoundEvents.ARMOR_EQUIP_LEATHER.value(), SoundSource.BLOCKS, 0.6F, 1.0F);
        }
        return InteractionResult.CONSUME;
    }

    // ---- 셜커 상자와 같은 처리 ----

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        // 크리에이티브에서 부숴도 내용물이 사라지지 않게 (셜커 상자와 동일)
        if (!level.isClientSide && player.isCreative() && level.getBlockEntity(pos) instanceof BackpackBlockEntity be
                && !be.inventory().isEmpty()) {
            ItemStack stack = new ItemStack(this);
            stack.applyComponents(be.collectComponents());
            ItemEntity entity = new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, stack);
            entity.setDefaultPickUpDelay();
            level.addFreshEntity(entity);
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock())) {
            // 내용물은 전리품 테이블(copy_components)로 아이템에 담겨 떨어진다. 여기서는 흩뿌리지 않는다.
            level.updateNeighbourForOutputSignal(pos, state.getBlock());
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        return AbstractContainerMenu.getRedstoneSignalFromBlockEntity(level.getBlockEntity(pos));
    }
}
