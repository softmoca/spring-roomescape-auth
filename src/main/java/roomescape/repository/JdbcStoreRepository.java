package roomescape.repository;

import java.sql.PreparedStatement;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import roomescape.domain.Store;

@Repository
public class JdbcStoreRepository implements StoreRepository {

    private final JdbcTemplate jdbcTemplate;

    private static final RowMapper<Store> ROW_MAPPER = (rs, rowNum) ->
            Store.reconstitute(
                    rs.getLong("id"),
                    rs.getString("name")
            );

    public JdbcStoreRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Store save(Store store) {
        String sql = "INSERT INTO store (name) VALUES (?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, new String[]{"id"});
            ps.setString(1, store.getName());
            return ps;
        }, keyHolder);

        Long id = keyHolder.getKey().longValue();
        return Store.reconstitute(id, store.getName());
    }

    @Override
    public Optional<Store> findById(Long id) {
        List<Store> result = jdbcTemplate.query(
                "SELECT id, name FROM store WHERE id = ?",
                ROW_MAPPER,
                id
        );
        return result.stream().findFirst();
    }
}
