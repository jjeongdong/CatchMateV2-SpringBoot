package com.back.catchmate.enroll.application.port.out.external;

import com.back.catchmate.enroll.application.port.out.dto.EnrollBoardInfo;

import java.util.List;

public interface BoardFetchPort {
    EnrollBoardInfo getBoard(Long boardId);

    EnrollBoardInfo getCompletedBoard(Long boardId);

    List<EnrollBoardInfo> getBoards(List<Long> boardIds);
}
