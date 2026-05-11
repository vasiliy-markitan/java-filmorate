package ru.yandex.practicum.filmorate.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.storage.genre.GenreStorage;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class GenreService {

    private final GenreStorage genreStorage;

    public List<Genre> getAllGenres() {
        List<Genre> genres = genreStorage.getAllGenres();
        log.debug("Запрос всех жанров, найдено: {}", genres.size());
        return genres;
    }

    public Genre getGenreById(int id) {
        return genreStorage.getGenreById(id)
                .orElseThrow(() -> {
                    log.warn("Жанр с id={} не найден", id);
                    return new NotFoundException("Жанр с id=" + id + " не найден");
                });
    }

    public void validateGenreIds(Set<Genre> genres) {
        if (genres == null || genres.isEmpty()) {
            return;
        }
        List<Integer> ids = genres.stream().map(Genre::getId).collect(Collectors.toList());
        List<Genre> found = genreStorage.getGenresByIds(ids);
        if (found.size() < ids.size()) {
            Set<Integer> foundIds = found.stream().map(Genre::getId).collect(Collectors.toSet());
            int missingId = ids.stream().filter(id -> !foundIds.contains(id)).findFirst().orElseThrow();
            log.warn("Валидация не пройдена: жанр с id={} не найден", missingId);
            throw new NotFoundException("Жанр с id=" + missingId + " не найден");
        }
    }
}
