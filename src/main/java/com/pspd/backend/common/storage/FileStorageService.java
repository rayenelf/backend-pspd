package com.pspd.backend.common.storage;

import com.pspd.backend.common.error.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

/**
 * Stockage de fichiers sur disque (dev). En prod → S3/MinIO (Phase 2).
 * Répertoire configurable via {@code app.upload.dir} (défaut : ./uploads).
 */
@Service
public class FileStorageService {

    private static final long MAX_SIZE_BYTES = 5 * 1024 * 1024; // 5 Mo
    private final Path root;

    public FileStorageService(@Value("${app.upload.dir:uploads}") String dir) {
        this.root = Paths.get(dir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(root);
        } catch (IOException e) {
            throw new IllegalStateException("Impossible de créer le répertoire d'upload : " + root, e);
        }
    }

    /**
     * Sauvegarde le fichier sous un nom unique et renvoie son chemin relatif
     * (à stocker dans documents_legaux.url_fichier).
     */
    public String store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw ApiException.badRequest("FILE_EMPTY", "Le fichier est vide ou absent.");
        }
        if (file.getSize() > MAX_SIZE_BYTES) {
            throw ApiException.badRequest("FILE_TOO_LARGE", "Le fichier dépasse 5 Mo.");
        }

        String original = file.getOriginalFilename() == null ? "document" : file.getOriginalFilename();
        // On ne garde que l'extension ; le nom est régénéré pour éviter tout path traversal.
        String ext = "";
        int dot = original.lastIndexOf('.');
        if (dot >= 0) ext = original.substring(dot).replaceAll("[^a-zA-Z0-9.]", "");

        String filename = UUID.randomUUID() + ext;
        Path target = root.resolve(filename).normalize();
        if (!target.startsWith(root)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "FILE_INVALID", "Nom de fichier invalide.");
        }

        try {
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "FILE_STORE_FAILED",
                "Échec de l'enregistrement du fichier.");
        }

        return "/uploads/" + filename;
    }

    /**
     * Charge un document stocké à partir de son chemin relatif (ex. {@code /uploads/xxx.pdf}).
     * Sécurisé contre le path traversal : seul le nom de fichier est conservé.
     * Utilisé par l'admin pour consulter les documents légaux (B9).
     */
    public Resource loadAsResource(String storedPath) {
        Path file = resolveSafe(storedPath);
        try {
            Resource resource = new UrlResource(file.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                throw ApiException.notFound("Fichier introuvable.");
            }
            return resource;
        } catch (MalformedURLException e) {
            throw ApiException.notFound("Fichier introuvable.");
        }
    }

    /** Type MIME du document (pour l'affichage inline côté admin). */
    public String contentTypeOf(String storedPath) {
        try {
            String ct = Files.probeContentType(resolveSafe(storedPath));
            return ct != null ? ct : "application/octet-stream";
        } catch (IOException e) {
            return "application/octet-stream";
        }
    }

    private Path resolveSafe(String storedPath) {
        String filename = Paths.get(storedPath).getFileName().toString();
        Path file = root.resolve(filename).normalize();
        if (!file.startsWith(root)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "FILE_INVALID", "Chemin de fichier invalide.");
        }
        return file;
    }
}
