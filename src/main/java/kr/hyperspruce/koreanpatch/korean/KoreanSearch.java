package kr.hyperspruce.koreanpatch.korean;

import kr.hyperspruce.koreanpatch.config.KpConfig;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * 한국어 사용자가 실제로 치는 방식으로 아이템을 찾아 준다.
 *
 * <p>두 가지를 처리한다.
 * <ul>
 *   <li><b>초성</b> — {@code ㄱㄱㅇ} 로 "곡괭이" 를 찾는다.</li>
 *   <li><b>영타</b> — 한/영 전환을 깜빡하고 친 {@code ckaskan} 을 "참나무" 로 읽는다.</li>
 * </ul>
 *
 * <p>바닐라 검색이 <b>아무것도 못 찾았을 때만</b> 끼어든다. 결과가 있으면 그건 사용자가 의도한
 * 검색이므로 우리가 결과를 늘려서 흐트러뜨릴 이유가 없다. 이 방식이면 기존 검색 동작이
 * 한 글자도 바뀌지 않는다.
 */
public final class KoreanSearch {

    /**
     * 한국어식 해석으로 다시 찾는다.
     *
     * @param contents 검색 대상 전체
     * @param names    대상 하나에서 검색용 문자열들을 뽑는 함수 (아이템 이름, 툴팁 등)
     * @param query    사용자가 친 것
     * @return 찾은 것들. 해당 사항이 없으면 빈 목록.
     */
    public static <T> List<T> search(List<T> contents,
                                     Function<T, Stream<String>> names,
                                     Function<T, Stream<Identifier>> identifiers,
                                     String query) {

        String trimmed = query.trim();
        if (trimmed.isEmpty() || contents == null || names == null) {
            return List.of();
        }

        KpConfig.Search settings = KpConfig.get().search;

        if (settings.choseong && Hangul.isChoseongQuery(trimmed)) {
            List<T> found = byChoseong(contents, names, trimmed);
            if (!found.isEmpty()) {
                return found;
            }
        }

        if (settings.latinToHangul) {
            String converted = Hangul.latinToHangul(trimmed);
            // 바뀐 게 없으면 애초에 한글 자판으로 해석할 수 있는 입력이 아니었다.
            if (!converted.equals(trimmed)) {
                List<T> found = byPlainText(contents, names, converted);
                if (!found.isEmpty()) {
                    return found;
                }
            }
        }

        if (settings.byItemId && identifiers != null) {
            return byIdentifier(contents, identifiers, trimmed);
        }

        return List.of();
    }

    /**
     * 아이템 ID 로 찾는다. 사실상 영어 검색이다.
     *
     * <p>게임이 한국어면 아이템 이름도 한국어라 "diamond" 로는 아무것도 안 나온다. 마인크래프트
     * 아이템 ID 는 전부 영어({@code diamond_sword}, {@code oak_log})라서, ID 를 같이 보면 영어로도
     * 찾을 수 있다.
     *
     * <p>비교 전에 밑줄과 공백을 모두 지운다. "oak log" 와 "oak_log" 와 "oaklog" 가 같은 것을
     * 가리키는데 사용자가 어느 쪽으로 칠지 알 수 없다.
     */
    private static <T> List<T> byIdentifier(List<T> contents,
                                            Function<T, Stream<Identifier>> identifiers,
                                            String query) {
        String needle = squash(query);
        if (needle.isEmpty()) {
            return List.of();
        }

        List<T> found = new ArrayList<>();
        for (T item : contents) {
            try (Stream<Identifier> stream = identifiers.apply(item)) {
                if (stream.anyMatch(id -> squash(id.getPath()).contains(needle))) {
                    found.add(item);
                }
            } catch (RuntimeException e) {
                // 이 항목만 건너뛴다. 검색 하나 때문에 창고 화면이 죽으면 안 된다.
            }
        }
        return found;
    }

    /** 비교용으로 소문자화하고 구분자를 지운다. */
    private static String squash(String text) {
        return text.toLowerCase(Locale.ROOT).replace("_", "").replace(" ", "").replace("-", "");
    }

    private static <T> List<T> byChoseong(List<T> contents,
                                          Function<T, Stream<String>> names,
                                          String query) {
        String needle = query.replace(" ", "").toLowerCase(Locale.ROOT);
        List<T> found = new ArrayList<>();

        for (T item : contents) {
            if (anyName(names, item, name -> Hangul.toChoseong(name).replace(" ", "").contains(needle))) {
                found.add(item);
            }
        }
        return found;
    }

    private static <T> List<T> byPlainText(List<T> contents,
                                           Function<T, Stream<String>> names,
                                           String query) {
        String needle = query.toLowerCase(Locale.ROOT);
        List<T> found = new ArrayList<>();

        for (T item : contents) {
            if (anyName(names, item, name -> name.toLowerCase(Locale.ROOT).contains(needle))) {
                found.add(item);
            }
        }
        return found;
    }

    /**
     * 대상 하나의 이름들 중 하나라도 조건에 맞는가.
     *
     * <p>이름을 뽑는 함수가 터질 수 있다(모드 아이템의 툴팁이 예외를 내는 경우가 실제로 있다).
     * 검색 하나 때문에 창고 화면이 죽으면 안 되므로 그 항목만 건너뛴다.
     */
    private static <T> boolean anyName(Function<T, Stream<String>> names,
                                       T item,
                                       java.util.function.Predicate<String> test) {
        try (Stream<String> stream = names.apply(item)) {
            return stream.anyMatch(test);
        } catch (RuntimeException e) {
            return false;
        }
    }

    private KoreanSearch() {
    }
}
