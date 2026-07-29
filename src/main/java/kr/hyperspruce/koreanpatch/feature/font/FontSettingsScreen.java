package kr.hyperspruce.koreanpatch.feature.font;

import kr.hyperspruce.koreanpatch.compat.McCompat;
import kr.hyperspruce.koreanpatch.config.KpConfig;
import kr.hyperspruce.koreanpatch.korean.Hangul;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 설치된 폰트를 골라 게임 글꼴로 쓰는 화면.
 *
 * <p>폰트 파일을 어딘가에 넣으라고 하지 않는다 — 컴퓨터에 이미 깔려 있는 폰트를 그대로 훑어
 * 보여 준다({@link SystemFonts}).
 *
 * <p>폰트가 백 개를 넘는 컴퓨터가 흔해서 <b>검색</b>이 목록보다 중요하다. 초성으로도 찾을 수
 * 있어서 "ㅁㅇㄱㄷ" 로 맑은 고딕이 나온다.
 *
 * <p>목록을 스크롤이 아니라 쪽 넘김으로 만든 이유: 마인크래프트의 자동 GUI 배율은 논리
 * 해상도를 320×240 언저리까지 떨어뜨려서, 창을 키워도 세로로 들어가는 줄 수가 크게 늘지
 * 않는다. 그 안에서 스크롤 막대까지 얹는 것보다 쪽을 넘기는 편이 조작이 확실하다.
 */
public final class FontSettingsScreen extends Screen {

    private static final int ROW_HEIGHT = 20;
    private static final int ROW_PITCH = 22;
    private static final int LIST_WIDTH = 220;

    /** 제목·검색창이 차지하는 높이. 목록은 이 아래부터 시작한다. */
    private static final int LIST_TOP = 46;

    /** 목록 아래에 필요한 높이 — 쪽 넘김 + 크기 조절 + 우선순위 + 완료 버튼. */
    private static final int FOOTER_HEIGHT = 100;

    private static final float MIN_SCALE = 0.5f;
    private static final float MAX_SCALE = 2.0f;
    private static final float SCALE_STEP = 0.05f;

    private final Screen parent;
    private final List<SystemFonts.Entry> allFonts;

    private List<SystemFonts.Entry> filtered;
    private String query = "";
    private int page;

    private String selected;
    private float scale;
    private boolean overrideResourcePacks;

    private final String initialSelection;
    private final float initialScale;
    private final boolean initialOverride;

    /**
     * 목록을 다시 만들어야 하는가.
     *
     * <p>검색창 입력 콜백 안에서 바로 위젯을 갈아엎으면, 마인크래프트가 그 위젯 목록을 훑는
     * 도중에 목록이 바뀌어 터진다. 그래서 표시만 해 두고 다음 틱에 처리한다.
     */
    private boolean needsRebuild;

    private EditBox search;

    public FontSettingsScreen(Screen parent) {
        super(Component.translatable("koreanpatch.font.title"));
        this.parent = parent;
        this.allFonts = SystemFonts.list();
        this.filtered = allFonts;

        KpConfig.FontSettings settings = KpConfig.get().font;
        this.selected = settings.enabled ? settings.fontFile : "";
        this.scale = settings.scale;
        this.overrideResourcePacks = settings.overrideResourcePacks;
        this.initialSelection = this.selected;
        this.initialScale = this.scale;
        this.initialOverride = this.overrideResourcePacks;
    }

    private int rowsPerPage() {
        int available = this.height - LIST_TOP - FOOTER_HEIGHT;
        return Math.max(3, Math.min(10, available / ROW_PITCH));
    }

    private int pageCount() {
        return Math.max(1, (entries().size() + rowsPerPage() - 1) / rowsPerPage());
    }

    /** 첫 항목은 늘 "기본 글꼴" 이다. 되돌리는 길이 검색 결과에 묻히면 안 된다. */
    private List<SystemFonts.Entry> entries() {
        List<SystemFonts.Entry> list = new ArrayList<>(filtered.size() + 1);
        list.add(null);   // null 이 기본 글꼴을 뜻한다
        list.addAll(filtered);
        return list;
    }

