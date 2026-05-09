package ru.yandex.practicum.filmorate.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.service.UserService;
import ru.yandex.practicum.filmorate.storage.user.InMemoryUserStorage;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class UserControllerTest {

    private UserController controller;

    @BeforeEach
    void setUp() {
        controller = new UserController(new UserService(new InMemoryUserStorage()));
    }

    private User validUser() {
        User user = new User();
        user.setEmail("user@example.com");
        user.setLogin("userlogin");
        user.setName("Имя");
        user.setBirthday(LocalDate.of(1990, 1, 1));
        return user;
    }

    @Test
    void createUser_valid_returnsUserWithId() {
        User result = controller.createUser(validUser());
        assertNotNull(result.getId());
        assertTrue(result.getId() > 0);
    }

    @Test
    void createUser_emptyEmail_throwsValidationException() {
        User user = validUser();
        user.setEmail("");
        assertThrows(ValidationException.class, () -> controller.createUser(user));
    }

    @Test
    void createUser_nullEmail_throwsValidationException() {
        User user = validUser();
        user.setEmail(null);
        assertThrows(ValidationException.class, () -> controller.createUser(user));
    }

    @Test
    void createUser_emailWithoutAt_throwsValidationException() {
        User user = validUser();
        user.setEmail("userexample.com");
        assertThrows(ValidationException.class, () -> controller.createUser(user));
    }

    @Test
    void createUser_emailWithAt_ok() {
        User user = validUser();
        user.setEmail("a@b");
        assertDoesNotThrow(() -> controller.createUser(user));
    }

    @Test
    void createUser_emptyLogin_throwsValidationException() {
        User user = validUser();
        user.setLogin("");
        assertThrows(ValidationException.class, () -> controller.createUser(user));
    }

    @Test
    void createUser_nullLogin_throwsValidationException() {
        User user = validUser();
        user.setLogin(null);
        assertThrows(ValidationException.class, () -> controller.createUser(user));
    }

    @Test
    void createUser_loginWithSpace_throwsValidationException() {
        User user = validUser();
        user.setLogin("user login");
        assertThrows(ValidationException.class, () -> controller.createUser(user));
    }

    @Test
    void createUser_loginWithLeadingSpace_throwsValidationException() {
        User user = validUser();
        user.setLogin(" userlogin");
        assertThrows(ValidationException.class, () -> controller.createUser(user));
    }

    @Test
    void createUser_emptyName_usesLoginAsName() {
        User user = validUser();
        user.setName("");
        User result = controller.createUser(user);
        assertEquals(user.getLogin(), result.getName());
    }

    @Test
    void createUser_nullName_usesLoginAsName() {
        User user = validUser();
        user.setName(null);
        User result = controller.createUser(user);
        assertEquals(user.getLogin(), result.getName());
    }

    @Test
    void createUser_blankName_usesLoginAsName() {
        User user = validUser();
        user.setName("   ");
        User result = controller.createUser(user);
        assertEquals(user.getLogin(), result.getName());
    }

    @Test
    void createUser_birthdayToday_ok() {
        User user = validUser();
        user.setBirthday(LocalDate.now());
        assertDoesNotThrow(() -> controller.createUser(user));
    }

    @Test
    void createUser_birthdayTomorrow_throwsValidationException() {
        User user = validUser();
        user.setBirthday(LocalDate.now().plusDays(1));
        assertThrows(ValidationException.class, () -> controller.createUser(user));
    }

    @Test
    void createUser_nullBirthday_throwsValidationException() {
        User user = validUser();
        user.setBirthday(null);
        assertThrows(ValidationException.class, () -> controller.createUser(user));
    }

    @Test
    void updateUser_valid_returnsUpdatedUser() {
        User user = controller.createUser(validUser());
        user.setName("Новое имя");
        User updated = controller.updateUser(user);
        assertEquals("Новое имя", updated.getName());
    }

    @Test
    void getAllUsers_empty_returnsEmptyList() {
        assertTrue(controller.getAllUsers().isEmpty());
    }

    @Test
    void getAllUsers_afterCreate_returnsUser() {
        controller.createUser(validUser());
        assertEquals(1, controller.getAllUsers().size());
    }
}
