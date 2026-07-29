package com.kbait.anchack.repository;

import com.kbait.anchack.entity.KakaoMember;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

@Repository
public class MemberRepository {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private static final RowMapper<KakaoMember> ROW_MAPPER = new RowMapper<KakaoMember>() {
        @Override
        public KakaoMember mapRow(ResultSet rs, int rowNum) throws SQLException {
            KakaoMember member = new KakaoMember();
            member.setId(rs.getLong("id"));
            member.setKakaoId(rs.getLong("kakao_id"));
            member.setNickname(rs.getString("nickname"));
            member.setProfileImage(rs.getString("profile_image"));
            member.setEmail(rs.getString("email"));
            return member;
        }
    };

    public KakaoMember findByKakaoId(Long kakaoId) {
        List<KakaoMember> result = jdbcTemplate.query(
                "SELECT id, kakao_id, nickname, profile_image, email " +
                        "FROM kakao_member WHERE kakao_id = ?",
                ROW_MAPPER, kakaoId);
        return result.isEmpty() ? null : result.get(0);
    }

    public void insert(KakaoMember member) {
        jdbcTemplate.update(
                "INSERT INTO kakao_member (kakao_id, nickname, profile_image, email) " +
                        "VALUES (?, ?, ?, ?)",
                member.getKakaoId(), member.getNickname(), member.getProfileImage(), member.getEmail());
    }

    public void update(KakaoMember member) {
        jdbcTemplate.update(
                "UPDATE kakao_member SET nickname = ?, profile_image = ?, email = ? " +
                        "WHERE kakao_id = ?",
                member.getNickname(), member.getProfileImage(), member.getEmail(), member.getKakaoId());
    }
}
