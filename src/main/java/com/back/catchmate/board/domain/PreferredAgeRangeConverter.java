package com.back.catchmate.board.domain;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * {@link PreferredAgeRange} ↔ DB 문자열 변환.
 *
 * <p>전환 전에는 {@code BoardEntity} 가 String 컬럼을, 도메인 모델이 값 객체를 각각 들고
 * {@code fromDomain}/{@code toDomain} 에서 변환했다. 둘을 합치면서 저장 형식은 그대로 두고
 * (컬럼 타입·구분자 변경 없음) 변환만 이 컨버터로 옮긴다.
 */
@Converter(autoApply = false)
public class PreferredAgeRangeConverter implements AttributeConverter<PreferredAgeRange, String> {

    @Override
    public String convertToDatabaseColumn(PreferredAgeRange attribute) {
        return attribute == null ? PreferredAgeRange.empty().asStored() : attribute.asStored();
    }

    @Override
    public PreferredAgeRange convertToEntityAttribute(String dbData) {
        return PreferredAgeRange.fromStored(dbData);
    }
}
