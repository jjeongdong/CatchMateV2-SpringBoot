package com.back.catchmate.orchestration.enroll.dto.response;

import com.back.catchmate.domain.enroll.model.AcceptStatus;
import com.back.catchmate.domain.enroll.model.Enroll;
import com.back.catchmate.orchestration.board.dto.response.BoardResponse;
import com.back.catchmate.orchestration.user.dto.response.UserResponse;
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
