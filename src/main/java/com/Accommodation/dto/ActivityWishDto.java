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
        if (activityWish.getActivity() != null) {
            this.activityKey = activityWish.getActivity().getActivityKey();
            this.title = activityWish.getActivity().getTitle();
            this.imageUrl = activityWish.getActivity().getImageUrl();
            this.address = activityWish.getActivity().getAddress();
            this.period = activityWish.getActivity().getPeriod();
            this.detailUrl = activityWish.getActivity().getDetailUrl();
            this.externalUrl = activityWish.getActivity().getExternalUrl();
            this.category = activityWish.getActivity().getCategory();
            this.tel = activityWish.getActivity().getTel();
            this.regionName = activityWish.getActivity().getRegionName();
            return;
        }

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
