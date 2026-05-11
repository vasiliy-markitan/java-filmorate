package ru.yandex.practicum.filmorate.storage.user;

import ru.yandex.practicum.filmorate.model.FriendshipStatus;
import ru.yandex.practicum.filmorate.model.User;

import java.util.List;
import java.util.Optional;

public interface UserStorage {
    User createUser(User user);

    User updateUser(User user);

    void deleteUser(Long id);

    List<User> getAllUsers();

    Optional<User> getUserById(Long id);

    void addFriend(Long userId, Long friendId, FriendshipStatus status);

    void removeFriend(Long userId, Long friendId);

    void updateFriendshipStatus(Long userId, Long friendId, FriendshipStatus status);

    boolean friendshipExists(Long userId, Long friendId);

    List<User> getFriends(Long userId);

    List<User> getCommonFriends(Long userId, Long otherId);
}
