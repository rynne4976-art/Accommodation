let map;
let accomLat;
let accomLng;

let accomMarker;
let startMarker;
let routeLine;

document.addEventListener("DOMContentLoaded", init);

async function init(){

  try{

    const address=document.getElementById("accomLocation").value;

    if(!address) return;

    const url=`https://nominatim.openstreetmap.org/search?format=json&q=${encodeURIComponent(address)}`;

    const res=await fetch(url);
    const data=await res.json();

    if(!data || data.length===0) return;

    accomLat=parseFloat(data[0].lat);
    accomLng=parseFloat(data[0].lon);

    initMap();

    loadNearbyTransport();

  }catch(e){

    console.log("지도 초기화 오류",e);

  }

}


function initMap(){

  map=L.map("map").setView([accomLat,accomLng],15);

  L.tileLayer("https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png",{
    maxZoom:19
  }).addTo(map);

  accomMarker=L.marker([accomLat,accomLng])
      .addTo(map)
      .bindPopup("🏨 숙소 위치")
      .openPopup();

}



async function loadNearbyTransport(){

  try{

    const radius = 800;

    const query = `
[out:json];
(
node["railway"="station"](around:${radius},${accomLat},${accomLng});
node["highway"="bus_stop"](around:${radius},${accomLat},${accomLng});
);
out;
`;

    const res = await fetch("https://overpass-api.de/api/interpreter",{
      method:"POST",
      body:query
    });

    const text = await res.text();

    // XML 에러 방지
    if(text.trim().startsWith("<")){
      console.warn("Overpass API XML 오류 반환");
      return;
    }

    const data = JSON.parse(text);

    let listHTML = "";

    data.elements.forEach(p=>{

      const name = p.tags?.name || "이름 없음";

      const dist = getDistance(accomLat,accomLng,p.lat,p.lon);
      const walk = Math.round(dist/80);

      if(p.tags.highway === "bus_stop"){

        L.marker([p.lat,p.lon],{
          icon:L.divIcon({
            html:"🚌",
            className:"bus-marker",
            iconSize:[24,24]
          })
        }).addTo(map)
            .bindTooltip(`${name}<br>도보 ${walk}분`);

        listHTML += `<li>🚌 ${name} · 도보 ${walk}분</li>`;
      }

      if(p.tags.railway === "station"){

        L.marker([p.lat,p.lon],{
          icon:L.divIcon({
            html:"🚇",
            className:"subway-marker",
            iconSize:[24,24]
          })
        }).addTo(map)
            .bindTooltip(`${name}<br>도보 ${walk}분`);

        listHTML += `<li>🚇 ${name} · 도보 ${walk}분</li>`;
      }

    });

    document.getElementById("transportList").innerHTML = listHTML;

  }catch(e){

    console.error("교통 검색 실패",e);

  }

}


async function searchRoute(){

  const start=document.getElementById("startLocation").value;

  if(!start){
    alert("출발지를 입력해주세요");
    return;
  }

  const url=`https://nominatim.openstreetmap.org/search?format=json&q=${encodeURIComponent(start)}`;

  const res=await fetch(url);
  const data=await res.json();

  if(data.length===0){
    alert("위치를 찾을 수 없습니다");
    return;
  }

  const lat=parseFloat(data[0].lat);
  const lng=parseFloat(data[0].lon);

  setStart(lat,lng);

  drawRoute(lat,lng);

}



function useMyLocation(){

  navigator.geolocation.getCurrentPosition(

      pos=>{

        const lat=pos.coords.latitude;
        const lng=pos.coords.longitude;

        setStart(lat,lng);

        drawRoute(lat,lng);

      },

      ()=>{
        alert("위치 권한을 허용해주세요");
      }

  );

}



function setStart(lat,lng){

  if(startMarker){
    map.removeLayer(startMarker);
  }

  startMarker=L.marker([lat,lng])
      .addTo(map)
      .bindPopup("출발 위치")
      .openPopup();

}



async function drawRoute(startLat,startLng){

  const url=
      `https://router.project-osrm.org/route/v1/driving/`
      +`${startLng},${startLat};${accomLng},${accomLat}`
      +`?overview=full&geometries=geojson`;

  const res=await fetch(url);
  const data=await res.json();

  const route=data.routes[0];

  if(routeLine){
    map.removeLayer(routeLine);
  }

  routeLine=L.geoJSON(route.geometry).addTo(map);

  map.fitBounds(routeLine.getBounds());

  const km=(route.distance/1000).toFixed(2);
  const min=Math.round(route.duration/60);

  document.getElementById("distanceInfo").innerText=
      `거리 ${km}km · 약 ${min}분`;

}



function getDistance(lat1,lon1,lat2,lon2){

  const R=6371e3;

  const φ1=lat1*Math.PI/180;
  const φ2=lat2*Math.PI/180;

  const Δφ=(lat2-lat1)*Math.PI/180;
  const Δλ=(lon2-lon1)*Math.PI/180;

  const a=
      Math.sin(Δφ/2)*Math.sin(Δφ/2)+
      Math.cos(φ1)*Math.cos(φ2)*
      Math.sin(Δλ/2)*Math.sin(Δλ/2);

  const c=2*Math.atan2(Math.sqrt(a),Math.sqrt(1-a));

  return R*c;

}