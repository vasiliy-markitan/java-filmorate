package ru.yandex.practicum.filmorate.storage.mpa;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.model.MpaRating;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class MpaRatingDbStorage implements MpaRatingStorage {

    private final JdbcTemplate jdbc;

    @Override
    public List<MpaRating> getAllMpaRatings() {
        return jdbc.query(
                "SELECT mpa_rating_id, name FROM mpa_ratings ORDER BY mpa_rating_id",
                this::mapRow
        );
    }

    @Override
    public Optional<MpaRating> getMpaRatingById(int id) {
        List<MpaRating> ratings = jdbc.query(
                "SELECT mpa_rating_id, name FROM mpa_ratings WHERE mpa_rating_id=?",
                this::mapRow, id
        );
        return ratings.isEmpty() ? Optional.empty() : Optional.of(ratings.getFirst());
    }

    private MpaRating mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new MpaRating(rs.getInt("mpa_rating_id"), rs.getString("name"));
    }
}
