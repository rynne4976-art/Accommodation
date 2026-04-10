package com.Accommodation.service;

import com.Accommodation.dto.ActivityWishDto;
import com.Accommodation.dto.RegionActivityItemDto;
import com.Accommodation.entity.ActivityWish;
import com.Accommodation.entity.Member;
import com.Accommodation.repository.ActivityWishRepository;
import com.Accommodation.repository.MemberRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@Transactional
@RequiredArgsConstructor
public class ActivityWishService {

    private final ActivityWishRepository activityWishRepository;
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

        ActivityWish activityWish = new ActivityWish();
        activityWish.setMember(member);
        activityWish.setActivityKey(request.getActivityKey());
        activityWish.setTitle(request.getTitle());
        activityWish.setImageUrl(request.getImageUrl());
        activityWish.setAddress(request.getAddress());
        activityWish.setPeriod(request.getPeriod());
        activityWish.setDetailUrl(request.getDetailUrl());
        activityWish.setExternalUrl(request.getExternalUrl());
        activityWish.setCategory(request.getCategory());
        activityWish.setTel(request.getTel());
        activityWish.setRegionName(request.getRegionName());
        activityWishRepository.save(activityWish);
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
        return Math.toIntExact(activityWishRepository.countByMemberEmail(email));
    }

    @Transactional(readOnly = true)
    public List<ActivityWishDto> getWishList(String email) {
        return activityWishRepository.findByMemberEmailOrderByRegTimeDesc(email).stream()
                .map(ActivityWishDto::new)
                .toList();
    }
}