    @Override
    protected void init() {
        int left = (this.width - LIST_WIDTH) / 2;

        search = new EditBox(this.font, left, 20, LIST_WIDTH, ROW_HEIGHT,
                Component.translatable("koreanpatch.font.search"));
        search.setHint(Component.translatable("koreanpatch.font.search"));
        search.setMaxLength(64);
        search.setValue(query);
        search.setResponder(text -> {
            if (!text.equals(query)) {
                query = text;
                applyFilter();
                page = 0;
                needsRebuild = true;
            }
        });
        addRenderableWidget(search);

        List<SystemFonts.Entry> entries = entries();
        int rows = rowsPerPage();
        int start = page * rows;
        int end = Math.min(entries.size(), start + rows);

        for (int i = start; i < end; i++) {
            SystemFonts.Entry entry = entries.get(i);
            int y = LIST_TOP + (i - start) * ROW_PITCH;

            addRenderableWidget(Button.builder(label(entry), button -> {
                        selected = entry == null ? "" : entry.file().toString();
                        needsRebuild = true;
                    })
                    .bounds(left, y, LIST_WIDTH, ROW_HEIGHT)
                    .build());
        }

        int navigationY = LIST_TOP + rows * ROW_PITCH;

        addRenderableWidget(Button.builder(Component.literal("◀"), button -> turnPage(-1))
                .bounds(left, navigationY, 30, ROW_HEIGHT)
                .build());

        addRenderableWidget(Button.builder(Component.literal("▶"), button -> turnPage(1))
                .bounds(left + LIST_WIDTH - 30, navigationY, 30, ROW_HEIGHT)
                .build());

        int sizeY = navigationY + ROW_PITCH;

        addRenderableWidget(Button.builder(Component.literal("－"), button -> changeScale(-SCALE_STEP))
                .bounds(left, sizeY, 30, ROW_HEIGHT)
                .build());

        addRenderableWidget(Button.builder(sizeLabel(), button -> {
                    scale = 1.0f;
                    needsRebuild = true;
                })
                .bounds(left + 34, sizeY, LIST_WIDTH - 68, ROW_HEIGHT)
                .build());

        addRenderableWidget(Button.builder(Component.literal("＋"), button -> changeScale(SCALE_STEP))
                .bounds(left + LIST_WIDTH - 30, sizeY, 30, ROW_HEIGHT)
                .build());

        addRenderableWidget(Button.builder(priorityLabel(), button -> {
                    overrideResourcePacks = !overrideResourcePacks;
                    button.setMessage(priorityLabel());
                })
                .bounds(left, sizeY + ROW_PITCH, LIST_WIDTH, ROW_HEIGHT)
                .build());

        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> onClose())
                .bounds(this.width / 2 - 100, this.height - 28, 200, ROW_HEIGHT)
                .build());
    }

    /** 이름이나 초성으로 거른다. */
    private void applyFilter() {
        String needle = query.trim().toLowerCase(Locale.ROOT);
        if (needle.isEmpty()) {
            filtered = allFonts;
            return;
        }

        boolean choseong = Hangul.isChoseongQuery(needle);
        List<SystemFonts.Entry> result = new ArrayList<>();

        for (SystemFonts.Entry entry : allFonts) {
            String name = entry.displayName();
            boolean hit = choseong
                    ? Hangul.toChoseong(name).contains(needle.replace(" ", ""))
                    : name.toLowerCase(Locale.ROOT).contains(needle);
            if (hit) {
                result.add(entry);
            }
        }

        filtered = result;
    }

    private void turnPage(int delta) {
        int next = page + delta;
        if (next < 0 || next >= pageCount()) {
            return;
        }
        page = next;
        needsRebuild = true;
    }

    private void changeScale(float delta) {
        float next = Math.round((scale + delta) * 100.0f) / 100.0f;
        scale = Math.max(MIN_SCALE, Math.min(MAX_SCALE, next));
        needsRebuild = true;
    }

    @Override
    public void tick() {
        super.tick();

        // 위젯 교체는 이벤트 처리 밖에서 해야 안전하다.
        if (needsRebuild) {
            needsRebuild = false;

            boolean searchWasFocused = search != null && search.isFocused();

            this.clearWidgets();
            this.init();

            if (searchWasFocused) {
                this.setFocused(search);
                search.setFocused(true);
            }
        }
    }

    private Component label(SystemFonts.Entry entry) {
        Component name = entry == null
                ? Component.translatable("koreanpatch.font.vanilla")
                : Component.literal(entry.displayName());

        String path = entry == null ? "" : entry.file().toString();
        return path.equals(selected) ? Component.translatable("koreanpatch.font.selected", name) : name;
    }

    private Component sizeLabel() {
        return Component.translatable("koreanpatch.font.size", Math.round(scale * 100.0f));
    }

    private Component priorityLabel() {
        return Component.translatable(overrideResourcePacks
                ? "koreanpatch.font.priority.mod"
                : "koreanpatch.font.priority.pack");
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor extractor, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(extractor, mouseX, mouseY, partialTick);

        extractor.centeredText(this.font, this.title, this.width / 2, 6, 0xFFFFFFFF);

        Component summary = allFonts.isEmpty()
                ? Component.translatable("koreanpatch.font.none")
                : Component.translatable("koreanpatch.font.page", page + 1, pageCount(), filtered.size());

        int summaryY = LIST_TOP + rowsPerPage() * ROW_PITCH + 6;
        extractor.centeredText(this.font, summary, this.width / 2, summaryY, 0xFFA0A0A0);
    }

    @Override
    public void onClose() {
        KpConfig config = KpConfig.get();
        config.font.enabled = !selected.isEmpty();
        config.font.fontFile = selected;
        config.font.scale = scale;
        config.font.overrideResourcePacks = overrideResourcePacks;
        config.save();

        McCompat.setScreen(this.minecraft, parent);

        // 글꼴은 리소스 로딩 단계에서 만들어진다. 바꿨으면 다시 읽어야 화면에 반영된다.
        // 안 바꿨는데 리로드를 돌리면 몇 초씩 멈추므로 달라졌을 때만 한다.
        boolean changed = !selected.equals(initialSelection)
                || scale != initialScale
                || overrideResourcePacks != initialOverride;
        if (changed && this.minecraft != null) {
            this.minecraft.reloadResourcePacks();
        }
    }
}
