package com.nordlyse.springrag.user;

import java.util.List;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "spring-rag.seed-test-users", havingValue = "true", matchIfMissing = true)
class TestUserSeed implements ApplicationRunner {

    private final UserDirectory users;

    TestUserSeed(UserDirectory users) {
        this.users = users;
    }

    @Override
    public void run(ApplicationArguments args) {
        add("1001", "ada", List.of("ADMIN"));
        add("1002", "nora", List.of("EDITOR", "VIEWER"));
        add("1003", "milo", List.of("VIEWER"));
    }

    private void add(String id, String username, List<String> roles) {
        if (users.findUser(id, "").found()) {
            return;
        }
        users.add(id, username, roles);
    }
}
