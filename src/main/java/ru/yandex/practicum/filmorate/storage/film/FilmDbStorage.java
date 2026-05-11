package ru.yandex.practicum.filmorate.storage.film;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
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
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
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

    @Autowired
    public FilmDbStorage(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
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
        jdbc.update("DELETE FROM film_genres WHERE film_id=?", id);
        jdbc.update("DELETE FROM likes WHERE film_id=?", id);
        jdbc.update("DELETE FROM films WHERE film_id=?", id);
    }

    @Override
    public List<Film> getAllFilms() {
        List<Film> films = jdbc.query(FILM_SELECT, this::mapRowToFilm);
        films.forEach(f -> {
            loadGenres(f);
            loadLikes(f);
        });
        return films;
    }

    @Override
    public Optional<Film> getFilmById(Long id) {
        String sql = FILM_SELECT + " WHERE f.film_id=?";
        List<Film> films = jdbc.query(sql, this::mapRowToFilm, id);
        if (films.isEmpty()) {
            return Optional.empty();
        }
        Film film = films.get(0);
        loadGenres(film);
        loadLikes(film);
        return Optional.of(film);
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

    private void loadGenres(Film film) {
        String sql = "SELECT g.genre_id, g.name " +
                     "FROM film_genres fg JOIN genres g ON fg.genre_id = g.genre_id " +
                     "WHERE fg.film_id=? ORDER BY g.genre_id";
        Set<Genre> genres = new LinkedHashSet<>();
        jdbc.query(sql, rs -> {
            genres.add(new Genre(rs.getInt("genre_id"), rs.getString("name")));
        }, film.getId());
        film.setGenres(genres);
    }

    private void loadLikes(Film film) {
        String sql = "SELECT user_id FROM likes WHERE film_id=?";
        Set<Long> likes = new HashSet<>();
        jdbc.query(sql, rs -> {
            likes.add(rs.getLong("user_id"));
        }, film.getId());
        film.setLikes(likes);
    }

    private void saveGenres(Film film) {
        if (film.getGenres() == null || film.getGenres().isEmpty()) {
            return;
        }
        for (Genre genre : film.getGenres()) {
            jdbc.update("INSERT INTO film_genres (film_id, genre_id) VALUES (?, ?)",
                    film.getId(), genre.getId());
        }
    }

    @Override
    public void addLike(Long filmId, Long userId) {
        jdbc.update("INSERT INTO likes (film_id, user_id) VALUES (?, ?)", filmId, userId);
    }

    @Override
    public void removeLike(Long filmId, Long userId) {
        jdbc.update("DELETE FROM likes WHERE film_id=? AND user_id=?", filmId, userId);
    }
}
