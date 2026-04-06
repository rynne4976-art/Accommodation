package com.Accommodation.constant;

/**
 * 예약(방 점유) 상태 FSM
 *
 * <pre>
 *   PENDING ─── cancel() ───► CANCELLED (종단)
 *      │
 *      │ confirm()
 *      ▼
 *   CONFIRMED ─── cancel() ──► CANCELLED (종단)
 *      │
 *      │ complete()  ← 스케줄러 전용
 *      ▼
 *   COMPLETED (종단)
 * </pre>
 *
 * 규칙:
 * - CANCELLED / COMPLETED 는 종단 상태 → 추가 전이 불가
 * - COMPLETED → CANCELLED 불가 (이용 완료 후 취소 불가)
 */
public enum BookingStatus {

    PENDING {
        @Override public BookingStatus confirm()  { return CONFIRMED; }
        @Override public BookingStatus cancel()   { return CANCELLED; }
    },

    CONFIRMED {
        @Override public BookingStatus cancel()   { return CANCELLED; }
        @Override public BookingStatus complete() { return COMPLETED; }
    },

    /** 종단 상태 – 추가 전이 불가 */
    CANCELLED,

    /** 종단 상태 – 추가 전이 불가 */
    COMPLETED;

    // ── 기본 전이 메서드: 오버라이드 없으면 예외 ──────────────────────────
    public BookingStatus confirm() {
        throw new IllegalStateException(
                "[예약 상태 오류] " + this.name() + " → CONFIRMED 전환 불가");
    }

    public BookingStatus cancel() {
        throw new IllegalStateException(
                "[예약 상태 오류] " + this.name() + " → CANCELLED 전환 불가");
    }

    public BookingStatus complete() {
        throw new IllegalStateException(
                "[예약 상태 오류] " + this.name() + " → COMPLETED 전환 불가");
    }

    /** 취소 가능 여부 (예외 없이 boolean) */
    public boolean canCancel()   { return this == PENDING || this == CONFIRMED; }

    /** 완료 처리 가능 여부 (예외 없이 boolean) */
    public boolean canComplete() { return this == CONFIRMED; }
}
