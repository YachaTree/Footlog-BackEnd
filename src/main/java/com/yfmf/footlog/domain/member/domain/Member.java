package com.yfmf.footlog.domain.member.domain;

import com.yfmf.footlog.BaseTimeEntity;
import com.yfmf.footlog.domain.member.enums.Area;
import com.yfmf.footlog.domain.member.enums.MainFoot;
import com.yfmf.footlog.domain.member.enums.Position;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;

import java.time.LocalDate;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "tbl_member")
@Builder
public class Member extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 20, nullable = false)
    private String name;

    @Column(length = 100, nullable = false, unique = true)
    private String email;

    @Column(length = 100, nullable = false)
    private String password;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Gender gender;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    @ColumnDefault("'NONE'")
    private SocialType socialType;

    @Enumerated(value = EnumType.STRING)
    @ColumnDefault("'ROLE_USER'")
    private Authority authority;

    private LocalDate birth;

    @Enumerated(EnumType.STRING)
    private MainFoot mainFoot;

    @Enumerated(EnumType.STRING)
    private Area area;

    @Enumerated(EnumType.STRING)
    private Position position;

    private String introduction;

    private Boolean isPro;

    private Double height;

    private Double weight;

    private String profileImageUrl;

    private String phoneNumber;


    @Embedded
    private Stat stat;

    @Embedded
    private Record record;

    public Member(Long id, String name, String email, String password, Gender gender, SocialType socialType,
                  Authority authority, LocalDate birth, MainFoot mainFoot, Area area, Position position,
                  String introduction, Boolean isPro, Double height, Double weight, String profileImageUrl,
                  String phoneNumber, Stat stat, Record record) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.password = password;
        this.gender = gender;
        this.socialType = socialType;
        this.authority = authority;
        this.birth = birth;
        this.mainFoot = mainFoot;
        this.area = area;
        this.position = position;
        this.introduction = introduction;
        this.isPro = isPro;
        this.height = height;
        this.weight = weight;
        this.profileImageUrl = profileImageUrl;
        this.phoneNumber = phoneNumber;
        this.stat = stat;
        this.record = record;
    }

    @Builder
    public Member(LocalDate birth, MainFoot mainFoot, Area area, Position position,
                  String introduction, Boolean isPro, Double height, Double weight, String profileImageUrl,
                  String phoneNumber, Stat stat, Record record) {
        this.birth = birth;
        this.mainFoot = mainFoot;
        this.area = area;
        this.position = position;
        this.introduction = introduction;
        this.isPro = isPro;
        this.height = height;
        this.weight = weight;
        this.profileImageUrl = profileImageUrl;
        this.phoneNumber = phoneNumber;
        this.stat = stat;
        this.record = record;
    }
}
