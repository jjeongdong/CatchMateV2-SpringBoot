package com.back.catchmate.domain.board.model;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * 게시글의 선호 연령대 목록을 표현하는 값 객체.
 *
 * <p>도메인 내부에서는 정규화된 불변 목록 형태로 다루며,
 * 영속성 / API 표현으로는 콤마 구분 문자열로 직렬화한다.
 *
 * <ul>
 *   <li>null / 공백 원소는 자동 제거되고, 양끝 공백은 trim 처리된다.</li>
 *   <li>{@link #empty()} 는 빈 선호 연령대를 의미한다.</li>
 * </ul>
 */
public final class PreferredAgeRange {
    private static final String DELIMITER = ",";
    private static final PreferredAgeRange EMPTY = new PreferredAgeRange(List.of());

    private final List<String> values;

    private PreferredAgeRange(List<String> values) {
        this.values = values;
    }

    public static PreferredAgeRange empty() {
        return EMPTY;
    }

    /**
     * API/Command 계층에서 전달된 목록을 값 객체로 변환한다.
     */
    public static PreferredAgeRange of(List<String> values) {
        if (values == null || values.isEmpty()) {
            return EMPTY;
        }
        List<String> sanitized = values.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(v -> !v.isEmpty())
                .toList();
        return sanitized.isEmpty() ? EMPTY : new PreferredAgeRange(sanitized);
    }

    /**
     * 영속 저장된 콤마 구분 문자열을 값 객체로 복원한다.
     */
    public static PreferredAgeRange fromStored(String stored) {
        if (stored == null || stored.isBlank()) {
            return EMPTY;
        }
        return of(Arrays.asList(stored.split(DELIMITER)));
    }

    public List<String> asList() {
        return values;
    }

    /**
     * 영속/응답용 콤마 구분 문자열로 직렬화한다.
     */
    public String asStored() {
        return String.join(DELIMITER, values);
    }

    public boolean isEmpty() {
        return values.isEmpty();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PreferredAgeRange that)) return false;
        return Objects.equals(values, that.values);
    }

    @Override
    public int hashCode() {
        return Objects.hash(values);
    }
}
