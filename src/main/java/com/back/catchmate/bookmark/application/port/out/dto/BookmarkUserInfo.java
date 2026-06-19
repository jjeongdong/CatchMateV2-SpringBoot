package com.back.catchmate.bookmark.application.port.out.dto;

public record BookmarkUserInfo(
        Long userId,
        Long clubId,
        String nickName,
        String profileImageUrl
) {
}
