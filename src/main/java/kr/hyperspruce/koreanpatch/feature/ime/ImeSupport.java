package kr.hyperspruce.koreanpatch.feature.ime;

import kr.hyperspruce.koreanpatch.KoreanPatch;
import kr.hyperspruce.koreanpatch.compat.McCompat;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.PreeditEvent;
import org.lwjgl.glfw.GLFW;

import java.util.List;

/**
 * 한글 입력기(IME)를 게임 상황에 맞게 켜고 끈다.
 *
 * <p><b>고치는 문제</b>: 한글 상태로 두면 캐릭터가 움직이지 않는다. 운영체제 IME 가 켜져 있는
 * 동안 키 입력을 먼저 가로채서, W·A·S·D 가 이동 키가 아니라 "ㅈ·ㅁ·ㄴ·ㅇ" 을 조합하는
 * 입력으로 흘러가고 게임에는 키 이벤트가 도달하지 않는다.
 *
 * <p><b>두 갈래로 손댄다.</b>
 * <ol>
 *   <li>글자를 칠 자리가 없으면 {@code GLFW_IME} 입력 모드로 IME 자체를 끈다.</li>
 *   <li>마인크래프트가 들고 있는 조합 상태를 빈 값으로 덮어 조합창(네모 상자)을 지운다.
 *       GLFW 쪽만 건드리면 게임이 마지막 조합 내용을 계속 그리고 있어서 상자가 남는다.</li>
 * </ol>
 *
 * <p>GLFW 는 호출이 실패해도 예외를 던지지 않고 오류 콜백으로만 알린다. 그래서 값을 넣은 뒤
 * <b>다시 읽어</b> 실제로 먹혔는지 확인하고, 안 먹으면 로그에 남긴다 — 조용히 아무 일도
 * 안 하는 게 가장 나쁘다.
 */
public final class ImeSupport {

    /** 지금 IME 가 켜져 있다고 우리가 알고 있는 상태. */
    private static boolean imeEnabled = true;

    /** 한 번이라도 상태를 맞춘 적이 있는가. */
    private static boolean initialised;

    /** 이 환경에서 {@code GLFW_IME} 제어가 통하는가. 통하지 않으면 더 시도하지 않는다. */
    private static boolean imeControlWorks = true;

    /** 진단 로그를 한 번만 남기기 위한 표시. */
    private static boolean loggedSupport;

    /** 마지막으로 글자를 받던 위젯. 화면이 닫힌 뒤 조합을 지울 때 필요하다. */
    private static EditBox lastTextTarget;

    private static int lastCaretX = -1;
    private static int lastCaretY = -1;

    public static void tick(Minecraft minecraft) {
        long window = minecraft.getWindow().handle();
        if (window == 0L) {
            return;
        }

        Screen screen = McCompat.currentScreen(minecraft);
        EditBox focused = screen == null ? null : findFocusedEditBox(screen);
        boolean wantsText = screen != null && (focused != null || containsEditBox(screen));

        if (focused != null) {
            lastTextTarget = focused;
        }

        if (!initialised || wantsText != imeEnabled) {
            setImeEnabled(minecraft, window, wantsText);
            initialised = true;
        }

        if (wantsText && focused != null) {
            updateCaretRectangle(minecraft, window, focused);
        }
    }

