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

    @NotBlank(message = "휴대폰 번호는 필수 입력 값입니다.")
    @Pattern(
            regexp = "^(01[016789])-?(\\d{3,4})-?(\\d{4})$",
            message = "휴대폰 번호는 010-0000-0000 또는 01000000000 형식으로 입력해주세요."
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
        dto.setNumber(SocialMemberDefaults.DEFAULT_NUMBER.equals(member.getNumber()) ? "" : member.getNumber());
        dto.setPostcode(SocialMemberDefaults.DEFAULT_ADDRESS.equals(member.getAddress()) ? "" : parsedAddress.getPostcode());
        dto.setAddress(SocialMemberDefaults.DEFAULT_ADDRESS.equals(member.getAddress()) ? "" : parsedAddress.getAddress());
        dto.setDetailAddress(parsedAddress.getDetailAddress());
        return dto;
    }

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
