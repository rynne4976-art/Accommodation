package com.Accommodation.constant;

/**
 * 주문(신청) 상태 FSM
 *
 * <pre>
 *   ORDER ─── cancel() ──► CANCEL (종단)
 * </pre>
 *
 * 규칙:
 * - CANCEL 은 종단 상태 → ORDER 로 되돌릴 수 없음
 */
public enum OrderStatus {

    ORDER {
        @Override public OrderStatus cancel() { return CANCEL; }
    },

    /** 종단 상태 – 추가 전이 불가 */
    CANCEL;

    // ── 기본 전이 메서드 ──────────────────────────────────────────────────
    public OrderStatus cancel() {
        throw new IllegalStateException(
                "[주문 상태 오류] " + this.name() + " → CANCEL 전환 불가 (이미 취소된 주문)");
    }

    /** 취소 가능 여부 */
    public boolean canCancel() { return this == ORDER; }
}
