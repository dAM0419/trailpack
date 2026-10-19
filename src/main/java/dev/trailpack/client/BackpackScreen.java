package dev.trailpack.client;

import dev.trailpack.Registration;
import dev.trailpack.backpack.BackpackLayout;
import dev.trailpack.item.AttachmentItem;
import dev.trailpack.menu.BackpackMenu;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * 임시 화면. 텍스처 없이 사각형으로 바닐라 상자 화면 비슷하게 그린다.
 *
 * 부착물 패널(왼쪽): 배낭의 실제 위치대로 — 윗줄 침낭(↑), 가운데 줄 왼쪽(←)·오른쪽(→), 아랫줄 갑옷.
 * 부착물 칸(갈색)은 안쪽, 내용물 칸은 바깥쪽. 부착물이 없으면 내용물 칸에 X 가 그려지고 쓸 수 없다.
 * 모든 부착물 관련 칸은 커서를 올리면 무엇을 하는 칸인지, 어떤 키로 쓰는지 안내한다.
 */
public class BackpackScreen extends AbstractContainerScreen<BackpackMenu> {
    private static final int PANEL = 0xFFC6C6C6;
    private static final int PANEL_EDGE_DARK = 0xFF555555;
    private static final int PANEL_EDGE_LIGHT = 0xFFFFFFFF;
    private static final int SLOT_DARK = 0xFF373737;
    private static final int SLOT_FILL = 0xFF8B8B8B;
    private static final int SLOT_LIGHT = 0xFFFFFFFF;
    private static final int MODULE_FILL = 0xFF8B6E4E;   // 부착물 칸: 가죽색
    private static final int LOCKED_FILL = 0xFF6F6F6F;
    private static final int LOCK_X = 0xFFB03A3A;
    private static final int LINK = 0xFF8B6E4E;         // 부착물 → 여는 칸 연결선
    private static final int TOOLTIP_WIDTH = 200;


    public BackpackScreen(BackpackMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = BackpackMenu.IMAGE_WIDTH;
        this.imageHeight = 114 + menu.rows() * 18;
        this.titleLabelX = BackpackMenu.MAIN_X + 8;
        this.inventoryLabelX = BackpackMenu.MAIN_X + 8;
        this.inventoryLabelY = this.imageHeight - 94;
    }




