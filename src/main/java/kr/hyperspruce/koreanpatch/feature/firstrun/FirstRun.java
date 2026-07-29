package kr.hyperspruce.koreanpatch.feature.firstrun;

import kr.hyperspruce.koreanpatch.KoreanPatch;
import kr.hyperspruce.koreanpatch.config.KpConfig;
import com.mojang.blaze3d.platform.InputConstants;
import kr.hyperspruce.koreanpatch.event.ClientReady;
import kr.hyperspruce.koreanpatch.feature.servers.HyperfServer;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.debug.DebugScreenProfile;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.ServerList;
import net.minecraft.client.resources.language.LanguageManager;
import org.lwjgl.glfw.GLFW;

import java.util.Locale;

/**
 * 모드를 처음 켰을 때 한 번만 하는 준비 작업.
 *
 * <p>둘 다 "사용자가 나중에 되돌릴 수 있어야 한다"는 원칙을 따른다. 한 번 했다는 사실을
 * 설정에 남기고, 그 뒤로는 사용자가 언어를 바꾸든 서버를 지우든 다시 손대지 않는다.
 * 매번 되돌려 놓는 모드는 고장 난 모드다.
 *
 * <p>{@code onInitializeClient} 가 아니라 {@link ClientLifecycleEvents#CLIENT_STARTED} 에서
 * 도는 이유: 진입점은 {@code Minecraft} 생성자 한가운데서 불리므로 옵션·언어 관리자가
 * 아직 다 서 있다고 보장할 수 없다. 클라이언트가 완전히 뜬 뒤가 안전하다.
 */
public final class FirstRun {

    /** 마인크래프트의 한국어 로케일 코드. */
    private static final String KOREAN = "ko_kr";

    public static void register() {
        ClientReady.onReady(FirstRun::onClientStarted);
    }

    private static void onClientStarted(Minecraft minecraft) {
        KpConfig config = KpConfig.get();
        boolean dirty = false;

        if (!config.koreanLanguageApplied) {
            applyKoreanLanguage(minecraft);
            config.koreanLanguageApplied = true;
            dirty = true;
        }

        if (!config.hyperfServerAdded) {
            addHyperfServer(minecraft);
            config.hyperfServerAdded = true;
            dirty = true;
        }

        if (!config.chatKeyRebound) {
            bindChatToEnter(minecraft);
            config.chatKeyRebound = true;
            dirty = true;
        }

        if (!config.hyperfServerRenamed) {
            renameHyperfServer(minecraft);
            config.hyperfServerRenamed = true;
            dirty = true;
        }

        if (!config.debugEntriesRestored) {
            restoreDebugEntries(minecraft);
            config.debugEntriesRestored = true;
            dirty = true;
        }

        if (dirty) {
            config.save();
        }
    }

    /**
     * 게임 언어를 한국어로 맞춘다.
     *
     * <p>언어를 바꾸면 번역 파일을 다시 읽어야 하므로 리소스 리로드가 따라온다. 이미 한국어면
     * 아무것도 하지 않는다 — 첫 실행마다 이유 없이 리로드가 도는 걸 막는다.
     */
    private static void applyKoreanLanguage(Minecraft minecraft) {
        LanguageManager languages = minecraft.getLanguageManager();

        if (KOREAN.equals(languages.getSelected())) {
            KoreanPatch.LOG.info("이미 한국어로 설정되어 있다");
            return;
        }

        if (languages.getLanguage(KOREAN) == null) {
            // 정상적인 바닐라라면 있을 수 없다. 없다면 리소스가 덜 준비된 상태이므로
            // 억지로 밀어넣지 않는다.
            KoreanPatch.LOG.warn("한국어({}) 를 찾을 수 없어 언어를 바꾸지 않는다", KOREAN);
            return;
        }

        languages.setSelected(KOREAN);
        minecraft.options.languageCode = KOREAN;
        minecraft.options.save();

        minecraft.reloadResourcePacks().exceptionally(error -> {
            KoreanPatch.LOG.error("언어 변경 후 리소스를 다시 읽지 못했다", error);
            return null;
        });

        KoreanPatch.LOG.info("게임 언어를 한국어로 설정했다");
    }

