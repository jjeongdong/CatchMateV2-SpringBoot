import com.back.catchmate.CatchmateApplication;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.PreparedStatement;
import java.sql.SQLException;

@SpringBootTest(classes = CatchmateApplication.class)
class BulkChatDataInsertTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("1번 채팅방에 10만 건의 더미 데이터 벌크 인서트")
    void insert100kChatMessages() {
        int totalRecords = 100_000;
        int batchSize = 5_000; // 5000건씩 쪼개서 Insert

        // BaseTimeEntity에 있는 created_at, updated_at도 함께 넣어줍니다. (MySQL/MariaDB/H2 기준 NOW() 사용)
        String sql = "INSERT INTO chat_messages " +
                "(chat_room_id, sender_id, content, message_type, sequence, created_at, modified_at) " +
                "VALUES (?, ?, ?, ?, ?, NOW(), NOW())";

        System.out.println("⏳ 10만 건 더미 데이터 삽입 시작...");
        long startTime = System.currentTimeMillis();

        for (int i = 0; i < totalRecords; i += batchSize) {
            final int start = i;
            final int currentBatchSize = Math.min(batchSize, totalRecords - start);

            jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
                @Override
                public void setValues(PreparedStatement ps, int j) throws SQLException {
                    long sequence = start + j + 1;
                    long senderId = (sequence % 2 == 0) ? 2L : 1L; // 1번 유저, 2번 유저 번갈아가며

                    ps.setLong(1, 1L); // chat_room_id = 1 (반드시 DB에 1번 채팅방이 존재해야 함)
                    ps.setLong(2, senderId); // sender_id (반드시 DB에 1번, 2번 유저가 존재해야 함)
                    ps.setString(3, "성능 테스트를 위한 대용량 메시지 " + sequence); // content
                    ps.setString(4, "TEXT"); // message_type (Enum의 name 문자열)
                    ps.setLong(5, sequence); // sequence
                }

                @Override
                public int getBatchSize() {
                    return currentBatchSize;
                }
            });
            
            System.out.println("✅ " + (start + currentBatchSize) + "건 Insert 완료...");
        }

        long endTime = System.currentTimeMillis();
        System.out.println("🎉 10만 건 삽입 완료! 소요 시간: " + (endTime - startTime) + "ms");
    }
}
