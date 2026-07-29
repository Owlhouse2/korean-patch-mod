package kr.hyperspruce.koreanpatch.event;

import kr.hyperspruce.koreanpatch.KoreanPatch;
import net.minecraft.client.Minecraft;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * "클라이언트가 완전히 떴다" 알림.
 *
 * <p>Fabric API 의 {@code ClientLifecycleEvents.CLIENT_STARTED} 자리를 대신한다. 이 모드는
 * Fabric API 를 의존하지 않으므로(사용자가 jar 하나만 넣으면 되게 하려고) 필요한 훅만 직접 만든다.
 *
 * <p>발화 지점은 {@code MinecraftMixin} 이다. {@code onGameLoadFinished} 를 직접 잡지 않는
 * 이유가 있다 — 그 메서드의 파라미터 타입이 26.1 의 {@code Minecraft$GameLoadCookie} 에서
 * 26.2 의 {@code GameLoadCookie}(최상위 클래스)로 바뀌었다. 믹스인은 메서드 디스크립터로
 * 대상을 찾으므로 한쪽에서는 반드시 빗나간다. 그래서 두 버전에서 시그니처가 같은
 * {@code tick()} 에 붙고, {@code isGameLoadFinished()} 로 시점을 판단한다.
 */
public final class ClientReady {

    private static final List<Consumer<Minecraft>> LISTENERS = new ArrayList<>();

    /** 클라이언트가 뜬 뒤 한 번 불릴 작업을 등록한다. */
    public static void onReady(Consumer<Minecraft> listener) {
        LISTENERS.add(listener);
    }

    /**
     * 등록된 작업을 모두 실행한다. 믹스인만 부른다.
     *
     * <p>하나가 터져도 나머지는 돈다. 서버 목록 등록이 실패했다고 언어 설정까지 날아갈
     * 이유가 없다.
     */
    public static void fire(Minecraft minecraft) {
        for (Consumer<Minecraft> listener : LISTENERS) {
            try {
                listener.accept(minecraft);
            } catch (Throwable e) {
                KoreanPatch.LOG.error("클라이언트 시작 작업이 실패했다", e);
            }
        }
    }

    private ClientReady() {
    }
}
