let map;
let currentMarker;

window.onload = function () {

    const location = document.getElementById("transportAccomLocation").value;

    if (!location) return;

    map = new naver.maps.Map('map', {
        center: new naver.maps.LatLng(37.5665, 126.9780),
        zoom: 15
    });

    naver.maps.Service.geocode(
        { query: location },
        function (status, response) {

            if (status !== naver.maps.Service.Status.OK) {
                console.log("주소 변환 실패");
                return;
            }

            const result = response.v2.addresses[0];

            const lat = result.y;
            const lng = result.x;

            const position = new naver.maps.LatLng(lat, lng);

            map.setCenter(position);

            new naver.maps.Marker({
                position: position,
                map: map
            });

        }
    );
};

/* 현위치 */

function setCurrentLocation(){

    if(!navigator.geolocation){
        alert("위치 기능을 지원하지 않습니다.");
        return;
    }

    navigator.geolocation.getCurrentPosition(function(position){

        const lat = position.coords.latitude;
        const lng = position.coords.longitude;

        const location = new naver.maps.LatLng(lat,lng);

        map.setCenter(location);

        if(currentMarker){
            currentMarker.setMap(null);
        }

        currentMarker = new naver.maps.Marker({
            position: location,
            map: map
        });

    });

}


/* 길찾기 */

function findRoute(){

    if(!navigator.geolocation){
        alert("위치 기능을 지원하지 않습니다.");
        return;
    }

    const accomAddress = document.getElementById("transportAccomLocation").value;

    if(!accomAddress){
        alert("숙소 위치 정보가 없습니다.");
        return;
    }

    navigator.geolocation.getCurrentPosition(function(position){

        const lat = position.coords.latitude;
        const lng = position.coords.longitude;

        const url = "https://map.naver.com/v5/directions/" +
            lat + "," + lng + "," + "현재위치" +
            "/" +
            encodeURIComponent(accomAddress) +
            ",숙소";

        window.open(url,"_blank");

    });

}


/* 주변 교통 */

function searchNearby(){

    const center = map.getCenter();

    const url = "https://map.naver.com/v5/search/지하철역?c="
        + center.y + "," + center.x;

    window.open(url,"_blank");

}