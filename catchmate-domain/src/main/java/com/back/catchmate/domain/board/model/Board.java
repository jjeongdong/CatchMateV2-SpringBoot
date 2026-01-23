package com.back.catchmate.domain.board.model;

import com.back.catchmate.domain.club.model.Club;
import com.back.catchmate.domain.game.model.Game;
import com.back.catchmate.domain.user.model.User;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class Board {
    private Long id;
    private String title;
    private String content;
    private int maxPerson;
    private int currentPerson;
    private User user;
    private Club cheerClub;
    private Game game;
    private String preferredGender;
    private String preferredAgeRange;
    private boolean completed;
    private LocalDateTime createdAt;
    private LocalDateTime liftUpDate;

    public static Board createBoard(String title, String content, int maxPerson, User user,
                                    Club cheerClub, Game game, String preferredGender,
                                    List<String> preferredAgeRange, boolean completed) {
        return Board.builder()
                .title(title)
                .content(content)
                .maxPerson(maxPerson)
                .currentPerson(1)
                .user(user)
                .cheerClub(cheerClub)
                .game(game)
                .preferredGender(preferredGender)
                .preferredAgeRange(String.join(",", preferredAgeRange))
                .completed(completed)
                .createdAt(LocalDateTime.now())
                .liftUpDate(LocalDateTime.now())
                .build();
    }

    public void updateBoard(String title, String content, int maxPerson, Club cheerClub,
                            String preferredGender, List<String> preferredAgeRange,
                            boolean completed) {
        this.title = title;
        this.content = content;
        this.maxPerson = maxPerson;
        this.cheerClub = cheerClub;
        this.preferredGender = preferredGender;
        this.preferredAgeRange = String.join(",", preferredAgeRange);
        this.completed = completed;
    }
}
