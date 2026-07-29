package kr.hyperspruce.koreanpatch.feature.screenshot;

import kr.hyperspruce.koreanpatch.KoreanPatch;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 스크린샷 폴더를 운영체제 파일 탐색기로 연다.
 *
 * <p>바닐라는 스크린샷을 찍고 나면 채팅에 파일명을 링크로 띄우지만, 그건 방금 찍은 한 장만
 * 가리킨다. 예전 스크린샷을 꺼내려면 게임을 끄고 폴더를 찾아 들어가야 한다.
 */
public final class ScreenshotFolder {

    /** 게임 폴더 기준 스크린샷 디렉터리 이름. 바닐라가 쓰는 이름과 같아야 한다. */
    private static final String DIRECTORY_NAME = "screenshots";

    /** 폴더를 열고, 실패하면 채팅으로 알린다. */
    public static void openAndReport(Minecraft minecraft) {
        Component failure = open(minecraft);
        if (failure != null && minecraft.player != null) {
            minecraft.player.sendSystemMessage(failure);
        }
    }

    /**
     * 스크린샷 폴더를 연다.
     *
     * <p>한 장도 안 찍었으면 폴더 자체가 없다. 그때 "열 수 없다"고 하면 사용자는 모드가
     * 고장 났다고 생각하므로, 없으면 만들어서 빈 폴더를 열어 준다.
     *
     * @return 실패 사유. 성공했으면 {@code null}.
     */
    public static Component open(Minecraft minecraft) {
        Path directory = minecraft.gameDirectory.toPath().resolve(DIRECTORY_NAME);

        try {
            Files.createDirectories(directory);
        } catch (IOException e) {
            KoreanPatch.LOG.error("스크린샷 폴더를 만들지 못했다: {}", directory, e);
            return Component.translatable("koreanpatch.screenshot.failed");
        }

        try {
            Util.getPlatform().openPath(directory);
        } catch (Throwable e) {
            // 파일 관리자가 없는 환경(일부 리눅스 최소 설치)에서는 열기 자체가 실패할 수 있다.
            KoreanPatch.LOG.error("스크린샷 폴더를 열지 못했다: {}", directory, e);
            return Component.translatable("koreanpatch.screenshot.failed");
        }

        return null;
    }

    private ScreenshotFolder() {
    }
}
