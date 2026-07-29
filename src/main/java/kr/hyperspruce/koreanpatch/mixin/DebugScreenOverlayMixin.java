package kr.hyperspruce.koreanpatch.mixin;

import kr.hyperspruce.koreanpatch.config.KpConfig;
import kr.hyperspruce.koreanpatch.feature.f3.SimpleDebugHud;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.DebugScreenOverlay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 간소화 F3 가 켜져 있으면 바닐라 디버그 화면을 통째로 대신 그린다.
 *
 * <p>항목만 꺼서는 원하는 모양이 안 나온다.
 * <ul>
 *   <li>{@code addLine} 으로 넣은 줄은 오버레이가 <b>좌우로 반씩 나눠</b> 배치한다. 일곱 줄을
 *       넣으면 넷은 왼쪽, 셋은 오른쪽으로 갈라진다.</li>
 *   <li>{@code Debug charts: [F3+1] ...} 안내문은 오버레이가 직접 찍는 것이라 항목을 다 꺼도
 *       그대로 남는다.</li>
 * </ul>
 *
 * <p>그래서 여기서 잘라내고 {@link SimpleDebugHud} 로 넘긴다. 배치·색·내용을 전부 우리가
 * 정하게 된다.
 *
 * <p>{@code require = 0} 인 이유: 주입이 빗나가도 게임은 떠야 한다. 그 경우 바닐라 F3 가
 * 그대로 나오고 나머지 기능은 영향이 없다.
 */
@Mixin(DebugScreenOverlay.class)
public abstract class DebugScreenOverlayMixin {

    @Inject(method = "extractRenderState", at = @At("HEAD"), cancellable = true, require = 0)
    private void koreanpatch$drawSimpleOverlay(GuiGraphicsExtractor extractor, CallbackInfo callback) {
        if (!KpConfig.get().debugScreen.simplified) {
            return;
        }

        DebugScreenOverlay self = (DebugScreenOverlay) (Object) this;
        callback.cancel();

        if (self.showDebugScreen()) {
            SimpleDebugHud.render(extractor);
        }
    }
}
