package com.Accommodation.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Getter
@Setter
public class ReviewFormDto {

    private Long accomId;

    @Min(value = 1, message = "별점은 1점 이상이어야 합니다.")
    @Max(value = 5, message = "별점은 5점 이하여야 합니다.")
    private Integer rating;

    @NotBlank(message = "리뷰 내용을 입력해 주세요.")
    private String content;

    private List<MultipartFile> reviewImgFileList;

    public Long getAccomId() {
        return accomId;
    }

    public void setAccomId(Long accomId) {
        this.accomId = accomId;
    }

    public Integer getRating() {
        return rating;
    }

    public void setRating(Integer rating) {
        this.rating = rating;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public List<MultipartFile> getReviewImgFileList() {
        return reviewImgFileList;
    }

    public void setReviewImgFileList(List<MultipartFile> reviewImgFileList) {
        this.reviewImgFileList = reviewImgFileList;
    }
}