package com.Accommodation.service;

import com.Accommodation.constant.BookingStatus;
import com.Accommodation.constant.Role;
import com.Accommodation.dto.ReviewFormDto;
import com.Accommodation.entity.Accom;
import com.Accommodation.entity.Member;
import com.Accommodation.entity.Review;
import com.Accommodation.entity.ReviewImg;
import com.Accommodation.repository.AccomRepository;
import com.Accommodation.repository.MemberRepository;
import com.Accommodation.repository.OrderItemRepository;
import com.Accommodation.repository.ReviewImgRepository;
import com.Accommodation.repository.ReviewRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final ReviewImgRepository reviewImgRepository;
    private final MemberRepository memberRepository;
    private final AccomRepository accomRepository;
    private final OrderItemRepository orderItemRepository;
    private final S3FileService s3FileService;

    public void saveReview(ReviewFormDto reviewFormDto, String email) throws Exception {
        Member member = getMemberOrThrow(email);

        Accom accom = accomRepository.findById(reviewFormDto.getAccomId())
                .orElseThrow(() -> new EntityNotFoundException("숙소 정보를 찾을 수 없습니다."));

        boolean exists = reviewRepository.existsByMemberIdAndAccomId(member.getId(), accom.getId());
        if (exists) {
            throw new IllegalStateException("이미 해당 숙소에 리뷰를 작성했습니다.");
        }

        if (!canManageReview(member, accom.getId())) {
            throw new IllegalStateException("체크아웃 이력이 있는 회원만 해당 숙소에 리뷰를 작성할 수 있습니다.");
        }

        Review review = new Review();
        review.setMember(member);
        review.setAccom(accom);
        review.setRating(reviewFormDto.getRating());
        review.setContent(reviewFormDto.getContent());

        reviewRepository.save(review);
        saveReviewImages(review, reviewFormDto.getReviewImgFileList());
        updateAccomReviewInfo(accom);
    }

    public void updateReview(Long reviewId, ReviewFormDto reviewFormDto, String email) throws Exception {
        Member member = getMemberOrThrow(email);

        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new EntityNotFoundException("리뷰 정보를 찾을 수 없습니다."));

        if (!isAdmin(member) && !review.getMember().getId().equals(member.getId())) {
            throw new IllegalStateException("본인 리뷰만 수정할 수 있습니다.");
        }

        if (!canManageReview(member, review.getAccom().getId())) {
            throw new IllegalStateException("체크아웃 이력이 있는 숙소의 리뷰만 수정할 수 있습니다.");
        }

        review.setRating(reviewFormDto.getRating());
        review.setContent(reviewFormDto.getContent());

        saveReviewImages(review, reviewFormDto.getReviewImgFileList());
        updateAccomReviewInfo(review.getAccom());
    }

    public void deleteReview(Long reviewId, String email) throws Exception {
        Member member = getMemberOrThrow(email);

        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new EntityNotFoundException("리뷰 정보를 찾을 수 없습니다."));

        if (!isAdmin(member) && !review.getMember().getId().equals(member.getId())) {
            throw new IllegalStateException("본인 리뷰만 삭제할 수 있습니다.");
        }

        Accom accom = review.getAccom();

        deleteAllReviewImages(review);
        reviewRepository.delete(review);
        updateAccomReviewInfo(accom);
    }

    public void deleteReviewImage(Long reviewId, Long reviewImgId, String email) throws Exception {
        Member member = getMemberOrThrow(email);

        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new EntityNotFoundException("리뷰 정보를 찾을 수 없습니다."));

        if (!isAdmin(member) && !review.getMember().getId().equals(member.getId())) {
            throw new IllegalStateException("본인 리뷰 이미지만 삭제할 수 있습니다.");
        }

        ReviewImg reviewImg = reviewImgRepository.findByIdAndReviewId(reviewImgId, reviewId)
                .orElseThrow(() -> new EntityNotFoundException("리뷰 이미지를 찾을 수 없습니다."));

        deleteReviewImageFile(reviewImg.getImgName());
        reviewImgRepository.delete(reviewImg);
        review.getReviewImgList().removeIf(img -> img.getId().equals(reviewImgId));
    }

    @Transactional(readOnly = true)
    public List<Review> getReviewList(Long accomId) {
        List<Review> reviewList = reviewRepository.findByAccomIdOrderByRegTimeDesc(accomId);
        reviewList.forEach(this::applyAccessibleImageUrls);
        return reviewList;
    }

    @Transactional(readOnly = true)
    public Page<Review> getReviewPage(Long accomId, Pageable pageable) {
        Page<Review> reviewPage = reviewRepository.findByAccomIdOrderByRegTimeDesc(accomId, pageable);
        reviewPage.getContent().forEach(this::applyAccessibleImageUrls);
        return reviewPage;
    }

    @Transactional(readOnly = true)
    public boolean hasMyReview(Long accomId, String email) {
        Member member = memberRepository.findByEmail(email);
        if (member == null) {
            return false;
        }

        return reviewRepository.existsByMemberIdAndAccomId(member.getId(), accomId);
    }

    @Transactional(readOnly = true)
    public Review getMyReview(Long accomId, String email) {
        Member member = memberRepository.findByEmail(email);
        if (member == null) {
            return null;
        }

        Review review = reviewRepository.findByMemberIdAndAccomId(member.getId(), accomId).orElse(null);
        applyAccessibleImageUrls(review);
        return review;
    }

    @Transactional(readOnly = true)
    public boolean canWriteReview(Long accomId, String email) {
        Member member = memberRepository.findByEmail(email);
        if (member == null) {
            return false;
        }

        return canManageReview(member, accomId);
    }

    @Transactional(readOnly = true)
    public String getReviewWriteDenyMessage(Long accomId, String email) {
        if (email == null || email.isBlank()) {
            return "로그인 후 리뷰를 작성할 수 있습니다.";
        }

        Member member = memberRepository.findByEmail(email);
        if (member == null) {
            return "로그인한 회원 정보를 찾을 수 없습니다.";
        }

        if (canManageReview(member, accomId)) {
            return "";
        }

        return "체크아웃 이력이 있는 회원만 해당 숙소에 리뷰를 작성할 수 있습니다.";
    }

    private void saveReviewImages(Review review, List<MultipartFile> reviewImgFileList) throws Exception {
        if (reviewImgFileList == null || reviewImgFileList.isEmpty()) {
            return;
        }

        for (MultipartFile reviewImgFile : reviewImgFileList) {
            if (reviewImgFile == null || reviewImgFile.isEmpty()) {
                continue;
            }

            String oriImgName = reviewImgFile.getOriginalFilename();
            String imgName = s3FileService.uploadFile("review", oriImgName, reviewImgFile);
            String imgUrl = s3FileService.getFileUrl(imgName);

            ReviewImg reviewImg = new ReviewImg();
            reviewImg.updateReviewImg(imgName, oriImgName, imgUrl);
            reviewImg.setReview(review);

            reviewImgRepository.save(reviewImg);
            review.addReviewImg(reviewImg);
        }
    }

    private void deleteAllReviewImages(Review review) throws Exception {
        List<ReviewImg> reviewImgList = reviewImgRepository.findByReviewId(review.getId());

        for (ReviewImg reviewImg : reviewImgList) {
            deleteReviewImageFile(reviewImg.getImgName());
        }

        reviewImgRepository.deleteAll(reviewImgList);
        review.getReviewImgList().clear();
    }

    private void deleteReviewImageFile(String imgName) throws Exception {
        if (imgName == null || imgName.isEmpty()) {
            return;
        }
        s3FileService.deleteFile(imgName);
    }

    private void updateAccomReviewInfo(Accom accom) {
        List<Review> reviewList = reviewRepository.findByAccomIdOrderByRegTimeDesc(accom.getId());

        int reviewCount = reviewList.size();
        double avgRating = 0.0;

        if (reviewCount > 0) {
            int totalRating = reviewList.stream()
                    .mapToInt(Review::getRating)
                    .sum();
            avgRating = (double) totalRating / reviewCount;
        }

        accom.setReviewCount(reviewCount);
        accom.setAvgRating(avgRating);
        accomRepository.save(accom);
    }

    private void applyAccessibleImageUrls(Review review) {
        if (review == null || review.getReviewImgList() == null) {
            return;
        }

        review.getReviewImgList().forEach(reviewImg ->
                reviewImg.setImgUrl(s3FileService.getProxyImageUrl(reviewImg.getImgName()))
        );
    }

    private Member getMemberOrThrow(String email) {
        Member member = memberRepository.findByEmail(email);
        if (member == null) {
            throw new IllegalArgumentException("로그인한 회원 정보를 찾을 수 없습니다.");
        }
        return member;
    }

    private boolean canManageReview(Member member, Long accomId) {
        return isAdmin(member) || hasCompletedStay(member.getId(), accomId);
    }

    private boolean isAdmin(Member member) {
        return member.getRole() == Role.ADMIN;
    }

    private boolean hasCompletedStay(Long memberId, Long accomId) {
        return orderItemRepository.existsByOrderMemberIdAndAccomIdAndCheckOutDateLessThanEqualAndBookingStatusNot(
                memberId,
                accomId,
                LocalDate.now(),
                BookingStatus.CANCELLED
        );
    }
}
