package com.back.catchmate.domain.club.model;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class Club {
    private final Long id;
    private final String name;
    private final String homeStadium;
    private final String region;
}
