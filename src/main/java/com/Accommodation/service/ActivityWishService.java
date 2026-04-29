package com.Accommodation.service;

import com.Accommodation.dto.ActivityWishDto;
import com.Accommodation.dto.RegionActivityItemDto;
import com.Accommodation.entity.Activity;
import com.Accommodation.entity.ActivityWish;
import com.Accommodation.entity.Member;
import com.Accommodation.repository.ActivityRepository;
import com.Accommodation.repository.ActivityWishRepository;
import com.Accommodation.repository.MemberRepository;
import com.Accommodation.util.ActivityPeriodUtils;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@Transactional
@RequiredArgsConstructor
public class ActivityWishService {

    private final ActivityWishRepository activityWishRepository;
    private final ActivityRepository activityRepository;
    private final MemberRepository memberRepository;

    public void addWish(ActivityWishDto request, String email) {
        if (request == null || request.getActivityKey() == null || request.getActivityKey().isBlank()) {
            throw new IllegalArgumentException("활동 찜 정보가 올바르지 않습니다.");
        }

        if (activityWishRepository.existsByMemberEmailAndActivityKey(email, request.getActivityKey())) {
            return;
        }

        Member member = memberRepository.findByEmail(email);
        if (member == null) {
            throw new EntityNotFoundException("Member not found.");
        }

        Activity activity = activityRepository.findByActivityKey(request.getActivityKey())
                .orElseGet(() -> createActivityFromWishRequest(request));

        ActivityWish activityWish = new ActivityWish();
        activityWish.setMember(member);
        activityWish.setActivity(activity);
        activityWish.setActivityKey(activity.getActivityKey());
        activityWishRepository.save(activityWish);
    }

    private Activity createActivityFromWishRequest(ActivityWishDto request) {
        LocalDateTime now = LocalDateTime.now();
        RegionActivityItemDto item = RegionActivityItemDto.builder()
                .activityKey(request.getActivityKey())
                .title(firstNotBlank(request.getTitle(), "즐길거리"))
                .imageUrl(request.getImageUrl())
                .address(request.getAddress())
                .period(request.getPeriod())
                .detailUrl(request.getDetailUrl())
                .externalUrl(request.getExternalUrl())
                .category(request.getCategory())
                .tel(request.getTel())
                .regionName(request.getRegionName())
                .build();

        Activity activity = new Activity();
        activity.updateFrom(item, 0, now, now.plusDays(7));
        activity.setSource("WISH_REQUEST");
        return activityRepository.save(activity);
    }

    private String firstNotBlank(String... values) {
        if (values == null) {
            return "";
        }

        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value.trim();
            }
        }

        return "";
    }

    public void removeWish(String activityKey, String email) {
        activityWishRepository.findByMemberEmailAndActivityKey(email, activityKey)
                .ifPresent(activityWishRepository::delete);
    }

    @Transactional(readOnly = true)
    public List<RegionActivityItemDto> applyWishState(List<RegionActivityItemDto> items, String email) {
        if (items == null || items.isEmpty() || email == null || email.isBlank()) {
            return items;
        }

        List<String> activityKeys = items.stream()
                .map(RegionActivityItemDto::getActivityKey)
                .filter(key -> key != null && !key.isBlank())
                .toList();

        if (activityKeys.isEmpty()) {
            return items;
        }

        Set<String> wishedKeys = new HashSet<>(
                activityWishRepository.findByMemberEmailAndActivityKeyIn(email, activityKeys).stream()
                        .map(ActivityWish::getActivityKey)
                        .toList()
        );

        items.forEach(item -> item.setWished(wishedKeys.contains(item.getActivityKey())));
        return items;
    }

    @Transactional(readOnly = true)
    public int getWishCount(String email) {
        return getWishList(email).size();
    }

    @Transactional(readOnly = true)
    public List<ActivityWishDto> getWishList(String email) {
        LocalDate today = LocalDate.now();
        return activityWishRepository.findByMemberEmailOrderByRegTimeDesc(email).stream()
                .filter(activityWish -> {
                    Activity activity = activityWish.getActivity();
                    return activity != null && !ActivityPeriodUtils.isExpiredEvent(
                            activity.getCategory(),
                            activity.getPeriod(),
                            today
                    );
                })
                .map(ActivityWishDto::new)
                .toList();
    }
}
