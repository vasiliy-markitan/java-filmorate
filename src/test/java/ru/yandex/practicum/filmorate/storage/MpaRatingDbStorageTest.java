package ru.yandex.practicum.filmorate.storage;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import ru.yandex.practicum.filmorate.model.MpaRating;
import ru.yandex.practicum.filmorate.storage.mpa.MpaRatingDbStorage;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@JdbcTest
@AutoConfigureTestDatabase
@RequiredArgsConstructor(onConstructor_ = @Autowired)
@Import(MpaRatingDbStorage.class)
class MpaRatingDbStorageTest {

    private final MpaRatingDbStorage mpaRatingStorage;

    @Test
    void getAllMpaRatings_returnsFiveRatings() {
        List<MpaRating> ratings = mpaRatingStorage.getAllMpaRatings();
        assertThat(ratings).hasSize(5);
    }

    @Test
    void getAllMpaRatings_returnsSortedById() {
        List<MpaRating> ratings = mpaRatingStorage.getAllMpaRatings();
        for (int i = 0; i < ratings.size() - 1; i++) {
            assertThat(ratings.get(i).getId()).isLessThan(ratings.get(i + 1).getId());
        }
    }

    @Test
    void getMpaRatingById_existingId_returnsCorrectRating() {
        Optional<MpaRating> rating = mpaRatingStorage.getMpaRatingById(1);
        assertThat(rating)
                .isPresent()
                .hasValueSatisfying(r -> {
                    assertThat(r.getId()).isEqualTo(1);
                    assertThat(r.getName()).isEqualTo("G");
                });
    }

    @Test
    void getMpaRatingById_allIds_returnCorrectNames() {
        assertThat(mpaRatingStorage.getMpaRatingById(1)).isPresent()
                .hasValueSatisfying(r -> assertThat(r.getName()).isEqualTo("G"));
        assertThat(mpaRatingStorage.getMpaRatingById(2)).isPresent()
                .hasValueSatisfying(r -> assertThat(r.getName()).isEqualTo("PG"));
        assertThat(mpaRatingStorage.getMpaRatingById(3)).isPresent()
                .hasValueSatisfying(r -> assertThat(r.getName()).isEqualTo("PG-13"));
        assertThat(mpaRatingStorage.getMpaRatingById(4)).isPresent()
                .hasValueSatisfying(r -> assertThat(r.getName()).isEqualTo("R"));
        assertThat(mpaRatingStorage.getMpaRatingById(5)).isPresent()
                .hasValueSatisfying(r -> assertThat(r.getName()).isEqualTo("NC-17"));
    }

    @Test
    void getMpaRatingById_nonExistentId_returnsEmpty() {
        Optional<MpaRating> rating = mpaRatingStorage.getMpaRatingById(999);
        assertThat(rating).isEmpty();
    }
}
