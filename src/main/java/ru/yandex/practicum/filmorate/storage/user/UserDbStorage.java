package ru.yandex.practicum.filmorate.storage.user;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.model.FriendshipStatus;
import ru.yandex.practicum.filmorate.model.User;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Slf4j
@Component
@Qualifier("userDbStorage")
public class UserDbStorage implements UserStorage {

    private final JdbcTemplate jdbc;

    @Autowired
    public UserDbStorage(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public User createUser(User user) {
        String sql = "INSERT INTO users (email, login, name, birthday) VALUES (?, ?, ?, ?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, user.getEmail());
            ps.setString(2, user.getLogin());
            ps.setString(3, user.getName());
            ps.setDate(4, user.getBirthday() != null ? Date.valueOf(user.getBirthday()) : null);
            return ps;
        }, keyHolder);
        user.setId(Objects.requireNonNull(keyHolder.getKey()).longValue());
        log.debug("Пользователь сохранён в БД: id={}", user.getId());
        return user;
    }

    @Override
    public User updateUser(User user) {
        String sql = "UPDATE users SET email=?, login=?, name=?, birthday=? WHERE user_id=?";
        jdbc.update(sql,
                user.getEmail(),
                user.getLogin(),
                user.getName(),
                user.getBirthday() != null ? Date.valueOf(user.getBirthday()) : null,
                user.getId());
        log.debug("Пользователь обновлён в БД: id={}", user.getId());
        return user;
    }

    @Override
    public void deleteUser(Long id) {
        // friendships и likes удаляются каскадно через ON DELETE CASCADE
        jdbc.update("DELETE FROM users WHERE user_id=?", id);
    }

    @Override
    public List<User> getAllUsers() {
        String sql = "SELECT user_id, email, login, name, birthday FROM users";
        return jdbc.query(sql, this::mapRowToUser);
    }

    @Override
    public Optional<User> getUserById(Long id) {
        String sql = "SELECT user_id, email, login, name, birthday FROM users WHERE user_id=?";
        List<User> users = jdbc.query(sql, this::mapRowToUser, id);
        return users.isEmpty() ? Optional.empty() : Optional.of(users.get(0));
    }

    @Override
    public void addFriend(Long userId, Long friendId, FriendshipStatus status) {
        jdbc.update(
                "INSERT INTO friendships (user_id, friend_id, status) VALUES (?, ?, ?)",
                userId, friendId, status.name()
        );
        log.debug("Дружба добавлена: userId={}, friendId={}, status={}", userId, friendId, status);
    }

    @Override
    public void removeFriend(Long userId, Long friendId) {
        jdbc.update("DELETE FROM friendships WHERE user_id=? AND friend_id=?", userId, friendId);
        log.debug("Дружба удалена: userId={}, friendId={}", userId, friendId);
    }

    @Override
    public void updateFriendshipStatus(Long userId, Long friendId, FriendshipStatus status) {
        jdbc.update(
                "UPDATE friendships SET status=? WHERE user_id=? AND friend_id=?",
                status.name(), userId, friendId
        );
        log.debug("Статус дружбы обновлён: userId={}, friendId={}, status={}", userId, friendId, status);
    }

    @Override
    public boolean friendshipExists(Long userId, Long friendId) {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM friendships WHERE user_id=? AND friend_id=?",
                Integer.class, userId, friendId
        );
        return count != null && count > 0;
    }

    @Override
    public List<User> getFriends(Long userId) {
        String sql = "SELECT u.user_id, u.email, u.login, u.name, u.birthday " +
                     "FROM users u " +
                     "JOIN friendships f ON u.user_id = f.friend_id " +
                     "WHERE f.user_id = ?";
        return jdbc.query(sql, this::mapRowToUser, userId);
    }

    @Override
    public List<User> getCommonFriends(Long userId, Long otherId) {
        String sql = "SELECT u.user_id, u.email, u.login, u.name, u.birthday " +
                     "FROM users u " +
                     "JOIN friendships f1 ON u.user_id = f1.friend_id AND f1.user_id = ? " +
                     "JOIN friendships f2 ON u.user_id = f2.friend_id AND f2.user_id = ?";
        return jdbc.query(sql, this::mapRowToUser, userId, otherId);
    }

    private User mapRowToUser(ResultSet rs, int rowNum) throws SQLException {
        User user = new User();
        user.setId(rs.getLong("user_id"));
        user.setEmail(rs.getString("email"));
        user.setLogin(rs.getString("login"));
        user.setName(rs.getString("name"));
        Date birthday = rs.getDate("birthday");
        user.setBirthday(birthday != null ? birthday.toLocalDate() : null);
        return user;
    }

}
