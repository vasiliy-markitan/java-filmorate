package ru.yandex.practicum.filmorate.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.FriendshipStatus;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.user.UserStorage;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
public class UserService {
    private final UserStorage userStorage;

    @Autowired
    public UserService(@Qualifier("userDbStorage") UserStorage userStorage) {
        this.userStorage = userStorage;
    }

    public User createUser(User user) {
        validate(user);
        applyNameFallback(user);
        User saved = userStorage.createUser(user);
        log.info("Пользователь создан: id={}, login={}", saved.getId(), saved.getLogin());
        return saved;
    }

    public User updateUser(User user) {
        validate(user);
        applyNameFallback(user);
        getById(user.getId());
        User updated = userStorage.updateUser(user);
        log.info("Пользователь обновлён: id={}, login={}", updated.getId(), updated.getLogin());
        return updated;
    }

    public List<User> getAllUsers() {
        List<User> users = userStorage.getAllUsers();
        log.debug("Запрос всех пользователей, найдено: {}", users.size());
        return users;
    }

    public User getById(Long id) {
        return userStorage.getUserById(id)
                .orElseThrow(() -> {
                    log.warn("Пользователь с id={} не найден", id);
                    return new NotFoundException("Пользователь с id=" + id + " не найден");
                });
    }

    public void addFriend(Long userId, Long friendId) {
        if (userId.equals(friendId)) {
            log.warn("Валидация не пройдена: пользователь id={} пытается добавить себя в друзья", userId);
            throw new ValidationException("Пользователь не может добавить самого себя в друзья");
        }
        getById(userId);
        getById(friendId);
        if (userStorage.friendshipExists(friendId, userId)) {
            userStorage.addFriend(userId, friendId, FriendshipStatus.CONFIRMED);
            userStorage.updateFriendshipStatus(friendId, userId, FriendshipStatus.CONFIRMED);
            log.info("Встречная заявка: дружба id={} и id={} подтверждена", userId, friendId);
        } else {
            userStorage.addFriend(userId, friendId, FriendshipStatus.UNCONFIRMED);
            log.info("Пользователь id={} отправил заявку в друзья пользователю id={}", userId, friendId);
        }
    }

    public void removeFriend(Long userId, Long friendId) {
        if (userId.equals(friendId)) {
            log.warn("Валидация не пройдена: пользователь id={} пытается удалить себя из друзей", userId);
            throw new ValidationException("Пользователь не может удалить самого себя из друзей");
        }
        getById(userId);
        getById(friendId);
        userStorage.removeFriend(userId, friendId);
        if (userStorage.friendshipExists(friendId, userId)) {
            userStorage.updateFriendshipStatus(friendId, userId, FriendshipStatus.UNCONFIRMED);
        }
        log.info("Пользователь id={} удалил из друзей пользователя id={}", userId, friendId);
    }

    public List<User> getFriends(Long userId) {
        getById(userId);
        List<User> friends = userStorage.getFriends(userId);
        log.debug("Запрос друзей пользователя id={}, найдено: {}", userId, friends.size());
        return friends;
    }

    public List<User> getCommonFriends(Long userId, Long otherId) {
        if (userId.equals(otherId)) {
            log.warn("Валидация не пройдена: запрос общих друзей с самим собой, id={}", userId);
            throw new ValidationException("Нельзя запрашивать общих друзей с самим собой");
        }
        getById(userId);
        getById(otherId);
        List<User> common = userStorage.getCommonFriends(userId, otherId);
        log.debug("Запрос общих друзей id={} и id={}, найдено: {}", userId, otherId, common.size());
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
            user.setName(user.getLogin());
        }
    }
}
