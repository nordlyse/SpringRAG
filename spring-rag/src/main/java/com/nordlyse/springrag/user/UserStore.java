package com.nordlyse.springrag.user;

import java.util.List;
import java.util.Optional;

public interface UserStore {

    Optional<UserDirectory.UserAccount> findById(String id);

    Optional<UserDirectory.UserAccount> findByUsername(String username);

    void add(String id, String username, List<String> roles);
}
