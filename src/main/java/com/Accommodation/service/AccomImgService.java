package com.Accommodation.service;

import com.Accommodation.entity.AccomImg;
import com.Accommodation.repository.AccomImgRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
@Transactional
public class AccomImgService {

    @Value("${accomImgLocation}")
    private String accomImgLocation;
    private final AccomImgRepository accomImgRepository;
    private final FileService fileService;

    public void saveAccomImg(AccomImg accomImg, MultipartFile accomImgFile) throws Exception {

        String oriImgName = accomImgFile.getOriginalFilename();
        String imgName = "";
        String imgUrl = "";

        if (oriImgName != null && !oriImgName.isEmpty()){
            imgName = fileService.uploadFile(accomImgLocation, oriImgName, accomImgFile);
            imgUrl = "/images/accom/" + imgName;
        }

        accomImg.updateAccomImg(imgName, oriImgName, imgUrl);
        accomImgRepository.save(accomImg);


    }
}
