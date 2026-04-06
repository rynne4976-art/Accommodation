package com.Accommodation.service;

import com.Accommodation.dto.WishListDto;
import com.Accommodation.entity.Accom;
import com.Accommodation.entity.AccomImg;
import com.Accommodation.entity.Member;
import com.Accommodation.entity.Wish;
import com.Accommodation.repository.AccomRepository;
import com.Accommodation.repository.MemberRepository;
import com.Accommodation.repository.WishRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    public boolean isWished(Long accomId, String email) {
        return wishRepository.existsByMemberEmailAndAccomId(email, accomId);
    }

    @Transactional(readOnly = true)
    public List<Long> getWishedAccomIds(String email) {
        return wishRepository.findByMemberEmailOrderByRegTimeDesc(email).stream()
                .map(wish -> wish.getAccom().getId())
                .toList();
    }

    @Transactional(readOnly = true)
    public List<WishListDto> getWishList(String email) {
        return wishRepository.findByMemberEmailOrderByRegTimeDesc(email).stream()
                .map(wish -> {
                    Accom accom = wish.getAccom();
                    return new WishListDto(
                            accom.getId(),
                            accom.getAccomName(),
                            accom.getAccomDetail(),
                            accom.getLocation(),
                            accom.getPricePerNight(),
                            getRepImgUrl(accom),
                            accom.getAvgRating(),
                            accom.getReviewCount()
                    );
                })
                .toList();
    }

    private String getRepImgUrl(Accom accom) {
        return accom.getAccomImgList().stream()
                .filter(img -> "Y".equals(img.getRepImgYn()))
                .map(AccomImg::getImgUrl)
                .findFirst()
                .orElse("");
    }
}
