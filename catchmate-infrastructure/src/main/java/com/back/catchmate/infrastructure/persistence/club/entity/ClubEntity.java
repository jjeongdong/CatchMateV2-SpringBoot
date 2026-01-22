package com.back.catchmate.infrastructure.persistence.club.entity;

import com.back.catchmate.domain.club.model.Club;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "clubs")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class ClubEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "club_id")
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String homeStadium;

    @Column(nullable = false)
    private String region;

    public static ClubEntity from(Club club) {
        return ClubEntity.builder()
                .id(club.getId())
                .name(club.getName())
                .homeStadium(club.getHomeStadium())
                .region(club.getRegion())
                .build();
    }

    public Club toModel() {
        return Club.builder()
                .id(id)
                .name(name)
                .homeStadium(homeStadium)
                .region(region)
                .build();
    }
}
