package roomescape.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import roomescape.domain.Member;
import roomescape.domain.MemberRole;

@Repository
public class JdbcMemberRepository implements MemberRepository {

    private final JdbcTemplate jdbcTemplate;

    private static final RowMapper<Member> ROW_MAPPER = (rs, rowNum) ->
            Member.reconstitute(
                    rs.getLong("id"),
                    rs.getString("email"),
                    rs.getString("password"),
                    rs.getString("name"),
                    MemberRole.valueOf(rs.getString("role"))
            );

    public JdbcMemberRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Optional<Member> findByEmail(String email) {
        String sql = "SELECT id, email, password, name, role FROM member WHERE email = ?";
        List<Member> result = jdbcTemplate.query(sql, ROW_MAPPER, email);
        return result.stream().findFirst();
    }

    @Override
    public Optional<Member> findById(Long id) {
        String sql = "SELECT id, email, password, name, role FROM member WHERE id = ?";
        List<Member> result = jdbcTemplate.query(sql, ROW_MAPPER, id);
        return result.stream().findFirst();
    }
}
