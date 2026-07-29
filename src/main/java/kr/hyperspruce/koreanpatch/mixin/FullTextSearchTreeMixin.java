package kr.hyperspruce.koreanpatch.mixin;

import kr.hyperspruce.koreanpatch.korean.KoreanSearch;
import net.minecraft.client.searchtree.FullTextSearchTree;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * 창고·크리에이티브·레시피북 검색에 초성/영타 해석을 얹는다.
 *
 * <p>화면마다 따로 손대지 않고 여기 한 곳만 잡는 이유: 마인크래프트의 아이템 검색은 화면이
 * 아니라 {@link FullTextSearchTree} 가 담당한다. 이 클래스를 고치면 검색창을 쓰는 모든 화면이
 * 한꺼번에 따라온다.
 *
 * <p>생성자에서 검색 대상과 이름 추출 함수를 붙잡아 둔다. {@code FullTextSearchTree} 는 그
 * 둘을 내부 검색 구조로만 넘기고 필드로 갖고 있지 않아서, 나중에 우리가 직접 훑으려면
 * 만들어질 때 받아 두는 수밖에 없다.
 */
@Mixin(FullTextSearchTree.class)
public abstract class FullTextSearchTreeMixin<T> {

    @Unique
    private Function<T, Stream<String>> koreanpatch$names;

    @Unique
    private Function<T, Stream<Identifier>> koreanpatch$identifiers;

    @Unique
    private List<T> koreanpatch$contents;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void koreanpatch$captureContents(Function<T, Stream<String>> names,
                                             Function<T, Stream<Identifier>> identifiers,
                                             List<T> contents,
                                             CallbackInfo callback) {
        this.koreanpatch$names = names;
        this.koreanpatch$identifiers = identifiers;
        this.koreanpatch$contents = contents;
    }

    /**
     * 바닐라가 아무것도 못 찾았을 때만 한국어식으로 다시 찾는다.
     *
     * <p>결과가 있으면 손대지 않는다 — 사용자가 의도한 검색을 우리가 늘려서 흐트러뜨릴 이유가
     * 없고, 기존 동작이 그대로 유지된다.
     */
    @Inject(method = "searchPlainText", at = @At("RETURN"), cancellable = true)
    private void koreanpatch$koreanFallback(String query, CallbackInfoReturnable<List<T>> callback) {
        List<T> vanilla = callback.getReturnValue();
        if (vanilla != null && !vanilla.isEmpty()) {
            return;
        }

        List<T> found = KoreanSearch.search(
                koreanpatch$contents, koreanpatch$names, koreanpatch$identifiers, query);
        if (!found.isEmpty()) {
            callback.setReturnValue(found);
        }
    }
}
