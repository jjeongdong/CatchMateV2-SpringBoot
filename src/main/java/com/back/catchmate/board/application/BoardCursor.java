package com.back.catchmate.board.application;

import com.back.catchmate.board.domain.exception.BoardCursorInvalidException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.Base64;

// 게시글 목록 정렬 키(끌어올린 시각, id)를 응답의 nextCursor 문자열 하나로 주고받는다.
public record BoardCursor(LocalDateTime liftUpDate, Long boardId) {
    private static final String DELIMITER = "|";

    public String encode() {
        String raw = liftUpDate + DELIMITER + boardId;
        return Base64.getUrlEncoder().withoutPadding().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    public static BoardCursor decode(String cursor) {
        try {
            String raw = new String(Base64.getUrlDecoder().decode(cursor), StandardCharsets.UTF_8);
            int delimiterIndex = raw.indexOf(DELIMITER);
            if (delimiterIndex < 0) {
                throw new BoardCursorInvalidException();
            }
            return new BoardCursor(
                    LocalDateTime.parse(raw.substring(0, delimiterIndex)),
                    Long.valueOf(raw.substring(delimiterIndex + 1)));
        } catch (IllegalArgumentException | DateTimeParseException e) {
            // base64·숫자 형식 오류(NumberFormatException 포함)는 앱이 커서를 변조했거나 손상된 경우다.
            throw new BoardCursorInvalidException();
        }
    }
}
