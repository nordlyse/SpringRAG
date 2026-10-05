package com.nordlyse.springrag.user;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import com.nordlyse.springrag.user.UserDirectory.UserAccount;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JdbcUserStoreTest {

    @Test
    void addInsertsTheUserAndEachRole() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        JdbcUserStore store = new JdbcUserStore(jdbc);

        store.add("1001", "Ada", List.of("ADMIN", "VIEWER"));

        verify(jdbc).execute(contains("CREATE TABLE IF NOT EXISTS users"));
        verify(jdbc).execute(contains("PRIMARY KEY (user_id, role_name)"));
        verify(jdbc).update("INSERT INTO users (id, username) VALUES (?, ?)", "1001", "ada");
        verify(jdbc).update("INSERT INTO roles (user_id, role_name) VALUES (?, ?)", "1001", "ADMIN");
        verify(jdbc).update("INSERT INTO roles (user_id, role_name) VALUES (?, ?)", "1001", "VIEWER");
    }

    @Test
    void findByIdLoadsTheUserAndOrderedRoles() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        when(jdbc.query(startsWith("SELECT id, username FROM users WHERE id"), any(RowMapper.class), eq("1002")))
                .thenReturn(List.of(new UserAccount("1002", "nora", List.of())));
        when(jdbc.query(startsWith("SELECT role_name"), any(RowMapper.class), eq("1002")))
                .thenReturn(List.of("EDITOR", "VIEWER"));
        JdbcUserStore store = new JdbcUserStore(jdbc);

        UserAccount account = store.findById("1002").orElseThrow();

        assertThat(account.id()).isEqualTo("1002");
        assertThat(account.username()).isEqualTo("nora");
        assertThat(account.roles()).containsExactly("EDITOR", "VIEWER");
    }

    @Test
    void aRepeatedPrimaryKeyIsRejected() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        doThrow(new DuplicateKeyException("duplicate key"))
                .when(jdbc)
                .update("INSERT INTO users (id, username) VALUES (?, ?)", "1001", "ada");
        JdbcUserStore store = new JdbcUserStore(jdbc);

        assertThatThrownBy(() -> store.add("1001", "ada", List.of("ADMIN")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("unique");
    }
}
