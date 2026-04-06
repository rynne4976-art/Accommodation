package com.Accommodation.entity;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(
        name = "accom_img",
        indexes = {
                @Index(name = "idx_accom_img_accom_rep_id", columnList = "accom_id, repimg_yn, accom_img_id")
        }
)
@Getter @Setter
@ToString(exclude = "accom")
public class AccomImg {

    @Id
    @Column(name = "accom_img_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "img_name")
    private String imgName; //상품 이미지 파일명
    @Column(name = "ori_img_name")
    private String oriImgName; //업로드할 원본 이미지 파일명
    @Column(name = "img_url")
    private String imgUrl;     //이미지 조회 경로
    @Column(name = "repimg_yn")
    private String repImgYn;   //대표 이미지 여부 -> "Y" 인 경우 메인 페이지에서 상품을 보여줄때 사용합니다.

    // 여러 이미지가 하나의 숙소를 참조하므로 N:1 관계입니다.
    // accom_img 테이블의 accom_id 컬럼이 accom 테이블의 PK를 참조합니다.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "accom_id", nullable = false)
    private Accom accom;

    public void updateAccomImg(String imgName, String oriImgName, String imgUrl){
        this.imgName = imgName;
        this.oriImgName = oriImgName;
        this.imgUrl = imgUrl;
    }

    // 이미지가 다른 숙소로 변경될 때 기존 숙소/새 숙소 컬렉션도 같이 갱신해서
    // 양방향 연관관계가 서로 다른 상태로 남지 않도록 맞춰줍니다.
    public void setAccom(Accom accom) {
        if (this.accom != null) {
            this.accom.getAccomImgList().remove(this);
        }

        this.accom = accom;

        if (accom != null && !accom.getAccomImgList().contains(this)) {
            accom.getAccomImgList().add(this);
        }
    }
}
