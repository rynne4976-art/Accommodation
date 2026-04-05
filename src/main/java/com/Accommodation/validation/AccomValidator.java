package com.Accommodation.validation;

import com.Accommodation.dto.AccomFormDto;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import java.time.LocalDate;

@Component
public class AccomValidator implements Validator {

    @Override
    public boolean supports(Class<?> clazz) {
        return AccomFormDto.class.isAssignableFrom(clazz);
    }

    @Override
    public void validate(Object target, Errors errors) {
        AccomFormDto accomFormDto = (AccomFormDto) target;

        validateGuestCount(accomFormDto, errors);
        validateOperationInfo(accomFormDto, errors);
    }

    public void validateOrThrow(AccomFormDto accomFormDto) {
        ValidationError error = findValidationError(accomFormDto);

        if (error != null) {
            throw new IllegalArgumentException(error.message());
        }
    }

    private void validateGuestCount(AccomFormDto accomFormDto, Errors errors) {
        if (errors.hasFieldErrors("accomType") || errors.hasFieldErrors("guestCount")) {
            return;
        }

        ValidationError error = findGuestCountError(accomFormDto);
        if (error != null) {
            errors.rejectValue(error.field(), error.code(), error.message());
        }
    }

    private void validateOperationInfo(AccomFormDto accomFormDto, Errors errors) {
        if (!errors.hasFieldErrors("operationStartDate")
                && !errors.hasFieldErrors("operationEndDate")
                && accomFormDto.getOperationStartDate() != null
                && accomFormDto.getOperationEndDate() != null
                && accomFormDto.getOperationStartDate().isAfter(accomFormDto.getOperationEndDate())) {
            errors.rejectValue(
                    "operationEndDate",
                    "operationEndDate.range",
                    "운영 종료일은 운영 시작일보다 빠를 수 없습니다."
            );
        }

        if (!errors.hasFieldErrors("checkInTime")
                && !errors.hasFieldErrors("checkOutTime")
                && accomFormDto.getCheckInTime() != null
                && accomFormDto.getCheckOutTime() != null
                && accomFormDto.getCheckInTime().equals(accomFormDto.getCheckOutTime())) {
            errors.rejectValue(
                    "checkOutTime",
                    "checkOutTime.duplicate",
                    "체크인 시간과 체크아웃 시간은 같을 수 없습니다."
            );
        }

        if (errors.hasFieldErrors("operationDateList")) {
            return;
        }

        ValidationError error = findOperationDateError(accomFormDto);
        if (error != null) {
            errors.rejectValue(error.field(), error.code(), error.message());
        }
    }

    private ValidationError findValidationError(AccomFormDto accomFormDto) {
        ValidationError guestCountError = findGuestCountError(accomFormDto);
        if (guestCountError != null) {
            return guestCountError;
        }

        if (accomFormDto.getOperationStartDate() != null
                && accomFormDto.getOperationEndDate() != null
                && accomFormDto.getOperationStartDate().isAfter(accomFormDto.getOperationEndDate())) {
            return new ValidationError(
                    "operationEndDate",
                    "operationEndDate.range",
                    "운영 종료일은 운영 시작일보다 빠를 수 없습니다."
            );
        }

        if (accomFormDto.getCheckInTime() != null
                && accomFormDto.getCheckOutTime() != null
                && accomFormDto.getCheckInTime().equals(accomFormDto.getCheckOutTime())) {
            return new ValidationError(
                    "checkOutTime",
                    "checkOutTime.duplicate",
                    "체크인 시간과 체크아웃 시간은 같을 수 없습니다."
            );
        }

        return findOperationDateError(accomFormDto);
    }

    private ValidationError findGuestCountError(AccomFormDto accomFormDto) {
        if (accomFormDto.getAccomType() == null || accomFormDto.getGuestCount() == null) {
            return null;
        }

        return switch (accomFormDto.getAccomType()) {
            case HOTEL, RESORT, PENSION ->
                    accomFormDto.getGuestCount() < 2 || accomFormDto.getGuestCount() > 10
                            ? new ValidationError(
                            "guestCount",
                            "guestCount.range",
                            "호텔, 리조트, 펜션의 투숙 가능 인원은 2명에서 10명 사이여야 합니다."
                    )
                            : null;
            case GUESTHOUSE, MOTEL ->
                    accomFormDto.getGuestCount() < 1 || accomFormDto.getGuestCount() > 6
                            ? new ValidationError(
                            "guestCount",
                            "guestCount.range",
                            "게스트하우스와 모텔의 투숙 가능 인원은 1명에서 6명 사이여야 합니다."
                    )
                            : null;
        };
    }

    private ValidationError findOperationDateError(AccomFormDto accomFormDto) {
        if (accomFormDto.getOperationDateList() == null || accomFormDto.getOperationDateList().isEmpty()) {
            return new ValidationError(
                    "operationDateList",
                    "operationDateList.empty",
                    "운영일을 한 개 이상 선택해 주세요."
            );
        }

        if (accomFormDto.getOperationStartDate() == null || accomFormDto.getOperationEndDate() == null) {
            return null;
        }

        for (String operationDateStr : accomFormDto.getOperationDateList()) {
            try {
                LocalDate operationDate = LocalDate.parse(operationDateStr);

                if (operationDate.isBefore(accomFormDto.getOperationStartDate())
                        || operationDate.isAfter(accomFormDto.getOperationEndDate())) {
                    return new ValidationError(
                            "operationDateList",
                            "operationDateList.range",
                            "운영일은 운영 기간 안에서만 선택해 주세요."
                    );
                }
            } catch (Exception e) {
                return new ValidationError(
                        "operationDateList",
                        "operationDateList.format",
                        "운영일 형식이 올바르지 않습니다."
                );
            }
        }

        return null;
    }

    private record ValidationError(String field, String code, String message) {
    }
}
