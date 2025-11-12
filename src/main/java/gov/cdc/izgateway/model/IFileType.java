package gov.cdc.izgateway.model;

/**
 * Represents a file type record.
 * <p>
 * Provides file type name, description, last changed date, and last changed by information.
 * </p>
 */
public interface IFileType extends DbAudit {
    /**
     * Gets the name of the file type.
     * @return the file type name
     */
    String getFileTypeName();

    /**
     * Sets the name of the file type.
     * @param fileTypeName the file type name
     */
    void setFileTypeName(String fileTypeName);

    /**
     * Gets the description of the file type.
     * @return the description
     */
    String getDescription();

    /**
     * Sets the description of the file type.
     * @param description the description
     */
    void setDescription(String description);
}
