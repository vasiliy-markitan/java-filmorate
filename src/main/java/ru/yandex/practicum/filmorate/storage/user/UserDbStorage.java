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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
        saveFriendships(user);
        log.debug("Пользователь обновлён в БД: id={}", user.getId());
        return user;
    }

    @Override
    public void deleteUser(Long id) {
        jdbc.update("DELETE FROM friendships WHERE user_id=? OR friend_id=?", id, id);
        jdbc.update("DELETE FROM likes WHERE user_id=?", id);
        jdbc.update("DELETE FROM users WHERE user_id=?", id);
    }

    @Override
    public List<User> getAllUsers() {
        String sql = "SELECT user_id, email, login, name, birthday FROM users";
        List<User> users = jdbc.query(sql, this::mapRowToUser);
        users.forEach(this::loadFriends);
        return users;
    }

    @Override
    public Optional<User> getUserById(Long id) {
        String sql = "SELECT user_id, email, login, name, birthday FROM users WHERE user_id=?";
        List<User> users = jdbc.query(sql, this::mapRowToUser, id);
        if (users.isEmpty()) {
            return Optional.empty();
        }
        User user = users.get(0);
        loadFriends(user);
        return Optional.of(user);
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

    private void loadFriends(User user) {
        String sql = "SELECT friend_id, status FROM friendships WHERE user_id=?";
        Map<Long, FriendshipStatus> friends = new HashMap<>();
        jdbc.query(sql, rs -> {
            friends.put(rs.getLong("friend_id"), FriendshipStatus.valueOf(rs.getString("status")));
        }, user.getId());
        user.setFriends(friends);
    }

    private void saveFriendships(User user) {
        jdbc.update("DELETE FROM friendships WHERE user_id=?", user.getId());
        if (user.getFriends() == null || user.getFriends().isEmpty()) {
            return;
        }
        for (Map.Entry<Long, FriendshipStatus> entry : user.getFriends().entrySet()) {
            jdbc.update(
                    "INSERT INTO friendships (user_id, friend_id, status) VALUES (?, ?, ?)",
                    user.getId(), entry.getKey(), entry.getValue().name()
            );
        }
    }
}
