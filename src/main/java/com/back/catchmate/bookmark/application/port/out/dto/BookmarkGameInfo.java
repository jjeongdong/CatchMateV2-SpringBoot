package com.back.catchmate.bookmark.application.port.out.dto;

public record BookmarkGameInfo(Long gameId, Long homeClubId, Long awayClubId, String location) {}
