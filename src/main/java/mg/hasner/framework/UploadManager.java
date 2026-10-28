package mg.hasner.framework;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.Part;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Lit les fichiers multipart et les sauvegarde dans le dossier configure par l'application.
 */
public final class UploadManager {
    /** Nom du parametre web.xml qui configure le dossier de stockage. */
    public static final String UPLOAD_DIRECTORY_PARAMETER = "uploadDirectory";

    /**
     * Classe utilitaire : aucune instance n'est necessaire.
     */
    private UploadManager() {
    }

    /**
     * Retourne les fichiers de la requete, puis sauvegarde chacun dans le dossier configure.
     */
    public static Map<String, MaFile> readAndStore(HttpServletRequest request,
            ServletContext servletContext) throws IOException, ServletException {
        if (!isMultipartRequest(request)) {
            return Collections.emptyMap();
        }

        Path uploadDirectory = getUploadDirectory(servletContext);
        Map<String, MaFile> files = new LinkedHashMap<>();

        for (Part part : request.getParts()) {
            String submittedFileName = part.getSubmittedFileName();
            if (submittedFileName == null || submittedFileName.trim().isEmpty()) {
                continue;
            }

            if (files.containsKey(part.getName())) {
                throw new ServletException("Un seul fichier est accepte pour le champ " + part.getName() + ".");
            }

            MaFile file = new MaFile(part.getName(), sanitizeFileName(submittedFileName),
                    part.getContentType(), readContent(part));
            Path storedFile = store(file, uploadDirectory);
            file.setStoragePath(storedFile.toString());
            files.put(part.getName(), file);
        }

        return files;
    }

    /**
     * Lit tous les octets du fichier en fermant le flux fourni par le conteneur Servlet.
     */
    private static byte[] readContent(Part part) throws IOException {
        try (InputStream input = part.getInputStream()) {
            return input.readAllBytes();
        }
    }

    /**
     * Verifie que la requete est bien un formulaire multipart.
     */
    private static boolean isMultipartRequest(HttpServletRequest request) {
        String contentType = request.getContentType();
        return contentType != null && contentType.toLowerCase().startsWith("multipart/");
    }

    /**
     * Lit le dossier depuis web.xml et accepte aussi un chemin interne a l'application.
     */
    private static Path getUploadDirectory(ServletContext servletContext) throws ServletException {
        String configuredPath = servletContext.getInitParameter(UPLOAD_DIRECTORY_PARAMETER);
        if (configuredPath == null || configuredPath.trim().isEmpty()) {
            throw new ServletException("Le parametre " + UPLOAD_DIRECTORY_PARAMETER
                    + " doit etre configure dans web.xml.");
        }

        String value = configuredPath.trim();
        if (value.startsWith("/")) {
            String realPath = servletContext.getRealPath(value);
            if (realPath == null) {
                throw new ServletException("Le dossier d'upload interne est indisponible : " + value + ".");
            }
            return Paths.get(realPath).toAbsolutePath().normalize();
        }

        return Paths.get(value).toAbsolutePath().normalize();
    }

    /**
     * Sauvegarde le fichier avec un nom disponible pour ne pas ecraser un fichier existant.
     */
    private static Path store(MaFile file, Path uploadDirectory) throws IOException {
        Files.createDirectories(uploadDirectory);
        Path target = findAvailablePath(uploadDirectory, file.getFileName());
        Files.write(target, file.getContent(), StandardOpenOption.CREATE_NEW);
        return target;
    }

    /**
     * Produit un nom unique en ajoutant -1, -2, etc. avant l'extension si necessaire.
     */
    private static Path findAvailablePath(Path directory, String fileName) {
        String name = fileName;
        String extension = "";
        int pointIndex = fileName.lastIndexOf('.');
        if (pointIndex > 0) {
            name = fileName.substring(0, pointIndex);
            extension = fileName.substring(pointIndex);
        }

        Path candidate = directory.resolve(fileName).normalize();
        int duplicateIndex = 1;
        while (Files.exists(candidate)) {
            candidate = directory.resolve(name + "-" + duplicateIndex + extension).normalize();
            duplicateIndex++;
        }

        if (!candidate.getParent().equals(directory)) {
            throw new IllegalArgumentException("Nom de fichier non autorise : " + fileName + ".");
        }

        return candidate;
    }

    /**
     * Retire tout chemin fourni par le navigateur pour eviter une sortie du dossier configure.
     */
    private static String sanitizeFileName(String submittedFileName) throws ServletException {
        Path fileName = Paths.get(submittedFileName).getFileName();
        if (fileName == null || fileName.toString().trim().isEmpty()) {
            throw new ServletException("Le nom du fichier envoye est invalide.");
        }

        return fileName.toString();
    }
}
