package kr.hyperspruce.koreanpatch.feature.f3;

import kr.hyperspruce.koreanpatch.config.KpConfig;
import net.minecraft.client.Minecraft;

/**
 * 누적 플레이 시간을 센다.
 *
 * <p>바닐라 통계의 "플레이 시간" 을 쓰지 않는 이유: 그건 서버가 들고 있는 값이고 통계 화면을
 * 열 때만 클라이언트로 내려온다. 화면에 계속 띄우려면 우리가 직접 세는 수밖에 없다.
 *
 * <p>벽시계가 아니라 <b>틱</b>을 센다. 게임이 멈춰 있거나(일시정지, 창 최소화) 프레임이 떨어져도
 * 실제로 플레이한 만큼만 늘어난다. 20 틱이 1 초다.
 *
 * <p>저장은 1 분에 한 번만 한다. 매 틱 파일을 쓰면 디스크를 초당 20 번 두드리게 된다.
 */
public final class PlayTime {

    private static final int TICKS_PER_SECOND = 20;
    private static final int TICKS_PER_MINUTE = TICKS_PER_SECOND * 60;

    /** 마지막 저장 이후 흐른 틱. 이게 1 분을 넘으면 설정에 반영한다. */
    private static int unsavedTicks;

    /** 월드 안에 있을 때만 1 틱씩 센다. */
    public static void tick(Minecraft minecraft) {
        if (minecraft.level == null) {
            return;
        }

        KpConfig config = KpConfig.get();
        config.playTimeTicks++;
        unsavedTicks++;

        if (unsavedTicks >= TICKS_PER_MINUTE) {
            unsavedTicks = 0;
            config.save();
        }
    }

    /** "12시간 34분" 형태의 문자열. */
    public static String formatted() {
        long totalMinutes = KpConfig.get().playTimeTicks / TICKS_PER_MINUTE;
        long hours = totalMinutes / 60;
        long minutes = totalMinutes % 60;
        return hours + "시간 " + minutes + "분";
    }

    private PlayTime() {
    }
}
