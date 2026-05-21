package roomescape.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import roomescape.domain.Member;

@Repository
public class JdbcMemberRepository implements MemberRepository {

    private final JdbcTemplate jdbcTemplate;

    private static final RowMapper<Member> ROW_MAPPER = (rs, rowNum) ->
            Member.reconstitute(
                    rs.getLong("id"),
                    rs.getString("email"),
                    rs.getString("password"),
                    rs.getString("name")
            );

    public JdbcMemberRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Optional<Member> findByEmail(String email) {
        List<Member> result = jdbcTemplate.query(
                "SELECT id, email, password, name FROM member WHERE email = ?",
                ROW_MAPPER,
                email
        );
        return result.stream().findFirst();
    }

    @Override
    public Optional<Member> findById(Long id) {
        List<Member> result = jdbcTemplate.query(
                "SELECT id, email, password, name FROM member WHERE id = ?",
                ROW_MAPPER,
                id
        );
        return result.stream().findFirst();
    }
}