    /** 부착물 칸 → 그것이 여는 칸 (연결선용). */
    private static final int[][] PAIRS = {
            {BackpackLayout.ATT_RIGHT, BackpackLayout.RIGHT},
            {BackpackLayout.ATT_LEFT, BackpackLayout.LEFT},
            {BackpackLayout.ATT_TOP, BackpackLayout.TOP},
    };

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        super.renderLabels(g, mouseX, mouseY);
        if (!menu.isFull()) return;
        int[][] pos = BackpackMenu.EXTRA_POS;
        int color = 0xFF404040;
        // ↑ 위 부착물 왼쪽, ← 왼쪽 내용물 바깥, → 오른쪽 내용물 바깥
        g.drawString(font, "↑", pos[BackpackLayout.ATT_TOP][0] - 10, pos[BackpackLayout.ATT_TOP][1] + 4, color, false);
        g.drawString(font, "←", pos[BackpackLayout.LEFT][0] - 9, pos[BackpackLayout.LEFT][1] + 4, color, false);
        g.drawString(font, "→", pos[BackpackLayout.RIGHT][0] + 20, pos[BackpackLayout.RIGHT][1] + 4, color, false);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        if (!renderPanelTooltip(graphics, mouseX, mouseY)) {
            renderTooltip(graphics, mouseX, mouseY);
        }
    }

    // ------------------------------------------------------------------
    // 그리기
    // ------------------------------------------------------------------

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;
        panel(g, x + BackpackMenu.MAIN_X, y, 176, imageHeight);
        if (menu.isFull()) {
            panel(g, x, y, BackpackMenu.PANEL_WIDTH, BackpackMenu.PANEL_HEIGHT);
            drawLinks(g, x, y);
        }
        for (Slot slot : menu.slots) {
            if (slot instanceof BackpackMenu.ExtraSlot extra) {
                drawExtraSlot(g, x, y, extra);
            } else if (slot.isActive()) {
                slotBg(g, x + slot.x - 1, y + slot.y - 1, SLOT_FILL);
            }
        }
        if (menu.isFull()) drawModuleGhosts(g, x, y);
    }

    private void drawExtraSlot(GuiGraphics g, int x, int y, BackpackMenu.ExtraSlot slot) {
        int sx = x + slot.x - 1;
        int sy = y + slot.y - 1;
        int index = slot.extraIndex();
        if (BackpackLayout.isAttachmentSlot(index)) {
            slotBg(g, sx, sy, MODULE_FILL);
        } else if (slot.isActive()) {
            slotBg(g, sx, sy, SLOT_FILL);
        } else {
            slotBg(g, sx, sy, LOCKED_FILL);
            drawX(g, sx + 3, sy + 3, 12);
        }
    }

    /** 부착물 칸과 그것이 여는 칸 사이에 짧은 연결선 (부착물이 있으면 진하게). */
    private void drawLinks(GuiGraphics g, int x, int y) {
        int[][] pos = BackpackMenu.EXTRA_POS;
        // 오른쪽·왼쪽·위: 부착물 칸과 내용물 칸이 가로로 붙어 있으므로 두 칸 아래에 밑줄
        for (int[] pair : PAIRS) {
            int left = Math.min(pos[pair[0]][0], pos[pair[1]][0]);
            int right = Math.max(pos[pair[0]][0], pos[pair[1]][0]);
            int ay = y + pos[pair[0]][1];
            int color = menu.inventory().hasAttachment(pair[0]) ? LINK : 0xFFA9A9A9;
            g.fill(x + left - 1, ay + 18, x + right + 17, ay + 19, color);
        }
        // 갑옷: 주머니에서 마지막 갑옷 칸까지 한 줄 밑줄
        int ay = y + pos[BackpackLayout.ATT_ARMOR][1];
        int color = menu.inventory().hasAttachment(BackpackLayout.ATT_ARMOR) ? LINK : 0xFFA9A9A9;
        g.fill(x + pos[BackpackLayout.ATT_ARMOR][0] - 1, ay + 18,
                x + pos[BackpackLayout.ARMOR_FIRST + 3][0] + 17, ay + 19, color);
    }

    /** 빈 부착물 칸에 해당 부착물 그림을 흐리게 보여 준다. */
    private void drawModuleGhosts(GuiGraphics g, int x, int y) {
        for (int i = 0; i < BackpackLayout.ATTACHMENT_COUNT; i++) {
            if (menu.inventory().hasAttachment(i)) continue;
            int sx = x + BackpackMenu.EXTRA_POS[i][0];
            int sy = y + BackpackMenu.EXTRA_POS[i][1];
            g.renderFakeItem(new ItemStack(moduleItem(i)), sx, sy);
            g.pose().pushPose();
            g.pose().translate(0, 0, 300);
            g.fill(sx, sy, sx + 16, sy + 16, 0xA08B6E4E);
            g.pose().popPose();
        }
    }

    private static void drawX(GuiGraphics g, int x, int y, int size) {
        for (int i = 0; i < size; i++) {
            g.fill(x + i, y + i, x + i + 2, y + i + 2, LOCK_X);
            g.fill(x + size - 1 - i, y + i, x + size + 1 - i, y + i + 2, LOCK_X);
        }
    }

    private static void panel(GuiGraphics g, int x, int y, int w, int h) {
        g.fill(x, y, x + w, y + h, PANEL_EDGE_DARK);
        g.fill(x, y, x + w - 1, y + h - 1, PANEL_EDGE_LIGHT);
        g.fill(x + 1, y + 1, x + w - 1, y + h - 1, PANEL);
    }

    private static void slotBg(GuiGraphics g, int x, int y, int fill) {
        g.fill(x, y, x + 18, y + 18, SLOT_LIGHT);
        g.fill(x, y, x + 17, y + 17, SLOT_DARK);
        g.fill(x + 1, y + 1, x + 17, y + 17, fill);
    }

    // ------------------------------------------------------------------
    // 안내 툴팁
    // ------------------------------------------------------------------

    /** 부착물 패널 칸 위에 커서가 있으면 안내 툴팁을 그리고 true. */
    private boolean renderPanelTooltip(GuiGraphics g, int mouseX, int mouseY) {
        if (!menu.isFull() || !menu.getCarried().isEmpty()) return false;
        for (Slot slot : menu.slots) {
            if (!(slot instanceof BackpackMenu.ExtraSlot extra)) continue;
            int sx = leftPos + slot.x;
            int sy = topPos + slot.y;
            if (mouseX < sx - 1 || mouseX >= sx + 17 || mouseY < sy - 1 || mouseY >= sy + 17) continue;
            List<FormattedCharSequence> wrapped = new ArrayList<>();
            for (Component line : tooltipFor(extra)) {
                wrapped.addAll(font.split(line, TOOLTIP_WIDTH));
            }
            g.renderTooltip(font, wrapped, mouseX, mouseY);
            return true;
        }
        return false;
    }

    private List<Component> tooltipFor(BackpackMenu.ExtraSlot slot) {
        int index = slot.extraIndex();
        ItemStack stack = slot.getItem();
        List<Component> lines = new ArrayList<>();

        if (!stack.isEmpty()) {
            // 들어 있는 아이템의 원래 툴팁 + 칸 안내 한 줄
            lines.addAll(getTooltipFromContainerItem(stack));
            if (BackpackLayout.isAttachmentSlot(index)) {
                lines.add(Component.translatable("gui.trailpack.module_remove").withStyle(ChatFormatting.GOLD));
            }
            Component key = keyHint(index);
            if (key != null) lines.add(key);
            return lines;
        }

        String id = slotId(index);
        lines.add(Component.translatable("gui.trailpack.slot." + id + ".title"));
        lines.add(Component.translatable("gui.trailpack.slot." + id + ".desc").withStyle(ChatFormatting.GRAY));
        int required = BackpackLayout.attachmentOf(index);
        if (required >= 0 && !slot.isActive()) {
            lines.add(Component.translatable("gui.trailpack.locked." + attachmentId(required)).withStyle(ChatFormatting.RED));
        }
        Component key = keyHint(index);
        if (key != null) lines.add(key);
        return lines;
    }

    /** 칸 안내 문구 이름. 양옆 내용물 칸은 달린 부착물(도구걸이 / 간식 주머니)에 따라 달라진다. */
    private String slotId(int index) {
        if (BackpackLayout.isAttachmentSlot(index)) return "att_" + attachmentId(index);
        if (index == BackpackLayout.RIGHT || index == BackpackLayout.LEFT) {
            AttachmentItem.Kind kind = BackpackLayout.kindOf(menu.inventory().extra.getItem(BackpackLayout.attachmentOf(index)));
            if (kind == AttachmentItem.Kind.HOOK) return "hook";
            if (kind == AttachmentItem.Kind.POUCH) return "pouch";
            return "side";
        }
        if (index == BackpackLayout.TOP) return "bag";
        return "armor_" + (index - BackpackLayout.ARMOR_FIRST);
    }

    private static String attachmentId(int attachment) {
        return switch (attachment) {
            case BackpackLayout.ATT_RIGHT, BackpackLayout.ATT_LEFT -> "side";
            case BackpackLayout.ATT_TOP -> "top";
            default -> "armor";
        };
    }

    /** 그 위치를 쓰는 키 안내 (지금 설정된 키 이름으로). */
    private static Component keyHint(int index) {
        int att = BackpackLayout.isAttachmentSlot(index) ? index : BackpackLayout.attachmentOf(index);
        return switch (att) {
            case BackpackLayout.ATT_RIGHT -> Component.translatable("gui.trailpack.key.right",
                    ClientSetup.SIDE_KEY.getTranslatedKeyMessage()).withStyle(ChatFormatting.YELLOW);
            case BackpackLayout.ATT_LEFT -> Component.translatable("gui.trailpack.key.left",
                    ClientSetup.SIDE_KEY.getTranslatedKeyMessage()).withStyle(ChatFormatting.YELLOW);
            case BackpackLayout.ATT_TOP -> Component.translatable("gui.trailpack.key.top",
                    ClientSetup.TOP_KEY.getTranslatedKeyMessage()).withStyle(ChatFormatting.YELLOW);
            case BackpackLayout.ATT_ARMOR -> Component.translatable("gui.trailpack.key.armor",
                    net.minecraft.client.Minecraft.getInstance().options.keySwapOffhand.getTranslatedKeyMessage())
                    .withStyle(ChatFormatting.YELLOW);
            default -> null;
        };
    }

    private static Item moduleItem(int attachment) {
        return switch (attachment) {
            case BackpackLayout.ATT_RIGHT, BackpackLayout.ATT_LEFT -> Registration.TOOL_HOOK.get();
            case BackpackLayout.ATT_TOP -> Registration.BEDROLL_STRAP.get();
            default -> Registration.ARMOR_POUCH.get();
        };
    }
}
