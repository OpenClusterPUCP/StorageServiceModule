package com.example.storageservicemodule.Bean;

import java.util.Date;

public class FileInfo {
    private String name;
    private String url;
    private long size;
    private String contentType;
    private Date lastModified;

    public FileInfo(String name, String url, long size, String contentType, Date lastModified) {
        this.name = name;
        this.url = url;
        this.size = size;
        this.contentType = contentType;
        this.lastModified = lastModified;
    }

    // Getters y setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public Date getLastModified() {
        return lastModified;
    }

    public void setLastModified(Date lastModified) {
        this.lastModified = lastModified;
    }
}