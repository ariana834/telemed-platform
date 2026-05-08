package com.telemedicina.security;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private final JdbcTemplate jdbc;

    public UserDetailsServiceImpl(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        String sql = """
                SELECT id, email, password_hash, role, is_active
                FROM users
                WHERE email = ?
                """;

        return jdbc.query(sql, rs -> {
            if (!rs.next()) {
                throw new UsernameNotFoundException("Utilizator negăsit: " + email);
            }
            return new CustomUserDetails(
                    rs.getLong("id"),
                    rs.getString("email"),
                    rs.getString("password_hash"),
                    rs.getString("role"),
                    rs.getBoolean("is_active")
            );
        }, email);
    }
}