package kr.hyperspruce.koreanpatch.feature.servers;

import kr.hyperspruce.koreanpatch.KoreanPatch;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.util.Util;

import java.util.Locale;

/**
 * 서버 목록에서 하이퍼팜 항목을 특별 취급한다 — 홈페이지 바로가기 아이콘.
 *
 * <p>아이콘은 텍스처 파일이 아니라 사각형 몇 개로 그린다. "네모 밖으로 나가는 화살표"는
 * 선으로만 이루어져 있어서, 10 픽셀 크기에서는 직접 그리는 편이 오히려 또렷하다. 리소스팩이나
 * 텍스처 등록도 필요 없다.
 */
public final class HyperfServer {

    /** 서버 주소. 이 주소를 가진 항목에만 아이콘이 붙는다. */
    public static final String ADDRESS = "hyperf.kr";

    /** 서버 목록에 표시될 이름. */
    public static final String DISPLAY_NAME = "귀여운 마인팜서버";

    /** 초기 버전이 넣었던 이름. 이 이름일 때만 새 이름으로 바꾼다. */
    public static final String LEGACY_NAME = "Hyperf";

    private static final String WEBSITE = "https://hyperf.kr";

    /** 아이콘 한 변의 크기(px). */
    private static final int SIZE = 10;

    /** 항목 오른쪽·아래 모서리에서 띄울 거리(px). */
    private static final int MARGIN = 2;

    private static final int COLOR = 0xFFB0C4DE;
    private static final int COLOR_HOVERED = 0xFFFFFFFF;

    public static boolean isHyperf(ServerData data) {
        return data != null
                && data.ip != null
                && data.ip.trim().toLowerCase(Locale.ROOT).equals(ADDRESS);
    }

    private static int iconX(int contentRight) {
        return contentRight - SIZE - MARGIN;
    }

    private static int iconY(int contentBottom) {
        return contentBottom - SIZE - MARGIN;
    }

    /** 마우스가 아이콘 위에 있는가. 손가락이 미끄러져도 눌리도록 1 픽셀씩 넉넉히 잡는다. */
    public static boolean isOverLink(double mouseX, double mouseY, int contentRight, int contentBottom) {
        int x = iconX(contentRight);
        int y = iconY(contentBottom);
        return mouseX >= x - 1 && mouseX <= x + SIZE + 1
                && mouseY >= y - 1 && mouseY <= y + SIZE + 1;
    }

    /** 홈페이지를 연다. 사용자가 아이콘을 직접 눌렀을 때만 불린다. */
    public static void openWebsite() {
        try {
            Util.getPlatform().openUri(WEBSITE);
        } catch (Throwable e) {
            KoreanPatch.LOG.error("홈페이지를 열지 못했다: {}", WEBSITE, e);
        }
    }

    /**
     * "네모 밖으로 나가는 화살표" 아이콘을 그린다.
     *
     * <p>아래쪽 네모와 오른쪽 위로 뻗는 화살표. 웹 링크를 뜻하는 관용적인 모양이라 따로 설명이
     * 필요 없다.
     */
    public static void drawLink(GuiGraphicsExtractor extractor,
                                int contentRight, int contentBottom,
                                int mouseX, int mouseY) {

        int x = iconX(contentRight);
        int y = iconY(contentBottom);
        int color = isOverLink(mouseX, mouseY, contentRight, contentBottom) ? COLOR_HOVERED : COLOR;

        // 아래쪽 네모 (테두리만)
        extractor.fill(x, y + 3, x + 7, y + 4, color);           // 위
        extractor.fill(x, y + 9, x + 7, y + 10, color);          // 아래
        extractor.fill(x, y + 3, x + 1, y + 10, color);          // 왼쪽
        extractor.fill(x + 6, y + 6, x + 7, y + 10, color);      // 오른쪽 (화살표가 지나갈 위는 비운다)

        // 오른쪽 위로 뻗는 대각선
        for (int step = 0; step < 5; step++) {
            int px = x + 4 + step;
            int py = y + 5 - step;
            extractor.fill(px, py, px + 1, py + 1, color);
        }

        // 화살촉 (ㄱ 자 모양)
        extractor.fill(x + 5, y, x + 10, y + 1, color);
        extractor.fill(x + 9, y, x + 10, y + 5, color);
    }

    private HyperfServer() {
    }
}
