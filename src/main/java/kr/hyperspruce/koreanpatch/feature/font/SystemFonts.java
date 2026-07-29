package kr.hyperspruce.koreanpatch.feature.font;

import kr.hyperspruce.koreanpatch.KoreanPatch;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;

/**
 * 컴퓨터에 설치된 폰트를 찾아 이름과 함께 목록으로 만든다.
 *
 * <p>사용자가 폰트 파일을 따로 폴더에 넣을 필요가 없다 — 운영체제 폰트 폴더를 그대로 훑는다.
 *
 * <p>이름은 파일명이 아니라 폰트 파일 안의 이름표를 읽어서 쓴다. {@code malgun.ttf} 라고
 * 보여 주면 그게 맑은 고딕인지 알 수 없다. 한국어 이름이 들어 있으면 그쪽을 우선한다.
 *
 * <p><b>AWT 를 쓰지 않는다.</b> {@code java.awt.Font} 로 읽으면 간단하지만, 마인크래프트는
 * AWT 를 건드리면 macOS 에서 창이 깨지는 문제가 있어 게임 전체가 AWT 를 피한다. 그래서
 * sfnt(트루타입/오픈타입) 의 {@code name} 테이블을 직접 읽는다.
 */
public final class SystemFonts {

    /** 폰트 하나. */
    public record Entry(String displayName, Path file) {
    }

    /** 이름표에서 "글꼴 계열" 을 뜻하는 번호. */
    private static final int NAME_ID_FAMILY = 1;

    /** 한국어 이름표의 언어 번호(Windows 플랫폼 기준). */
    private static final int LANGUAGE_KOREAN = 0x0412;

    /** 영어(미국) 언어 번호. 한국어가 없을 때의 차선. */
    private static final int LANGUAGE_ENGLISH_US = 0x0409;

    private static List<Entry> cached;

    /** 설치된 폰트 목록. 한 번 훑고 캐시한다 — 폴더를 뒤지는 비용이 작지 않다. */
    public static synchronized List<Entry> list() {
        if (cached == null) {
            cached = scan();
        }
        return cached;
    }

    /** 캐시를 버린다. 설정 화면에서 "다시 검색" 을 누르면 부른다. */
    public static synchronized void refresh() {
        cached = null;
    }

    private static List<Entry> scan() {
        // 같은 폰트가 여러 폴더에 있을 수 있다. 이름 기준으로 하나만 남긴다.
        Map<String, Entry> byName = new LinkedHashMap<>();

        for (Path directory : fontDirectories()) {
            if (!Files.isDirectory(directory)) {
                continue;
            }

            try (Stream<Path> files = Files.walk(directory, 3)) {
                files.filter(SystemFonts::isFontFile).forEach(file -> {
                    String name = readFamilyName(file);
                    if (name != null && !byName.containsKey(name)) {
                        byName.put(name, new Entry(name, file));
                    }
                });
            } catch (IOException | RuntimeException e) {
                KoreanPatch.LOG.debug("폰트 폴더를 읽지 못했다: {}", directory, e);
            }
        }

        List<Entry> found = new ArrayList<>(byName.values());
        found.sort(Comparator.comparing(entry -> entry.displayName().toLowerCase(Locale.ROOT)));

        KoreanPatch.LOG.info("설치된 폰트 {} 개를 찾았다", found.size());
        return found;
    }

    /** 운영체제별 폰트 폴더. 없는 경로는 뒤에서 걸러진다. */
    private static List<Path> fontDirectories() {
        List<Path> directories = new ArrayList<>();
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        String home = System.getProperty("user.home", "");

        if (os.contains("win")) {
            String windows = System.getenv("SystemRoot");
            directories.add(Path.of(windows == null ? "C:\\Windows" : windows, "Fonts"));

            // 관리자 권한 없이 설치한 폰트는 사용자 폴더에 들어간다.
            String localAppData = System.getenv("LOCALAPPDATA");
            if (localAppData != null) {
                directories.add(Path.of(localAppData, "Microsoft", "Windows", "Fonts"));
            }
        } else if (os.contains("mac")) {
            directories.add(Path.of("/System/Library/Fonts"));
            directories.add(Path.of("/Library/Fonts"));
            directories.add(Path.of(home, "Library", "Fonts"));
        } else {
            directories.add(Path.of("/usr/share/fonts"));
            directories.add(Path.of("/usr/local/share/fonts"));
            directories.add(Path.of(home, ".local", "share", "fonts"));
            directories.add(Path.of(home, ".fonts"));
        }

        return directories;
    }

