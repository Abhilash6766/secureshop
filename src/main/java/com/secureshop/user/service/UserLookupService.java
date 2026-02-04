package com.secureshop.user.service;

import com.secureshop.user.domain.User;
import com.secureshop.user.repo.UserRepository;
import org.springframework.stereotype.Service;

@Service
public class UserLookupService {

    private final UserRepository users;

    public UserLookupService(UserRepository users) {
        this.users = users;
    }

    public Long requireUserIdByEmail(String email) {
        return users.findByEmail(email.toLowerCase())
                .map(User::getId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
    }
}