    /**
     * 서버 목록에 하이퍼팜을 넣는다.
     *
     * <p>이미 같은 주소가 있으면(사용자가 직접 넣었거나 이전에 추가된 경우) 중복으로 넣지 않는다.
     */
    private static void addHyperfServer(Minecraft minecraft) {
        ServerList servers = new ServerList(minecraft);
        servers.load();

        for (int i = 0; i < servers.size(); i++) {
            if (HyperfServer.isHyperf(servers.get(i))) {
                KoreanPatch.LOG.info("서버 목록에 {} 가 이미 있다", HyperfServer.ADDRESS);
                return;
            }
        }

        servers.add(new ServerData(HyperfServer.DISPLAY_NAME, HyperfServer.ADDRESS, ServerData.Type.OTHER), false);
        servers.save();

        KoreanPatch.LOG.info("서버 목록에 {} 를 추가했다", HyperfServer.ADDRESS);
    }

    /**
     * 초기 버전이 넣은 영문 이름을 새 이름으로 바꾼다.
     *
     * <p>사용자가 직접 이름을 고쳐 뒀으면 건드리지 않는다 — 예전 기본 이름일 때만 바꾼다.
     */
    private static void renameHyperfServer(Minecraft minecraft) {
        ServerList servers = new ServerList(minecraft);
        servers.load();

        for (int i = 0; i < servers.size(); i++) {
            ServerData data = servers.get(i);
            if (HyperfServer.isHyperf(data) && HyperfServer.LEGACY_NAME.equals(data.name)) {
                data.name = HyperfServer.DISPLAY_NAME;
                servers.save();
                KoreanPatch.LOG.info("서버 이름을 '{}' 로 바꿨다", HyperfServer.DISPLAY_NAME);
                return;
            }
        }
    }

    /**
     * 채팅 열기를 T 에서 엔터로 옮긴다.
     *
     * <p>한국 서버에서 굳어진 관습이다. 엔터로 열고 엔터로 보내는 흐름이 손에 익어 있고,
     * 바닐라에서 엔터는 월드 안에서 아무 데도 안 쓰이므로 충돌하지 않는다.
     *
     * <p>이미 사용자가 T 가 아닌 다른 키로 바꿔 뒀다면 건드리지 않는다 — 취향을 덮어쓰는 건
     * 편의가 아니다.
     */
    private static void bindChatToEnter(Minecraft minecraft) {
        KeyMapping chat = minecraft.options.keyChat;

        if (!chat.isDefault()) {
            KoreanPatch.LOG.info("채팅 키가 이미 기본값이 아니라 그대로 둔다: {}",
                    chat.getTranslatedKeyMessage().getString());
            return;
        }

        chat.setKey(InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_ENTER));
        KeyMapping.resetMapping();
        minecraft.options.save();

        KoreanPatch.LOG.info("채팅 열기 키를 엔터로 바꿨다");
    }

    /**
     * 바닐라 F3 항목 상태를 기본값으로 되돌린다.
     *
     * <p>초기 버전이 간소화를 위해 모든 항목을 "표시 안 함" 으로 저장해 두었다. 지금은 화면을
     * 직접 그리므로 그 흔적이 남아 있으면, 간소화를 껐을 때 텅 빈 F3 가 나온다.
     */
    private static void restoreDebugEntries(Minecraft minecraft) {
        minecraft.debugEntries.loadProfile(DebugScreenProfile.DEFAULT);
        minecraft.debugEntries.save();
        KoreanPatch.LOG.info("바닐라 F3 항목 구성을 기본값으로 되돌렸다");
    }

    private FirstRun() {
    }
}
