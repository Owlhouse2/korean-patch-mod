package kr.hyperspruce.koreanpatch.feature.font;

import com.mojang.blaze3d.font.GlyphProvider;
import com.mojang.blaze3d.font.TrueTypeGlyphProvider;
import kr.hyperspruce.koreanpatch.KoreanPatch;
import kr.hyperspruce.koreanpatch.config.KpConfig;
import net.minecraft.client.gui.font.FontOption;
import net.minecraft.client.gui.font.providers.FreeTypeUtil;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.util.freetype.FT_Face;
import org.lwjgl.util.freetype.FreeType;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 고른 폰트 파일을 마인크래프트가 쓸 수 있는 글리프 공급자로 만든다.
 *
 * <p>바닐라가 트루타입 폰트를 읽을 때 쓰는 것과 같은 길을 탄다 — FreeType 으로 얼굴(face)을
 * 열고 {@link TrueTypeGlyphProvider} 로 감싼다. 마인크래프트에 이미 FreeType 이 들어 있어서
 * (LWJGL 3.4.1) 추가 의존성이 없다.
 *
 * <p>폰트 데이터는 <b>힙 밖</b>에 올려야 한다. FreeType 은 네이티브 코드라 자바 가비지 컬렉터가
 * 옮겨 다니는 배열을 가리킬 수 없다. {@code MemoryUtil.memAlloc} 으로 잡은 버퍼는
 * {@code TrueTypeGlyphProvider.close()} 가 얼굴과 함께 정리한다.
 */
public final class FontLoader {

    /**
     * 기준 글자 크기(pt).
     *
     * <p>바닐라 기본 글꼴의 글자 높이가 8 픽셀이라, 같은 자리에 들어가려면 이 정도가 맞다.
     * 사용자가 배율을 주면 여기에 곱한다.
     */
    private static final float BASE_SIZE = 11.0f;

    /**
     * 확대 배율. 실제보다 크게 그려서 줄이면 가장자리가 덜 뭉갠다.
     *
     * <p>MSDF 를 쓰기 전까지 선명도를 확보하는 가장 값싼 방법이다.
     */
    private static final float OVERSAMPLE = 2.0f;

    /**
     * 설정에 지정된 폰트로 글리프 공급자를 만든다.
     *
     * @return 만들지 못했으면 {@code null} — 이 경우 바닐라 글꼴이 그대로 쓰인다.
     */
    public static GlyphProvider.Conditional createFromConfig() {
        KpConfig.FontSettings settings = KpConfig.get().font;
        if (!settings.enabled || settings.fontFile == null || settings.fontFile.isBlank()) {
            return null;
        }

        Path file = Path.of(settings.fontFile);
        if (!Files.isRegularFile(file)) {
            KoreanPatch.LOG.warn("설정된 폰트 파일이 없다: {}", file);
            return null;
        }

        float size = BASE_SIZE * Math.max(0.5f, settings.scale);

        try {
            return new GlyphProvider.Conditional(load(file, size), FontOption.Filter.ALWAYS_PASS);
        } catch (IOException | RuntimeException e) {
            // 폰트가 깨졌거나 FreeType 이 못 읽는 형식이다. 글꼴 하나 때문에 게임이 글자를
            // 못 그리게 되면 안 되므로 조용히 바닐라로 돌아간다.
            KoreanPatch.LOG.error("폰트를 읽지 못해 기본 글꼴을 쓴다: {}", file, e);
            return null;
        }
    }

    private static TrueTypeGlyphProvider load(Path file, float size) throws IOException {
        byte[] bytes = Files.readAllBytes(file);

        ByteBuffer buffer = MemoryUtil.memAlloc(bytes.length);
        buffer.put(bytes);
        buffer.flip();

        try {
            synchronized (FreeTypeUtil.LIBRARY_LOCK) {
                try (MemoryStack stack = MemoryStack.stackPush()) {
                    PointerBuffer facePointer = stack.mallocPointer(1);

                    FreeTypeUtil.assertError(
                            FreeType.FT_New_Memory_Face(FreeTypeUtil.getLibrary(), buffer, 0L, facePointer),
                            "폰트 얼굴을 여는 중");

                    FT_Face face = FT_Face.create(facePointer.get(0));

                    // 이 시점부터 buffer 의 수명은 provider 가 책임진다 (close 에서 함께 정리).
                    return new TrueTypeGlyphProvider(buffer, face, size, OVERSAMPLE, 0.0f, 0.0f, "");
                }
            }
        } catch (RuntimeException e) {
            // 얼굴을 못 열었으면 provider 가 생기지 않았으므로 버퍼를 우리가 반납해야 한다.
            MemoryUtil.memFree(buffer);
            throw e;
        }
    }

    private FontLoader() {
    }
}
