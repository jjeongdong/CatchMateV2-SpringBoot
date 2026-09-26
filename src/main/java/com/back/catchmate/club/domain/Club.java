package com.back.catchmate.club.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "clubs")
public class Club {
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

    private Club(String name, String homeStadium, String region) {
        this.name = name;
        this.homeStadium = homeStadium;
        this.region = region;
    }

    public static Club create(String name, String homeStadium, String region) {
        return new Club(name, homeStadium, region);
    }
}
