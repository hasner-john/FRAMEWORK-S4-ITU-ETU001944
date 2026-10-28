package mg.hasner.framework;

/**
 * Represente un fichier recu depuis un formulaire multipart.
 */
public class MaFile {
    private final String fieldName;
    private final String fileName;
    private final String contentType;
    private final String extension;
    private final byte[] content;
    private String storagePath;

    /**
     * Construit le fichier avec ses metadonnees et son contenu binaire.
     */
    public MaFile(String fieldName, String fileName, String contentType, byte[] content) {
        this.fieldName = fieldName;
        this.fileName = fileName;
        this.contentType = contentType;
        this.extension = findExtension(fileName);
        this.content = content == null ? new byte[0] : content.clone();
    }

    /**
     * Retourne le nom du champ HTML qui contenait le fichier.
     */
    public String getFieldName() {
        return fieldName;
    }

    /**
     * Retourne le nom original du fichier envoye par le navigateur.
     */
    public String getFileName() {
        return fileName;
    }

    /**
     * Retourne le type MIME fourni par le navigateur.
     */
    public String getContentType() {
        return contentType;
    }

    /**
     * Retourne l'extension du fichier, sans le point initial.
     */
    public String getExtension() {
        return extension;
    }

    /**
     * Retourne une copie des octets recus afin de proteger le contenu interne.
     */
    public byte[] getContent() {
        return content.clone();
    }

    /**
     * Retourne la taille du fichier en octets.
     */
    public int getSize() {
        return content.length;
    }

    /**
     * Retourne le chemin du fichier sauvegarde par le framework.
     */
    public String getStoragePath() {
        return storagePath;
    }

    /**
     * Defini le chemin de stockage apres l'ecriture du fichier par UploadManager.
     */
    void setStoragePath(String storagePath) {
        this.storagePath = storagePath;
    }

    /**
     * Extrait l'extension depuis le dernier point du nom de fichier.
     */
    private String findExtension(String name) {
        if (name == null) {
            return "";
        }

        int pointIndex = name.lastIndexOf('.');
        if (pointIndex <= 0 || pointIndex == name.length() - 1) {
            return "";
        }

        return name.substring(pointIndex + 1);
    }
}
