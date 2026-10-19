package dev.trailpack.backpack;

import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.DyeColor;
import org.jetbrains.annotations.Nullable;

/** 배낭 위에 묶인 침낭 롤의 색. NONE = 롤 없음. 순서는 바닐라 DyeColor 와 같다 (아이콘 코드에 쓰인다). */
public enum RollColor implements StringRepresentable {
    NONE("none"),
    WHITE("white"), ORANGE("orange"), MAGENTA("magenta"), LIGHT_BLUE("light_blue"),
    YELLOW("yellow"), LIME("lime"), PINK("pink"), GRAY("gray"),
    LIGHT_GRAY("light_gray"), CYAN("cyan"), PURPLE("purple"), BLUE("blue"),
    BROWN("brown"), GREEN("green"), RED("red"), BLACK("black");

    private final String name;

    RollColor(String name) {
        this.name = name;
    }

    public static RollColor of(@Nullable DyeColor dye) {
        return dye == null ? NONE : values()[dye.getId() + 1];
    }

    @Override
    public String getSerializedName() {
        return name;
    }
}
