package dev.trailpack.client;

import dev.trailpack.Registration;
import dev.trailpack.backpack.BackpackLayout;
import dev.trailpack.backpack.BackpackLayout.Position;
import dev.trailpack.backpack.WornBackpack;
import net.minecraft.client.AttackIndicatorStatus;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.NonNullList;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;

/**
 * 핫바 오른쪽의 부착물 2칸. 위에 화살표로 위치를 표시한다: ← 왼쪽(웅크리기+G) / → 오른쪽(G).
 * 부착물이 없는 위치는 어둡게 그린다.
 */
public final class BackpackHud {
    private BackpackHud() {}

    private static final Position[] ORDER = {Position.LEFT, Position.RIGHT};
    private static final String[] ARROWS = {"←", "→"};
    private static final int SIZE = 22;
    private static final int GAP = 1;

    public static void render(GuiGraphics g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null || mc.options.hideGui || player.isSpectator()) return;
        ItemStack backpack = WornBackpack.find(player);
        if (backpack.isEmpty()) return;

        NonNullList<ItemStack> extra = NonNullList.withSize(BackpackLayout.EXTRA_SIZE, ItemStack.EMPTY);
        backpack.getOrDefault(Registration.EXTRA.get(), ItemContainerContents.EMPTY).copyInto(extra);

        int width = g.guiWidth();
        int x = width / 2 + 91 + 6;
        if (player.getMainArm() == HumanoidArm.RIGHT) {
            if (mc.options.attackIndicator().get() == AttackIndicatorStatus.HOTBAR) x += 22; // 공격 쿨다운 표시를 비킨다
        } else if (!player.getOffhandItem().isEmpty()) {
            x += 29; // 왼손잡이 설정이면 보조 손 칸이 이쪽에 온다
        }
        int y = g.guiHeight() - SIZE;

        for (int i = 0; i < ORDER.length; i++) {
            Position pos = ORDER[i];
            int sx = x + i * (SIZE + GAP);
            boolean attached = !extra.get(pos.attachment).isEmpty();
            drawSlot(g, sx, y, attached);
            ItemStack content = extra.get(pos.content);
            if (attached && !content.isEmpty()) {
                g.renderItem(content, sx + 3, y + 3);
                g.renderItemDecorations(mc.font, content, sx + 3, y + 3);
            }
            int arrowWidth = mc.font.width(ARROWS[i]);
            g.drawString(mc.font, ARROWS[i], sx + (SIZE - arrowWidth) / 2, y - 10, attached ? 0xFFFFFFFF : 0xFF808080, true);
        }
    }

    private static void drawSlot(GuiGraphics g, int x, int y, boolean attached) {
        g.fill(x, y, x + SIZE, y + SIZE, 0xFF1A1A1A);                       // 바깥 테두리
        g.fill(x + 1, y + 1, x + SIZE - 1, y + SIZE - 1, attached ? 0x99808080 : 0x99303030);
        g.fill(x + 1, y + 1, x + SIZE - 1, y + 2, attached ? 0x66FFFFFF : 0x33FFFFFF);   // 위 밝은 선
    }
}
