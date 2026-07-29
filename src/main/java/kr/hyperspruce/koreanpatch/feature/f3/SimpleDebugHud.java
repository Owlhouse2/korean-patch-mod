package kr.hyperspruce.koreanpatch.feature.f3;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * F3 화면을 직접 그린다.
 *
 * <p>처음에는 바닐라 디버그 항목으로 등록하는 방식을 썼지만 두 가지가 걸렸다.
 * <ul>
 *   <li>{@code DebugScreenDisplayer.addLine} 으로 넣은 줄은 오버레이가 <b>좌우로 반씩 나눠</b>
 *       배치한다({@code DebugScreenOverlay$1} 바이트코드로 확인). 일곱 줄을 넣으면 넷은 왼쪽,
 *       셋은 오른쪽으로 갈라져 "심플" 과 정반대가 된다.</li>
 *   <li>{@code Debug charts: [F3+1] ...} 안내문은 오버레이가 직접 찍는 것이라 항목을 아무리
 *       꺼도 남는다.</li>
 * </ul>
 *
 * <p>그래서 오버레이 자체를 대신 그린다. 배치·색·내용을 전부 우리가 정하게 되고, 안내문도
 * 자연히 사라진다.
 */
public final class SimpleDebugHud {

    /** 화면 가장자리에서 띄울 거리(px). */
    private static final int MARGIN = 2;

    /** 글자색 — 노란색. 어떤 지형 위에서도 읽힌다. */
    private static final int TEXT_COLOR = 0xFFFFFF55;

    /** 글자 뒤에 까는 반투명 바탕. 바닐라 디버그 화면과 같은 값이다. */
    private static final int BACKGROUND_COLOR = 0x90505050;

    public static void render(GuiGraphicsExtractor extractor) {
        Minecraft minecraft = Minecraft.getInstance();
        Font font = minecraft.font;

        List<String> lines = lines(minecraft);
        int y = MARGIN + 2;

        for (String line : lines) {
            int width = font.width(line);

            extractor.fill(MARGIN, y - 1, MARGIN + width + 2, y + font.lineHeight - 1, BACKGROUND_COLOR);
            extractor.text(font, line, MARGIN + 1, y, TEXT_COLOR);

            y += font.lineHeight;
        }
    }

    private static List<String> lines(Minecraft minecraft) {
        List<String> lines = new ArrayList<>(7);
        lines.add("FPS : " + minecraft.getFps());

        Entity camera = minecraft.getCameraEntity();
        Level level = minecraft.level;
        if (camera == null || level == null) {
            return lines;
        }

        lines.add(String.format(Locale.ROOT, "X : %.1f", camera.getX()));
        lines.add(String.format(Locale.ROOT, "Y : %.1f", camera.getY()));
        lines.add(String.format(Locale.ROOT, "Z : %.1f", camera.getZ()));
        lines.add("방향 : " + facing(camera));
        lines.add("바이옴 : " + biomeName(level, camera.blockPosition()));
        lines.add("플레이 시간 : " + PlayTime.formatted());

        return lines;
    }

    /** 바라보는 방향을 한글로. */
    private static String facing(Entity camera) {
        return switch (camera.getDirection()) {
            case NORTH -> "북쪽";
            case SOUTH -> "남쪽";
            case EAST -> "동쪽";
            case WEST -> "서쪽";
            case UP -> "위";
            case DOWN -> "아래";
        };
    }

    /**
     * 바이옴을 {@code taiga (타이가)} 형태로 만든다.
     *
     * <p>영문 ID 를 같이 보여 주는 이유: 명령어나 위키에서 쓰는 이름은 영문이라 한글만 있으면
     * 오히려 찾기 어렵다. 번역이 없는 바이옴(모드가 추가한 것 등)은 영문만 나온다.
     */
    private static String biomeName(Level level, BlockPos pos) {
        Holder<Biome> biome = level.getBiome(pos);

        return biome.unwrapKey()
                .map(key -> {
                    Identifier id = key.identifier();
                    String translationKey = "biome." + id.getNamespace() + "." + id.getPath();
                    String translated = Component.translatable(translationKey).getString();

                    // 번역이 없으면 Component 가 키를 그대로 돌려준다.
                    return translated.equals(translationKey)
                            ? id.getPath()
                            : id.getPath() + " (" + translated + ")";
                })
                .orElse("알 수 없음");
    }

    private SimpleDebugHud() {
    }
}
