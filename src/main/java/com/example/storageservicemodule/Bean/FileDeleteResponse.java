package com.example.storageservicemodule.Bean;

import java.util.Date;

public class FileDeleteResponse {
    private String filename;
    private String message;
    private Date deletedAt;

    public FileDeleteResponse(String filename, String message, Date deletedAt) {
        this.filename = filename;
        this.message = message;
        this.deletedAt = deletedAt;
    }

    // Getters y setters
    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Date getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(Date deletedAt) {
        this.deletedAt = deletedAt;
    }
}