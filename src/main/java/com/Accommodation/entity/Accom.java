package com.Accommodation.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
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

    @Column(name = "accom_nm")
    private String accomNm;

    @Column(name = "stars")
    private Integer stars;

    @Column(name = "price")
    private Integer price;

    @Column(name = "accom_detail")
    private String accomDetail;

    @Column(name = "reserve_stat_cd")
    private String reserveStatCd;

    @Column(name = "reg_time")
    private LocalDateTime regTime;

    @Column(name = "update_time")
    private LocalDateTime updateTime;

    @Column(name = "check_in")
    private Date checkIn;

    @Column(name = "check_out")
    private Date checkOut;

    @Column(name = "reserve_day")
    private Date reserveDay;

    @Column(name = "star_rating")
    private Integer starRating;

    // 숙소 1개가 여러 장의 이미지를 가질 수 있으므로 1:N 관계로 매핑합니다.
    // mappedBy = "accom" 은 연관관계의 주인이 AccomImg.accom 이라는 뜻입니다.
    // cascade = ALL 은 숙소 저장/삭제 시 이미지도 함께 반영되게 합니다.
    // orphanRemoval = true 는 컬렉션에서 빠진 이미지를 DB에서도 삭제합니다.
    @OneToMany(mappedBy = "accom", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    private List<AccomImg> accomImgList = new ArrayList<>();

    // 숙소에 이미지를 추가할 때 양쪽 객체의 참조를 함께 맞춰주는 편의 메서드입니다.
    public void addAccomImg(AccomImg accomImg) {
        accomImgList.add(accomImg);
        accomImg.setAccom(this);
    }

    // 숙소에서 이미지를 제거할 때 양쪽 연관관계를 함께 끊어주는 메서드입니다.
    public void removeAccomImg(AccomImg accomImg) {
        accomImgList.remove(accomImg);
        accomImg.setAccom(null);
    }
}
