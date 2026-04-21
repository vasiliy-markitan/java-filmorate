package ru.yandex.practicum.filmorate.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Film;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class FilmControllerTest {

    private FilmController controller;

    @BeforeEach
    void setUp() {
        controller = new FilmController();
    }

    private Film validFilm() {
        Film film = new Film();
        film.setName("Название");
        film.setDescription("Описание");
        film.setReleaseDate(LocalDate.of(2000, 1, 1));
        film.setDuration(120);
        return film;
    }

    @Test
    void addFilm_valid_returnsFilmWithId() {
        Film film = validFilm();
        Film result = controller.addFilm(film);
        assertNotNull(result.getId());
        assertTrue(result.getId() > 0);
    }

    @Test
    void addFilm_emptyName_throwsValidationException() {
        Film film = validFilm();
        film.setName("");
        assertThrows(ValidationException.class, () -> controller.addFilm(film));
    }

    @Test
    void addFilm_blankName_throwsValidationException() {
        Film film = validFilm();
        film.setName("   ");
        assertThrows(ValidationException.class, () -> controller.addFilm(film));
    }

    @Test
    void addFilm_nullName_throwsValidationException() {
        Film film = validFilm();
        film.setName(null);
        assertThrows(ValidationException.class, () -> controller.addFilm(film));
    }

    @Test
    void addFilm_descriptionExactly200Chars_ok() {
        Film film = validFilm();
        film.setDescription("a".repeat(200));
        assertDoesNotThrow(() -> controller.addFilm(film));
    }

    @Test
    void addFilm_description201Chars_throwsValidationException() {
        Film film = validFilm();
        film.setDescription("a".repeat(201));
        assertThrows(ValidationException.class, () -> controller.addFilm(film));
    }

    @Test
    void addFilm_releaseDateExactlyMinDate_ok() {
        Film film = validFilm();
        film.setReleaseDate(LocalDate.of(1895, 12, 28));
        assertDoesNotThrow(() -> controller.addFilm(film));
    }

    @Test
    void addFilm_releaseDateBeforeMinDate_throwsValidationException() {
        Film film = validFilm();
        film.setReleaseDate(LocalDate.of(1895, 12, 27));
        assertThrows(ValidationException.class, () -> controller.addFilm(film));
    }

    @Test
    void addFilm_durationZero_throwsValidationException() {
        Film film = validFilm();
        film.setDuration(0);
        assertThrows(ValidationException.class, () -> controller.addFilm(film));
    }

    @Test
    void addFilm_durationNegative_throwsValidationException() {
        Film film = validFilm();
        film.setDuration(-1);
        assertThrows(ValidationException.class, () -> controller.addFilm(film));
    }

    @Test
    void addFilm_durationOne_ok() {
        Film film = validFilm();
        film.setDuration(1);
        assertDoesNotThrow(() -> controller.addFilm(film));
    }

    @Test
    void addFilm_nullReleaseDate_throwsValidationException() {
        Film film = validFilm();
        film.setReleaseDate(null);
        assertThrows(ValidationException.class, () -> controller.addFilm(film));
    }

    @Test
    void updateFilm_valid_returnsUpdatedFilm() {
        Film film = controller.addFilm(validFilm());
        film.setName("Новое название");
        Film updated = controller.updateFilm(film);
        assertEquals("Новое название", updated.getName());
    }

    @Test
    void getAllFilms_empty_returnsEmptyList() {
        assertTrue(controller.getAllFilms().isEmpty());
    }

    @Test
    void getAllFilms_afterAdd_returnsFilm() {
        controller.addFilm(validFilm());
        assertEquals(1, controller.getAllFilms().size());
    }
}
