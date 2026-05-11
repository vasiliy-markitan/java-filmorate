package ru.yandex.practicum.filmorate.storage.user;

import ru.yandex.practicum.filmorate.model.FriendshipStatus;
import ru.yandex.practicum.filmorate.model.User;

import java.util.*;
import java.util.stream.Collectors;

public class InMemoryUserStorage implements UserStorage {
    private final Map<Long, User> users = new HashMap<>();

    @Override
    public User createUser(User user) {
        user.setId(getNextId());
        users.put(user.getId(), user);
        return user;
    }

    @Override
    public User updateUser(User user) {
        users.put(user.getId(), user);
        return user;
    }

    @Override
    public void deleteUser(Long id) {
        users.remove(id);
    }

    @Override
    public List<User> getAllUsers() {
        return new ArrayList<>(users.values());
    }

    @Override
    public Optional<User> getUserById(Long id) {
        return Optional.ofNullable(users.get(id));
    }

    @Override
    public void addFriend(Long userId, Long friendId, FriendshipStatus status) {
        User user = users.get(userId);
        if (user != null) {
            user.getFriends().put(friendId, status);
        }
    }

    @Override
    public void removeFriend(Long userId, Long friendId) {
        User user = users.get(userId);
        if (user != null) {
            user.getFriends().remove(friendId);
        }
    }

    @Override
    public void updateFriendshipStatus(Long userId, Long friendId, FriendshipStatus status) {
        User user = users.get(userId);
        if (user != null && user.getFriends().containsKey(friendId)) {
            user.getFriends().put(friendId, status);
        }
    }

    @Override
    public boolean friendshipExists(Long userId, Long friendId) {
        User user = users.get(userId);
        return user != null && user.getFriends().containsKey(friendId);
    }

    @Override
    public List<User> getCommonFriends(Long userId, Long otherId) {
        Set<Long> otherFriendIds = users.getOrDefault(otherId, new User()).getFriends().keySet();
        return getFriends(userId).stream()
                .filter(u -> otherFriendIds.contains(u.getId()))
                .collect(Collectors.toList());
    }

    @Override
    public List<User> getFriends(Long userId) {
        User user = users.get(userId);
        if (user == null) {
            return new ArrayList<>();
        }
        return user.getFriends().keySet().stream()
                .map(users::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private long getNextId() {
        long currentMaxId = users.keySet()
                .stream()
                .mapToLong(id -> id)
                .max()
                .orElse(0);
        return ++currentMaxId;
    }
}
