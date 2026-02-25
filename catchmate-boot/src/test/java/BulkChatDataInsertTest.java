import com.back.catchmate.CatchmateApplication;
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
    @DisplayName("1번 채팅방에 100만 건의 더미 데이터 벌크 인서트")
    void insert1MillionChatMessages() {
        int totalRecords = 1_000_000;   // 🔥 100만 건
        int batchSize = 10_000;         // 배치 사이즈 증가 (성능 향상)

        String sql = "INSERT INTO chat_messages " +
                "(chat_room_id, sender_id, content, message_type, sequence, created_at, modified_at) " +
                "VALUES (?, ?, ?, ?, ?, NOW(), NOW())";

        System.out.println("⏳ 100만 건 더미 데이터 삽입 시작...");
        long startTime = System.currentTimeMillis();

        for (int i = 0; i < totalRecords; i += batchSize) {
            final int start = i;
            final int currentBatchSize = Math.min(batchSize, totalRecords - start);

            jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
                @Override
                public void setValues(PreparedStatement ps, int j) throws SQLException {
                    long sequence = start + j + 1;
                    long senderId = (sequence % 2 == 0) ? 10L : 9L;

                    ps.setLong(1, 9L);
                    ps.setLong(2, senderId);
                    ps.setString(3, "성능 테스트를 위한 대용량 메시지 " + sequence);
                    ps.setString(4, "TEXT");
                    ps.setLong(5, sequence);
                }

                @Override
                public int getBatchSize() {
                    return currentBatchSize;
                }
            });

            System.out.println("✅ " + (start + currentBatchSize) + "건 Insert 완료...");
        }

        long endTime = System.currentTimeMillis();
        System.out.println("🎉 100만 건 삽입 완료! 소요 시간: " + (endTime - startTime) + "ms");
    }
}
