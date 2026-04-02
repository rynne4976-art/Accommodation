package com.Accommodation.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.UUID;

@Service
public class FileService {

    public String uploadFile(String uploadPath, String originalFileName, MultipartFile multipartFile) throws Exception{

        File uploadDir = new File(uploadPath);

        if (!uploadDir.exists()){
            uploadDir.mkdirs();
        }

        UUID uuid = UUID.randomUUID();
        String extension = extractExtension(originalFileName);
        String savedFileName = uuid + extension;
        String fileUploadFullUrl = uploadPath + File.separator + savedFileName;

        multipartFile.transferTo(new File(fileUploadFullUrl));
        return savedFileName;
    }

    public void deleteFile(String filePath) throws Exception {

        File deleteFile = new File(filePath);
        if(deleteFile.exists()){
            deleteFile.delete();
        }
    }

    private String extractExtension(String originalFileName) {
        if (originalFileName == null || originalFileName.isBlank()) {
            return "";
        }

        int lastDotIndex = originalFileName.lastIndexOf('.');
        if (lastDotIndex < 0 || lastDotIndex == originalFileName.length() - 1) {
            return "";
        }

        return originalFileName.substring(lastDotIndex);
    }
}
