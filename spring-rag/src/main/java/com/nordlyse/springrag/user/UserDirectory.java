package com.nordlyse.springrag.user;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

@Component
public class UserDirectory {

    private final UserStore users;

    public UserDirectory(UserStore users) {
        this.users = users;
    }

    @Tool(
            name = "findUser",
            description = "Find one test user by user id or by username. User id and username are both primary keys, so either value identifies at most one user. Pass an empty string for the key you do not have. When both keys are present they must belong to the same user.")
    public UserMatch findUser(
            @ToolParam(description = "User id. Primary key. Use an empty string when the id is unknown.", required = false)
            String userId,
            @ToolParam(description = "Username. Primary key. Use an empty string when the username is unknown.", required = false)
            String username) {
        UserAccount account = accountFor(userId, username);
        if (account == null) {
            return new UserMatch("", "", false);
        }
        return new UserMatch(account.id(), account.username(), true);
    }

    @Tool(
            name = "rolesForUser",
            description = "Return the roles of one test user. Identify the user by user id or by username. Both values are primary keys. Pass an empty string for the key you do not have.")
    public UserRoles rolesForUser(
            @ToolParam(description = "User id. Primary key. Use an empty string when the id is unknown.", required = false)
            String userId,
            @ToolParam(description = "Username. Primary key. Use an empty string when the username is unknown.", required = false)
            String username) {
        UserAccount account = accountFor(userId, username);
        if (account == null) {
            return new UserRoles("", "", List.of(), false);
        }
        return new UserRoles(account.id(), account.username(), account.roles(), true);
    }

    void add(String id, String username, List<String> roles) {
        String userId = key(id);
        String name = usernameKey(username);
        if (userId.isEmpty() || name.isEmpty()) {
            throw new IllegalArgumentException("User id and username are required.");
        }
        users.add(userId, name, roles == null ? List.of() : List.copyOf(roles));
    }

    private UserAccount accountFor(String userId, String username) {
        String id = key(userId);
        String name = usernameKey(username);
        Optional<UserAccount> byUserId = id.isEmpty() ? Optional.empty() : users.findById(id);
        Optional<UserAccount> byName = name.isEmpty() ? Optional.empty() : users.findByUsername(name);
        if (!id.isEmpty() && !name.isEmpty()) {
            if (byUserId.isEmpty() || byName.isEmpty() || !byUserId.get().id().equals(byName.get().id())) {
                return null;
            }
            return byUserId.get();
        }
        if (byUserId.isPresent()) {
            return byUserId.get();
        }
        return byName.orElse(null);
    }

    private static String key(String value) {
        return value == null ? "" : value.trim();
    }

    private static String usernameKey(String username) {
        return key(username).toLowerCase(Locale.ROOT);
    }

    public record UserAccount(String id, String username, List<String> roles) {
    }

    public record UserMatch(String id, String username, boolean found) {
    }

    public record UserRoles(String id, String username, List<String> roles, boolean found) {
        public UserRoles {
            roles = roles == null ? List.of() : List.copyOf(roles);
        }
    }
}