    private static void setImeEnabled(Minecraft minecraft, long window, boolean enabled) {
        imeEnabled = enabled;

        if (!enabled) {
            // 순서가 중요하다. 조합 중이던 글자를 먼저 버려야 조합창이 남지 않는다.
            clearComposition(minecraft, window);
            lastCaretX = -1;
            lastCaretY = -1;
        }

        // 실제로 효과가 있는 쪽. 입력기를 창에서 떼거나 붙인다.
        // 한/영 상태는 건드리지 않는다 — 그건 사용자 몫이다.
        WindowsIme.setAttached(window, enabled);

        // 윈도우가 아닌 환경에서는 GLFW 쪽이 유일한 통로다. 호출 자체는 해롭지 않다.
        if (imeControlWorks && !WindowsIme.isAvailable()) {
            GLFW.glfwSetInputMode(window, GLFW.GLFW_IME, enabled ? GLFW.GLFW_TRUE : GLFW.GLFW_FALSE);

            int actual = GLFW.glfwGetInputMode(window, GLFW.GLFW_IME);
            if (actual != (enabled ? GLFW.GLFW_TRUE : GLFW.GLFW_FALSE)) {
                imeControlWorks = false;
                KoreanPatch.LOG.info("이 환경의 GLFW 는 IME 입력 모드를 무시한다. "
                        + "운영체제 입력기 제어만 쓴다 (사용 가능: {}).", WindowsIme.isAvailable());
            }
        }

        if (!loggedSupport) {
            loggedSupport = true;
            KoreanPatch.LOG.info("IME 연결 전환 시작 — 글자 칠 때만 입력기를 창에 붙인다 "
                    + "(운영체제 제어: {}, GLFW 제어: {})", WindowsIme.isAvailable(), imeControlWorks);
        }
    }

    /**
     * 조합 중이던 글자를 지운다.
     *
     * <p>GLFW 에 알리는 것만으로는 부족하다. 마인크래프트는 마지막으로 받은 조합 이벤트를
     * 들고 있다가 계속 그리므로, 빈 조합 이벤트를 한 번 밀어 넣어야 화면에서 사라진다.
     */
    private static void clearComposition(Minecraft minecraft, long window) {
        try {
            GLFW.glfwResetPreeditText(window);
        } catch (Throwable e) {
            KoreanPatch.LOG.debug("조합 초기화를 지원하지 않는 환경이다", e);
        }

        EditBox target = lastTextTarget;
        if (target == null) {
            return;
        }

        try {
            KeyboardHandler.submitPreeditEvent(target, new PreeditEvent("", 0, List.of(), 0));
        } catch (Throwable e) {
            KoreanPatch.LOG.debug("조합 상태를 비우지 못했다", e);
        } finally {
            lastTextTarget = null;
        }
    }

    /**
     * 조합창을 입력 커서 옆으로 옮긴다.
     *
     * <p>운영체제 조합창은 기본적으로 창 왼쪽 아래 같은 엉뚱한 자리에 뜬다. GLFW 에는 게임 창
     * 픽셀 좌표로 넘겨야 하므로 GUI 배율을 곱한다.
     */
    private static void updateCaretRectangle(Minecraft minecraft, long window, EditBox box) {
        if (!imeControlWorks) {
            return;
        }

        int scale = minecraft.getWindow().getGuiScale();
        int x = box.getX() * scale;
        int y = box.getY() * scale;
        int height = box.getHeight() * scale;

        if (x == lastCaretX && y == lastCaretY) {
            return;
        }

        GLFW.glfwSetPreeditCursorRectangle(window, x, y, 1, height);
        lastCaretX = x;
        lastCaretY = y;
    }

    /** 포커스 사슬을 따라 내려가 텍스트 위젯을 찾는다. */
    private static EditBox findFocusedEditBox(GuiEventListener listener) {
        GuiEventListener current = listener;
        for (int depth = 0; depth < 16 && current != null; depth++) {
            if (current instanceof EditBox editBox) {
                return editBox;
            }
            if (current instanceof ContainerEventHandler container) {
                current = container.getFocused();
            } else {
                return null;
            }
        }
        return null;
    }

    /** 화면 어딘가에 텍스트 위젯이 있는가. 검색창이 있지만 아직 포커스가 없는 화면 때문이다. */
    private static boolean containsEditBox(Screen screen) {
        for (GuiEventListener child : screen.children()) {
            if (child instanceof EditBox) {
                return true;
            }
        }
        return false;
    }

    private ImeSupport() {
    }
}
