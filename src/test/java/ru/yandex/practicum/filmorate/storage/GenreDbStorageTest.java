package ru.yandex.practicum.filmorate.storage;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.storage.genre.GenreDbStorage;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@JdbcTest
@AutoConfigureTestDatabase
@RequiredArgsConstructor(onConstructor_ = @Autowired)
@Import(GenreDbStorage.class)
class GenreDbStorageTest {

    private final GenreDbStorage genreStorage;

    @Test
    void getAllGenres_returnsSixGenres() {
        List<Genre> genres = genreStorage.getAllGenres();
        assertThat(genres).hasSize(6);
    }

    @Test
    void getAllGenres_returnsSortedById() {
        List<Genre> genres = genreStorage.getAllGenres();
        for (int i = 0; i < genres.size() - 1; i++) {
            assertThat(genres.get(i).getId()).isLessThan(genres.get(i + 1).getId());
        }
    }

    @Test
    void getGenreById_existingId_returnsCorrectGenre() {
        Optional<Genre> genre = genreStorage.getGenreById(1);
        assertThat(genre)
                .isPresent()
                .hasValueSatisfying(g -> {
                    assertThat(g.getId()).isEqualTo(1);
                    assertThat(g.getName()).isEqualTo("Комедия");
                });
    }

    @Test
    void getGenreById_allIds_returnCorrectNames() {
        assertThat(genreStorage.getGenreById(1)).isPresent()
                .hasValueSatisfying(g -> assertThat(g.getName()).isEqualTo("Комедия"));
        assertThat(genreStorage.getGenreById(2)).isPresent()
                .hasValueSatisfying(g -> assertThat(g.getName()).isEqualTo("Драма"));
        assertThat(genreStorage.getGenreById(3)).isPresent()
                .hasValueSatisfying(g -> assertThat(g.getName()).isEqualTo("Мультфильм"));
        assertThat(genreStorage.getGenreById(4)).isPresent()
                .hasValueSatisfying(g -> assertThat(g.getName()).isEqualTo("Триллер"));
        assertThat(genreStorage.getGenreById(5)).isPresent()
                .hasValueSatisfying(g -> assertThat(g.getName()).isEqualTo("Документальный"));
        assertThat(genreStorage.getGenreById(6)).isPresent()
                .hasValueSatisfying(g -> assertThat(g.getName()).isEqualTo("Боевик"));
    }

    @Test
    void getGenreById_nonExistentId_returnsEmpty() {
        Optional<Genre> genre = genreStorage.getGenreById(999);
        assertThat(genre).isEmpty();
    }
}
