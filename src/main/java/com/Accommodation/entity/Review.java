package com.Accommodation.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

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

        @OneToMany(mappedBy = "review", cascade = CascadeType.ALL, orphanRemoval = true)
        private List<ReviewImg> reviewImgList = new ArrayList<>();

        public void addReviewImg(ReviewImg reviewImg) {
                reviewImgList.add(reviewImg);
                reviewImg.setReview(this);
        }
}