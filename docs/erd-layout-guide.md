# README ERD Layout Guide

README용 ERD는 전체 데이터베이스 구조를 한 장에 모두 담지 않고, 흐름별 핵심 관계만 분리한 이미지 3장으로 구성한다.

공통 원칙

- 테이블명 중심으로 보여주고, 컬럼은 가능하면 숨긴다.
- 박스 크기와 폰트 크기는 통일한다.
- 선은 최대한 직선으로 유지하고, 교차선을 줄인다.
- 관계 라벨은 전체 그림에서 같은 톤으로 맞춘다.
- 색상은 1~2개만 사용하고, 중심 테이블만 약하게 강조한다.
- README용 이미지는 설명용이므로 실제 전체 ERD보다 단순해도 된다.

## 1. 예약 관련 ERD

설명 문구:

회원이 주문을 생성하고, 주문 항목과 숙박 일자를 기준으로 예약 정보가 관리됩니다.

중심 테이블:

- `MEMBER`
- `ORDERS`
- `ORDER_ITEM`
- `ORDER_STAY_DATE`

보조 테이블:

- `ACCOM`

권장 배치:

```text
MEMBER
  |
ORDERS
  |
ORDER_ITEM ---- ACCOM
  |
ORDER_STAY_DATE
```

권장 관계 라벨:

- `MEMBER -> ORDERS`: `creates`
- `ORDERS -> ORDER_ITEM`: `contains`
- `ORDER_ITEM -> ACCOM`: `reserves`
- `ORDER_ITEM -> ORDER_STAY_DATE`: `expands`

배치 팁:

- `MEMBER → ORDERS → ORDER_ITEM → ORDER_STAY_DATE`를 세로 중심축으로 둔다.
- `ACCOM`은 `ORDER_ITEM` 오른쪽에만 배치한다.
- `ORDER_STAY_DATE`는 `ORDER_ITEM` 바로 아래에 붙인다.
- `ACCOM`과 `ORDER_STAY_DATE`를 직접 연결하지 않는다.

## 2. 회원 활동 관련 ERD

설명 문구:

회원의 찜, 장바구니, 리뷰, 알림 기능을 중심으로 구성했습니다.

중심 테이블:

- `MEMBER`

주요 연결 테이블:

- `WISH`
- `CART_ITEM`
- `REVIEW`
- `NOTIFICATION`

보조 테이블:

- `ACCOM`
- `REVIEW_IMG`

권장 배치:

```text
                ACCOM

        REVIEW   WISH   CART_ITEM
             \    |    /
                MEMBER
                  |
            NOTIFICATION

REVIEW
  |
REVIEW_IMG
```

대안 배치:

```text
              ACCOM
         /      |      \
     REVIEW    WISH   CART_ITEM
         \       |       /
               MEMBER
                  |
            NOTIFICATION

REVIEW
  |
REVIEW_IMG
```

권장 관계 라벨:

- `MEMBER -> WISH`: `saves`
- `MEMBER -> CART_ITEM`: `owns`
- `MEMBER -> REVIEW`: `writes`
- `MEMBER -> NOTIFICATION`: `receives`
- `ACCOM -> WISH`: `targets`
- `ACCOM -> CART_ITEM`: `added_to`
- `ACCOM -> REVIEW`: `reviewed_in`
- `REVIEW -> REVIEW_IMG`: `has`

배치 팁:

- `MEMBER`를 그림의 정중앙에 둔다.
- `WISH`, `CART_ITEM`, `REVIEW`는 `MEMBER` 가까이에 배치한다.
- `NOTIFICATION`은 `MEMBER` 바로 아래에 둔다.
- `ACCOM`은 상단 보조 축으로만 두고, `REVIEW`, `WISH`, `CART_ITEM`에 짧게 연결한다.
- `REVIEW_IMG`는 `REVIEW` 바로 아래에 붙인다.
- `ACCOM`과 `MEMBER`를 직접 연결하지 않는다.

## 3. 숙소 상세 정보 관련 ERD

설명 문구:

숙소 이미지, 운영 정책, 운영일 정보를 분리해 관리합니다.

중심 테이블:

- `ACCOM`

연결 테이블:

- `ACCOM_IMG`
- `ACCOM_OPERATION_POLICY`
- `ACCOM_OPERATION_DAY`

권장 배치:

```text
             ACCOM
        /      |      \
ACCOM_IMG ACCOM_OPERATION_POLICY ACCOM_OPERATION_DAY
```

권장 관계 라벨:

- `ACCOM -> ACCOM_IMG`: `has`
- `ACCOM -> ACCOM_OPERATION_POLICY`: `has`
- `ACCOM -> ACCOM_OPERATION_DAY`: `operates_on`

배치 팁:

- `ACCOM`을 상단 중앙에 둔다.
- 하단에 세 테이블을 가로로 균등하게 배치한다.
- 세 연결선 길이를 최대한 비슷하게 맞춘다.

## README 반영 형식

README에는 아래 형식으로 넣는다.

```markdown
## 데이터베이스 구조

### 1. 예약 관련 ERD
[이미지]
회원이 주문을 생성하고, 주문 항목과 숙박 일자를 기준으로 예약 정보가 관리됩니다.

### 2. 회원 활동 관련 ERD
[이미지]
회원의 찜, 장바구니, 리뷰, 알림 기능을 중심으로 구성했습니다.

### 3. 숙소 상세 정보 관련 ERD
[이미지]
숙소 이미지, 운영 정책, 운영일 정보를 분리해 관리합니다.
```
