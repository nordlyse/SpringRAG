package com.nordlyse.springrag.user;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;

import com.nordlyse.springrag.user.UserDirectory.UserAccount;
import com.nordlyse.springrag.user.UserDirectory.UserMatch;
import com.nordlyse.springrag.user.UserDirectory.UserRoles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserDirectoryTest {

    private final UserDirectory users = new UserDirectory(new MemoryUserStore());

    @Test
    void findUserUsesEitherPrimaryKey() {
        users.add("1002", "nora", List.of("EDITOR", "VIEWER"));

        UserMatch byId = users.findUser("1002", "");
        UserMatch byName = users.findUser("", "Nora");

        assertThat(byId.found()).isTrue();
        assertThat(byId.id()).isEqualTo("1002");
        assertThat(byId.username()).isEqualTo("nora");
        assertThat(byName).isEqualTo(byId);
        assertThat(users.findUser("1002", "nora")).isEqualTo(byId);
    }

    @Test
    void findUserRejectsKeysThatPointAtDifferentPeople() {
        users.add("1001", "ada", List.of("ADMIN"));
        users.add("1003", "milo", List.of("VIEWER"));

        UserMatch match = users.findUser("1001", "milo");

        assertThat(match.found()).isFalse();
    }

    @Test
    void rolesForUserReturnsTheAssignedRoles() {
        users.add("1001", "ada", List.of("ADMIN"));
        users.add("1003", "milo", List.of("VIEWER"));

        UserRoles roles = users.rolesForUser("", "ada");

        assertThat(roles.found()).isTrue();
        assertThat(roles.id()).isEqualTo("1001");
        assertThat(roles.roles()).containsExactly("ADMIN");
        assertThat(users.rolesForUser("1003", "").roles()).containsExactly("VIEWER");
        assertThat(users.rolesForUser("missing", "").found()).isFalse();
    }

    @Test
    void bothLookupMethodsAreChatTools() throws Exception {
        ToolCallback[] tools = ToolCallbacks.from(users);

        assertThat(tools).extracting(tool -> tool.getToolDefinition().name())
                .containsExactly("findUser", "rolesForUser");
        assertThat(tools[0].getToolDefinition().description()).contains("primary keys");
        assertThat(tools[1].getToolDefinition().description()).contains("roles");
        assertThat(UserDirectory.class.getMethod("findUser", String.class, String.class)
                .getAnnotation(McpTool.class).name()).isEqualTo("findUser");
        assertThat(UserDirectory.class.getMethod("rolesForUser", String.class, String.class)
                .getAnnotation(McpTool.class).name()).isEqualTo("rolesForUser");
    }

    @Test
    void aRepeatedPrimaryKeyIsRejected() {
        users.add("1001", "ada", List.of("ADMIN"));

        assertThatThrownBy(() -> users.add("1001", "other", List.of("VIEWER")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> users.add("9999", "ada", List.of("VIEWER")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static final class MemoryUserStore implements UserStore {

        private final Map<String, UserAccount> byId = new LinkedHashMap<>();
        private final Map<String, UserAccount> byUsername = new LinkedHashMap<>();

        @Override
        public Optional<UserAccount> findById(String id) {
            UserAccount account = byId.get(id);
            return account == null ? Optional.empty() : Optional.of(account);
        }

        @Override
        public Optional<UserAccount> findByUsername(String username) {
            UserAccount account = byUsername.get(username.toLowerCase(Locale.ROOT));
            return account == null ? Optional.empty() : Optional.of(account);
        }

        @Override
        public void add(String id, String username, List<String> roles) {
            if (byId.containsKey(id) || byUsername.containsKey(username)) {
                throw new IllegalArgumentException("User id and username must each be unique.");
            }
            List<String> sorted = new ArrayList<>(roles);
            sorted.sort(String::compareTo);
            UserAccount account = new UserAccount(id, username, List.copyOf(sorted));
            byId.put(id, account);
            byUsername.put(username, account);
        }
    }
}
