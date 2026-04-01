const urlParams = new URLSearchParams(window.location.search);

if (urlParams.has("logout")) {
    alert("로그아웃되었습니다.");
    urlParams.delete("logout");

    const nextQuery = urlParams.toString();
    const nextUrl = nextQuery
        ? `${window.location.pathname}?${nextQuery}`
        : window.location.pathname;

    window.history.replaceState({}, "", nextUrl);
}
