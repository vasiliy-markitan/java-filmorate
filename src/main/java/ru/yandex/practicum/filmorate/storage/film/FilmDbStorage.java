package ru.yandex.practicum.filmorate.storage.film;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.model.MpaRating;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

@Slf4j
@Component
@Qualifier("filmDbStorage")
public class FilmDbStorage implements FilmStorage {

    private static final String FILM_SELECT =
            "SELECT f.film_id, f.name, f.description, f.release_date, f.duration, " +
            "f.mpa_rating_id, m.name AS mpa_name " +
            "FROM films f " +
            "JOIN mpa_ratings m ON f.mpa_rating_id = m.mpa_rating_id";

    private final JdbcTemplate jdbc;
    private final NamedParameterJdbcTemplate namedJdbc;

    @Autowired
    public FilmDbStorage(JdbcTemplate jdbc, NamedParameterJdbcTemplate namedJdbc) {
        this.jdbc = jdbc;
        this.namedJdbc = namedJdbc;
    }

    @Override
    public Film addFilm(Film film) {
        String sql = "INSERT INTO films (name, description, release_date, duration, mpa_rating_id) " +
                     "VALUES (?, ?, ?, ?, ?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, film.getName());
            ps.setString(2, film.getDescription());
            ps.setDate(3, film.getReleaseDate() != null ? Date.valueOf(film.getReleaseDate()) : null);
            ps.setInt(4, film.getDuration());
            ps.setInt(5, film.getMpa().getId());
            return ps;
        }, keyHolder);
        film.setId(Objects.requireNonNull(keyHolder.getKey()).longValue());
        saveGenres(film);
        log.debug("Фильм сохранён в БД: id={}", film.getId());
        return film;
    }

    @Override
    public Film updateFilm(Film film) {
        String sql = "UPDATE films SET name=?, description=?, release_date=?, duration=?, mpa_rating_id=? " +
                     "WHERE film_id=?";
        jdbc.update(sql,
                film.getName(),
                film.getDescription(),
                film.getReleaseDate() != null ? Date.valueOf(film.getReleaseDate()) : null,
                film.getDuration(),
                film.getMpa().getId(),
                film.getId());
        jdbc.update("DELETE FROM film_genres WHERE film_id=?", film.getId());
        saveGenres(film);
        log.debug("Фильм обновлён в БД: id={}", film.getId());
        return film;
    }

    @Override
    public void deleteFilm(Long id) {
        // film_genres и likes удаляются каскадно через ON DELETE CASCADE
        jdbc.update("DELETE FROM films WHERE film_id=?", id);
    }

    @Override
    public List<Film> getAllFilms() {
        List<Film> films = jdbc.query(FILM_SELECT, this::mapRowToFilm);
        if (!films.isEmpty()) {
            loadGenresForFilms(films);
            loadLikesForFilms(films);
        }
        return films;
    }

    @Override
    public Optional<Film> getFilmById(Long id) {
        String sql = FILM_SELECT + " WHERE f.film_id=?";
        List<Film> films = jdbc.query(sql, this::mapRowToFilm, id);
        if (films.isEmpty()) {
            return Optional.empty();
        }
        loadGenresForFilms(films);
        loadLikesForFilms(films);
        return Optional.of(films.getFirst());
    }

    @Override
    public List<Film> getPopularFilms(int count) {
        String sql = FILM_SELECT +
                " LEFT JOIN likes l ON f.film_id = l.film_id" +
                " GROUP BY f.film_id, m.name" +
                " ORDER BY COUNT(l.user_id) DESC" +
                " LIMIT ?";
        List<Film> films = jdbc.query(sql, this::mapRowToFilm, count);
        if (!films.isEmpty()) {
            loadGenresForFilms(films);
            loadLikesForFilms(films);
        }
        return films;
    }

    @Override
    public void addLike(Long filmId, Long userId) {
        jdbc.update("INSERT INTO likes (film_id, user_id) VALUES (?, ?)", filmId, userId);
        log.debug("Лайк добавлен: filmId={}, userId={}", filmId, userId);
    }

    @Override
    public void removeLike(Long filmId, Long userId) {
        jdbc.update("DELETE FROM likes WHERE film_id=? AND user_id=?", filmId, userId);
        log.debug("Лайк удалён: filmId={}, userId={}", filmId, userId);
    }

    private Film mapRowToFilm(ResultSet rs, int rowNum) throws SQLException {
        Film film = new Film();
        film.setId(rs.getLong("film_id"));
        film.setName(rs.getString("name"));
        film.setDescription(rs.getString("description"));
        Date releaseDate = rs.getDate("release_date");
        film.setReleaseDate(releaseDate != null ? releaseDate.toLocalDate() : null);
        film.setDuration(rs.getInt("duration"));
        MpaRating mpa = new MpaRating(rs.getInt("mpa_rating_id"), rs.getString("mpa_name"));
        film.setMpa(mpa);
        return film;
    }

    private void loadGenresForFilms(List<Film> films) {
        List<Long> filmIds = films.stream().map(Film::getId).toList();
        MapSqlParameterSource params = new MapSqlParameterSource("filmIds", filmIds);
        String sql = "SELECT fg.film_id, g.genre_id, g.name " +
                     "FROM film_genres fg JOIN genres g ON fg.genre_id = g.genre_id " +
                     "WHERE fg.film_id IN (:filmIds) ORDER BY g.genre_id";

        Map<Long, Set<Genre>> genresByFilm = new HashMap<>();
        namedJdbc.query(sql, params, rs -> {
            long filmId = rs.getLong("film_id");
            genresByFilm.computeIfAbsent(filmId, k -> new LinkedHashSet<>())
                        .add(new Genre(rs.getInt("genre_id"), rs.getString("name")));
        });

        films.forEach(f -> f.setGenres(genresByFilm.getOrDefault(f.getId(), new LinkedHashSet<>())));
    }

    private void loadLikesForFilms(List<Film> films) {
        List<Long> filmIds = films.stream().map(Film::getId).toList();
        MapSqlParameterSource params = new MapSqlParameterSource("filmIds", filmIds);
        String sql = "SELECT film_id, user_id FROM likes WHERE film_id IN (:filmIds)";

        Map<Long, Set<Long>> likesByFilm = new HashMap<>();
        namedJdbc.query(sql, params, rs -> {
            long filmId = rs.getLong("film_id");
            likesByFilm.computeIfAbsent(filmId, k -> new HashSet<>())
                       .add(rs.getLong("user_id"));
        });

        films.forEach(f -> f.setLikes(likesByFilm.getOrDefault(f.getId(), new HashSet<>())));
    }

    private void saveGenres(Film film) {
        if (film.getGenres() == null || film.getGenres().isEmpty()) {
            return;
        }
        List<Genre> genres = List.copyOf(film.getGenres());
        jdbc.batchUpdate(
                "INSERT INTO film_genres (film_id, genre_id) VALUES (?, ?)",
                genres,
                genres.size(),
                (ps, genre) -> {
                    ps.setLong(1, film.getId());
                    ps.setInt(2, genre.getId());
                }
        );
    }
}
