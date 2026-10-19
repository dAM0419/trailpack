package dev.trailpack.backpack;

import dev.trailpack.Registration;
import dev.trailpack.block.BackpackBlock;
import dev.trailpack.block.SleepingBagBlock;
import dev.trailpack.item.AttachmentItem;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/**
 * 부착물 상태에서 정해지는 외형. 설치 블록·등에 멘 모습·인벤토리 아이콘이 모두 이것으로 그려진다.
 *
 * @param lantern    걸린 랜턴 중 가장 밝은 것 (설치 블록 빛)
 * @param rightPouch 오른쪽에 간식 주머니
 * @param leftPouch  왼쪽에 간식 주머니
 * @param roll       위에 묶인 침낭 색
 * @param armor      갑옷주머니
 * @param rightHung  오른쪽 도구걸이에 걸린 것 (도구 또는 랜턴, 없으면 EMPTY)
 * @param leftHung   왼쪽 도구걸이에 걸린 것
 */
public record BackpackVisuals(LanternType lantern, boolean rightPouch, boolean leftPouch, RollColor roll,
                              boolean armor, ItemStack rightHung, ItemStack leftHung) {

    public static BackpackVisuals of(List<ItemStack> extra) {
        ItemStack right = hung(extra, BackpackLayout.ATT_RIGHT, BackpackLayout.RIGHT);
        ItemStack left = hung(extra, BackpackLayout.ATT_LEFT, BackpackLayout.LEFT);
        LanternType lantern = brighter(lanternOf(right), lanternOf(left));

        RollColor roll = RollColor.NONE;
        if (BackpackLayout.kindOf(extra.get(BackpackLayout.ATT_TOP)) == AttachmentItem.Kind.STRAP
                && extra.get(BackpackLayout.TOP).getItem() instanceof BlockItem bi
                && bi.getBlock() instanceof SleepingBagBlock bag) {
            roll = RollColor.of(bag.getColor());
        }
        return new BackpackVisuals(lantern,
                BackpackLayout.kindOf(extra.get(BackpackLayout.ATT_RIGHT)) == AttachmentItem.Kind.POUCH,
                BackpackLayout.kindOf(extra.get(BackpackLayout.ATT_LEFT)) == AttachmentItem.Kind.POUCH,
                roll,
                !extra.get(BackpackLayout.ATT_ARMOR).isEmpty(),
                right, left);
    }

    public static BackpackVisuals of(ItemStack backpack) {
        NonNullList<ItemStack> list = NonNullList.withSize(BackpackLayout.EXTRA_SIZE, ItemStack.EMPTY);
        backpack.getOrDefault(Registration.EXTRA.get(), ItemContainerContents.EMPTY).copyInto(list);
        return of(list);
    }

    private static ItemStack hung(List<ItemStack> extra, int attachment, int content) {
        return BackpackLayout.kindOf(extra.get(attachment)) == AttachmentItem.Kind.HOOK ? extra.get(content) : ItemStack.EMPTY;
    }

    private static LanternType lanternOf(ItemStack stack) {
        if (stack.isEmpty() || !BackpackLayout.isLantern(stack)) return LanternType.NONE;
        return stack.is(Items.SOUL_LANTERN) ? LanternType.SOUL : LanternType.LANTERN;
    }

    private static LanternType brighter(LanternType a, LanternType b) {
        return a.light() >= b.light() ? a : b;
    }

    public boolean hasRoll() {
        return roll != RollColor.NONE;
    }

    public boolean hasSoulLantern() {
        return lanternOf(rightHung) == LanternType.SOUL || lanternOf(leftHung) == LanternType.SOUL;
    }

    public BlockState apply(BlockState state) {
        return state.setValue(BackpackBlock.LANTERN, lantern)
                .setValue(BackpackBlock.RIGHT_POUCH, rightPouch)
                .setValue(BackpackBlock.LEFT_POUCH, leftPouch)
                .setValue(BackpackBlock.ROLL, roll)
                .setValue(BackpackBlock.ARMOR, armor);
    }
}
