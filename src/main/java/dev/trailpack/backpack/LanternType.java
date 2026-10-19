package dev.trailpack.backpack;

import net.minecraft.util.StringRepresentable;

/** 배낭에 걸린 랜턴 종류. 설치된 배낭의 빛 세기와 모델을 정한다. */
public enum LanternType implements StringRepresentable {
    NONE("none", 0),
    LANTERN("lantern", 15),
    SOUL("soul", 10);

    private final String name;
    private final int light;

    LanternType(String name, int light) {
        this.name = name;
        this.light = light;
    }

    public int light() {
        return light;
    }

    @Override
    public String getSerializedName() {
        return name;
    }
}
