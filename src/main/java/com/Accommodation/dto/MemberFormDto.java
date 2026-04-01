package com.Accommodation.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * 🧾 MemberFormDto (회원가입 입력 데이터)
 *
 * ▶ 회원가입 화면에서 입력한 값을 담는 객체입니다.
 * ▶ Controller → Service로 데이터를 전달할 때 사용됩니다.
 *
 * Entity와 분리하는 이유:
 * - 입력값 검증을 쉽게 하기 위해
 * - 화면 전용 데이터 관리
 */
@Getter
@Setter
public class MemberFormDto {

    /**
     * 👤 이름
     */
    @NotBlank(message = "이름은 필수 입력 값입니다.")
    private String name;

    /**
     * 📧 이메일
     */
    @NotBlank(message = "이메일은 필수 입력 값입니다.")
    @Email(message = "이메일 형식으로 입력해주세요.")
    private String email;

    /**
     * 🔒 비밀번호
     */
    @NotBlank(message = "비밀번호는 필수 입력 값입니다.")
    @Size(min = 8, max = 16, message = "비밀번호는 8자 이상 16자 이하로 입력해주세요.")
    private String password;

    /**
     * 🏠 휴대폰 번호
     */
    @NotBlank(message = "휴대폰 번호는 필수 입력 값입니다.")
    private String number;

    /**
     * 🏠 주소
     */
    @NotBlank(message = "주소는 필수 입력 값입니다.")
    private String address;

}