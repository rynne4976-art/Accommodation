package com.Accommodation.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

/**
 * CartItem - 장바구니(예약 대기) 엔티티
 *
 * <ul>
 *   <li>장바구니 추가 시 생성되며, 객실 수(roomCount)에는 영향을 주지 않는다.</li>
 *   <li>예약 확정(confirmCartItem) 시 Order 로 전환되고 이 레코드는 삭제된다.</li>
 * </ul>
 */
@Entity
@Table(name = "cart_item")
@Getter
@Setter
public class CartItem extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "cart_item_id")
    private Long id;

    // 장바구니 소유 회원
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    // 예약 대기 숙소
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "accom_id", nullable = false)
    private Accom accom;

    @Column(nullable = false)
    private LocalDate checkInDate;

    @Column(nullable = false)
    private LocalDate checkOutDate;

    /** 성인 수 (최소 1명) */
    @Column(nullable = false)
    private int adultCount;

    /** 아동 수 (0명 이상) */
    @Column(nullable = false)
    private int childCount = 0;

    /** 객실 수 (최소 1실) */
    @Column(nullable = false)
    private Integer roomCount = 1;

    public boolean hasInvalidRoomCount() {
        return roomCount == null || roomCount < 1;
    }

    public int getRoomCount() {
        return roomCount == null || roomCount < 1 ? 1 : roomCount;
    }
}
