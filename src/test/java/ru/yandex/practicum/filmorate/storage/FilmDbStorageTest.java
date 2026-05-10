package ru.yandex.practicum.filmorate.storage;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.model.MpaRating;
import ru.yandex.practicum.filmorate.storage.film.FilmDbStorage;

import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@JdbcTest
@AutoConfigureTestDatabase
@RequiredArgsConstructor(onConstructor_ = @Autowired)
@Import(FilmDbStorage.class)
class FilmDbStorageTest {

    private final FilmDbStorage filmStorage;
    private final JdbcTemplate jdbc;

    private Film makeFilm(String name) {
        Film film = new Film();
        film.setName(name);
        film.setDescription("Тестовое описание");
        film.setReleaseDate(LocalDate.of(2000, 6, 15));
        film.setDuration(120);
        film.setMpa(new MpaRating(1, null));
        return film;
    }

    private Long insertUser(String email, String login) {
        jdbc.update(
                "INSERT INTO users (email, login, name, birthday) VALUES (?, ?, 'Тест', '1990-01-01')",
                email, login
        );
        return jdbc.queryForObject(
                "SELECT user_id FROM users WHERE login=?", Long.class, login
        );
    }

    @Test
    void addFilm_savesAndReturnsWithId() {
        Film film = filmStorage.addFilm(makeFilm("Тестовый фильм"));
        assertThat(film.getId()).isNotNull().isGreaterThan(0L);
    }

    @Test
    void getFilmById_existingFilm_returnsCorrectFilm() {
        Film created = filmStorage.addFilm(makeFilm("Найти меня"));

        Optional<Film> found = filmStorage.getFilmById(created.getId());

        assertThat(found)
                .isPresent()
                .hasValueSatisfying(f -> {
                    assertThat(f.getId()).isEqualTo(created.getId());
                    assertThat(f.getName()).isEqualTo("Найти меня");
                    assertThat(f.getDescription()).isEqualTo("Тестовое описание");
                    assertThat(f.getReleaseDate()).isEqualTo(LocalDate.of(2000, 6, 15));
                    assertThat(f.getDuration()).isEqualTo(120);
                    assertThat(f.getMpa().getId()).isEqualTo(1);
                    assertThat(f.getMpa().getName()).isEqualTo("G");
                });
    }

    @Test
    void getFilmById_nonExistentId_returnsEmpty() {
        assertThat(filmStorage.getFilmById(999L)).isEmpty();
    }

    @Test
    void updateFilm_changesFieldsInDb() {
        Film film = filmStorage.addFilm(makeFilm("Оригинал"));
        film.setName("Обновлено");
        film.setDuration(90);
        film.setMpa(new MpaRating(2, null));
        filmStorage.updateFilm(film);

        Optional<Film> found = filmStorage.getFilmById(film.getId());

        assertThat(found).isPresent()
                .hasValueSatisfying(f -> {
                    assertThat(f.getName()).isEqualTo("Обновлено");
                    assertThat(f.getDuration()).isEqualTo(90);
                    assertThat(f.getMpa().getId()).isEqualTo(2);
                    assertThat(f.getMpa().getName()).isEqualTo("PG");
                });
    }

    @Test
    void getAllFilms_returnsAllAddedFilms() {
        filmStorage.addFilm(makeFilm("Фильм один"));
        filmStorage.addFilm(makeFilm("Фильм два"));

        List<Film> films = filmStorage.getAllFilms();

        assertThat(films).hasSize(2);
    }

    @Test
    void deleteFilm_removesFilmFromDb() {
        Film film = filmStorage.addFilm(makeFilm("Удалить"));
        filmStorage.deleteFilm(film.getId());

        assertThat(filmStorage.getFilmById(film.getId())).isEmpty();
    }

    @Test
    void addFilm_withGenres_loadsGenresFromDb() {
        Film film = makeFilm("Фильм с жанрами");
        LinkedHashSet<Genre> genres = new LinkedHashSet<>();
        genres.add(new Genre(1, null));
        genres.add(new Genre(2, null));
        film.setGenres(genres);

        Film saved = filmStorage.addFilm(film);
        Optional<Film> found = filmStorage.getFilmById(saved.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getGenres()).hasSize(2);
        assertThat(found.get().getGenres())
                .extracting(Genre::getId)
                .containsExactly(1, 2);
        assertThat(found.get().getGenres())
                .extracting(Genre::getName)
                .containsExactly("Комедия", "Драма");
    }

    @Test
    void updateFilm_replacesGenresInDb() {
        Film film = makeFilm("Жанровая замена");
        LinkedHashSet<Genre> initial = new LinkedHashSet<>();
        initial.add(new Genre(1, null));
        film.setGenres(initial);
        filmStorage.addFilm(film);

        LinkedHashSet<Genre> updated = new LinkedHashSet<>();
        updated.add(new Genre(3, null));
        film.setGenres(updated);
        filmStorage.updateFilm(film);

        Optional<Film> found = filmStorage.getFilmById(film.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getGenres())
                .hasSize(1)
                .extracting(Genre::getId)
                .containsExactly(3);
    }

    @Test
    void updateFilm_savesLikesToDb() {
        Long userId = insertUser("liker@test.com", "likerlogin");
        Film film = filmStorage.addFilm(makeFilm("Лайкаемый"));
        film.getLikes().add(userId);
        filmStorage.updateFilm(film);

        Optional<Film> found = filmStorage.getFilmById(film.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getLikes()).contains(userId);
    }

    @Test
    void updateFilm_removeLike_persistsChange() {
        Long userId = insertUser("unliker@test.com", "unlikerlogin");
        Film film = filmStorage.addFilm(makeFilm("Разлайкаемый"));
        film.getLikes().add(userId);
        filmStorage.updateFilm(film);
        film.getLikes().remove(userId);
        filmStorage.updateFilm(film);

        Optional<Film> found = filmStorage.getFilmById(film.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getLikes()).doesNotContain(userId);
    }

    @Test
    void getFilmById_filmWithMpa_returnsMpaName() {
        Film film = makeFilm("МПА тест");
        film.setMpa(new MpaRating(3, null));
        Film saved = filmStorage.addFilm(film);

        Optional<Film> found = filmStorage.getFilmById(saved.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getMpa().getName()).isEqualTo("PG-13");
    }
}
