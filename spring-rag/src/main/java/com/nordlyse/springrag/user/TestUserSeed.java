package com.nordlyse.springrag.user;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Component
@ConditionalOnProperty(name = "spring-rag.seed-test-users", havingValue = "true", matchIfMissing = true)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor(onConstructor_ = @Autowired)
class TestUserSeed implements ApplicationRunner {

    private UserDirectory users;

    @Override
    public void run(ApplicationArguments args) {
        add("1001", "ada", List.of("ADMIN"));
        add("1002", "nora", List.of("EDITOR", "VIEWER"));
        add("1003", "milo", List.of("VIEWER"));
    }

    private void add(String id, String username, List<String> roles) {
        if (users.findUser(id, "").isFound()) {
            return;
        }
        users.add(id, username, roles);
    }
}
