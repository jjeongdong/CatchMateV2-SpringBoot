package com.back.catchmate.admin.application.port.out;

import com.back.catchmate.enroll.domain.model.Enroll;
import java.util.List;

public interface EnrollFetchPort {
    List<Enroll> getEnrollListByBoardIds(List<Long> boardIds);
}
