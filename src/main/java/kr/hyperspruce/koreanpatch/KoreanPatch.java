package kr.hyperspruce.koreanpatch;

import kr.hyperspruce.koreanpatch.compat.McCompat;
import kr.hyperspruce.koreanpatch.feature.firstrun.FirstRun;
import net.fabricmc.api.ClientModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 한국인 패치 진입점.
 *
 * <p>기능마다 독립된 패키지를 갖고, 여기서는 등록 순서만 정한다. 한 기능이 초기화에 실패해도
 * 나머지가 같이 죽지 않도록 각각을 따로 감싼다 — 클라이언트 모드가 게임 부팅을 막는 건
 * 사용자 입장에서 최악이고, 그 원인이 폰트 설정 하나였다면 더더욱 그렇다.
 *
 * <p>ESC 화면 버튼과 IME 제어는 여기서 등록하지 않는다. 각각 {@code PauseScreenMixin} 과
 * {@code MinecraftMixin} 이 직접 붙잡는다.
 */
public final class KoreanPatch implements ClientModInitializer {

    public static final String MOD_ID = "koreanpatch";

    public static final Logger LOG = LoggerFactory.getLogger("한국인 패치");

    @Override
    public void onInitializeClient() {
        LOG.info("한국인 패치 초기화 (화면 관리 배치: {})",
                McCompat.isGuiScreenLayout() ? "26.2+ Gui" : "26.1 Minecraft");

        register("최초 실행 준비", FirstRun::register);
    }

    /**
     * 기능 하나를 등록한다. 터지면 그 기능만 포기하고 로그를 남긴다.
     *
     * <p>{@link Throwable} 까지 잡는 이유: 버전이 바뀌어 API 가 사라지면
     * {@link NoSuchMethodError} 같은 {@link Error} 로 오는데, 이건 {@link Exception} 이 아니다.
     * 여기서 안 잡으면 26.x 마이너 업데이트 하나에 모드가 통째로 부팅을 막는다.
     */
    private static void register(String name, Runnable registration) {
        try {
            registration.run();
        } catch (Throwable e) {
            LOG.error("[{}] 초기화에 실패해 이 기능은 꺼진 채로 계속한다", name, e);
        }
    }
}
