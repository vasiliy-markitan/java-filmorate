package ru.yandex.practicum.filmorate.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.MpaRating;
import ru.yandex.practicum.filmorate.storage.mpa.MpaRatingStorage;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MpaService {

    private final MpaRatingStorage mpaRatingStorage;

    public List<MpaRating> getAllMpaRatings() {
        List<MpaRating> ratings = mpaRatingStorage.getAllMpaRatings();
        log.debug("Запрос всех рейтингов MPA, найдено: {}", ratings.size());
        return ratings;
    }

    public MpaRating getMpaRatingById(int id) {
        return mpaRatingStorage.getMpaRatingById(id)
                .orElseThrow(() -> {
                    log.warn("Рейтинг MPA с id={} не найден", id);
                    return new NotFoundException("Рейтинг MPA с id=" + id + " не найден");
                });
    }
}
