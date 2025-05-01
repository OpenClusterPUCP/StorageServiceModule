package com.example.storageservicemodule.Bean;

public class FileTransferRequest {
    private String sourcePath;
    private String destinationPath;
    private String transferType; // "local", "sftp", "ftp"
    private String host;
    private int port;
    private String username;
    private String password;
    
    // Getters y setters
    public String getSourcePath() {
        return sourcePath;
    }
    
    public void setSourcePath(String sourcePath) {
        this.sourcePath = sourcePath;
    }
    
    public String getDestinationPath() {
        return destinationPath;
    }
    
    public void setDestinationPath(String destinationPath) {
        this.destinationPath = destinationPath;
    }
    
    public String getTransferType() {
        return transferType;
    }
    
    public void setTransferType(String transferType) {
        this.transferType = transferType;
    }
    
    public String getHost() {
        return host;
    }
    
    public void setHost(String host) {
        this.host = host;
    }
    
    public int getPort() {
        return port;
    }
    
    public void setPort(int port) {
        this.port = port;
    }
    
    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
    
    public String getPassword() {
        return password;
    }
    
    public void setPassword(String password) {
        this.password = password;
    }
}