package com.back.catchmate.oauth.application.port.out.external;

public interface ClubFetchPort {
    /**
     * 회원가입 시 선택한 favoriteClubId 가 존재하는 club 인지 검증.
     * 존재하지 않으면 어댑터가 BaseException 을 던진다.
     */
    void validateClubExists(Long clubId);
}
