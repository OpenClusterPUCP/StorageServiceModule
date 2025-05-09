package com.example.storageservicemodule.Controller;

import com.example.storageservicemodule.Bean.*;
import com.example.storageservicemodule.Exception.ResourceNotFoundException;
import com.example.storageservicemodule.Exception.StorageException;
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
import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;
import com.jcraft.jsch.SftpATTRS;
import org.apache.commons.io.FileUtils;
import org.springframework.core.io.Resource;

import java.io.File;
import java.io.IOException;
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

    @PostMapping("/deploy")
    public ResponseEntity<FileDeployResponse> deployImageToServer(
            @RequestParam("idServer") Integer  idServer,
            @RequestParam("idImage") String idImage) {

        // Obtener servidor físico
        PhysicalServer server = physicalServerRepository.findById(idServer)
                .orElseThrow(() -> new ResourceNotFoundException("Servidor físico no encontrado con ID: " + idServer));

        String serverIp = server.getGatewayAccessIp();
        String username = server.getSshUsername();
        String password = server.getSshPassword();
        Integer port  =  server.getGatewayAccessPort();
        String remoteTempPath ="/tmp";

        try {
            // Cargar el recurso de imagen
            Resource imageResource = storageService.loadAsResource(idImage);
            String remoteFilePath = remoteTempPath + "/" + imageResource.getFilename();

            // Transferir el archivo al servidor remoto
            boolean success = transferResourceToRemoteServer(
                    imageResource,
                    serverIp,
                    username,
                    port ,
                    password,
                    remoteFilePath
            );

            if (!success) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(new FileDeployResponse(
                                idImage,
                                server.getName(),
                                null,
                                "Falló la transferencia del archivo al servidor",
                                new Date()
                        ));
            }

            // Crear respuesta
            FileDeployResponse response = new FileDeployResponse(
                    idImage,
                    server.getName(),
                    remoteFilePath,
                    "Imagen desplegada exitosamente en el servidor " + serverIp,
                    new Date()
            );

            return ResponseEntity.ok().body(response);

        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new FileDeployResponse(
                            idImage,
                            server.getName(),
                            null,
                            "Error al procesar el archivo: " + e.getMessage(),
                            new Date()
                    ));
        }
    }

    /**
     * Transfiere un recurso a un servidor remoto usando SSH/SFTP
     *
     * @param resource       El recurso a transferir
     * @param serverIp       La dirección IP del servidor destino
     * @param username       El nombre de usuario para la conexión SSH
     * @param password       La contraseña para la conexión SSH
     * @param remoteFilePath La ruta completa donde se guardará el archivo en el servidor remoto
     * @return true si la transferencia fue exitosa, false en caso contrario
     * @throws IOException si hay un problema al acceder al recurso
     */
    public boolean transferResourceToRemoteServer(
            Resource resource,
            String serverIp,
            String username,Integer port ,
            String password,
            String remoteFilePath) throws IOException {

        JSch jsch = new JSch();
        Session session = null;
        ChannelSftp channelSftp = null;

        try {
            // Convertir el recurso a un archivo temporal si no es un archivo
            File fileToTransfer;
            if (resource.isFile()) {
                fileToTransfer = resource.getFile();
            } else {
                // Crear un archivo temporal para transferir recursos que no son archivos
                fileToTransfer = File.createTempFile("temp_transfer_", "_" + resource.getFilename());
                FileUtils.copyInputStreamToFile(resource.getInputStream(), fileToTransfer);
                fileToTransfer.deleteOnExit(); // Eliminar el archivo al salir
            }

            // Configurar la sesión SSH
            session = jsch.getSession(username, serverIp, port); // Puerto SSH por defecto
            session.setPassword(password);

            // Configuración para evitar verificación de host conocido (ajustar para producción)
            java.util.Properties config = new java.util.Properties();
            config.put("StrictHostKeyChecking", "no");
            session.setConfig(config);

            // Conectar al servidor
            session.connect(30000); // Timeout de 30 segundos

            // Abrir canal SFTP
            Channel channel = session.openChannel("sftp");
            channel.connect(30000);
            channelSftp = (ChannelSftp) channel;

            // Asegurar que el directorio de destino existe
            String remoteDir = remoteFilePath.substring(0, remoteFilePath.lastIndexOf('/'));
            try {
                // Intentar crear estructura de directorios recursivamente
                createRemoteDirectoryStructure(channelSftp, remoteDir);
            } catch (SftpException e) {
                // Ignorar error si el directorio ya existe
                if (e.id != ChannelSftp.SSH_FX_NO_SUCH_FILE) {
                    throw e;
                }
            }

            // Transferir el archivo
            channelSftp.put(fileToTransfer.getAbsolutePath(), remoteFilePath, ChannelSftp.OVERWRITE);

            // Verificar que el archivo existe en el destino después de la transferencia
            try {
                SftpATTRS attrs = channelSftp.stat(remoteFilePath);
                // Verificar tamaño para asegurar que la transferencia fue completa
                if (attrs.getSize() != fileToTransfer.length()) {
                    System.err.println("¡Advertencia! El tamaño del archivo transferido no coincide.");
                    return false;
                }
            } catch (SftpException e) {
                System.err.println("Error al verificar el archivo transferido: " + e.getMessage());
                return false;
            }

            return true;
        } catch (JSchException | SftpException e) {
            System.err.println("Error durante la transferencia SFTP: " + e.getMessage());
            e.printStackTrace();
            return false;
        } finally {
            // Cerrar conexiones
            if (channelSftp != null && channelSftp.isConnected()) {
                channelSftp.disconnect();
            }
            if (session != null && session.isConnected()) {
                session.disconnect();
            }
        }
    }

    /**
     * Crea una estructura de directorios en el servidor remoto de forma recursiva
     *
     * @param channelSftp Canal SFTP ya conectado
     * @param remotePath Ruta a crear
     * @throws SftpException Si ocurre un error durante la operación
     */
    private void createRemoteDirectoryStructure(ChannelSftp channelSftp, String remotePath) throws SftpException {
        // Si estamos en la raíz, no hay nada que crear
        if (remotePath.equals("") || remotePath.equals("/")) {
            return;
        }

        String[] folders = remotePath.split("/");
        String currentPath = "";

        // Para servidores Unix/Linux que comienzan con /
        if (remotePath.startsWith("/")) {
            currentPath = "/";
        }

        // Crear cada nivel de directorio
        for (String folder : folders) {
            if (folder.isEmpty()) continue;

            currentPath += folder + "/";
            try {
                channelSftp.cd(currentPath);
            } catch (SftpException e) {
                // Si el directorio no existe, crearlo
                if (e.id == ChannelSftp.SSH_FX_NO_SUCH_FILE) {
                    channelSftp.mkdir(currentPath);
                    channelSftp.cd(currentPath);
                } else {
                    throw e;
                }
            }
        }
    }
}