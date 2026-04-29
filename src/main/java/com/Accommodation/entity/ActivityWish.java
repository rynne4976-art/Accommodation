package com.Accommodation.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(
        name = "activity_wish",
        uniqueConstraints = @UniqueConstraint(columnNames = {"member_id", "activity_key"}),
        indexes = @Index(name = "idx_activity_wish_activity", columnList = "activity_id")
)
@Getter
@Setter
public class ActivityWish extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "activity_wish_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "activity_id")
    private Activity activity;

    @Column(name = "activity_key", nullable = false, length = 64)
    private String activityKey;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(length = 1000)
    private String imageUrl;

    @Column(length = 500)
    private String address;

    @Column(length = 120)
    private String period;

    @Column(length = 1000)
    private String detailUrl;

    @Column(length = 1000)
    private String externalUrl;

    @Column(length = 80)
    private String category;

    @Column(length = 80)
    private String tel;

    @Column(length = 80)
    private String regionName;
}
