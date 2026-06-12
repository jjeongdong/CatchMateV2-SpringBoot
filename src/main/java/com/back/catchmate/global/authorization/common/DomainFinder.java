package com.back.catchmate.global.authorization.common;

import com.back.catchmate.global.authorization.common.ResourceOwnership;

public interface DomainFinder<T extends ResourceOwnership> {
    T searchById(Long id); // ID로 도메인 객체 조회
}
