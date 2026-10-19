package dev.trailpack.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/** 배낭 부착물. 어느 위치에 달 수 있는지와 무엇을 담는지는 BackpackLayout 이 정한다. */
public class AttachmentItem extends Item {

    public enum Kind {
        /** 도구걸이: 양옆, 도구·랜턴 */
        HOOK,
        /** 간식 주머니: 양옆 중 하나, 음식 */
        POUCH,
        /** 침낭끈: 위, 침낭 */
        STRAP,
        /** 갑옷주머니: 앞, 갑옷 4칸 */
        ARMOR
    }

    private final Kind kind;

    public AttachmentItem(Kind kind, Properties properties) {
        super(properties);
        this.kind = kind;
    }

    public Kind kind() {
        return kind;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable(getDescriptionId() + ".desc").withStyle(ChatFormatting.GRAY));
    }
}
