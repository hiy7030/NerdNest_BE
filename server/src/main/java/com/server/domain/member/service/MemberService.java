package com.server.domain.member.service;

import com.server.domain.member.entity.Member;
import com.server.domain.member.repository.MemberRepository;
import com.server.exception.BusinessLogicException;
import com.server.exception.ExceptionCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class MemberService {
    private final MemberRepository memberRepository;

    // 회원 생성
    public Member createMember(Member member) {
        // 이메일 중복 확인
        verifyExistEmail(member.getEmail());
        // 멤버 리포지토리에 저장
        Member saveMember = memberRepository.save(member);
        // 저장한 값 반환
        return saveMember;
    }
    // 회원 정보 수정
    public Member updateMember(long memberId, Member member) {
        Member findMember = findVerifiedMember(memberId);

        Optional.ofNullable(member.getNickName())
                .ifPresent(findMember::setNickName);
        Optional.ofNullable(member.getAbout())
                .ifPresent(findMember::setAbout);

        return memberRepository.save(findMember);
    }
    // 회원 정보 가져오기
    public Member findMember(long memberId) {
        Member findMember = findVerifiedMember(memberId);
        return findMember;
    }

    // 닉네임 중복 확인
    public void verifyExistNickName(String nickName) {
        Optional<Member> optionalMember =
                memberRepository.findByNickName(nickName);

        if(optionalMember.isPresent()){
            throw new BusinessLogicException(ExceptionCode.MEMBER_EXISTS);
        }
    }
    public void verifyExistEmail(String email){
        Optional<Member> optionalMember =
                memberRepository.findByEmail(email);

        if(optionalMember.isPresent()){
            throw new BusinessLogicException(ExceptionCode.MEMBER_EXISTS);
        }
    }

    public Member findVerifiedMember(long memberId) {
        Optional<Member> optionalMember =
                memberRepository.findById(memberId);

        Member findMember =
                optionalMember.orElseThrow(
                        ()-> new BusinessLogicException(ExceptionCode.MEMBER_NOT_FOUND)
                );

        return findMember;
    }
}
