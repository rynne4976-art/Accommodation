package com.Accommodation.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.UUID;

@Service
public class FileService {

    public String uploadFile(String uploadPath, String originalFileName, MultipartFile multipartFile) throws Exception{

        UUID uuid = UUID.randomUUID();
        String savedFileName = uuid + "_" + originalFileName;
        String fileUploadFullUrl = uploadPath + "/" + savedFileName;

        multipartFile.transferTo(new File(fileUploadFullUrl));
        return savedFileName;
    }

    public void deleteFile(String filePath) throws Exception {

        File deleteFile = new File(filePath);
        if(deleteFile.exists()){
            deleteFile.delete();
        }
    }
}
