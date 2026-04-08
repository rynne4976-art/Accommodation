package com.Accommodation.service;

import com.Accommodation.dto.WishListDto;
import com.Accommodation.entity.Accom;
import com.Accommodation.entity.Member;
import com.Accommodation.entity.Wish;
import com.Accommodation.repository.AccomRepository;
import com.Accommodation.repository.MemberRepository;
import com.Accommodation.repository.WishRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class WishService {

    private final WishRepository wishRepository;
    private final MemberRepository memberRepository;
    private final AccomRepository accomRepository;

    public void addWish(Long accomId, String email) {
        if (wishRepository.existsByMemberEmailAndAccomId(email, accomId)) {
            return;
        }

        Member member = memberRepository.findByEmail(email);
        if (member == null) {
            throw new EntityNotFoundException("Member not found.");
        }

        Accom accom = accomRepository.findById(accomId)
                .orElseThrow(() -> new EntityNotFoundException("Accommodation not found."));

        Wish wish = new Wish();
        wish.setMember(member);
        wish.setAccom(accom);
        wishRepository.save(wish);
    }

    public void removeWish(Long accomId, String email) {
        wishRepository.findByMemberEmailAndAccomId(email, accomId)
                .ifPresent(wishRepository::delete);
    }

    @Transactional(readOnly = true)
    public List<WishListDto> getWishList(String email, String sort) {
        List<WishListDto> wishItems = wishRepository.findWishListDtosByMemberEmailOrderByRegTimeDesc(email);

        return sortWishItems(wishItems, sort);
    }

    @Transactional(readOnly = true)
    public int getWishCount(String email) {
        return Math.toIntExact(wishRepository.countByMemberEmail(email));
    }

    private List<WishListDto> sortWishItems(List<WishListDto> wishItems, String sort) {
        Comparator<WishListDto> comparator = switch (sort) {
            case "priceDesc" -> Comparator
                    .comparing(WishListDto::getPricePerNight, Comparator.nullsLast(Integer::compareTo))
                    .reversed();
            case "priceAsc" -> Comparator
                    .comparing(WishListDto::getPricePerNight, Comparator.nullsLast(Integer::compareTo));
            case "ratingDesc" -> Comparator
                    .comparing(WishListDto::getAvgRating, Comparator.nullsLast(Double::compareTo))
                    .reversed()
                    .thenComparing(WishListDto::getReviewCount, Comparator.nullsLast(Integer::compareTo).reversed());
            default -> null;
        };

        if (comparator == null) {
            return wishItems;
        }

        return wishItems.stream()
                .sorted(comparator)
                .toList();
    }
}
