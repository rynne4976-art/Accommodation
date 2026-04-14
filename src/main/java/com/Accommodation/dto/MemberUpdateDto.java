package com.Accommodation.dto;

import com.Accommodation.constant.SocialMemberDefaults;
import com.Accommodation.entity.Member;
import com.Accommodation.util.AddressUtils;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MemberUpdateDto {

    @NotBlank(message = "이름은 필수 입력 값입니다.")
    private String name;

    private String currentPassword;

    @NotBlank(message = "휴대폰 번호는 필수 입력 값입니다.")
    @Pattern(
            regexp = "^(01[016789])-?(\\d{3,4})-?(\\d{4})$",
            message = "휴대폰 번호를 정확히 입력해주세요."
    )
    private String number;

    @NotBlank(message = "우편번호는 필수 입력 값입니다.")
    @Pattern(regexp = "^\\d{5}$", message = "우편번호 5자리를 확인해주세요.")
    private String postcode;

    @NotBlank(message = "주소 검색을 통해 기본 주소를 입력해주세요.")
    private String address;

    @NotBlank(message = "상세 주소를 입력해주세요.")
    private String detailAddress;

    public static MemberUpdateDto from(Member member) {
        AddressUtils.ParsedAddress parsedAddress = AddressUtils.parseStoredAddress(member.getAddress());

        MemberUpdateDto dto = new MemberUpdateDto();
        dto.setName(member.getName());
        dto.setNumber(SocialMemberDefaults.isDefaultNumber(member.getNumber(), member.isSocialMember()) ? "" : member.getNumber());
        dto.setPostcode(SocialMemberDefaults.DEFAULT_ADDRESS.equals(member.getAddress()) ? "" : parsedAddress.getPostcode());
        dto.setAddress(SocialMemberDefaults.DEFAULT_ADDRESS.equals(member.getAddress()) ? "" : parsedAddress.getAddress());
        dto.setDetailAddress(parsedAddress.getDetailAddress());
        return dto;
    }

    public String getFullAddress() {
        return AddressUtils.formatStoredAddress(postcode, address, detailAddress);
    }
}
