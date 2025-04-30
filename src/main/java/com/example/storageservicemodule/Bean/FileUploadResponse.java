package com.example.storageservicemodule.Bean;

public class FileUploadResponse {
    private String originalFilename;
    private String savedFilename;
    private String fileUrl;
    private String contentType;
    private long size;

    public FileUploadResponse(String originalFilename, String savedFilename, String fileUrl, String contentType, long size) {
        this.originalFilename = originalFilename;
        this.savedFilename = savedFilename;
        this.fileUrl = fileUrl;
        this.contentType = contentType;
        this.size = size;
    }

    // Getters y setters
    public String getOriginalFilename() {
        return originalFilename;
    }

    public void setOriginalFilename(String originalFilename) {
        this.originalFilename = originalFilename;
    }

    public String getSavedFilename() {
        return savedFilename;
    }

    public void setSavedFilename(String savedFilename) {
        this.savedFilename = savedFilename;
    }

    public String getFileUrl() {
        return fileUrl;
    }

    public void setFileUrl(String fileUrl) {
        this.fileUrl = fileUrl;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }
}