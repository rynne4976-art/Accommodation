package com.Accommodation.entity;

import com.Accommodation.constant.AccomGrade;
import com.Accommodation.constant.AccomStatus;
import com.Accommodation.constant.AccomType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "accom")
@Getter
@Setter
@ToString
public class Accom {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 숙소명
    private String accomName;

    // 1박 가격
    private Integer pricePerNight;

    // 숙소 설명
    private String accomDetail;

    // 숙소 유형
    @Enumerated(EnumType.STRING)
    private AccomType accomType;

    // 숙소 등급
    @Enumerated(EnumType.STRING)
    private AccomGrade grade;

    // 위치
    private String location;

    // 객실 수
    @Column(nullable = false)
    private Integer roomCount = 0;

    // 투숙 가능 인원
    @Column(nullable = false)
    private Integer guestCount = 2;

    // 평균 별점
    @Column(nullable = false)
    private Double avgRating = 0.0;

    // 리뷰 수
    @Column(nullable = false)
    private Integer reviewCount = 0;

    // 숙소 상태
    @Enumerated(EnumType.STRING)
    private AccomStatus status = AccomStatus.OPEN;

    // 소프트 삭제 여부
    @Column(nullable = false)
    private Boolean deleted = false;

    // 삭제 시각
    private LocalDateTime deletedAt;

    private LocalDateTime regTime;

    private LocalDateTime updateTime;

    @OneToMany(
            mappedBy = "accom",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    @ToString.Exclude
    private List<AccomImg> accomImgList = new ArrayList<>();

    public void addAccomImg(AccomImg accomImg) {
        accomImgList.add(accomImg);
        accomImg.setAccom(this);
    }

    public void removeAccomImg(AccomImg accomImg) {
        accomImgList.remove(accomImg);
        accomImg.setAccom(null);
    }

    public void updateAccom(String accomName,
                            Integer pricePerNight,
                            String accomDetail,
                            AccomType accomType,
                            AccomGrade grade,
                            String location,
                            Integer roomCount,
                            Integer guestCount,
                            AccomStatus status) {
        this.accomName = accomName;
        this.pricePerNight = pricePerNight;
        this.accomDetail = accomDetail;
        this.accomType = accomType;
        this.grade = grade;
        this.location = location;
        this.roomCount = roomCount;
        this.guestCount = guestCount;
        this.status = status;
    }

    public void softDelete() {
        this.deleted = true;
        this.deletedAt = LocalDateTime.now();
    }

    public void restore() {
        this.deleted = false;
        this.deletedAt = null;
    }

    @PrePersist
    public void onCreate() {
        this.regTime = LocalDateTime.now();
        this.updateTime = LocalDateTime.now();

        if (this.deleted == null) {
            this.deleted = false;
        }

        if (this.roomCount == null) {
            this.roomCount = 0;
        }

        if (this.guestCount == null) {
            this.guestCount = 2;
        }
    }

    @PreUpdate
    public void onUpdate() {
        this.updateTime = LocalDateTime.now();
    }

    public String getReserveStatCd() {
        // status가 OPEN이고 roomCount가 1 이상이면 예약 가능
        if (this.status == AccomStatus.OPEN && this.roomCount != null && this.roomCount > 0) {
            return "Y";
        }
        return "N";
    }
}
