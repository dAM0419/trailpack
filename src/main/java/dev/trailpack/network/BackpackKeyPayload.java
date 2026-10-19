package dev.trailpack.network;

import dev.trailpack.TrailPack;
import dev.trailpack.backpack.AttachmentActions;
import dev.trailpack.backpack.BackpackLayout;
import dev.trailpack.backpack.SnackEating;
import dev.trailpack.backpack.WornBackpack;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** 배낭 키 입력. 웅크리기 여부는 클라이언트에서 판단해 동작으로 바꿔 보낸다. */
public record BackpackKeyPayload(Action action) implements CustomPacketPayload {

    public enum Action {
        /** B: 배낭 열기 */
        OPEN,
        /** 웅크리기 + 손 바꾸기 키(기본 F): 갑옷 교환 */
        ARMOR,
        /** G: 오른쪽 부착물 */
        RIGHT,
        /** 웅크리기+G: 왼쪽 부착물 */
        LEFT,
        /** V: 위 부착물 (침낭 꺼내기·넣기) */
        TOP,
        /** G 를 뗌: 간식 먹기 취소 */
        SIDE_RELEASED
    }

    public static final Type<BackpackKeyPayload> TYPE = new Type<>(TrailPack.rl("backpack_key"));
    public static final StreamCodec<ByteBuf, BackpackKeyPayload> STREAM_CODEC = ByteBufCodecs.VAR_INT.map(
            i -> new BackpackKeyPayload(Action.values()[Math.floorMod(i, Action.values().length)]),
            p -> p.action().ordinal());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(BackpackKeyPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player) || player.isSpectator()) return;
            switch (payload.action()) {
                case OPEN -> WornBackpack.open(player, player);
                case ARMOR -> WornBackpack.swapArmor(player);
                case RIGHT -> AttachmentActions.use(player, BackpackLayout.Position.RIGHT);
                case LEFT -> AttachmentActions.use(player, BackpackLayout.Position.LEFT);
                case TOP -> AttachmentActions.use(player, BackpackLayout.Position.TOP);
                case SIDE_RELEASED -> SnackEating.stop(player);
            }
        });
    }
}
