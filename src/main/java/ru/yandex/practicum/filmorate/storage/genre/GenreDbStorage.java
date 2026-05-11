package ru.yandex.practicum.filmorate.storage.genre;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.model.Genre;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class GenreDbStorage implements GenreStorage {

    private final JdbcTemplate jdbc;
    private final NamedParameterJdbcTemplate namedJdbc;

    @Override
    public List<Genre> getAllGenres() {
        return jdbc.query(
                "SELECT genre_id, name FROM genres ORDER BY genre_id",
                this::mapRow
        );
    }

    @Override
    public Optional<Genre> getGenreById(int id) {
        List<Genre> genres = jdbc.query(
                "SELECT genre_id, name FROM genres WHERE genre_id=?",
                this::mapRow, id
        );
        return genres.isEmpty() ? Optional.empty() : Optional.of(genres.getFirst());
    }

    @Override
    public List<Genre> getGenresByIds(List<Integer> ids) {
        MapSqlParameterSource params = new MapSqlParameterSource("ids", ids);
        return namedJdbc.query(
                "SELECT genre_id, name FROM genres WHERE genre_id IN (:ids) ORDER BY genre_id",
                params,
                this::mapRow
        );
    }

    private Genre mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new Genre(rs.getInt("genre_id"), rs.getString("name"));
    }
}
