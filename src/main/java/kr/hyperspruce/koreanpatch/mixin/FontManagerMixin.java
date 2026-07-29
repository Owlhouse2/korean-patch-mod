package kr.hyperspruce.koreanpatch.mixin;

import com.mojang.blaze3d.font.GlyphProvider;
import kr.hyperspruce.koreanpatch.KoreanPatch;
import kr.hyperspruce.koreanpatch.config.KpConfig;
import kr.hyperspruce.koreanpatch.feature.font.FontLoader;
import net.minecraft.client.gui.font.FontManager;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.ArrayList;
import java.util.List;

/**
 * 사용자가 고른 폰트를 기본 글꼴 맨 앞에 끼워 넣는다.
 *
 * <p><b>왜 맨 앞인가</b>: {@code FontSet} 은 글자 하나를 그릴 때 공급자 목록을 앞에서부터 훑어
 * <b>처음</b> 글리프를 내주는 곳에서 멈춘다({@code FontSet.computeGlyphInfo} 바이트코드로 확인).
 * 그래서 목록 맨 앞에 두면 리소스팩이 넣은 글꼴보다 먼저 잡힌다 — 요구사항이던
 * "리소스팩보다 우선" 이 이 위치 하나로 해결된다.
 *
 * <p>뒤쪽 공급자를 지우지 않고 남겨 두는 것도 중요하다. 고른 폰트에 없는 글자(특수 기호,
 * 마인크래프트 고유 아이콘)는 자연스럽게 바닐라 글꼴로 넘어간다.
 *
 * <p>{@code minecraft:default} 에만 손댄다. 마법 부여대 글꼴({@code alt})까지 바꾸면
 * 읽히면 안 되는 글자가 읽히게 된다.
 *
 * <p>{@code require = 0} 인 이유: 이 주입이 실패해도 게임은 떠야 한다. 마인크래프트가 이
 * 메서드를 바꾸면 폰트 기능만 조용히 꺼지고 나머지는 그대로 동작한다.
 */
@Mixin(FontManager.class)
public abstract class FontManagerMixin {

    @ModifyVariable(method = "createFontSet", at = @At("HEAD"), argsOnly = true, index = 2, require = 0)
    private List<GlyphProvider.Conditional> koreanpatch$prependUserFont(
            List<GlyphProvider.Conditional> providers, Identifier fontId) {

        if (!Identifier.fromNamespaceAndPath("minecraft", "default").equals(fontId)) {
            return providers;
        }

        GlyphProvider.Conditional userFont = FontLoader.createFromConfig();
        if (userFont == null) {
            return providers;
        }

        boolean override = KpConfig.get().font.overrideResourcePacks;

        List<GlyphProvider.Conditional> combined = new ArrayList<>(providers.size() + 1);
        if (override) {
            // 맨 앞 = 우리 폰트가 먼저 잡힌다. 없는 글자만 뒤로 넘어간다.
            combined.add(userFont);
            combined.addAll(providers);
        } else {
            // 맨 뒤 = 리소스팩·바닐라가 먼저 잡히고, 그쪽에 없는 글자만 우리 폰트가 메운다.
            combined.addAll(providers);
            combined.add(userFont);
        }

        KoreanPatch.LOG.info("사용자 폰트를 적용했다 (리소스팩보다 {})", override ? "우선" : "나중");
        return combined;
    }
}
