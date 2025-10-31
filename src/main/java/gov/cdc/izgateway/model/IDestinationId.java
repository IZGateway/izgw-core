package gov.cdc.izgateway.model;

/**
 * Represents a unique identifier for a destination in IZ Gateway.
 * <p>
 * Provides methods to get and set the destination ID and type, and to copy the identifier.
 * </p>
 */
public interface IDestinationId {

	/**
	 * Gets the destination ID.
	 * @return the destination ID
	 */
	String getDestId();

	/**
	 * Gets the destination type as an integer.
	 * @return the destination type
	 */
	int getDestType();

	/**
	 * Sets the destination ID.
	 * @param destId the destination ID
	 */
	void setDestId(String destId);

	/**
	 * Creates a copy of this destination identifier.
	 * @return a copy of the destination identifier
	 */
	IDestinationId copy();

	/**
	 * Sets the destination type using a string value.
	 * @param destType the destination type as a string
	 */
	void setDestType(String destType);

	/**
	 * Sets the destination type using an integer value.
	 * @param destType the destination type as an integer
	 */
	void setDestType(int destType);
}