package com.Accommodation.dto;

import com.Accommodation.constant.AccomGrade;
import com.Accommodation.constant.AccomStatus;
import com.Accommodation.constant.AccomType;
import com.Accommodation.entity.Accom;
import com.Accommodation.entity.AccomOperationDay;
import com.Accommodation.entity.AccomOperationPolicy;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Getter
@Setter
public class AccomFormDto {

    private static final Pattern POSTCODE_PATTERN = Pattern.compile("^\\((\\d{5})\\)\\s*(.*)$");

    private Long id;

    @NotBlank(message = "숙소명을 입력해 주세요.")
    @Size(min = 5, max = 100, message = "숙소명은 5자 이상 100자 이하로 입력해 주세요.")
    private String accomName;

    @NotNull(message = "1박 가격을 입력해 주세요.")
    @Positive(message = "1박 가격은 0보다 커야 합니다.")
    private Integer pricePerNight;

    @NotBlank(message = "숙소 설명을 입력해 주세요.")
    @Size(min = 10, max = 1000, message = "숙소 설명은 10자 이상 1000자 이하로 입력해 주세요.")
    private String accomDetail;

    @NotNull(message = "숙소 유형을 선택해 주세요.")
    private AccomType accomType;

    @NotNull(message = "숙소 등급을 선택해 주세요.")
    private AccomGrade grade;

    @Size(max = 255, message = "주소는 255자 이하로 입력해 주세요.")
    private String location;

    private String postcode;

    private String address;

    private String detailAddress;

    @NotNull(message = "객실 수를 입력해 주세요.")
    @Min(value = 0, message = "객실 수는 0 이상이어야 합니다.")
    private Integer roomCount;

    @NotNull(message = "투숙 가능 인원을 입력해 주세요.")
    @Min(value = 1, message = "투숙 가능 인원은 최소 1명입니다.")
    @Max(value = 10, message = "투숙 가능 인원은 최대 10명입니다.")
    private Integer guestCount;

    @NotNull(message = "운영 시작일을 입력해 주세요.")
    private LocalDate operationStartDate;

    @NotNull(message = "운영 종료일을 입력해 주세요.")
    private LocalDate operationEndDate;

    @NotNull(message = "체크인 시간을 입력해 주세요.")
    private LocalTime checkInTime;

    @NotNull(message = "체크아웃 시간을 입력해 주세요.")
    private LocalTime checkOutTime;

    @Size(min = 1, message = "운영일을 한 개 이상 선택해 주세요.")
    private List<String> operationDateList = new ArrayList<>();

    @NotNull(message = "숙소 상태를 선택해 주세요.")
    private AccomStatus status;

    public String buildFullLocation() {
        StringBuilder fullLocation = new StringBuilder();

        if (postcode != null && !postcode.isBlank()) {
            fullLocation.append('(').append(postcode.trim()).append(") ");
        }

        if (address != null && !address.isBlank()) {
            fullLocation.append(address.trim());
        }

        if (detailAddress != null && !detailAddress.isBlank()) {
            fullLocation.append(' ').append(detailAddress.trim());
        }

        return fullLocation.toString().trim();
    }

    public static AccomFormDto of(Accom accom) {
        AccomFormDto dto = new AccomFormDto();
        dto.setId(accom.getId());
        dto.setAccomName(accom.getAccomName());
        dto.setPricePerNight(accom.getPricePerNight());
        dto.setAccomDetail(accom.getAccomDetail());
        dto.setAccomType(accom.getAccomType());
        dto.setGrade(accom.getGrade());
        dto.setLocation(accom.getLocation());
        dto.setRoomCount(accom.getRoomCount());
        dto.setGuestCount(accom.getGuestCount());
        dto.setStatus(accom.getStatus());
        dto.applyLocationParts(accom.getLocation());

        AccomOperationPolicy operationPolicy = accom.getOperationPolicy();
        if (operationPolicy != null) {
            dto.setOperationStartDate(operationPolicy.getOperationStartDate());
            dto.setOperationEndDate(operationPolicy.getOperationEndDate());
            dto.setCheckInTime(operationPolicy.getCheckInTime());
            dto.setCheckOutTime(operationPolicy.getCheckOutTime());
        }

        if (accom.getOperationDayList() != null && !accom.getOperationDayList().isEmpty()) {
            dto.setOperationDateList(
                    accom.getOperationDayList()
                            .stream()
                            .map(AccomOperationDay::getOperationDate)
                            .map(LocalDate::toString)
                            .collect(Collectors.toList())
            );
        }

        return dto;
    }

    private void applyLocationParts(String fullLocation) {
        if (fullLocation == null || fullLocation.isBlank()) {
            return;
        }

        Matcher matcher = POSTCODE_PATTERN.matcher(fullLocation.trim());
        if (matcher.matches()) {
            this.postcode = matcher.group(1);
            String remainder = matcher.group(2).trim();
            int splitIndex = remainder.lastIndexOf(' ');

            if (splitIndex > 0) {
                this.address = remainder.substring(0, splitIndex).trim();
                this.detailAddress = remainder.substring(splitIndex + 1).trim();
            } else {
                this.address = remainder;
                this.detailAddress = "";
            }
            return;
        }

        this.address = fullLocation;
        this.detailAddress = "";
    }
}
