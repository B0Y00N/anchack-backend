package com.kbait.anchack.user.service;

import com.kbait.anchack.user.dto.KakaoUserInfo;
import com.kbait.anchack.user.model.KakaoMember;
import com.kbait.anchack.user.mapper.MemberRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MemberService {

    @Autowired
    private MemberRepository memberRepository;

    /**
     * 카카오 로그인 사용자 정보를 DB에 저장(최초 로그인) 또는 갱신(기존 회원)한다.
     */
    @Transactional
    public KakaoMember saveOrUpdate(KakaoUserInfo userInfo) {
        KakaoMember existing = memberRepository.findByKakaoId(userInfo.getId());

        KakaoMember member = new KakaoMember();
        member.setKakaoId(userInfo.getId());
        member.setNickname(userInfo.getNickname());
        member.setProfileImage(userInfo.getProfileImage());
        member.setEmail(userInfo.getEmail());

        if (existing == null) {
            memberRepository.insert(member);
            KakaoMember saved = memberRepository.findByKakaoId(userInfo.getId());
            return saved;
        } else {
            memberRepository.update(member);
            member.setId(existing.getId());
            return member;
        }
    }
}
