package com.yfmf.footlog.config;

import com.yfmf.footlog.domain.member.domain.Authority;
import com.yfmf.footlog.domain.member.domain.Gender;
import com.yfmf.footlog.domain.member.domain.Member;
import com.yfmf.footlog.domain.member.domain.SocialType;
import com.yfmf.footlog.domain.member.repository.MemberRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

@Slf4j
@Configuration
public class DataLoader {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;

    public DataLoader(MemberRepository memberRepository, PasswordEncoder passwordEncoder) {
        this.memberRepository = memberRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Bean
    CommandLineRunner loadData() {
        return args -> {
            // 이메일 중복 확인
            String email = "test@example.com";
            Optional<Member> existingMember = memberRepository.findByEmail(email);

            if (existingMember.isPresent()) {
                // 이미 존재하는 회원이면 로그 기록 및 생략
                log.info("이미 존재하는 이메일로 인해 회원 생성을 생략합니다: {}", email);
            } else {
                // 새로운 회원 생성
                Member mockMember = Member.builder()
                        .name("김재협")
                        .email(email)
                        .password(passwordEncoder.encode("password123"))
                        .gender(Gender.MALE)
                        .socialType(SocialType.NONE)
                        .authority(Authority.ROLE_ADMIN)
                        .build();

                // 회원 데이터 저장
                memberRepository.save(mockMember);
                log.info("임의의 회원이 DB에 생성되었습니다: {}", mockMember);
            }
        };
    }
}