package com.Accommodation.entity;

import com.Accommodation.dto.RegionActivityItemDto;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "activity",
        uniqueConstraints = @UniqueConstraint(columnNames = "activity_key"),
        indexes = {
                @Index(name = "idx_activity_region_expires", columnList = "region_name, expires_at"),
                @Index(name = "idx_activity_region_sort", columnList = "region_name, sort_order")
        }
)
@Getter
@Setter
public class Activity extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "activity_id")
    private Long id;

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

    @Column(name = "region_name", length = 80)
    private String regionName;

    @Column(name = "source", length = 40)
    private String source;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(name = "last_fetched_at", nullable = false)
    private LocalDateTime lastFetchedAt;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    public void updateFrom(RegionActivityItemDto item,
                           int sortOrder,
                           LocalDateTime fetchedAt,
                           LocalDateTime expiresAt) {
        this.activityKey = item.getActivityKey();
        this.title = item.getTitle();
        this.imageUrl = item.getImageUrl();
        this.address = item.getAddress();
        this.period = item.getPeriod();
        this.detailUrl = item.getDetailUrl();
        this.externalUrl = item.getExternalUrl();
        this.category = item.getCategory();
        this.tel = item.getTel();
        this.regionName = item.getRegionName();
        this.source = "TOUR_API";
        this.sortOrder = sortOrder;
        this.lastFetchedAt = fetchedAt;
        this.expiresAt = expiresAt;
    }

    public RegionActivityItemDto toRegionActivityItemDto() {
        return RegionActivityItemDto.builder()
                .activityKey(activityKey)
                .title(title)
                .imageUrl(imageUrl)
                .address(address)
                .period(period)
                .detailUrl(detailUrl)
                .externalUrl(externalUrl)
                .category(category)
                .tel(tel)
                .regionName(regionName)
                .build();
    }
}
