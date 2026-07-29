package kr.hyperspruce.koreanpatch.mixin;

import kr.hyperspruce.koreanpatch.event.ClientReady;
import kr.hyperspruce.koreanpatch.feature.f3.PlayTime;
import kr.hyperspruce.koreanpatch.feature.ime.ImeSupport;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 클라이언트가 완전히 뜬 순간을 한 번만 잡아 {@link ClientReady} 를 발화한다.
 *
 * <p>{@code tick()} 에 붙는 이유는 {@link ClientReady} 주석에 적어 뒀다 — 요약하면
 * {@code onGameLoadFinished} 는 26.1 과 26.2 의 시그니처가 달라 한 jar 으로 둘 다 잡을 수 없다.
 * {@code tick()} 은 {@code ()V} 로 두 버전에서 같다.
 *
 * <p>매 틱 도는 코드지만 하는 일은 boolean 검사 하나다.
 */
@Mixin(Minecraft.class)
public abstract class MinecraftMixin {

    @Unique
    private boolean koreanpatch$readyFired;

    @Inject(method = "tick", at = @At("HEAD"))
    private void koreanpatch$onTick(CallbackInfo callback) {
        Minecraft minecraft = (Minecraft) (Object) this;

        // 리소스 로딩이 끝나기 전에는 언어 관리자나 옵션이 최종 상태가 아니다.
        if (!minecraft.isGameLoadFinished()) {
            return;
        }

        if (!koreanpatch$readyFired) {
            koreanpatch$readyFired = true;
            ClientReady.fire(minecraft);
        }

        ImeSupport.tick(minecraft);
        PlayTime.tick(minecraft);
    }
}
