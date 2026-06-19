package com.back.catchmate.admin.application.port.out.external;

import com.back.catchmate.admin.application.port.out.dto.AdminNoticeInfo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface NoticeFetchPort {
    AdminNoticeInfo getNotice(Long noticeId);

    Page<AdminNoticeInfo> getNoticeList(Pageable pageable);
}
