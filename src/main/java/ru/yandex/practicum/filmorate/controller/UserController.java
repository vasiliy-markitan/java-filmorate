package ru.yandex.practicum.filmorate.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.user.UserStorage;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {
    private final UserStorage userStorage;

    @PostMapping
    public User createUser(@RequestBody User user) {
        log.info("Создание пользователя: {}", user);
        validate(user);
        applyNameFallback(user);
        User saved = userStorage.createUser(user);
        log.info("Пользователь создан с id={}", saved.getId());
        return saved;
    }

    @PutMapping
    public User updateUser(@RequestBody User user) {
        log.info("Обновление пользователя: {}", user);
        userStorage.getUserById(user.getId()).orElseThrow(() -> {
            log.warn("Пользователь с id={} не найден", user.getId());
            return new NotFoundException("Пользователь с id=" + user.getId() + " не найден");
        });
        validate(user);
        applyNameFallback(user);
        User updated = userStorage.updateUser(user);
        log.info("Пользователь с id={} обновлён", updated.getId());
        return updated;
    }

    @GetMapping
    public List<User> getAllUsers() {
        log.info("Запрос списка всех пользователей");
        return userStorage.getAllUsers();
    }

    private void validate(User user) {
        if (user.getEmail() == null || user.getEmail().isBlank()) {
            log.warn("Валидация не пройдена: электронная почта пустая");
            throw new ValidationException("Электронная почта не может быть пустой");
        }
        if (!user.getEmail().contains("@")) {
            log.warn("Валидация не пройдена: электронная почта '{}' не содержит @", user.getEmail());
            throw new ValidationException("Электронная почта должна содержать символ @");
        }
        if (user.getLogin() == null || user.getLogin().isBlank()) {
            log.warn("Валидация не пройдена: логин пустой");
            throw new ValidationException("Логин не может быть пустым");
        }
        if (user.getLogin().contains(" ")) {
            log.warn("Валидация не пройдена: логин '{}' содержит пробелы", user.getLogin());
            throw new ValidationException("Логин не может содержать пробелы");
        }
        if (user.getBirthday() == null || user.getBirthday().isAfter(LocalDate.now())) {
            log.warn("Валидация не пройдена: дата рождения {} не указана или находится в будущем", user.getBirthday());
            throw new ValidationException("Дата рождения не может быть в будущем");
        }
    }

    private void applyNameFallback(User user) {
        if (user.getName() == null || user.getName().isBlank()) {
            user.setName(user.getLogin());
        }
    }
}
