package com.back.catchmate.board.application.dto.response;

import java.time.LocalDate;

/**
 * board 응답에 임베드되는 작성자 요약. board 컨텍스트가 자체 소유한 API 계약 — user 컨텍스트의 UserResponse 변경에 영향받지 않음.
 */
public record BoardWriterView(
        Long userId,
        String nickName,
        String email,
        String profileImageUrl,
        char gender,
        LocalDate birthDate,
        String watchStyle,
        BoardClubView club,
        String authority
) {
}
