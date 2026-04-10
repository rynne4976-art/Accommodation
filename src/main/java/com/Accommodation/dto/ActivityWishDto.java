package com.Accommodation.dto;

import com.Accommodation.entity.ActivityWish;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ActivityWishDto {

    private String activityKey;
    private String title;
    private String imageUrl;
    private String address;
    private String period;
    private String detailUrl;
    private String externalUrl;
    private String category;
    private String tel;
    private String regionName;

    public ActivityWishDto(ActivityWish activityWish) {
        this.activityKey = activityWish.getActivityKey();
        this.title = activityWish.getTitle();
        this.imageUrl = activityWish.getImageUrl();
        this.address = activityWish.getAddress();
        this.period = activityWish.getPeriod();
        this.detailUrl = activityWish.getDetailUrl();
        this.externalUrl = activityWish.getExternalUrl();
        this.category = activityWish.getCategory();
        this.tel = activityWish.getTel();
        this.regionName = activityWish.getRegionName();
    }
}
