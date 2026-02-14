package com.back.catchmate.authorization.common;

import com.back.catchmate.domain.common.permission.ResourceOwnership;

public interface DomainFinder<T extends ResourceOwnership> {
    T searchById(Long id); // ID로 도메인 객체 조회
}