    private static boolean isFontFile(Path path) {
        if (!Files.isRegularFile(path)) {
            return false;
        }
        String name = path.getFileName().toString().toLowerCase(Locale.ROOT);
        // .ttc(모음집)는 여러 폰트가 한 파일에 들어 있어 첫 번째만 읽게 된다. 일단 제외한다.
        return name.endsWith(".ttf") || name.endsWith(".otf");
    }

    // ------------------------------------------------------------------
    // sfnt name 테이블 읽기
    // ------------------------------------------------------------------

    /**
     * 폰트 파일에서 글꼴 계열 이름을 읽는다. 읽을 수 없으면 {@code null}.
     *
     * <p>구조는 이렇다: 파일 앞에 표 목록이 있고, 그중 {@code name} 표 안에 이름표가 여럿 들어
     * 있다. 이름표마다 플랫폼·언어·용도(번호)가 붙어 있어서 우리가 원하는 걸 골라야 한다.
     */
    private static String readFamilyName(Path file) {
        try {
            byte[] bytes = Files.readAllBytes(file);
            ByteBuffer buffer = ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN);

            if (buffer.remaining() < 12) {
                return null;
            }

            buffer.getInt();                         // sfnt 버전
            int tableCount = Short.toUnsignedInt(buffer.getShort());
            buffer.position(12);                     // 검색 힌트 3 개를 건너뛴다

            int nameOffset = -1;
            for (int i = 0; i < tableCount; i++) {
                if (buffer.remaining() < 16) {
                    return null;
                }
                byte[] tag = new byte[4];
                buffer.get(tag);
                buffer.getInt();                     // 체크섬
                int offset = buffer.getInt();
                buffer.getInt();                     // 길이

                if ("name".equals(new String(tag, StandardCharsets.US_ASCII))) {
                    nameOffset = offset;
                    break;
                }
            }

            if (nameOffset < 0 || nameOffset + 6 > bytes.length) {
                return null;
            }

            return readNameTable(buffer, bytes.length, nameOffset);
        } catch (IOException | RuntimeException e) {
            // 폰트가 아닌 파일이거나 손상된 경우. 목록에서 빠지면 그만이라 조용히 넘어간다.
            return null;
        }
    }

    private static String readNameTable(ByteBuffer buffer, int fileLength, int tableOffset) {
        buffer.position(tableOffset);
        buffer.getShort();                                          // 형식
        int recordCount = Short.toUnsignedInt(buffer.getShort());
        int stringsOffset = tableOffset + Short.toUnsignedInt(buffer.getShort());

        String best = null;
        int bestScore = -1;

        for (int i = 0; i < recordCount; i++) {
            if (buffer.remaining() < 12) {
                break;
            }

            int platformId = Short.toUnsignedInt(buffer.getShort());
            int encodingId = Short.toUnsignedInt(buffer.getShort());
            int languageId = Short.toUnsignedInt(buffer.getShort());
            int nameId = Short.toUnsignedInt(buffer.getShort());
            int length = Short.toUnsignedInt(buffer.getShort());
            int offset = Short.toUnsignedInt(buffer.getShort());

            if (nameId != NAME_ID_FAMILY) {
                continue;
            }

            int score = score(platformId, languageId);
            if (score <= bestScore) {
                continue;
            }

            int start = stringsOffset + offset;
            if (start < 0 || start + length > fileLength || length <= 0) {
                continue;
            }

            byte[] raw = new byte[length];
            int saved = buffer.position();
            buffer.position(start);
            buffer.get(raw);
            buffer.position(saved);

            // 플랫폼 3(Windows)과 0(유니코드)은 UTF-16BE, 1(매킨토시)은 단일 바이트다.
            String value = (platformId == 1)
                    ? new String(raw, StandardCharsets.ISO_8859_1)
                    : new String(raw, StandardCharsets.UTF_16BE);

            value = value.trim();
            if (!value.isEmpty()) {
                best = value;
                bestScore = score;
            }
        }

        return best;
    }

    /** 어떤 이름표를 고를지 점수를 매긴다. 한국어 이름이 있으면 가장 좋다. */
    private static int score(int platformId, int languageId) {
        if (platformId == 3 && languageId == LANGUAGE_KOREAN) {
            return 3;
        }
        if (platformId == 3 && languageId == LANGUAGE_ENGLISH_US) {
            return 2;
        }
        if (platformId == 3 || platformId == 0) {
            return 1;
        }
        return 0;
    }

    private SystemFonts() {
    }
}
