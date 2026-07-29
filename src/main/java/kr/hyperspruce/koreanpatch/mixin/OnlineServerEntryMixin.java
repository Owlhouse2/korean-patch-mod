package kr.hyperspruce.koreanpatch.mixin;

import kr.hyperspruce.koreanpatch.feature.servers.HyperfServer;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.multiplayer.ServerSelectionList;
import net.minecraft.client.input.MouseButtonEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 하이퍼팜 서버 항목에 홈페이지 바로가기 아이콘을 붙인다.
 *
 * <p>바닐라 목록 항목을 그대로 두고 오른쪽 아래 모서리에만 얹는다. 다른 서버 항목은 전혀
 * 건드리지 않는다 — 주소가 일치할 때만 그린다.
 *
 * <p>클릭은 항목 선택보다 <b>먼저</b> 가로챈다. 아이콘을 눌렀는데 서버가 선택되기까지 하면
 * 의도와 다르다.
 *
 * <p>{@code require = 0} 인 이유: 주입이 빗나가도 게임은 떠야 한다. 그 경우 아이콘만 안 나온다.
 */
@Mixin(ServerSelectionList.OnlineServerEntry.class)
public abstract class OnlineServerEntryMixin {

    @Inject(method = "extractContent", at = @At("TAIL"), require = 0)
    private void koreanpatch$drawWebsiteLink(GuiGraphicsExtractor extractor,
                                             int mouseX, int mouseY,
                                             boolean hovered, float partialTick,
                                             CallbackInfo callback) {
        ServerSelectionList.OnlineServerEntry self = (ServerSelectionList.OnlineServerEntry) (Object) this;
        if (!HyperfServer.isHyperf(self.getServerData())) {
            return;
        }

        HyperfServer.drawLink(extractor, self.getContentRight(), self.getContentBottom(), mouseX, mouseY);
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true, require = 0)
    private void koreanpatch$clickWebsiteLink(MouseButtonEvent event, boolean doubleClick,
                                              CallbackInfoReturnable<Boolean> callback) {
        ServerSelectionList.OnlineServerEntry self = (ServerSelectionList.OnlineServerEntry) (Object) this;
        if (!HyperfServer.isHyperf(self.getServerData())) {
            return;
        }

        if (!HyperfServer.isOverLink(event.x(), event.y(), self.getContentRight(), self.getContentBottom())) {
            return;
        }

        HyperfServer.openWebsite();
        callback.setReturnValue(true);
    }
}
