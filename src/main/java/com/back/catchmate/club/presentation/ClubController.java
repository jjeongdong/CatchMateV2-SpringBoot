package com.back.catchmate.club.presentation;

import com.back.catchmate.club.application.ClubQueryService;
import com.back.catchmate.club.application.dto.result.ClubResult;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/clubs")
@RequiredArgsConstructor
public class ClubController implements ClubApiDocs {
    private final ClubQueryService clubQueryService;

    @Override
    @GetMapping
    public ResponseEntity<List<ClubResult>> getClubs() {
        return ResponseEntity.ok(clubQueryService.getClubs());
    }
}
