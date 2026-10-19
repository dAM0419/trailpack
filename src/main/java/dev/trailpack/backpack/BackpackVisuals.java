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

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

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

    /** 배낭 아이템의 외형. 같은 부착물 상태면 계산 결과를 다시 쓴다 (아래 캐시). */
    public static BackpackVisuals of(ItemStack backpack) {
        return cached(backpack).visuals();
    }

    /**
     * 배낭 아이템의 부착물 칸 11개 (읽기 전용 사본). 매 프레임 그리는 곳(아이콘·등·HUD·셰이더 조명)과
     * 매 틱 읽는 곳(간식 먹기)이 같은 상태를 계속 다시 풀지 않도록 캐시한다. 절대 수정하지 말 것.
     */
    public static List<ItemStack> extraOf(ItemStack backpack) {
        return cached(backpack).extra();
    }

    // ------------------------------------------------------------------
    // 캐시: 아이템 컴포넌트(ItemContainerContents)는 불변이고, 내용이 바뀌면 새 객체로 교체된다.
    // 그래서 컴포넌트를 키로 쓰면 내용이 바뀌는 순간 자동으로 다른 항목을 보게 된다 (무효화가 필요 없음).
    // 약한 참조라 더는 쓰이지 않는 상태는 GC 가 치운다. 렌더 스레드와 서버 스레드가 함께 쓰므로 동기화.
    // ------------------------------------------------------------------

    private record Cached(List<ItemStack> extra, BackpackVisuals visuals) {}

    private static final Map<ItemContainerContents, Cached> CACHE = Collections.synchronizedMap(new WeakHashMap<>());

    private static Cached cached(ItemStack backpack) {
        ItemContainerContents contents = backpack.getOrDefault(Registration.EXTRA.get(), ItemContainerContents.EMPTY);
        return CACHE.computeIfAbsent(contents, c -> {
            NonNullList<ItemStack> list = NonNullList.withSize(BackpackLayout.EXTRA_SIZE, ItemStack.EMPTY);
            c.copyInto(list);
            List<ItemStack> readOnly = Collections.unmodifiableList(list);
            return new Cached(readOnly, of(readOnly));
        });
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
