package kr.hyperspruce.koreanpatch.feature.esc;

import kr.hyperspruce.koreanpatch.KoreanPatch;
import kr.hyperspruce.koreanpatch.compat.McCompat;
import kr.hyperspruce.koreanpatch.feature.font.FontSettingsScreen;
import kr.hyperspruce.koreanpatch.feature.screenshot.ScreenshotFolder;
import kr.hyperspruce.koreanpatch.feature.servers.InGameServerBrowser;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;

import java.util.function.Consumer;

/**
 * 일시정지(ESC) 화면을 이 모드에 맞게 고쳐 놓는다.
 *
 * <p>버튼을 화면 아래에 새 줄로 덧붙이지 않고 <b>기존 버튼 자리를 물려받는다</b>. 이유가 둘 있다.
 * 하나는 바닐라 격자가 이미 화면 중앙에 예쁘게 정렬돼 있어서 그 자리에 들어가면 별도 정렬이
 * 필요 없다는 것. 다른 하나는 "플레이어 신고"·"버그 제보하기" 가 개인 서버에서 사실상 쓸모가
 * 없어서 그 자리가 비어 있는 것과 다름없다는 것이다.
 *
 * <ul>
 *   <li>버그 제보하기 → <b>스크린샷 폴더</b></li>
 *   <li>플레이어 신고 → <b>서버 목록</b></li>
 *   <li>좌측 상단(빈 공간) → <b>폰트 설정</b></li>
 * </ul>
 *
 * <p>바닐라 버튼을 찾을 때 화면에서의 순서가 아니라 <b>번역 키</b>로 찾는다. 순서로 찾으면
 * 마인크래프트가 메뉴 구성을 한 줄만 바꿔도 엉뚱한 버튼이 사라진다.
 */
public final class PauseScreenTweaks {

    /** 자리를 물려받을 바닐라 버튼들의 번역 키. */
    private static final String REPORT_BUGS = "menu.reportBugs";
    private static final String PLAYER_REPORTING = "menu.playerReporting";

    /** 좌측 상단 폰트 버튼의 여백과 크기. */
    private static final int CORNER_MARGIN = 6;
    private static final int CORNER_WIDTH = 80;
    private static final int CORNER_HEIGHT = 20;

    public static void apply(PauseScreen screen,
                             Consumer<AbstractWidget> add,
                             Consumer<GuiEventListener> remove) {

        takeOver(screen, add, remove, REPORT_BUGS,
                Component.translatable("koreanpatch.esc.screenshot"),
                ScreenshotFolder::openAndReport);

        takeOver(screen, add, remove, PLAYER_REPORTING,
                Component.translatable("koreanpatch.esc.servers"),
                InGameServerBrowser::open);

        // 폰트 설정은 물려받을 자리가 없어서 비어 있는 좌측 상단에 둔다.
        add.accept(Button.builder(
                        Component.translatable("koreanpatch.esc.font"),
                        button -> open(FontSettingsScreen::new))
                .bounds(CORNER_MARGIN, CORNER_MARGIN, CORNER_WIDTH, CORNER_HEIGHT)
                .build());
    }

    /**
     * 번역 키로 바닐라 버튼을 찾아 같은 자리·같은 크기의 우리 버튼으로 바꾼다.
     *
     * <p>못 찾으면 아무것도 하지 않는다. 마인크래프트가 그 버튼을 없앤 버전이라면 우리가 굳이
     * 새 자리를 만들어 끼워 넣는 것보다 조용히 빠지는 편이 낫다.
     */
    private static void takeOver(PauseScreen screen,
                                 Consumer<AbstractWidget> add,
                                 Consumer<GuiEventListener> remove,
                                 String translationKey,
                                 Component label,
                                 Consumer<Minecraft> action) {

        AbstractWidget target = find(screen, translationKey);
        if (target == null) {
            KoreanPatch.LOG.debug("일시정지 화면에서 '{}' 버튼을 찾지 못했다", translationKey);
            return;
        }

        int x = target.getX();
        int y = target.getY();
        int width = target.getWidth();
        int height = target.getHeight();

        remove.accept(target);

        add.accept(Button.builder(label, button -> run(label, action))
                .bounds(x, y, width, height)
                .build());
    }

    private static AbstractWidget find(PauseScreen screen, String translationKey) {
        for (GuiEventListener child : screen.children()) {
            if (child instanceof AbstractWidget widget
                    && widget.getMessage().getContents() instanceof TranslatableContents contents
                    && translationKey.equals(contents.getKey())) {
                return widget;
            }
        }
        return null;
    }

    /** 화면을 여는 버튼들의 공통 동작. 현재 화면을 부모로 넘겨 닫으면 돌아오게 한다. */
    private static void open(java.util.function.Function<net.minecraft.client.gui.screens.Screen,
            net.minecraft.client.gui.screens.Screen> factory) {
        Minecraft minecraft = Minecraft.getInstance();
        McCompat.setScreen(minecraft, factory.apply(McCompat.currentScreen(minecraft)));
    }

    /** 버튼 동작이 터져도 화면이 닫히거나 게임이 멈추지 않게 한다. */
    private static void run(Component label, Consumer<Minecraft> action) {
        try {
            action.accept(Minecraft.getInstance());
        } catch (Throwable e) {
            KoreanPatch.LOG.error("ESC 버튼 동작이 실패했다: {}", label.getString(), e);
        }
    }

    private PauseScreenTweaks() {
    }
}
