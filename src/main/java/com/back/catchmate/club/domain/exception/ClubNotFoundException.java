package com.back.catchmate.club.domain.exception;

import com.back.catchmate.club.domain.ClubErrorCode;
import com.back.catchmate.global.error.BusinessException;

public class ClubNotFoundException extends BusinessException {
    public ClubNotFoundException() {
        super(ClubErrorCode.CLUB_NOT_FOUND);
    }
}
