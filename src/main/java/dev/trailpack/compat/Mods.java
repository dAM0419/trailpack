package dev.trailpack.compat;

import net.neoforged.fml.ModList;

/** 선택 연동 모드가 깔려 있는지. 연동 클래스는 이 값이 true 일 때만 불러야 한다 (클래스 로딩 오류 방지). */
public final class Mods {
    private Mods() {}

    public static final boolean CURIOS = ModList.get().isLoaded("curios");
}
