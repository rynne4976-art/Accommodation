(function () {
    'use strict';

    const accomTypeSelect = document.getElementById('accomType');
    const guestCountHint = document.getElementById('guestCountHint');

    if (!accomTypeSelect || !guestCountHint) {
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

    function setFocusedInvalid(fieldName, visible) {
        const field = fields[fieldName];
        if (!field) {
            return;
        }

        field.classList.toggle('is-invalid', visible);
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
                return value.length > 0 && value.length <= 100;
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
            case 'location': {
                const value = buildLocationValue();
                return value.length > 0 && value.length <= 255;
            }
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

    function syncLocationField() {
        fields.location.value = buildLocationValue();
        setFocusedInvalid('location', !validateField('location'));
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
        });

        field.addEventListener('change', function () {
            setFocusedInvalid(fieldName, !validateField(fieldName));
        });

        field.addEventListener('blur', function () {
            setFocusedInvalid(fieldName, !validateField(fieldName));
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
    });

    fields.checkInTime.addEventListener('change', function () {
        setFocusedInvalid('checkOutTime', !validateField('checkOutTime'));
    });

    fields.checkOutTime.addEventListener('input', function () {
        setFocusedInvalid('checkOutTime', !validateField('checkOutTime'));
    });

    updateGuestCountRule();
    syncLocationField();

    app.fields = fields;
    app.setFocusedInvalid = setFocusedInvalid;
    app.getGuestCountRange = getGuestCountRange;
    app.buildLocationValue = buildLocationValue;
    app.syncLocationField = syncLocationField;
    app.getCheckedOperationDates = getCheckedOperationDates;
    app.validateField = validateField;
    app.updateGuestCountRule = updateGuestCountRule;
    window.accomFormApp = app;
})();
