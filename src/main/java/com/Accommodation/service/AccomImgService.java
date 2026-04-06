package com.Accommodation.service;

import com.Accommodation.entity.AccomImg;
import com.Accommodation.repository.AccomImgRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
@Transactional
public class AccomImgService {

    private final AccomImgRepository accomImgRepository;
    private final S3FileService s3FileService;

    // =========================
    // 이미지 저장
    // =========================
    public void saveAccomImg(
            AccomImg accomImg,
            MultipartFile accomImgFile
    ) throws Exception {

        String oriImgName =
                accomImgFile.getOriginalFilename();

        String imgName = "";
        String imgUrl = "";

        if (oriImgName != null
                && !oriImgName.isEmpty()) {

            imgName =
                    s3FileService.uploadFile(
                            "accom",
                            oriImgName,
                            accomImgFile
                    );

            imgUrl =
                    s3FileService.getFileUrl(imgName);
        }

        accomImg.updateAccomImg(
                imgName,
                oriImgName,
                imgUrl
        );

        accomImgRepository.save(accomImg);
    }

    public void deleteAccomImg(AccomImg accomImg) {

        if (accomImg == null) {
            return;
        }

        String imgName =
                accomImg.getImgName();

        if (imgName != null
                && !imgName.isBlank()) {

            s3FileService.deleteFile(imgName);
        }
    }
}
