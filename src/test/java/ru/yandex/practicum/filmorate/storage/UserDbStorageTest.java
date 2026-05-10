package ru.yandex.practicum.filmorate.storage;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import ru.yandex.practicum.filmorate.model.FriendshipStatus;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.user.UserDbStorage;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@JdbcTest
@AutoConfigureTestDatabase
@RequiredArgsConstructor(onConstructor_ = @Autowired)
@Import(UserDbStorage.class)
class UserDbStorageTest {

    private final UserDbStorage userStorage;

    private User makeUser(String email, String login) {
        User user = new User();
        user.setEmail(email);
        user.setLogin(login);
        user.setName("Имя");
        user.setBirthday(LocalDate.of(1990, 1, 1));
        return user;
    }

    @Test
    void createUser_savesAndReturnsWithId() {
        User user = userStorage.createUser(makeUser("test@test.com", "testlogin"));
        assertThat(user.getId()).isNotNull().isGreaterThan(0L);
    }

    @Test
    void getUserById_existingUser_returnsCorrectUser() {
        User created = userStorage.createUser(makeUser("find@test.com", "findlogin"));

        Optional<User> found = userStorage.getUserById(created.getId());

        assertThat(found)
                .isPresent()
                .hasValueSatisfying(u -> {
                    assertThat(u.getId()).isEqualTo(created.getId());
                    assertThat(u.getEmail()).isEqualTo("find@test.com");
                    assertThat(u.getLogin()).isEqualTo("findlogin");
                    assertThat(u.getName()).isEqualTo("Имя");
                    assertThat(u.getBirthday()).isEqualTo(LocalDate.of(1990, 1, 1));
                });
    }

    @Test
    void getUserById_nonExistentId_returnsEmpty() {
        Optional<User> found = userStorage.getUserById(999L);
        assertThat(found).isEmpty();
    }

    @Test
    void updateUser_changesFieldsInDb() {
        User user = userStorage.createUser(makeUser("upd@test.com", "updlogin"));
        user.setName("Новое имя");
        user.setEmail("new@test.com");
        userStorage.updateUser(user);

        Optional<User> found = userStorage.getUserById(user.getId());

        assertThat(found).isPresent()
                .hasValueSatisfying(u -> {
                    assertThat(u.getName()).isEqualTo("Новое имя");
                    assertThat(u.getEmail()).isEqualTo("new@test.com");
                });
    }

    @Test
    void getAllUsers_returnsAllCreatedUsers() {
        userStorage.createUser(makeUser("a@test.com", "alogin"));
        userStorage.createUser(makeUser("b@test.com", "blogin"));

        List<User> users = userStorage.getAllUsers();

        assertThat(users).hasSize(2);
    }

    @Test
    void deleteUser_removesUserFromDb() {
        User user = userStorage.createUser(makeUser("del@test.com", "dellogin"));
        userStorage.deleteUser(user.getId());

        assertThat(userStorage.getUserById(user.getId())).isEmpty();
    }

    @Test
    void updateUser_savesFriendshipToDb() {
        User user = userStorage.createUser(makeUser("u1@test.com", "user1"));
        User friend = userStorage.createUser(makeUser("u2@test.com", "user2"));
        user.getFriends().put(friend.getId(), FriendshipStatus.UNCONFIRMED);
        userStorage.updateUser(user);

        Optional<User> found = userStorage.getUserById(user.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getFriends())
                .containsEntry(friend.getId(), FriendshipStatus.UNCONFIRMED);
    }

    @Test
    void updateUser_confirmedFriendship_persistsStatus() {
        User user = userStorage.createUser(makeUser("c1@test.com", "conf1"));
        User friend = userStorage.createUser(makeUser("c2@test.com", "conf2"));
        user.getFriends().put(friend.getId(), FriendshipStatus.CONFIRMED);
        userStorage.updateUser(user);

        Optional<User> found = userStorage.getUserById(user.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getFriends())
                .containsEntry(friend.getId(), FriendshipStatus.CONFIRMED);
    }

    @Test
    void deleteUser_alsoRemovesFriendships() {
        User user = userStorage.createUser(makeUser("owner@test.com", "ownerlogin"));
        User friend = userStorage.createUser(makeUser("fr@test.com", "frlogin"));
        user.getFriends().put(friend.getId(), FriendshipStatus.UNCONFIRMED);
        userStorage.updateUser(user);

        userStorage.deleteUser(user.getId());

        assertThat(userStorage.getUserById(user.getId())).isEmpty();
    }
}
