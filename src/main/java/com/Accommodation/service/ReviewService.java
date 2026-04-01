package com.Accommodation.service;

import com.Accommodation.dto.ReviewFormDto;
import com.Accommodation.entity.Accom;
import com.Accommodation.entity.Member;
import com.Accommodation.entity.Review;
import com.Accommodation.repository.AccomRepository;
import com.Accommodation.repository.MemberRepository;
import com.Accommodation.repository.ReviewRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ReviewService {

    @Value("${uploadPath}")
    private String uploadPath;

    private final ReviewRepository reviewRepository;
    private final MemberRepository memberRepository;
    private final AccomRepository accomRepository;
    private final FileService fileService;

    @Transactional(readOnly = true)
    public List<Review> getReviewList(Long accomId) {
        return reviewRepository.findByAccomIdOrderByRegTimeDesc(accomId);
    }

    @Transactional(readOnly = true)
    public boolean hasMyReview(Long accomId, String email) {
        Member member = memberRepository.findByEmail(email);

        if (member == null) {
            return false;
        }

        return reviewRepository.existsByMemberIdAndAccomId(member.getId(), accomId);
    }

    public void saveReview(ReviewFormDto reviewFormDto,
                           String email)
            throws Exception {

        Member member =
                memberRepository.findByEmail(email);

        if (member == null) {
            throw new IllegalArgumentException(
                    "로그인한 회원 정보를 찾을 수 없습니다."
            );
        }

        Accom accom =
                accomRepository.findById(
                                reviewFormDto.getAccomId()
                        )
                        .orElseThrow(
                                () -> new EntityNotFoundException(
                                        "숙소 정보를 찾을 수 없습니다."
                                )
                        );

        boolean exists =
                reviewRepository
                        .existsByMemberIdAndAccomId(
                                member.getId(),
                                accom.getId()
                        );

        if (exists) {
            throw new IllegalStateException(
                    "이미 해당 숙소에 리뷰를 작성하셨습니다."
            );
        }

        Review review = new Review();

        review.setMember(member);
        review.setAccom(accom);
        review.setRating(
                reviewFormDto.getRating()
        );
        review.setContent(
                reviewFormDto.getContent()
        );

        MultipartFile reviewImgFile =
                reviewFormDto.getReviewImgFile();

        if (reviewImgFile != null
                && !reviewImgFile.isEmpty()) {

            String reviewUploadPath =
                    System.getProperty("user.dir")
                            + "/"
                            + uploadPath
                            + "/review";

            String oriImgName =
                    reviewImgFile.getOriginalFilename();

            String imgName =
                    fileService.uploadFile(
                            reviewUploadPath,
                            oriImgName,
                            reviewImgFile
                    );

            String imgUrl =
                    "/images/review/"
                            + imgName;

            review.updateReviewImg(
                    imgName,
                    oriImgName,
                    imgUrl
            );
        }

        reviewRepository.save(review);

        updateAccomReviewInfo(accom);
    }

    private void updateAccomReviewInfo(
            Accom accom) {

        List<Review> reviewList =
                reviewRepository
                        .findByAccomIdOrderByRegTimeDesc(
                                accom.getId()
                        );

        int reviewCount =
                reviewList.size();

        double avgRating = 0.0;

        if (reviewCount > 0) {

            int totalRating =
                    reviewList.stream()
                            .mapToInt(Review::getRating)
                            .sum();

            avgRating =
                    (double) totalRating
                            / reviewCount;
        }

        accom.setReviewCount(
                reviewCount
        );

        accom.setAvgRating(
                avgRating
        );

        accomRepository.save(accom);
    }
}
