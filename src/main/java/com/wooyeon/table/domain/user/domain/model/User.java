package com.wooyeon.table.domain.user.domain.model;

import com.wooyeon.table.global.common.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "users")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 해커톤에서는 테스트 계정 로그인 (ACC-01) */
    @Column(nullable = false, unique = true, length = 50)
    private String loginId;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(length = 30)
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserRole role;

    private User(String loginId, String name, UserRole role) {
        this.loginId = loginId;
        this.name = name;
        this.role = role;
    }

    public static User participant(String loginId, String name) {
        return new User(loginId, name, UserRole.PARTICIPANT);
    }

    public static User admin(String loginId, String name) {
        return new User(loginId, name, UserRole.ADMIN);
    }
}
