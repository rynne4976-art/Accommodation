(function () {
    'use strict';

    const accomTypeSelect = document.getElementById('accomType');
    const guestCountHint = document.getElementById('guestCountHint');
    const accomNameSuccessMessage = document.getElementById('accomNameSuccessMessage');
    const guestCountSuccessMessage = document.getElementById('guestCountSuccessMessage');
    const form = document.querySelector('.accom-form');

    if (!accomTypeSelect || !guestCountHint || !form) {
        return;
    }

    const fields = {
        accomName: document.getElementById('accomName'),
        pricePerNight: document.getElementById('pricePerNight'),
        roomCount: document.getElementById('roomCount'),
        guestCount: document.getElementById('guestCount'),
        accomType: accomTypeSelect,
        grade: document.getElementById('grade'),
        location: document.getElementById('location'),
        postcode: document.getElementById('postcode'),
        address: document.getElementById('address'),
        detailAddress: document.getElementById('detailAddress'),
        accomDetail: document.getElementById('accomDetail'),
        status: document.getElementById('status'),
        operationStartDate: document.getElementById('operationStartDate'),
        operationEndDate: document.getElementById('operationEndDate'),
        checkInTime: document.getElementById('checkInTime'),
        checkOutTime: document.getElementById('checkOutTime')
    };

    const app = window.accomFormApp || {};

    function getVisibleField(fieldName) {
        if (fieldName === 'operationStartDate' || fieldName === 'operationEndDate') {
            return document.getElementById('operationPeriodTrigger') || fields[fieldName];
        }
        return document.getElementById(fieldName + 'Trigger') || fields[fieldName];
    }

    function setFocusedInvalid(fieldName, visible) {
        const field = fields[fieldName];
        if (!field) {
            return;
        }

        field.classList.toggle('is-invalid', visible);
        const visibleField = getVisibleField(fieldName);
        if (visibleField && visibleField !== field) {
            visibleField.classList.toggle('is-invalid', visible);
        }
    }

    function getGuestCountRange() {
        const accomType = accomTypeSelect.value;

        if (accomType === 'GUESTHOUSE' || accomType === 'MOTEL') {
            return { min: 1, max: 6, message: '게스트하우스와 모텔은 1명부터 6명까지 입력할 수 있습니다.' };
        }

        if (accomType === 'HOTEL' || accomType === 'RESORT' || accomType === 'PENSION') {
            return { min: 2, max: 10, message: '호텔, 리조트, 펜션은 2명부터 10명까지 입력할 수 있습니다.' };
        }

        return { min: 1, max: 10, message: '' };
    }

    function buildLocationValue() {
        const postcode = fields.postcode.value.trim();
        const address = fields.address.value.trim();
        const detailAddress = fields.detailAddress.value.trim();

        let location = '';
        if (postcode) {
            location += '(' + postcode + ') ';
        }
        if (address) {
            location += address;
        }
        if (detailAddress) {
            location += (location ? ' ' : '') + detailAddress;
        }

        return location.trim();
    }

    function getCheckedOperationDates() {
        return Array.from(document.querySelectorAll('input[name="operationDateList"]:checked'))
            .map(function (input) {
                return input.value;
            });
    }

    function validateField(fieldName) {
        switch (fieldName) {
            case 'accomName': {
                const value = fields.accomName.value.trim();
                return value.length >= 5 && value.length <= 100;
            }
            case 'pricePerNight': {
                const value = fields.pricePerNight.value.trim();
                return value !== '' && Number(value) > 0;
            }
            case 'roomCount': {
                const value = fields.roomCount.value.trim();
                return value !== '' && Number(value) >= 0;
            }
            case 'guestCount': {
                const value = fields.guestCount.value.trim();
                if (value === '' || !fields.accomType.value) {
                    return false;
                }

                const guestCount = Number(value);
                const range = getGuestCountRange();
                return guestCount >= range.min && guestCount <= range.max;
            }
            case 'accomType':
                return fields.accomType.value !== '';
            case 'grade':
                return fields.grade.value !== '';
            case 'location':
                return buildLocationValue().length <= 255;
            case 'accomDetail': {
                const value = fields.accomDetail.value.trim();
                return value.length >= 10 && value.length <= 1000;
            }
            case 'status':
                return fields.status.value !== '';
            case 'operationStartDate':
                return fields.operationStartDate.value !== '';
            case 'operationEndDate': {
                const endValue = fields.operationEndDate.value;
                if (!endValue) {
                    return false;
                }
                return !fields.operationStartDate.value || endValue >= fields.operationStartDate.value;
            }
            case 'checkInTime':
                return fields.checkInTime.value !== '';
            case 'checkOutTime':
                return fields.checkOutTime.value !== ''
                    && (!fields.checkInTime.value || fields.checkOutTime.value !== fields.checkInTime.value);
            default:
                return true;
        }
    }

    function validateOperationDateList() {
        const operationDateListBox = document.getElementById('operationDateListBox');
        const isValid = getCheckedOperationDates().length > 0;

        if (operationDateListBox) {
            operationDateListBox.classList.toggle('is-invalid', !isValid);
        }

        return isValid;
    }

    function validateForm() {
        syncLocationField();

        const fieldNames = [
            'accomName', 'pricePerNight', 'roomCount', 'guestCount', 'accomType', 'grade',
            'accomDetail', 'status', 'operationStartDate', 'operationEndDate',
            'checkInTime', 'checkOutTime'
        ];

        let firstInvalidField = null;

        fieldNames.forEach(function (fieldName) {
            const isValid = validateField(fieldName);
            setFocusedInvalid(fieldName, !isValid);

            if (!isValid && !firstInvalidField && fields[fieldName]) {
                firstInvalidField = getVisibleField(fieldName);
            }
        });

        const operationDateListValid = validateOperationDateList();
        if (!operationDateListValid && !firstInvalidField) {
            firstInvalidField = document.getElementById('operationDateListBox');
        }

        if (firstInvalidField && typeof firstInvalidField.focus === 'function') {
            firstInvalidField.focus();
        }

        return !firstInvalidField && operationDateListValid;
    }

    function syncLocationField() {
        fields.location.value = buildLocationValue();
    }

    function updateAccomNameSuccess() {
        if (!accomNameSuccessMessage) {
            return;
        }

        const isValid = validateField('accomName');
        const hasValue = fields.accomName.value.trim().length > 0;
        accomNameSuccessMessage.style.display = isValid && hasValue ? 'block' : 'none';
    }

    function updateGuestCountFeedback() {
        const hasValue = fields.guestCount.value.trim().length > 0;
        const isValid = validateField('guestCount');

        guestCountHint.classList.toggle('is-invalid', hasValue && !isValid);
        guestCountHint.style.display = isValid && hasValue ? 'none' : 'block';

        if (!guestCountSuccessMessage) {
            return;
        }

        guestCountSuccessMessage.style.display = isValid && hasValue ? 'block' : 'none';
    }

    function bindFocusValidation(fieldName) {
        const field = fields[fieldName];
        if (!field) {
            return;
        }

        field.addEventListener('focus', function () {
            setFocusedInvalid(fieldName, !validateField(fieldName));
        });

        field.addEventListener('input', function () {
            setFocusedInvalid(fieldName, !validateField(fieldName));
            if (fieldName === 'accomName') {
                updateAccomNameSuccess();
            }
            if (fieldName === 'guestCount') {
                updateGuestCountFeedback();
            }
        });

        field.addEventListener('change', function () {
            setFocusedInvalid(fieldName, !validateField(fieldName));
            if (fieldName === 'accomName') {
                updateAccomNameSuccess();
            }
            if (fieldName === 'guestCount') {
                updateGuestCountFeedback();
            }
        });

        field.addEventListener('blur', function () {
            setFocusedInvalid(fieldName, !validateField(fieldName));
            if (fieldName === 'accomName') {
                updateAccomNameSuccess();
            }
            if (fieldName === 'guestCount') {
                updateGuestCountFeedback();
            }
        });
    }

    function updateGuestCountRule() {
        const range = getGuestCountRange();
        fields.guestCount.min = String(range.min);
        fields.guestCount.max = String(range.max);
        guestCountHint.textContent = range.message || '숙소 유형을 먼저 선택해 주세요.';
    }

    [
        'accomName', 'pricePerNight', 'roomCount', 'guestCount', 'accomType', 'grade',
        'accomDetail', 'status', 'operationStartDate', 'operationEndDate', 'checkInTime', 'checkOutTime'
    ].forEach(bindFocusValidation);

    fields.postcode.addEventListener('focus', function () {
        setFocusedInvalid('location', !validateField('location'));
    });
    fields.postcode.addEventListener('blur', syncLocationField);

    fields.address.addEventListener('focus', function () {
        setFocusedInvalid('location', !validateField('location'));
    });
    fields.address.addEventListener('blur', syncLocationField);

    fields.detailAddress.addEventListener('focus', function () {
        setFocusedInvalid('location', !validateField('location'));
    });
    fields.detailAddress.addEventListener('input', syncLocationField);
    fields.detailAddress.addEventListener('blur', syncLocationField);

    accomTypeSelect.addEventListener('change', function () {
        updateGuestCountRule();
        setFocusedInvalid('guestCount', !validateField('guestCount'));
        updateGuestCountFeedback();
    });

    fields.checkInTime.addEventListener('change', function () {
        setFocusedInvalid('checkOutTime', !validateField('checkOutTime'));
    });

    fields.checkOutTime.addEventListener('input', function () {
        setFocusedInvalid('checkOutTime', !validateField('checkOutTime'));
    });

    updateGuestCountRule();
    syncLocationField();
    updateAccomNameSuccess();
    updateGuestCountFeedback();

    form.addEventListener('submit', function (event) {
        if (!validateForm()) {
            event.preventDefault();
        }
    });

    app.fields = fields;
    app.setFocusedInvalid = setFocusedInvalid;
    app.getGuestCountRange = getGuestCountRange;
    app.buildLocationValue = buildLocationValue;
    app.syncLocationField = syncLocationField;
    app.getCheckedOperationDates = getCheckedOperationDates;
    app.validateField = validateField;
    app.validateOperationDateList = validateOperationDateList;
    app.validateForm = validateForm;
    app.updateGuestCountRule = updateGuestCountRule;
    window.accomFormApp = app;
})();
