package com.back.catchmate.board;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;

@SpringBootTest
class BulkBoardDataInsertTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("게시글 100만 건 벌크 인서트")
//    @Disabled("데이터 삽입 시에만 주석 해제 후 실행하세요.")
    void insert1MBoards() {
        int totalRecords = 1_000_000;
        int batchSize = 5_000;

        // boards 테이블 컬럼에 맞춰 SQL 작성
        String sql = "INSERT INTO boards " +
                "(title, content, max_person, current_person, user_id, club_id, game_id, " +
                "preferred_gender, preferred_age_range, completed, lift_up_date, created_at, modified_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        System.out.println("⏳ 게시글 100만 건 삽입 시작...");
        long startTime = System.currentTimeMillis();

        for (int i = 0; i < totalRecords; i += batchSize) {
            final int start = i;
            final int currentBatchSize = Math.min(batchSize, totalRecords - start);

            jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
                @Override
                public void setValues(PreparedStatement ps, int j) throws SQLException {
                    int index = start + j + 1;
                    LocalDateTime now = LocalDateTime.now();
                    
                    ps.setString(1, "테스트 게시글 제목 " + index); // title
                    ps.setString(2, "테스트 내용입니다. 대용량 데이터 테스트 중입니다. " + index); // content
                    ps.setInt(3, 4); // maxPerson
                    ps.setInt(4, 1); // currentPerson
                    ps.setLong(5, 1L); // user_id (1번 유저가 쓴 것으로 가정)
                    ps.setLong(6, 1L); // club_id (1번 클럽 가정)
                    ps.setLong(7, 1L); // game_id (1번 경기 가정)
                    ps.setString(8, "무관"); // preferredGender
                    ps.setString(9, "20대"); // preferredAgeRange
                    ps.setBoolean(10, true); // completed
                    
                    // lift_up_date를 조금씩 다르게 해서 정렬 테스트가 가능하게 함
                    ps.setTimestamp(11, Timestamp.valueOf(now.minusMinutes(index))); 
                    ps.setTimestamp(12, Timestamp.valueOf(now)); // created_at
                    ps.setTimestamp(13, Timestamp.valueOf(now)); // modified_at
                }

                @Override
                public int getBatchSize() {
                    return currentBatchSize;
                }
            });

            if ((start + currentBatchSize) % 100_000 == 0) {
                System.out.println("✅ " + (start + currentBatchSize) + "건 완료...");
            }
        }

        long endTime = System.currentTimeMillis();
        System.out.println("🎉 100만 건 삽입 완료! 소요 시간: " + (endTime - startTime) + "ms");
    }
}
