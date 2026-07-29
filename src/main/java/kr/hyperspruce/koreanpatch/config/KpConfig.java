package kr.hyperspruce.koreanpatch.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import kr.hyperspruce.koreanpatch.KoreanPatch;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * {@code config/koreanpatch/config.json} 하나에 모든 설정을 담는다.
 *
 * <p>기능별로 파일을 쪼개면 사용자가 설정을 옮기거나 백업할 때 빠뜨리기 쉽다. 양이 많지 않으니
 * 한 파일로 둔다.
 *
 * <p>필드를 직접 노출하는 이유: Gson 이 그대로 읽고 쓰며, 설정 화면에서도 그대로 만진다.
 * 접근자를 두면 항목을 추가할 때마다 세 곳(필드/게터/세터)을 고쳐야 해서 오히려 어긋나기 쉽다.
 */
public final class KpConfig {

    // ------------------------------------------------------------------
    // 최초 실행 처리 — "한 번만" 해야 하는 동작의 기록
    //
    // 사용자가 나중에 직접 되돌린 걸 모드가 매번 되돌려 놓으면 그건 버그가 아니라 민폐다.
    // 그래서 "했다"는 사실만 남기고 두 번 다시 건드리지 않는다.
    // ------------------------------------------------------------------

    /** 게임 언어를 한국어로 맞춘 적이 있는가. */
    public boolean koreanLanguageApplied = false;

    /** 서버 목록에 하이퍼팜을 넣은 적이 있는가. */
    public boolean hyperfServerAdded = false;

    /** 채팅 열기 키를 엔터로 옮긴 적이 있는가. */
    public boolean chatKeyRebound = false;

    /** 하이퍼팜 서버 이름을 새 이름으로 바꾼 적이 있는가. */
    public boolean hyperfServerRenamed = false;

    /**
     * 바닐라 F3 항목 상태를 원래대로 되돌린 적이 있는가.
     *
     * <p>초기 버전은 F3 를 간소화하려고 바닐라 항목을 전부 "표시 안 함" 으로 저장했다. 지금은
     * 화면을 직접 그리므로 그럴 필요가 없어졌고, 그때 남긴 흔적을 한 번 정리해야 간소화를
     * 껐을 때 바닐라 F3 가 정상으로 돌아온다.
     */
    public boolean debugEntriesRestored = false;

    /**
     * 지금까지 월드 안에서 보낸 틱. 20 틱이 1 초다.
     *
     * <p>바닐라 통계의 플레이 시간은 서버가 들고 있어서 화면에 계속 띄울 수 없다. 그래서
     * 직접 센다.
     */
    public long playTimeTicks = 0L;

    // ------------------------------------------------------------------
    // 기능별 설정
    // ------------------------------------------------------------------

    public Search search = new Search();
    public DebugScreen debugScreen = new DebugScreen();
    public FontSettings font = new FontSettings();
    public Ime ime = new Ime();

    /** 한글 입력. */
    public static final class Ime {
        /**
         * 조합 중인 글자를 입력 커서 자리에 바로 그릴지.
         *
         * <p>끄면 바닐라처럼 별도 상자에 뜬다.
         */
        public boolean inlinePreedit = true;
    }

    /** 한국어 검색. */
    public static final class Search {
        /** 초성만 입력해도 찾는다. "ㄱㅊ" → "금 곡괭이". */
        public boolean choseong = true;

        /** 한/영 전환을 깜빡했을 때 자동으로 해석한다. "ckaskan" → "참나무". */
        public boolean latinToHangul = true;

        /**
         * 아이템 ID 로도 찾는다. "diamond" → 다이아몬드 계열 전부.
         *
         * <p>게임이 한국어면 아이템 이름도 한국어라 영어로는 아무것도 안 나온다. 마인크래프트
         * 아이템 ID 는 전부 영어({@code diamond_sword}, {@code oak_log})라, ID 를 같이 보면
         * 사실상 영어 검색이 된다.
         */
        public boolean byItemId = true;
    }

