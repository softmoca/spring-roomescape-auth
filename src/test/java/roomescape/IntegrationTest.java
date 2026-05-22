package roomescape;

import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;


/**
 * 통합 테스트 공통 베이스. 매 테스트 실행 전에 모든 테이블을 비우고 AUTO_INCREMENT를 리셋한다.
 * 컨텍스트는 모든 테스트가 공유하여 재기동 비용을 제거.
 * <p>
 * 이전엔 @DirtiesContext(BEFORE_EACH_TEST_METHOD)로 컨텍스트 자체를 매번 새로 띄웠지만,
 * 진짜 필요한 건 "DB 격리 + ID 시퀀스 리셋"뿐이라 그 두 가지만 명시적으로 수행.
 * <p>
 * [1단계 변경] member 테이블 추가:
 * - reservation이 member_id FK를 가지므로 reservation 먼저, member 나중에 삭제.
 * - data.sql의 시간/테마 시드가 매 컨텍스트 로드 시 삽입되므로 reservation_time, theme도 초기화.
 * - data.sql에서 회원 시드를 제거했으므로 member는 @BeforeEach에서 직접 삽입한다.
 *
 *  * [3단계 변경] store, manager 테이블 추가
 *  *   - FK 삭제 순서: reservation → manager → member → reservation_time → theme → store
 *  *   - manager.member_id → member, manager.store_id → store 참조하므로
 *  *     manager를 member/store보다 먼저 삭제해야 한다.
 *  *   - theme.store_id → store 참조하므로 theme을 store보다 먼저 삭제.
 *
 *
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class IntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @LocalServerPort
    private int port;

    @BeforeEach
    void setUpRestAssured() {
        RestAssured.port = port;
    }

    @BeforeEach
    void cleanDatabase() {
        // FK 제약 때문에 자식 테이블부터 비움
        // reservation → member, reservation_time, theme 순서로 참조하므로
        // reservation을 가장 먼저 삭제해야 한다.
        jdbcTemplate.execute("DELETE FROM reservation");
        jdbcTemplate.execute("DELETE FROM manager");          // 3단계 추가
        jdbcTemplate.execute("DELETE FROM member");          // 1단계 추가
        jdbcTemplate.execute("DELETE FROM reservation_time");
        jdbcTemplate.execute("DELETE FROM theme");
        jdbcTemplate.execute("DELETE FROM store");            // 3단계 추가

        // AUTO_INCREMENT 리셋 (테스트가 ID 1부터 시작한다고 가정할 수 있도록)
        jdbcTemplate.execute("ALTER TABLE reservation ALTER COLUMN id RESTART WITH 1");
        jdbcTemplate.execute("ALTER TABLE manager ALTER COLUMN id RESTART WITH 1");       // 3단계 추가
        jdbcTemplate.execute("ALTER TABLE member ALTER COLUMN id RESTART WITH 1");  // 1단계 추가
        jdbcTemplate.execute("ALTER TABLE reservation_time ALTER COLUMN id RESTART WITH 1");
        jdbcTemplate.execute("ALTER TABLE theme ALTER COLUMN id RESTART WITH 1");
        jdbcTemplate.execute("ALTER TABLE store ALTER COLUMN id RESTART WITH 1");         // 3단계 추가
    }
}
