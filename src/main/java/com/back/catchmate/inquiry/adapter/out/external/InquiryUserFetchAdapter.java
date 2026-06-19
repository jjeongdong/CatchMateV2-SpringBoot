package com.back.catchmate.inquiry.adapter.out.external;

import com.back.catchmate.inquiry.application.port.out.dto.InquiryUserInfo;
import com.back.catchmate.inquiry.application.port.out.external.UserFetchPort;
import com.back.catchmate.user.application.dto.response.UserInternalResponse;
import com.back.catchmate.user.application.port.in.UserInternalQueryUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class InquiryUserFetchAdapter implements UserFetchPort {
    private final UserInternalQueryUseCase userInternalQueryUseCase;

    @Override
    public InquiryUserInfo getUser(Long userId) {
        UserInternalResponse user = userInternalQueryUseCase.getUser(userId);
        return new InquiryUserInfo(user.userId(), user.nickName());
    }
}
