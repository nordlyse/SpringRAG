package com.nordlyse.springrag.user;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Component
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor(onConstructor_ = @Autowired)
public class UserDirectory {

    private UserStore users;

    @McpTool(
            name = "findUser",
            description = "Find one test user by user id or by username. User id and username are both primary keys, so either value identifies at most one user. Pass an empty string for the key you do not have. When both keys are present they must belong to the same user.")
    @Tool(
            name = "findUser",
            description = "Find one test user by user id or by username. User id and username are both primary keys, so either value identifies at most one user. Pass an empty string for the key you do not have. When both keys are present they must belong to the same user.")
    public UserMatch findUser(
            @McpToolParam(description = "User id. Primary key. Use an empty string when the id is unknown.", required = false)
            @ToolParam(description = "User id. Primary key. Use an empty string when the id is unknown.", required = false)
            String userId,
            @McpToolParam(description = "Username. Primary key. Use an empty string when the username is unknown.", required = false)
            @ToolParam(description = "Username. Primary key. Use an empty string when the username is unknown.", required = false)
            String username) {
        UserAccount account = accountFor(userId, username);
        if (account == null) {
            return new UserMatch("", "", false);
        }
        return new UserMatch(account.getId(), account.getUsername(), true);
    }

    @McpTool(
            name = "rolesForUser",
            description = "Return the roles of one test user. Identify the user by user id or by username. Both values are primary keys. Pass an empty string for the key you do not have.")
    @Tool(
            name = "rolesForUser",
            description = "Return the roles of one test user. Identify the user by user id or by username. Both values are primary keys. Pass an empty string for the key you do not have.")
    public UserRoles rolesForUser(
            @McpToolParam(description = "User id. Primary key. Use an empty string when the id is unknown.", required = false)
            @ToolParam(description = "User id. Primary key. Use an empty string when the id is unknown.", required = false)
            String userId,
            @McpToolParam(description = "Username. Primary key. Use an empty string when the username is unknown.", required = false)
            @ToolParam(description = "Username. Primary key. Use an empty string when the username is unknown.", required = false)
            String username) {
        UserAccount account = accountFor(userId, username);
        if (account == null) {
            return new UserRoles("", "", List.of(), false);
        }
        return new UserRoles(account.getId(), account.getUsername(), account.getRoles(), true);
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
            if (byUserId.isEmpty() || byName.isEmpty() || !byUserId.get().getId().equals(byName.get().getId())) {
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

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserAccount {

        private String id;
        private String username;
        private List<String> roles;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode
    public static class UserMatch {

        private String id;
        private String username;
        private boolean found;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserRoles {

        private String id;
        private String username;
        private List<String> roles;
        private boolean found;

        public List<String> getRoles() {
            return roles == null ? List.of() : List.copyOf(roles);
        }
    }
}
