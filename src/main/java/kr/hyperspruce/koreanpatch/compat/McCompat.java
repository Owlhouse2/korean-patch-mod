package kr.hyperspruce.koreanpatch.compat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.screens.Screen;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * 26.1 과 26.2 사이에서 옮겨간 API 를 흡수하는 얇은 호환층.
 *
 * <p>26.2 에서 화면 관리가 {@code Minecraft} 에서 {@code Minecraft.gui}({@link Gui}) 로 통째로
 * 옮겨갔다. {@code Minecraft.setScreen} 과 {@code Minecraft.screen} 필드가 아예 사라졌고,
 * {@code Gui.setScreen} / {@code Gui.screen()} 이 그 자리를 대신한다.
 * (26.2 클라이언트 jar 의 {@code Minecraft.setScreenAndShow} 를 디스어셈블해 확인했다.)
 *
 * <p>26.x 는 난독화가 없어 리매핑이 없으므로 <b>jar 하나로 두 버전을 모두 지원할 수 있다</b>.
 * 다만 컴파일 시점에 고른 쪽의 심볼이 바이트코드에 그대로 박히므로, 옮겨간 멤버는 직접
 * 호출하면 안 된다 — 반대쪽 버전에서 {@code NoSuchMethodError} 로 죽는다. 그래서 여기서만
 * 리플렉션으로 한 번 해석해 두고 나머지 코드는 이 클래스만 부른다.
 *
 * <p>화면 전환은 초당 한 번도 일어나지 않는 동작이라 리플렉션 비용은 문제가 되지 않는다.
 */
public final class McCompat {

    /** {@code Gui.setScreen} (26.2+) 또는 {@code Minecraft.setScreen} (26.1). */
    private static final Method SET_SCREEN;

    /** {@code Gui.screen()} — 26.2+ 에서만 존재한다. */
    private static final Method SCREEN_GETTER;

    /** {@code Minecraft.screen} 필드 — 26.1 에서만 존재한다. */
    private static final Field SCREEN_FIELD;

    /** 화면 관리가 {@link Gui} 로 옮겨간 버전(26.2+)인가. */
    private static final boolean VIA_GUI;

    static {
        Method setScreen;
        Method screenGetter;
        Field screenField;
        boolean viaGui;

        try {
            // 26.2+ 를 먼저 본다. 새 배치가 앞으로의 기본값이다.
            setScreen = Gui.class.getMethod("setScreen", Screen.class);
            screenGetter = Gui.class.getMethod("screen");
            screenField = null;
            viaGui = true;
        } catch (NoSuchMethodException newLayoutMissing) {
            try {
                setScreen = Minecraft.class.getMethod("setScreen", Screen.class);
                screenField = Minecraft.class.getField("screen");
                screenGetter = null;
                viaGui = false;
            } catch (ReflectiveOperationException oldLayoutMissing) {
                // 둘 다 없으면 지원 범위 밖의 버전이다. 조용히 넘어가면 화면 전환이
                // 전부 무반응이 되므로 여기서 확실히 터뜨린다.
                IllegalStateException failure = new IllegalStateException(
                        "지원하지 않는 마인크래프트 버전: 화면 전환 API 를 찾을 수 없다 "
                                + "(Gui.setScreen 도 Minecraft.setScreen 도 없음)", oldLayoutMissing);
                failure.addSuppressed(newLayoutMissing);
                throw failure;
            }
        }

        SET_SCREEN = setScreen;
        SCREEN_GETTER = screenGetter;
        SCREEN_FIELD = screenField;
        VIA_GUI = viaGui;
    }

    /** 이 런타임이 화면 관리를 {@link Gui} 로 옮긴 26.2 이후인지. 진단 로그용. */
    public static boolean isGuiScreenLayout() {
        return VIA_GUI;
    }

    /** 버전과 무관하게 화면을 교체한다. {@code null} 을 넘기면 화면을 닫는다. */
    public static void setScreen(Minecraft minecraft, Screen screen) {
        try {
            SET_SCREEN.invoke(VIA_GUI ? minecraft.gui : minecraft, screen);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("화면 전환에 실패했다", e);
        }
    }

    /** 현재 열려 있는 화면. 없으면 {@code null}. */
    public static Screen currentScreen(Minecraft minecraft) {
        try {
            return VIA_GUI
                    ? (Screen) SCREEN_GETTER.invoke(minecraft.gui)
                    : (Screen) SCREEN_FIELD.get(minecraft);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("현재 화면을 읽지 못했다", e);
        }
    }

    private McCompat() {
    }
}
