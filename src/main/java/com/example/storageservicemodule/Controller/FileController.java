package com.example.storageservicemodule.Controller;

import com.example.storageservicemodule.Bean.FileDeleteResponse;
import com.example.storageservicemodule.Bean.FileInfo;
import com.example.storageservicemodule.Bean.FileUploadResponse;
import com.example.storageservicemodule.Interfaces.StorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
@RestController
@RequestMapping("/api/files")
public class FileController {

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
}