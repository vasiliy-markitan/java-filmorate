package ru.yandex.practicum.filmorate.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.storage.film.FilmStorage;
import ru.yandex.practicum.filmorate.storage.mpa.MpaRatingStorage;
import ru.yandex.practicum.filmorate.storage.user.UserStorage;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
public class FilmService {
    private static final LocalDate MIN_RELEASE_DATE = LocalDate.of(1895, 12, 28);
    private static final int MAX_DESCRIPTION_LENGTH = 200;

    private final FilmStorage filmStorage;
    private final UserStorage userStorage;
    private final MpaRatingStorage mpaRatingStorage;
    private final GenreService genreService;

    @Autowired
    public FilmService(@Qualifier("filmDbStorage") FilmStorage filmStorage,
                       @Qualifier("userDbStorage") UserStorage userStorage,
                       MpaRatingStorage mpaRatingStorage,
                       GenreService genreService) {
        this.filmStorage = filmStorage;
        this.userStorage = userStorage;
        this.mpaRatingStorage = mpaRatingStorage;
        this.genreService = genreService;
    }

    public Film addFilm(Film film) {
        log.debug("Начало добавления фильма: {}", film);
        validate(film);
        Film saved = filmStorage.addFilm(film);
        log.debug("Фильм добавлен: id={}, name={}", saved.getId(), saved.getName());
        return saved;
    }

    public Film updateFilm(Film film) {
        log.debug("Начало обновления фильма: {}", film);
        validate(film);
        Film existing = getById(film.getId());
        log.debug("Текущее состояние фильма перед обновлением: {}", existing);
        Film updated = filmStorage.updateFilm(film);
        log.debug("Фильм обновлён: {}", updated);
        return updated;
    }

    public List<Film> getAllFilms() {
        List<Film> films = filmStorage.getAllFilms();
        log.debug("Получен список фильмов, количество: {}", films.size());
        return films;
    }

    public Film getById(Long id) {
        log.debug("Поиск фильма по id={}", id);
        return filmStorage.getFilmById(id)
                .orElseThrow(() -> {
                    log.warn("Фильм с id={} не найден", id);
                    return new NotFoundException("Фильм с id=" + id + " не найден");
                });
    }

    public void addLike(Long filmId, Long userId) {
        log.debug("Пользователь id={} добавляет лайк фильму id={}", userId, filmId);
        getById(filmId);
        getUserById(userId);
        filmStorage.addLike(filmId, userId);
        log.info("Пользователь id={} поставил лайк фильму id={}", userId, filmId);
    }

    public void removeLike(Long filmId, Long userId) {
        log.debug("Пользователь id={} удаляет лайк с фильма id={}", userId, filmId);
        getById(filmId);
        getUserById(userId);
        filmStorage.removeLike(filmId, userId);
        log.info("Пользователь id={} убрал лайк с фильма id={}", userId, filmId);
    }

    public List<Film> getPopularFilms(int count) {
        if (count <= 0) {
            log.warn("Валидация не пройдена: count={} не является положительным числом", count);
            throw new ValidationException("Количество фильмов должно быть положительным числом");
        }
        log.debug("Запрос топ-{} популярных фильмов", count);
        return filmStorage.getPopularFilms(count);
    }

    private void getUserById(Long userId) {
        userStorage.getUserById(userId)
                .orElseThrow(() -> {
                    log.warn("Пользователь с id={} не найден", userId);
                    return new NotFoundException("Пользователь с id=" + userId + " не найден");
                });
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
            log.warn("Валидация не пройдена: дата релиза {} раньше {} или не указана",
                    film.getReleaseDate(), MIN_RELEASE_DATE);
            throw new ValidationException("Дата релиза не может быть раньше 28 декабря 1895 года");
        }
        if (film.getDuration() <= 0) {
            log.warn("Валидация не пройдена: продолжительность {} не является положительным числом",
                    film.getDuration());
            throw new ValidationException("Продолжительность фильма должна быть положительным числом");
        }
        if (film.getMpa() == null) {
            log.warn("Валидация не пройдена: рейтинг MPA не указан");
            throw new ValidationException("Рейтинг MPA обязателен");
        }
        mpaRatingStorage.getMpaRatingById(film.getMpa().getId())
                .orElseThrow(() -> {
                    log.warn("Валидация не пройдена: MPA рейтинг с id={} не найден", film.getMpa().getId());
                    return new NotFoundException("MPA рейтинг с id=" + film.getMpa().getId() + " не найден");
                });
        genreService.validateGenreIds(film.getGenres());
    }
}
