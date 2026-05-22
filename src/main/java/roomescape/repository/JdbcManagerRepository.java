package roomescape.repository;

import java.sql.PreparedStatement;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import roomescape.domain.Manager;

@Repository
public class JdbcManagerRepository implements ManagerRepository {

    private final JdbcTemplate jdbcTemplate;

    private static final RowMapper<Manager> ROW_MAPPER = (rs, rowNum) ->
            Manager.reconstitute(
                    rs.getLong("id"),
                    rs.getLong("member_id"),
                    rs.getLong("store_id")
            );

    public JdbcManagerRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Manager save(Manager manager) {
        String sql = "INSERT INTO manager (member_id, store_id) VALUES (?, ?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, new String[]{"id"});
            ps.setLong(1, manager.getMemberId());
            ps.setLong(2, manager.getStoreId());
            return ps;
        }, keyHolder);

        Long id = keyHolder.getKey().longValue();
        return Manager.reconstitute(id, manager.getMemberId(), manager.getStoreId());
    }

    @Override
    public Optional<Manager> findByMemberId(Long memberId) {
        List<Manager> result = jdbcTemplate.query(
                "SELECT id, member_id, store_id FROM manager WHERE member_id = ?",
                ROW_MAPPER,
                memberId
        );
        return result.stream().findFirst();
    }
}
