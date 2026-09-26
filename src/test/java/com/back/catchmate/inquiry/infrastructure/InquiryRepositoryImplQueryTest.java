package com.back.catchmate.inquiry.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.back.catchmate.global.config.data.JpaAuditingConfig;
import com.back.catchmate.global.config.data.QuerydslConfig;
import com.back.catchmate.inquiry.domain.Inquiry;
import com.back.catchmate.inquiry.domain.InquiryStatus;
import com.back.catchmate.inquiry.domain.InquiryType;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

// application.yml 의 기본 프로필 dev 는 운영 RDS 를 가리킨다. create-drop 이 운영 DB 에 닿지 않도록
// 존재하지 않는 test 프로필로 dev 설정을 끄고, datasource 는 컨테이너(@ServiceConnection)에서만 받는다.
@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Import({QuerydslConfig.class, JpaAuditingConfig.class, InquiryRepositoryImpl.class})
@Testcontainers(disabledWithoutDocker = true)
class InquiryRepositoryImplQueryTest {

    @Container
    @ServiceConnection
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0");

    @Autowired
    private InquiryRepositoryImpl inquiryRepository;

    @Test
    @DisplayName("사용자별로 최신순 offset/limit 을 적용하고 수를 센다")
    void findsLatestByUserWithPaging() {
        // given
        Inquiry first = inquiryRepository.save(Inquiry.create(1L, InquiryType.CHAT, "1"));
        Inquiry second = inquiryRepository.save(Inquiry.create(1L, InquiryType.CHAT, "2"));
        inquiryRepository.save(Inquiry.create(2L, InquiryType.CHAT, "남의 문의"));

        // when
        List<Inquiry> firstPage = inquiryRepository.findAllLatestByUserId(1L, 0, 1);
        List<Inquiry> secondPage = inquiryRepository.findAllLatestByUserId(1L, 1, 1);

        // then
        assertThat(firstPage).extracting(Inquiry::getId).containsExactly(second.getId());
        assertThat(secondPage).extracting(Inquiry::getId).containsExactly(first.getId());
        assertThat(inquiryRepository.countByUserId(1L)).isEqualTo(2);
        assertThat(inquiryRepository.findAllLatest(0, 10)).hasSize(3);
        assertThat(inquiryRepository.count()).isEqualTo(3);
    }

    @Test
    @DisplayName("답변 완료 문의만 최신순으로 가져오고 상태별 수를 센다")
    void findsOnlyAnswered() {
        // given
        Inquiry answered = Inquiry.create(1L, InquiryType.CHAT, "답변됨");
        answered.registerAnswer("답변");
        inquiryRepository.save(answered);
        inquiryRepository.save(Inquiry.create(1L, InquiryType.CHAT, "대기"));

        // when
        List<Inquiry> result = inquiryRepository.findAllLatestAnswered(10);

        // then
        assertThat(result).extracting(Inquiry::getId).containsExactly(answered.getId());
        assertThat(inquiryRepository.countByStatus(InquiryStatus.WAITING)).isEqualTo(1);
    }
}
