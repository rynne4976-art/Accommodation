const urlParams = new URLSearchParams(window.location.search);

if (urlParams.has("logout")) {
    // 로그아웃 완료 알림은 메인 화면 진입 시 한 번만 보여주고 URL은 즉시 정리합니다.
    alert("로그아웃되었습니다.");
    urlParams.delete("logout");

    const nextQuery = urlParams.toString();
    const nextUrl = nextQuery
        ? `${window.location.pathname}?${nextQuery}`
        : window.location.pathname;

    window.history.replaceState({}, "", nextUrl);
}
