package ru.yandex.practicum.filmorate.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.user.UserStorage;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
public class UserService {
    private final UserStorage userStorage;

    @Autowired
    public UserService(UserStorage userStorage) {
        this.userStorage = userStorage;
    }

    public User createUser(User user) {
        log.debug("Начало создания пользователя: {}", user);
        validate(user);
        applyNameFallback(user);
        User saved = userStorage.createUser(user);
        log.debug("Пользователь создан: id={}, login={}", saved.getId(), saved.getLogin());
        return saved;
    }

    public User updateUser(User user) {
        log.debug("Начало обновления пользователя: {}", user);
        validate(user);
        applyNameFallback(user);
        User existing = getById(user.getId());
        log.debug("Текущее состояние пользователя перед обновлением: {}", existing);
        User updated = userStorage.updateUser(user);
        log.debug("Пользователь обновлён: {}", updated);
        return updated;
    }

    public List<User> getAllUsers() {
        List<User> users = userStorage.getAllUsers();
        log.debug("Получен список пользователей, количество: {}", users.size());
        return users;
    }

    public User getById(Long id) {
        log.debug("Поиск пользователя по id={}", id);
        return userStorage.getUserById(id)
                .orElseThrow(() -> {
                    log.warn("Пользователь с id={} не найден", id);
                    return new NotFoundException("Пользователь с id=" + id + " не найден");
                });
    }

    public void addFriend(Long userId, Long friendId) {
        log.debug("Пользователь id={} добавляет в друзья пользователя id={}", userId, friendId);
        if (userId.equals(friendId)) {
            log.warn("Валидация не пройдена: пользователь id={} пытается добавить себя в друзья", userId);
            throw new ValidationException("Пользователь не может добавить самого себя в друзья");
        }
        User user = getById(userId);
        User friend = getById(friendId);
        user.getFriends().add(friendId);
        friend.getFriends().add(userId);
        log.info("Пользователь id={} и пользователь id={} теперь друзья. " +
                "Друзей у id={}: {}, у id={}: {}",
                userId, friendId, userId, user.getFriends().size(), friendId, friend.getFriends().size());
    }

    public void removeFriend(Long userId, Long friendId) {
        log.debug("Пользователь id={} удаляет из друзей пользователя id={}", userId, friendId);
        if (userId.equals(friendId)) {
            log.warn("Валидация не пройдена: пользователь id={} пытается удалить себя из друзей", userId);
            throw new ValidationException("Пользователь не может удалить самого себя из друзей");
        }
        User user = getById(userId);
        User friend = getById(friendId);
        user.getFriends().remove(friendId);
        friend.getFriends().remove(userId);
        log.info("Пользователь id={} и пользователь id={} больше не друзья", userId, friendId);
    }

    public List<User> getFriends(Long userId) {
        log.debug("Запрос списка друзей пользователя id={}", userId);
        User user = getById(userId);
        List<User> friends = user.getFriends().stream()
                .map(this::getById)
                .collect(Collectors.toList());
        log.debug("Пользователь id={} имеет {} друзей", userId, friends.size());
        return friends;
    }

    public List<User> getCommonFriends(Long userId, Long otherId) {
        log.debug("Запрос общих друзей пользователей id={} и id={}", userId, otherId);
        if (userId.equals(otherId)) {
            log.warn("Валидация не пройдена: запрос общих друзей с самим собой, id={}", userId);
            throw new ValidationException("Нельзя запрашивать общих друзей с самим собой");
        }
        Set<Long> userFriends = getById(userId).getFriends();
        Set<Long> otherFriends = getById(otherId).getFriends();
        List<User> common = userFriends.stream()
                .filter(otherFriends::contains)
                .map(this::getById)
                .collect(Collectors.toList());
        log.debug("Общих друзей у пользователей id={} и id={}: {}", userId, otherId, common.size());
        return common;
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
            log.warn("Валидация не пройдена: дата рождения {} не указана или находится в будущем",
                    user.getBirthday());
            throw new ValidationException("Дата рождения не может быть в будущем");
        }
    }

    private void applyNameFallback(User user) {
        if (user.getName() == null || user.getName().isBlank()) {
            log.debug("Имя пользователя не задано, используется логин '{}' в качестве имени", user.getLogin());
            user.setName(user.getLogin());
        }
    }
}
