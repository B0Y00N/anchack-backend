package com.kbait.anchack.user.mapper;

import com.kbait.anchack.user.domain.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

@Repository
public class UserRepository {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private static final RowMapper<User> ROW_MAPPER = new RowMapper<User>() {
        @Override
        public User mapRow(ResultSet rs, int rowNum) throws SQLException {
            User user = new User();
            user.setId(rs.getLong("user_id"));
            user.setEmail(rs.getString("email"));
            user.setPassword(rs.getString("password"));
            user.setName(rs.getString("name"));
            user.setNickname(rs.getString("nickname"));
            user.setBirthDate(rs.getObject("birth_date", java.time.LocalDate.class));
            user.setGender(rs.getString("gender"));
            user.setProfileImageUrl(rs.getString("profile_image_url"));
            user.setProvider(rs.getString("provider"));
            user.setProviderId(rs.getString("provider_id"));
            user.setStatus(rs.getString("status"));
            user.setRole(rs.getString("role"));
            user.setLastLoginAt(rs.getObject("last_login_at", java.time.LocalDateTime.class));
            user.setCreatedAt(rs.getObject("created_at", java.time.LocalDateTime.class));
            user.setUpdatedAt(rs.getObject("updated_at", java.time.LocalDateTime.class));
            user.setDeletedAt(rs.getObject("deleted_at", java.time.LocalDateTime.class));
            return user;
        }
    };

    public User findByProviderAndProviderId(String provider, String providerId) {
        List<User> result = jdbcTemplate.query(
                "SELECT * FROM users WHERE provider = ? AND provider_id = ?",
                ROW_MAPPER, provider, providerId);
        return result.isEmpty() ? null : result.get(0);
    }

    public void insert(User user) {
        jdbcTemplate.update(
                "INSERT INTO users (email, nickname, profile_image_url, provider, provider_id, last_login_at) " +
                        "VALUES (?, ?, ?, ?, ?, NOW())",
                user.getEmail(), user.getNickname(), user.getProfileImageUrl(), user.getProvider(), user.getProviderId());
    }

    public void update(User user) {
        jdbcTemplate.update(
                "UPDATE users SET nickname = ?, profile_image_url = ?, email = ?, last_login_at = NOW() " +
                        "WHERE provider = ? AND provider_id = ?",
                user.getNickname(), user.getProfileImageUrl(), user.getEmail(), user.getProvider(), user.getProviderId());
    }
}
