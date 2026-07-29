package kr.hyperspruce.koreanpatch.feature.ime;

import kr.hyperspruce.koreanpatch.KoreanPatch;
import org.lwjgl.glfw.GLFWNativeWin32;

import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;
import java.util.Locale;

/**
 * 글자를 칠 때가 아니면 입력기(IME)를 게임 창에서 떼어 놓는다.
 *
 * <p><b>고치는 문제</b>: 한글 상태로 두면 캐릭터가 움직이지 않는다. 입력기가 켜져 있는 동안
 * W·A·S·D 가 이동 키가 아니라 "ㅈ·ㅁ·ㄴ·ㅇ" 을 조합하는 입력으로 먹혀서 게임에는 키 이벤트가
 * 도달하지 않는다. 마인크래프트 자체는 입력기를 켜고 끄는 코드가 없어서 — 조합 콜백을
 * 받기만 한다 — 이 상태가 그대로 유지된다.
 *
 * <p><b>한/영 상태를 바꾸지 않는다.</b> 이게 이 클래스의 핵심이다. 예전 방식은 변환 모드의
 * {@code IME_CMODE_NATIVE} 비트를 직접 껐다 켰는데, 그러면 사용자가 채팅에서 영어를 치려고
 * 한/영을 눌러 놔도 다음에 채팅을 열 때 우리가 한글로 되돌려 버린다. 사용자 설정과 계속
 * 싸우는 셈이다.
 *
 * <p>대신 <b>입력 문맥(HIMC) 자체를 창에서 분리</b>한다({@code ImmAssociateContext} 에 NULL).
 * 한/영 상태는 그 문맥에 들어 있으므로, 다시 붙이면 사용자가 마지막에 고른 상태가 그대로
 * 살아난다. 우리는 상태를 읽지도 쓰지도 않는다 — 연결만 끊었다 잇는다.
 *
 * <p>호출은 {@code java.lang.foreign}(자바 표준 FFI)으로 {@code imm32.dll} 에 직접 한다.
 * 윈도우가 아니면 초기화 단계에서 스스로 꺼진다.
 */
public final class WindowsIme {

    private static MethodHandle immAssociateContext;

    private static boolean available;
    private static boolean initialised;

    /** 지금 입력기가 창에 붙어 있는가. */
    private static boolean attached = true;

    /**
     * 떼어 놓는 동안 보관해 둔 입력 문맥.
     *
     * <p>여기에 사용자의 한/영 상태가 들어 있다. 그대로 되돌려 붙이는 게 전부다.
     */
    private static MemorySegment detachedContext = MemorySegment.NULL;

    /** 이 환경에서 쓸 수 있는가. 처음 부를 때 한 번만 준비한다. */
    public static synchronized boolean isAvailable() {
        if (!initialised) {
            initialised = true;
            available = initialise();
        }
        return available;
    }

    private static boolean initialise() {
        if (!System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win")) {
            return false;
        }

        try {
            Linker linker = Linker.nativeLinker();
            SymbolLookup imm32 = SymbolLookup.libraryLookup("imm32", Arena.global());

            immAssociateContext = linker.downcallHandle(
                    imm32.find("ImmAssociateContext").orElseThrow(),
                    FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS));

            KoreanPatch.LOG.info("윈도우 IME 제어 준비 완료 (imm32)");
            return true;
        } catch (Throwable e) {
            // FFI 가 막혀 있거나 imm32 를 못 찾는 환경. 이 기능만 포기한다.
            KoreanPatch.LOG.warn("윈도우 IME 제어를 쓸 수 없다. 한글 상태에서 이동이 막히는 "
                    + "문제를 자동으로 고칠 수 없다.", e);
            return false;
        }
    }

    /**
     * 입력기를 창에 붙이거나 뗀다.
     *
     * @param glfwWindow GLFW 창 핸들
     * @param attach 글자를 칠 자리가 있으면 {@code true}. 월드를 조작하는 중이면 {@code false}.
     */
    public static void setAttached(long glfwWindow, boolean attach) {
        if (!isAvailable() || attach == attached) {
            return;
        }

        long hwnd = GLFWNativeWin32.glfwGetWin32Window(glfwWindow);
        if (hwnd == 0L) {
            return;
        }

        MemorySegment window = MemorySegment.ofAddress(hwnd);

        try {
            if (attach) {
                // 떼어 둔 문맥을 그대로 되돌린다. 한/영 상태가 여기 들어 있다.
                if (detachedContext.address() != 0L) {
                    immAssociateContext.invoke(window, detachedContext);
                }
                detachedContext = MemorySegment.NULL;
            } else {
                MemorySegment previous = (MemorySegment) immAssociateContext.invoke(window, MemorySegment.NULL);
                detachedContext = previous == null ? MemorySegment.NULL : previous;
            }

            attached = attach;
        } catch (Throwable e) {
            KoreanPatch.LOG.warn("입력기 연결을 바꾸지 못했다. 이 기능을 끈다.", e);
            available = false;
        }
    }

    private WindowsIme() {
    }
}
