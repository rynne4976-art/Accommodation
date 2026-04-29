package com.Accommodation.dto;

import com.Accommodation.entity.Activity;
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
        this(activityWish.getActivity());
    }

    public ActivityWishDto(Activity activity) {
        if (activity == null) {
            return;
        }

        this.activityKey = activity.getActivityKey();
        this.title = activity.getTitle();
        this.imageUrl = activity.getImageUrl();
        this.address = activity.getAddress();
        this.period = activity.getPeriod();
        this.detailUrl = activity.getDetailUrl();
        this.externalUrl = activity.getExternalUrl();
        this.category = activity.getCategory();
        this.tel = activity.getTel();
        this.regionName = activity.getRegionName();
    }
}
