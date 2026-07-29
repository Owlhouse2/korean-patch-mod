package kr.hyperspruce.koreanpatch.feature.servers;

import kr.hyperspruce.koreanpatch.KoreanPatch;
import kr.hyperspruce.koreanpatch.compat.McCompat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.network.chat.Component;

/**
 * 서버에 접속한 채로 다른 서버의 상태를 보고, 그대로 갈아탈 수 있는 화면.
 *
 * <p>바닐라 {@link JoinMultiplayerScreen} 을 <b>그대로 상속한다</b>. 목록·핑 표시·아이콘·정렬·
 * 추가/편집/삭제가 전부 바닐라 구현이라 생김새와 동작이 멀티플레이어 탭과 완전히 같다.
 * 화면을 새로 그리면 아무리 비슷하게 만들어도 결국 미묘하게 다르고, 바닐라가 바뀔 때마다
 * 따라가야 한다.
 *
 * <p>우리가 바꾸는 건 접속 동작 하나뿐이다. {@code join} 이 public 이라 상속만으로 가로챌 수
 * 있어서 믹스인이 필요 없다.
 *
 * <p>동시 접속은 만들지 않는다 — 마인크래프트 클라이언트는 연결을 하나만 들 수 있다.
 * 그래서 지금 서버에서 완전히 나간 다음 대상 서버로 붙는다.
 */
public final class InGameServerBrowser extends JoinMultiplayerScreen {

    /** 현재 화면을 부모로 삼아 서버 목록을 연다. */
    public static void open(Minecraft minecraft) {
        McCompat.setScreen(minecraft, new InGameServerBrowser(McCompat.currentScreen(minecraft)));
    }

    public InGameServerBrowser(Screen parent) {
        super(parent);
    }

    @Override
    public void join(ServerData server) {
        Minecraft minecraft = this.minecraft;
        if (minecraft == null) {
            return;
        }

        // 월드에 들어가 있지 않으면(타이틀에서 열린 경우) 바닐라 동작이 이미 정답이다.
        if (minecraft.level == null) {
            super.join(server);
            return;
        }

        KoreanPatch.LOG.info("서버 전환: {} 로 이동", server.ip);

        // 접속에 실패했을 때 돌아갈 곳. 이미 지금 서버에서 나온 뒤라 원래 화면으로는
        // 되돌아갈 수 없다 — 타이틀 위의 멀티플레이어 화면이 자연스러운 착지점이다.
        Screen fallback = new JoinMultiplayerScreen(new TitleScreen());

        // 연결을 먼저 확실히 끊는다. 끊기 전에 새 접속을 시작하면 두 연결이 겹친 상태가 되고,
        // 그 상태에서 서버가 킥을 보내면 어느 쪽 연결이 끊긴 건지 알 수 없게 된다.
        minecraft.disconnect(fallback, false);

        ConnectScreen.startConnecting(
                fallback,
                minecraft,
                ServerAddress.parseString(server.ip),
                server,
                false,
                null);
    }
}
