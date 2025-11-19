package gov.cdc.izgateway.model;

/**
 * Represents an endpoint in IZ Gateway, including destination and jurisdiction details.
 * <p>
 * Provides methods to get destination ID, URI, type, version, and jurisdiction information.
 * </p>
 */
public interface IEndpoint {

	/**
	 * Gets the destination ID for the endpoint.
	 * @return the destination ID
	 */
	String getDestId();

	/**
	 * Gets the destination URI for the endpoint.
	 * @return the destination URI
	 */
	String getDestUri();

	/**
	 * Gets the destination type for the endpoint.
	 * @return the destination type
	 */
	String getDestType();

	/**
	 * Gets the destination type ID for the endpoint.
	 * @return the destination type ID
	 */
	int getDestTypeId();

	/**
	 * Gets the jurisdiction name for the endpoint.
	 * @return the jurisdiction name
	 */
	String getJurisdictionName();

	/**
	 * Gets the jurisdiction description for the endpoint.
	 * @return the jurisdiction description
	 */
	String getJurisdictionDesc();

	/**
	 * Gets the jurisdiction ID for the endpoint.
	 * @return the jurisdiction ID
	 */
	int getJurisdictionId();

	/**
	 * Gets the destination version for the endpoint.
	 * @return the destination version
	 */
	String getDestVersion();

}