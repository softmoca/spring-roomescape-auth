package roomescape.repository;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import roomescape.domain.Member;
import roomescape.domain.MemberRole;
import roomescape.domain.Reservation;
import roomescape.domain.ReservationTime;
import roomescape.domain.Theme;

@Repository
public class JdbcReservationRepository implements ReservationRepository {

    private final JdbcTemplate jdbcTemplate;

    // 예약 조회 시 member, time, theme를 함께 조립한다 (3개 INNER JOIN)
    private static final String SELECT_RESERVATION = """
            SELECT
                r.id            AS reservation_id,
                r.date          AS reservation_date,
                m.id            AS member_id,
                m.email         AS member_email,
                m.password      AS member_password,
                m.name          AS member_name,
                m.role          AS member_role,
                t.id            AS time_id,
                t.start_at      AS time_start_at,
                th.id           AS theme_id,
                th.name         AS theme_name,
                th.description  AS theme_description,
                th.thumbnail_url AS theme_thumbnail
            FROM reservation r
            INNER JOIN member m ON r.member_id = m.id
            INNER JOIN reservation_time t ON r.time_id = t.id
            INNER JOIN theme th ON r.theme_id = th.id
            """;

    private static final RowMapper<Reservation> ROW_MAPPER = (rs, rowNum) -> {
        Member member = Member.reconstitute(
                rs.getLong("member_id"),
                rs.getString("member_email"),
                rs.getString("member_password"),
                rs.getString("member_name"),
                MemberRole.valueOf(rs.getString("member_role"))
        );
        ReservationTime time = ReservationTime.reconstitute(
                rs.getLong("time_id"),
                rs.getTime("time_start_at").toLocalTime()
        );
        Theme theme = Theme.reconstitute(
                rs.getLong("theme_id"),
                rs.getString("theme_name"),
                rs.getString("theme_description"),
                rs.getString("theme_thumbnail")
        );
        return Reservation.reconstitute(
                rs.getLong("reservation_id"),
                member,
                rs.getDate("reservation_date").toLocalDate(),
                time,
                theme
        );
    };

    public JdbcReservationRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<Reservation> findAll() {
        return jdbcTemplate.query(SELECT_RESERVATION, ROW_MAPPER);
    }

    @Override
    public Reservation save(Reservation reservation) {
        String sql = "INSERT INTO reservation (member_id, date, time_id, theme_id) VALUES (?, ?, ?, ?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, new String[]{"id"});
            ps.setLong(1, reservation.getMember().getId());
            ps.setDate(2, Date.valueOf(reservation.getDate()));
            ps.setLong(3, reservation.getTime().getId());
            ps.setLong(4, reservation.getTheme().getId());
            return ps;
        }, keyHolder);

        Long id = keyHolder.getKey().longValue();
        return Reservation.reconstitute(
                id,
                reservation.getMember(),
                reservation.getDate(),
                reservation.getTime(),
                reservation.getTheme()
        );
    }

    @Override
    public void deleteById(Long id) {
        jdbcTemplate.update("DELETE FROM reservation WHERE id = ?", id);
    }

    @Override
    public boolean existsByDateAndTimeAndTheme(LocalDate date, Long timeId, Long themeId) {
        String sql = """
                SELECT EXISTS (
                    SELECT 1 FROM reservation
                    WHERE date = ? AND time_id = ? AND theme_id = ?
                )
                """;
        Boolean result = jdbcTemplate.queryForObject(
                sql, Boolean.class, Date.valueOf(date), timeId, themeId
        );
        return Boolean.TRUE.equals(result);
    }

    @Override
    public boolean existsByTimeId(Long timeId) {
        String sql = """
                SELECT EXISTS (
                    SELECT 1 FROM reservation WHERE time_id = ?
                )
                """;
        Boolean result = jdbcTemplate.queryForObject(sql, Boolean.class, timeId);
        return Boolean.TRUE.equals(result);
    }

    @Override
    public List<Reservation> findByMemberIdOrderByDateAscTimeAsc(Long memberId) {
        String sql = SELECT_RESERVATION + """
                WHERE r.member_id = ?
                ORDER BY r.date ASC, t.start_at ASC
                """;
        return jdbcTemplate.query(sql, ROW_MAPPER, memberId);
    }

    @Override
    public Optional<Reservation> findById(Long id) {
        String sql = SELECT_RESERVATION + "WHERE r.id = ?";
        return jdbcTemplate.query(sql, ROW_MAPPER, id).stream().findFirst();
    }

    @Override
    public boolean existsByDateAndTimeAndThemeExcludingId(
            LocalDate date, Long timeId, Long themeId, Long excludeId) {
        String sql = """
                SELECT EXISTS (
                    SELECT 1 FROM reservation
                    WHERE date = ? AND time_id = ? AND theme_id = ? AND id != ?
                )
                """;
        Boolean result = jdbcTemplate.queryForObject(
                sql, Boolean.class, Date.valueOf(date), timeId, themeId, excludeId
        );
        return Boolean.TRUE.equals(result);
    }

    @Override
    public void updateDateAndTime(Long id, LocalDate date, Long timeId) {
        String sql = "UPDATE reservation SET date = ?, time_id = ? WHERE id = ?";
        jdbcTemplate.update(sql, Date.valueOf(date), timeId, id);
    }

    @Override
    public boolean existsByThemeId(Long themeId) {
        String sql = """
                SELECT EXISTS (
                    SELECT 1 FROM reservation WHERE theme_id = ?
                )
                """;
        Boolean result = jdbcTemplate.queryForObject(sql, Boolean.class, themeId);
        return Boolean.TRUE.equals(result);
    }
}
