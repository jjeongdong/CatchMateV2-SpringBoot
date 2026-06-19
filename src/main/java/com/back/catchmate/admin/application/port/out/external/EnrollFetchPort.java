package com.back.catchmate.admin.application.port.out.external;

import com.back.catchmate.admin.application.port.out.dto.AdminEnrollInfo;

import java.util.List;

public interface EnrollFetchPort {
    List<AdminEnrollInfo> getEnrollListByBoardIds(List<Long> boardIds);
}
