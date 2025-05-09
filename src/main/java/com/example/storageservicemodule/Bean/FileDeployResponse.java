package com.example.storageservicemodule.Bean;

import java.util.Date;

public class FileDeployResponse {
    private String imageId;
    private String serverName;
    private String deployPath;
    private String message;
    private Date timestamp;
    
    // Constructor
    public FileDeployResponse(String imageId, String serverName, String deployPath, String message, Date timestamp) {
        this.imageId = imageId;
        this.serverName = serverName;
        this.deployPath = deployPath;
        this.message = message;
        this.timestamp = timestamp;
    }
    
    // Getters y setters
    public String getImageId() {
        return imageId;
    }
    
    public void setImageId(String imageId) {
        this.imageId = imageId;
    }
    
    public String getServerName() {
        return serverName;
    }
    
    public void setServerName(String serverName) {
        this.serverName = serverName;
    }
    
    public String getDeployPath() {
        return deployPath;
    }
    
    public void setDeployPath(String deployPath) {
        this.deployPath = deployPath;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public Date getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }
}