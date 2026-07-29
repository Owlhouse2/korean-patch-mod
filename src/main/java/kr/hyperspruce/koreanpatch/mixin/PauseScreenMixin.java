package kr.hyperspruce.koreanpatch.mixin;

import kr.hyperspruce.koreanpatch.feature.esc.PauseScreenTweaks;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 일시정지(ESC) 화면에 이 모드의 버튼 줄을 붙인다.
 *
 * <p>Fabric API 의 {@code ScreenEvents.AFTER_INIT} 자리를 대신한다. {@code init} 이 끝난 뒤에
 * 얹으므로 바닐라 버튼은 그대로 남고, 다른 모드가 같은 화면에 붙인 위젯도 건드리지 않는다.
 *
 * <p>{@link Screen} 을 상속한 형태로 선언하는 건 믹스인의 관례다. 이렇게 해야 대상 클래스의
 * 상위에 있는 {@code addRenderableWidget}(protected) 을 그대로 부를 수 있다.
 */
@Mixin(PauseScreen.class)
public abstract class PauseScreenMixin extends Screen {

    private PauseScreenMixin(Component title) {
        // 믹스인 클래스는 실제로 생성되지 않는다. 상위 생성자를 만족시키기 위한 선언일 뿐이다.
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void koreanpatch$addMenuButtons(CallbackInfo callback) {
        PauseScreen self = (PauseScreen) (Object) this;

        // 월드 저장 중에 뜨는 "메뉴 없는" 일시정지 화면에는 붙이지 않는다.
        if (!self.showsPauseMenu()) {
            return;
        }

        PauseScreenTweaks.apply(self, this::addRenderableWidget, this::removeWidget);
    }
}
