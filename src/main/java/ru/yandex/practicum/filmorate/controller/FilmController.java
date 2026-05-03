package ru.yandex.practicum.filmorate.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.storage.film.FilmStorage;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/films")
@RequiredArgsConstructor
public class FilmController {
    private static final LocalDate MIN_RELEASE_DATE = LocalDate.of(1895, 12, 28);
    private static final Integer MAX_DESCRIPTION_LENGTH = 200;

    private final FilmStorage filmStorage;

    @PostMapping
    public Film addFilm(@RequestBody Film film) {
        log.info("Добавление фильма: {}", film);
        validate(film);
        Film saved = filmStorage.addFilm(film);
        log.info("Фильм добавлен с id={}", saved.getId());
        return saved;
    }

    @PutMapping
    public Film updateFilm(@RequestBody Film film) {
        log.info("Обновление фильма: {}", film);
        filmStorage.getFilmById(film.getId()).orElseThrow(() -> {
            log.warn("Фильм с id={} не найден", film.getId());
            return new NotFoundException("Фильм с id=" + film.getId() + " не найден");
        });
        validate(film);
        Film updated = filmStorage.updateFilm(film);
        log.info("Фильм с id={} обновлён", updated.getId());
        return updated;
    }

    @GetMapping
    public List<Film> getAllFilms() {
        log.info("Запрос списка всех фильмов");
        return filmStorage.getAllFilms();
    }

    private void validate(Film film) {
        if (film.getName() == null || film.getName().isBlank()) {
            log.warn("Валидация не пройдена: название фильма пустое");
            throw new ValidationException("Название фильма не может быть пустым");
        }
        if (film.getDescription() != null && film.getDescription().length() > MAX_DESCRIPTION_LENGTH) {
            log.warn("Валидация не пройдена: описание превышает {} символов", MAX_DESCRIPTION_LENGTH);
            throw new ValidationException("Описание не может превышать 200 символов");
        }
        if (film.getReleaseDate() == null || film.getReleaseDate().isBefore(MIN_RELEASE_DATE)) {
            log.warn("Валидация не пройдена: дата релиза {} раньше {} или не указана", film.getReleaseDate(), MIN_RELEASE_DATE);
            throw new ValidationException("Дата релиза не может быть раньше 28 декабря 1895 года");
        }
        if (film.getDuration() <= 0) {
            log.warn("Валидация не пройдена: продолжительность {} не является положительным числом", film.getDuration());
            throw new ValidationException("Продолжительность фильма должна быть положительным числом");
        }
    }
}
