package com.back.catchmate.enroll.application.dto.response;

import com.back.catchmate.enroll.domain.model.AcceptStatus;
import com.back.catchmate.enroll.domain.model.Enroll;
import com.back.catchmate.board.application.dto.response.BoardResponse;
import com.back.catchmate.user.application.dto.response.UserResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
public class EnrollDetailResponse {
    private Long enrollId;
    private AcceptStatus acceptStatus;
    private String description;
    private LocalDateTime requestDate;
    private UserResponse applicant;
    private BoardResponse boardResponse;

    public static EnrollDetailResponse from(Enroll enroll) {
        return EnrollDetailResponse.builder()
                .enrollId(enroll.getId())
                .acceptStatus(enroll.getAcceptStatus())
                .description(enroll.getDescription())
                .requestDate(enroll.getRequestedAt())
                .applicant(UserResponse.from(enroll.getUser()))
                .boardResponse(BoardResponse.from(enroll.getBoard(), false))
                .build();
    }
}
