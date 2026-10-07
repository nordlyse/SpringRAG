package com.nordlyse.springrag.user;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import com.nordlyse.springrag.user.UserDirectory.UserAccount;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Repository
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
class JdbcUserStore implements UserStore {

    private static final String USERS = """
            CREATE TABLE IF NOT EXISTS users (
                id varchar(80) PRIMARY KEY,
                username varchar(80) NOT NULL UNIQUE
            )
            """;

    private static final String ROLES = """
            CREATE TABLE IF NOT EXISTS roles (
                user_id varchar(80) NOT NULL REFERENCES users (id),
                role_name varchar(80) NOT NULL,
                PRIMARY KEY (user_id, role_name)
            )
            """;

    private JdbcTemplate jdbcTemplate;

    private volatile boolean tablesReady;

    @Autowired
    JdbcUserStore(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Optional<UserAccount> findById(String id) {
        ensureTables();
        return oneUser("SELECT id, username FROM users WHERE id = ?", id);
    }

    @Override
    public Optional<UserAccount> findByUsername(String username) {
        ensureTables();
        return oneUser("SELECT id, username FROM users WHERE username = ?", usernameKey(username));
    }

    @Override
    public void add(String id, String username, List<String> roles) {
        ensureTables();
        String userId = id == null ? "" : id.trim();
        String name = usernameKey(username);
        if (userId.isEmpty() || name.isEmpty()) {
            throw new IllegalArgumentException("User id and username are required.");
        }
        try {
            jdbcTemplate.update("INSERT INTO users (id, username) VALUES (?, ?)", userId, name);
        }
        catch (DuplicateKeyException exception) {
            throw new IllegalArgumentException("User id and username must each be unique.");
        }
        List<String> assigned = roles == null ? List.of() : roles;
        for (String role : assigned) {
            if (role == null || role.isBlank()) {
                continue;
            }
            jdbcTemplate.update(
                    "INSERT INTO roles (user_id, role_name) VALUES (?, ?)",
                    userId,
                    role.trim());
        }
    }

    private Optional<UserAccount> oneUser(String sql, String key) {
        if (key == null || key.isBlank()) {
            return Optional.empty();
        }
        List<UserAccount> rows = jdbcTemplate.query(sql, JdbcUserStore::mapUser, key);
        if (rows == null || rows.isEmpty()) {
            return Optional.empty();
        }
        UserAccount user = rows.getFirst();
        return Optional.of(new UserAccount(user.getId(), user.getUsername(), rolesOf(user.getId())));
    }

    private List<String> rolesOf(String userId) {
        List<String> roles = jdbcTemplate.query(
                "SELECT role_name FROM roles WHERE user_id = ? ORDER BY role_name",
                (resultSet, rowNumber) -> resultSet.getString("role_name"),
                userId);
        return roles == null ? List.of() : List.copyOf(roles);
    }

    private void ensureTables() {
        if (tablesReady) {
            return;
        }
        jdbcTemplate.execute(USERS);
        jdbcTemplate.execute(ROLES);
        tablesReady = true;
    }

    private static UserAccount mapUser(ResultSet resultSet, int rowNumber) throws SQLException {
        return new UserAccount(resultSet.getString("id"), resultSet.getString("username"), List.of());
    }

    private static String usernameKey(String username) {
        return username == null ? "" : username.trim().toLowerCase(Locale.ROOT);
    }
}
