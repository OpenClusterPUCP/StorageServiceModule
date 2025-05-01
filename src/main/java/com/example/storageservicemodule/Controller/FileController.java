package com.example.storageservicemodule.Controller;

import com.example.storageservicemodule.Bean.*;
import com.example.storageservicemodule.Interfaces.StorageService;
import com.example.storageservicemodule.Repository.PhysicalServerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import com.jcraft.jsch.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
@RestController
@RequestMapping("/api/files")
public class FileController {

    @Autowired
    private PhysicalServerRepository physicalServerRepository;
    private final StorageService storageService;

    @Autowired
    public FileController(StorageService storageService) {
        this.storageService = storageService;
    }

    @GetMapping
    public ResponseEntity<List<FileInfo>> listFiles() {
        List<FileInfo> fileInfos = storageService.loadAll()
                .map(path -> {
                    String filename = path.getFileName().toString();
                    String url = MvcUriComponentsBuilder
                            .fromMethodName(FileController.class, "getFile", filename)
                            .build().toUriString();
                    
                    // Obtener información adicional del archivo
                    long size = 0;
                    String contentType = "application/octet-stream";
                    Date lastModified = new Date();
                    
                    try {
                        Path filePath = storageService.load(filename);
                        size = Files.size(filePath);
                        contentType = Files.probeContentType(filePath);
                        if (contentType == null) {
                            contentType = "application/octet-stream";
                        }
                        lastModified = new Date(Files.getLastModifiedTime(filePath).toMillis());
                    } catch (IOException e) {
                        // Manejar error silenciosamente
                    }
                    
                    return new FileInfo(filename, url, size, contentType, lastModified);
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok().body(fileInfos);
    }

    @PostMapping
    public ResponseEntity<FileUploadResponse> uploadFile(@RequestParam("file") MultipartFile file) {
        String savedFilename = storageService.store(file);
        
        String fileUrl = MvcUriComponentsBuilder
                .fromMethodName(FileController.class, "getFile", savedFilename)
                .build().toUriString();
                
        FileUploadResponse response = new FileUploadResponse(
                file.getOriginalFilename(),
                savedFilename,
                fileUrl,
                file.getContentType(),
                file.getSize()
        );
        
        return ResponseEntity.ok().body(response);
    }

    @GetMapping("/{filename:.+}")
    public ResponseEntity<Resource> getFile(@PathVariable String filename) {
        Resource file = storageService.loadAsResource(filename);
        
        String contentType = "application/octet-stream";
        try {
            contentType = Files.probeContentType(file.getFile().toPath());
            if (contentType == null) {
                contentType = "application/octet-stream";
            }
        } catch (IOException e) {
            // Manejar error silenciosamente
        }
        
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.getFilename() + "\"")
                .body(file);
    }

    @DeleteMapping("/{filename:.+}")
    public ResponseEntity<FileDeleteResponse> deleteFile(@PathVariable String filename) {
        storageService.delete(filename);
        
        FileDeleteResponse response = new FileDeleteResponse(
                filename,
                "Archivo eliminado correctamente",
                new Date()
        );
        
        return ResponseEntity.ok().body(response);
    }


    @PostMapping("/transfer")
    public ResponseEntity<String> transferFile(
            @RequestParam(value = "sourceServerId") Integer sourceServerId,
            @RequestParam(value = "destinationServerId") Integer destinationServerId,
            @RequestParam(value = "sourceFilePath") String sourceFilePath,
            @RequestParam(value = "destinationFilePath") String destinationFilePath) {

        try {
            // Get source server details
            Optional<PhysicalServer> optionalSourceServer = physicalServerRepository.findById(sourceServerId);
            if (!optionalSourceServer.isPresent()) {
                return new ResponseEntity<>("Servidor de origen no encontrado", HttpStatus.NOT_FOUND);
            }
            PhysicalServer sourceServer = optionalSourceServer.get();

            // Get destination server details
            Optional<PhysicalServer> optionalDestinationServer = physicalServerRepository.findById(destinationServerId);
            if (!optionalDestinationServer.isPresent()) {
                return new ResponseEntity<>("Servidor de destino no encontrado", HttpStatus.NOT_FOUND);
            }
            PhysicalServer destinationServer = optionalDestinationServer.get();

            // First, download file from source server to a temporary location
            Path tempDir = Files.createTempDirectory("file_transfer");
            String tempFileName = UUID.randomUUID().toString();
            Path tempFilePath = tempDir.resolve(tempFileName);

            // Download from source server
            boolean downloadSuccess = downloadFromServer(sourceServer, sourceFilePath, tempFilePath);
            if (!downloadSuccess) {
                Files.deleteIfExists(tempFilePath);
                Files.deleteIfExists(tempDir);
                return new ResponseEntity<>("Error al descargar el archivo del servidor origen",
                        HttpStatus.INTERNAL_SERVER_ERROR);
            }

            // Upload to destination server
            boolean uploadSuccess = uploadToServer(destinationServer, tempFilePath, destinationFilePath);

            // Clean up temporary files
            Files.deleteIfExists(tempFilePath);
            Files.deleteIfExists(tempDir);

            if (!uploadSuccess) {
                return new ResponseEntity<>("Error al subir el archivo al servidor destino",
                        HttpStatus.INTERNAL_SERVER_ERROR);
            }

            return new ResponseEntity<>("Archivo transferido exitosamente entre servidores", HttpStatus.OK);

        } catch (Exception e) {
            return new ResponseEntity<>("Error durante la transferencia: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private boolean downloadFromServer(PhysicalServer server, String remotePath, Path localPath) throws Exception {
        // Determine transfer method based on server configuration
        String transferType = determineTransferType(server);

        switch (transferType.toLowerCase()) {
            case "local":
                return downloadLocal(remotePath, localPath);
            case "sftp":
                return downloadSftp(server, remotePath, localPath);
            case "ftp":
                return downloadFtp(server, remotePath, localPath);
            default:
                throw new IllegalArgumentException("Tipo de transferencia no soportado para el servidor: " + transferType);
        }
    }

    private boolean uploadToServer(PhysicalServer server, Path localPath, String remotePath) throws Exception {
        // Determine transfer method based on server configuration
        String transferType = determineTransferType(server);

        switch (transferType.toLowerCase()) {
            case "local":
                return uploadLocal(localPath, remotePath);
            case "sftp":
                return uploadSftp(server, localPath, remotePath);
            case "ftp":
                return uploadFtp(server, localPath, remotePath);
            default:
                throw new IllegalArgumentException("Tipo de transferencia no soportado para el servidor: " + transferType);
        }
    }

    private String determineTransferType(PhysicalServer server) {
        // Determine the transfer type based on the server configuration
        // This could be a property of the server or inferred from available connection details
        if (server.getInfrastructureType().equalsIgnoreCase("local")) {
            return "local";
        } else if (server.getAuthMethod().equalsIgnoreCase("ssh") ||
                server.getAuthMethod().equalsIgnoreCase("password")) {
            return "sftp";
        } else {
            return "ftp";
        }
    }

    // Local file transfer methods
    private boolean downloadLocal(String sourcePath, Path destPath) throws IOException {
        Path source = Paths.get(sourcePath);

        // Check if source file exists
        if (!Files.exists(source)) {
            throw new FileNotFoundException("El archivo de origen no existe: " + sourcePath);
        }

        // Copy the file
        Files.copy(source, destPath, StandardCopyOption.REPLACE_EXISTING);
        return true;
    }

    private boolean uploadLocal(Path sourcePath, String destPath) throws IOException {
        Path destination = Paths.get(destPath);

        // Create destination directory if it doesn't exist
        Files.createDirectories(destination.getParent());

        // Copy the file
        Files.copy(sourcePath, destination, StandardCopyOption.REPLACE_EXISTING);
        return true;
    }

    // SFTP transfer methods
    private boolean downloadSftp(PhysicalServer server, String remotePath, Path localPath) {
        Session session = null;
        Channel channel = null;
        ChannelSftp channelSftp = null;

        try {
            JSch jsch = new JSch();
            session = jsch.getSession(server.getSshUsername(), server.getIp(), server.getSshPort());

            // Handle different authentication methods
            if (server.getAuthMethod().equalsIgnoreCase("password")) {
                session.setPassword(server.getSshPassword());
            } else if (server.getAuthMethod().equalsIgnoreCase("key")) {
                jsch.addIdentity(server.getSshKeyPath());
            }

            // Configure session
            java.util.Properties config = new java.util.Properties();
            config.put("StrictHostKeyChecking", "no");
            session.setConfig(config);

            session.connect();

            channel = session.openChannel("sftp");
            channel.connect();
            channelSftp = (ChannelSftp) channel;

            // Create parent directories for local path
            Files.createDirectories(localPath.getParent());

            // Download the file
            try (OutputStream os = Files.newOutputStream(localPath)) {
                channelSftp.get(remotePath, os);
            }

            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
            if (channelSftp != null) channelSftp.exit();
            if (channel != null) channel.disconnect();
            if (session != null) session.disconnect();
        }
    }

    private boolean uploadSftp(PhysicalServer server, Path localPath, String remotePath) {
        Session session = null;
        Channel channel = null;
        ChannelSftp channelSftp = null;

        try {
            JSch jsch = new JSch();
            session = jsch.getSession(server.getSshUsername(), server.getIp(), server.getSshPort());

            // Handle different authentication methods
            if (server.getAuthMethod().equalsIgnoreCase("password")) {
                session.setPassword(server.getSshPassword());
            } else if (server.getAuthMethod().equalsIgnoreCase("key")) {
                jsch.addIdentity(server.getSshKeyPath());
            }

            // Configure session
            java.util.Properties config = new java.util.Properties();
            config.put("StrictHostKeyChecking", "no");
            session.setConfig(config);

            session.connect();

            channel = session.openChannel("sftp");
            channel.connect();
            channelSftp = (ChannelSftp) channel;

            // Create remote directory if it doesn't exist
            try {
                String destDir = remotePath.substring(0, remotePath.lastIndexOf('/'));
                channelSftp.mkdir(destDir);
            } catch (SftpException e) {
                // Ignore if directory already exists
            }

            // Upload the file
            try (InputStream is = Files.newInputStream(localPath)) {
                channelSftp.put(is, remotePath);
            }

            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
            if (channelSftp != null) channelSftp.exit();
            if (channel != null) channel.disconnect();
            if (session != null) session.disconnect();
        }
    }

    // FTP transfer methods
    private boolean downloadFtp(PhysicalServer server, String remotePath, Path localPath) {
        org.apache.commons.net.ftp.FTPClient ftpClient = new org.apache.commons.net.ftp.FTPClient();

        try {
            ftpClient.connect(server.getIp(), server.getSshPort()); // Using SSH port for FTP as well
            ftpClient.login(server.getSshUsername(), server.getSshPassword());
            ftpClient.enterLocalPassiveMode();
            ftpClient.setFileType(org.apache.commons.net.ftp.FTP.BINARY_FILE_TYPE);

            // Create parent directories for local path
            Files.createDirectories(localPath.getParent());

            // Download the file
            try (OutputStream outputStream = Files.newOutputStream(localPath)) {
                boolean success = ftpClient.retrieveFile(remotePath, outputStream);
                return success;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
            try {
                if (ftpClient.isConnected()) {
                    ftpClient.logout();
                    ftpClient.disconnect();
                }
            } catch (IOException e) {
                // Ignore exceptions on close
            }
        }
    }

    private boolean uploadFtp(PhysicalServer server, Path localPath, String remotePath) {
        org.apache.commons.net.ftp.FTPClient ftpClient = new org.apache.commons.net.ftp.FTPClient();

        try {
            ftpClient.connect(server.getIp(), server.getSshPort()); // Using SSH port for FTP as well
            ftpClient.login(server.getSshUsername(), server.getSshPassword());
            ftpClient.enterLocalPassiveMode();
            ftpClient.setFileType(org.apache.commons.net.ftp.FTP.BINARY_FILE_TYPE);

            // Create directory if it doesn't exist
            String destDir = remotePath.substring(0, remotePath.lastIndexOf('/'));
            ftpClient.makeDirectory(destDir);

            // Upload the file
            try (InputStream inputStream = Files.newInputStream(localPath)) {
                boolean success = ftpClient.storeFile(remotePath, inputStream);
                return success;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
            try {
                if (ftpClient.isConnected()) {
                    ftpClient.logout();
                    ftpClient.disconnect();
                }
            } catch (IOException e) {
                // Ignore exceptions on close
            }
        }
    }
}