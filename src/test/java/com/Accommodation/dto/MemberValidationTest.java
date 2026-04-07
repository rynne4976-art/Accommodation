package com.Accommodation.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;

class MemberValidationTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    @DisplayName("회원가입 DTO는 잘못된 이메일 형식을 검증한다")
    void memberFormDtoValidatesEmailFormat() {
        MemberFormDto dto = createValidMemberFormDto();
        dto.setEmail("invalid-email");

        Set<ConstraintViolation<MemberFormDto>> violations = validator.validate(dto);

        assertTrue(hasViolation(violations, "email", "이메일 형식으로 입력해주세요."));
    }

    @Test
    @DisplayName("회원가입 DTO는 비밀번호 복잡도 규칙을 검증한다")
    void memberFormDtoValidatesPasswordComplexity() {
        MemberFormDto dto = createValidMemberFormDto();
        dto.setPassword("password12");

        Set<ConstraintViolation<MemberFormDto>> violations = validator.validate(dto);

        assertTrue(hasViolation(violations, "password", "비밀번호는 영문, 숫자, 특수문자를 각각 1개 이상 포함해야 합니다."));
    }

    @Test
    @DisplayName("비밀번호 변경 DTO는 새 비밀번호 복잡도 규칙을 검증한다")
    void passwordChangeDtoValidatesNewPasswordComplexity() {
        PasswordChangeDto dto = new PasswordChangeDto();
        dto.setCurrentPassword("Current123!");
        dto.setNewPassword("onlyletters");
        dto.setConfirmPassword("onlyletters");

        Set<ConstraintViolation<PasswordChangeDto>> violations = validator.validate(dto);

        assertTrue(hasViolation(violations, "newPassword", "비밀번호는 영문, 숫자, 특수문자를 각각 1개 이상 포함해야 합니다."));
    }

    private MemberFormDto createValidMemberFormDto() {
        MemberFormDto dto = new MemberFormDto();
        dto.setName("테스트회원");
        dto.setEmail("test@example.com");
        dto.setPassword("Password123!");
        dto.setNumber("01012345678");
        dto.setPostcode("12345");
        dto.setAddress("서울시 강남구");
        dto.setDetailAddress("101호");
        return dto;
    }

    private boolean hasViolation(Set<? extends ConstraintViolation<?>> violations,
                                 String property,
                                 String message) {
        return violations.stream()
                .anyMatch(violation ->
                        property.equals(violation.getPropertyPath().toString())
                                && message.equals(violation.getMessage()));
    }
}
