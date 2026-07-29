package kr.hyperspruce.koreanpatch.mixin;

import kr.hyperspruce.koreanpatch.config.KpConfig;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.IMEPreeditOverlay;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 조합 중인 글자를 <b>입력 커서 자리에 바로</b> 그린다.
 *
 * <p>바닐라는 조합 중인 글자를 입력창 밖의 별도 상자(테두리 + 배경)에 띄운다. 한글은 거의 모든
 * 글자가 조합을 거치므로, 채팅을 칠 때마다 상자가 떴다 사라지길 반복해서 시선이 계속 끌린다.
 * 다른 프로그램에서 한글을 칠 때는 글자가 그 자리에 바로 찍히는데, 게임에서만 다르게 동작하는
 * 셈이다.
 *
 * <p>다행히 위치는 이미 정확하다 — {@code EditBox} 가 오버레이에 넘기는 좌표는 입력창 왼쪽이
 * 아니라 <b>커서의 화면 위치</b>다({@code EditBox} 바이트코드에서 커서 X 와 {@code textY} 를
 * 넘기는 것을 확인). 그래서 상자와 테두리를 안 그리고 글자만 같은 자리에 찍으면, 실제로 그
 * 자리에 타이핑되는 것처럼 보인다.
 *
 * <p>조합 중이라는 표시로 밑줄만 남긴다. 확정된 글자와 구분이 안 되면 오히려 헷갈린다.
 *
 * <p>{@code require = 0} 인 이유: 주입이 빗나가도 게임은 떠야 한다. 그 경우 바닐라 상자가
 * 그대로 나올 뿐이다.
 */
@Mixin(IMEPreeditOverlay.class)
public abstract class IMEPreeditOverlayMixin {

    @Shadow
    @Final
    private Font font;

    @Shadow
    @Final
    private Component preEditText;

    /** 확정된 글자와 같은 흰색. 조합 중이라고 색까지 다르면 오히려 읽기 어렵다. */
    @Unique
    private static final int TEXT_COLOR = 0xFFFFFFFF;

    @Shadow
    private int inputLeft;

    @Shadow
    private int inputTop;

    @Inject(method = "extractRenderState", at = @At("HEAD"), cancellable = true, require = 0)
    private void koreanpatch$drawInline(GuiGraphicsExtractor extractor,
                                        int mouseX, int mouseY, float partialTick,
                                        CallbackInfo callback) {
        if (!KpConfig.get().ime.inlinePreedit) {
            return;
        }

        callback.cancel();

        // 스타일이 붙은 컴포넌트가 아니라 글자만 뽑아 쓴다. 바닐라는 밝은 상자 배경에
        // 맞춰 어두운 글자색을 박아 두는데, 상자를 안 그리면 어두운 채팅 위에서 안 보인다.
        // 컴포넌트를 그대로 넘기면 그 색이 우리가 준 색을 이긴다.
        String text = preEditText.getString();
        if (text.isEmpty()) {
            return;
        }

        extractor.text(font, text, inputLeft, inputTop, TEXT_COLOR);

        // 조합 중임을 알리는 밑줄. 글자 아래 1px.
        int width = font.width(text);
        extractor.fill(inputLeft, inputTop + font.lineHeight - 1,
                inputLeft + width, inputTop + font.lineHeight,
                TEXT_COLOR);
    }
}
