package com.secureshop.auth.service;

import com.secureshop.user.repo.UserRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

import java.util.HashSet;

@Service
public class DbUserDetailsService implements UserDetailsService {

    private final UserRepository users;

    public DbUserDetailsService(UserRepository users) {
        this.users = users;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        var user = users.findByEmail(email.toLowerCase())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        boolean enabled = "ACTIVE".equalsIgnoreCase(user.getStatus());

        var authorities = new HashSet<SimpleGrantedAuthority>();


        user.getRoles().forEach(role -> {
            authorities.add(new SimpleGrantedAuthority("ROLE_" + role.getName()));


            role.getPermissions().forEach(p ->
                    authorities.add(new SimpleGrantedAuthority(p.getCode()))
            );
        });

        return org.springframework.security.core.userdetails.User
                .withUsername(user.getEmail())
                .password(user.getPasswordHash())
                .disabled(!enabled)
                .authorities(authorities)
                .build();
    }
}