    /** F3 간소화. */
    public static final class DebugScreen {
        /**
         * 간소화 F3 를 쓸지. 끄면 바닐라 F3 가 그대로 나온다.
         *
         * <p>설정 화면은 두지 않는다 — 켜고 끄는 것 말고 고를 게 없어서, 화면 하나를 더 만드는
         * 것보다 이 파일을 고치는 편이 낫다.
         */
        public boolean simplified = true;
    }

    /** 폰트 교체. */
    public static final class FontSettings {
        /** 폰트 교체 기능 자체를 쓸지. 끄면 바닐라/리소스팩 폰트가 그대로 쓰인다. */
        public boolean enabled = false;

        /**
         * 사용할 폰트 파일 경로. 시스템 폰트든 {@code config/koreanpatch/fonts/} 안의
         * 파일이든 절대 경로로 저장한다.
         */
        public String fontFile = "";

        /**
         * 리소스팩 글꼴보다 이 폰트를 앞세울지.
         *
         * <p>{@code true} 면 고른 폰트가 이기고, 그 폰트에 없는 글자만 리소스팩·바닐라로 넘어간다.
         * {@code false} 면 반대로 리소스팩이 이기고, 리소스팩에 없는 글자를 이 폰트가 메운다.
         */
        public boolean overrideResourcePacks = true;

        /** 글자 크기 배율. 1.0 이 바닐라 기본 크기다. */
        public float scale = 1.0f;

        /** 글자 두께 보정. 0 이 원본, 양수면 굵어진다. */
        public float weight = 0.0f;

        /** MSDF 아틀라스 한 변의 글리프 칸 크기(px). 크면 선명하지만 메모리를 더 쓴다. */
        public int glyphResolution = 48;
    }

    // ------------------------------------------------------------------
    // 로드 / 저장
    // ------------------------------------------------------------------

    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create();

    private static KpConfig instance;

    /** 설정을 읽는다. 처음 부르면 파일에서 로드하고, 없으면 기본값으로 만든다. */
    public static synchronized KpConfig get() {
        if (instance == null) {
            instance = load();
        }
        return instance;
    }

    private static Path file() {
        return FabricLoader.getInstance().getConfigDir().resolve(KoreanPatch.MOD_ID).resolve("config.json");
    }

    /** 사용자 폰트를 넣어 두는 폴더. 없으면 만든다. */
    public static Path fontDirectory() {
        Path dir = FabricLoader.getInstance().getConfigDir().resolve(KoreanPatch.MOD_ID).resolve("fonts");
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            KoreanPatch.LOG.warn("폰트 폴더를 만들지 못했다: {}", dir, e);
        }
        return dir;
    }

    private static KpConfig load() {
        Path path = file();
        if (!Files.isRegularFile(path)) {
            return new KpConfig();
        }

        try {
            String json = Files.readString(path, StandardCharsets.UTF_8);
            KpConfig loaded = GSON.fromJson(json, KpConfig.class);
            if (loaded == null) {
                // 파일이 비어 있으면 Gson 이 null 을 준다.
                return new KpConfig();
            }
            loaded.fillMissingSections();
            return loaded;
        } catch (IOException | JsonSyntaxException e) {
            // 설정이 깨졌다고 게임을 못 켜게 하면 안 된다. 기본값으로 계속 가고,
            // 저장 시점에 덮어써서 스스로 복구된다.
            KoreanPatch.LOG.warn("설정을 읽지 못해 기본값으로 시작한다: {}", path, e);
            return new KpConfig();
        }
    }

    /**
     * 예전 버전에서 만든 설정 파일에는 나중에 추가된 절이 통째로 없을 수 있다.
     * Gson 은 없는 필드를 {@code null} 로 두므로 그대로 쓰면 첫 접근에서 터진다.
     */
    private void fillMissingSections() {
        if (search == null) {
            search = new Search();
        }
        if (debugScreen == null) {
            debugScreen = new DebugScreen();
        }
        if (font == null) {
            font = new FontSettings();
        }
        if (ime == null) {
            ime = new Ime();
        }
    }

    /** 현재 설정을 파일에 쓴다. */
    public synchronized void save() {
        Path path = file();
        try {
            Files.createDirectories(path.getParent());
            Files.writeString(path, GSON.toJson(this) + System.lineSeparator(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            KoreanPatch.LOG.error("설정을 저장하지 못했다: {}", path, e);
        }
    }
}
