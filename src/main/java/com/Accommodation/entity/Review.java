package com.Accommodation.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(
        name = "review",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"member_id", "accom_id"})
        }
)
@Getter
@Setter
public class Review extends BaseEntity {

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        @Column(name = "review_id")
        private Long id;

        @ManyToOne(fetch = FetchType.LAZY)
        @JoinColumn(name = "member_id", nullable = false)
        private Member member;

        @ManyToOne(fetch = FetchType.LAZY)
        @JoinColumn(name = "accom_id", nullable = false)
        private Accom accom;

        @Column(nullable = false)
        private Integer rating;

        @Column(nullable = false, length = 1000)
        private String content;

        @Column(name = "review_img_name")
        private String reviewImgName;

        @Column(name = "review_ori_img_name")
        private String reviewOriImgName;

        @Column(name = "review_img_url")
        private String reviewImgUrl;

        public void updateReviewImg(String reviewImgName, String reviewOriImgName, String reviewImgUrl) {
                this.reviewImgName = reviewImgName;
                this.reviewOriImgName = reviewOriImgName;
                this.reviewImgUrl = reviewImgUrl;
        }
}