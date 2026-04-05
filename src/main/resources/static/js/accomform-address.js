(function () {
    'use strict';

    const app = window.accomFormApp;
    const addressSearchButton = document.getElementById('addressSearchButton');

    if (!app || !addressSearchButton || !window.kakao || !window.kakao.Postcode) {
        return;
    }

    addressSearchButton.addEventListener('click', function () {
        new kakao.Postcode({
            oncomplete: function (data) {
                let address = '';
                let extraAddress = '';

                if (data.userSelectedType === 'R') {
                    address = data.roadAddress;

                    if (data.bname && /[동로가]$/g.test(data.bname)) {
                        extraAddress += data.bname;
                    }

                    if (data.buildingName && data.apartment === 'Y') {
                        extraAddress += extraAddress ? ', ' + data.buildingName : data.buildingName;
                    }

                    if (extraAddress) {
                        address += ' (' + extraAddress + ')';
                    }
                } else {
                    address = data.jibunAddress;
                }

                app.fields.postcode.value = data.zonecode;
                app.fields.address.value = address;
                app.syncLocationField();
                app.fields.detailAddress.focus();
            }
        }).open();
    });
})();
