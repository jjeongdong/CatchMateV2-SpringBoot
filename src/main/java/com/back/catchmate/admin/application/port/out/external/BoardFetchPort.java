package com.back.catchmate.admin.application.port.out.external;

import com.back.catchmate.admin.application.port.out.dto.AdminBoardInfo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface BoardFetchPort {
    Page<AdminBoardInfo> getBoardList(Pageable pageable);

    Page<AdminBoardInfo> getBoardListByUserId(Long userId, Pageable pageable);

    AdminBoardInfo getCompletedBoard(Long boardId);

    long getTotalBoardCount();
}
