package com.back.catchmate.inquiry.application.port.out.external;

import com.back.catchmate.inquiry.application.port.out.dto.InquiryUserInfo;

public interface UserFetchPort {
    InquiryUserInfo getUser(Long userId);
}
