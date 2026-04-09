package com.Accommodation.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
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
    @Pattern(
            regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[^A-Za-z\\d]).{8,16}$",
            message = "비밀번호는 영문, 숫자, 특수문자를 각각 1개 이상 포함해야 합니다."
    )
    private String password;

    private String confirmPassword;

    /**
     * 🏠 휴대폰 번호
     */
    @NotBlank(message = "휴대폰 번호는 필수 입력 값입니다.")
    @Pattern(
            regexp = "^(01[016789])-?(\\d{3,4})-?(\\d{4})$",
            message = "휴대폰 번호를 정확히 입력해주세요."
    )
    private String number;

    /**
     * 우편번호
     */
    @NotBlank(message = "우편번호는 필수 입력 값입니다.")
    @Pattern(regexp = "^\\d{5}$", message = "우편번호 5자리를 확인해주세요.")
    private String postcode;

    /**
     * 🏠 기본 주소
     */
    @NotBlank(message = "주소 검색을 통해 기본 주소를 입력해주세요.")
    private String address;

    /**
     * 상세 주소
     */
    private String detailAddress;

    public String getFullAddress() {
        StringBuilder fullAddress = new StringBuilder();

        if (postcode != null && !postcode.isBlank()) {
            fullAddress.append('(').append(postcode.trim()).append(") ");
        }

        if (address != null && !address.isBlank()) {
            fullAddress.append(address.trim());
        }

        if (detailAddress != null && !detailAddress.isBlank()) {
            fullAddress.append(' ').append(detailAddress.trim());
        }

        return fullAddress.toString().trim();
    }
}
