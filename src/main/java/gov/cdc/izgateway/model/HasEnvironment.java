package gov.cdc.izgateway.model;

/**
 * Interface implemented by entities that have an environment attribute.
 * 
 * NOTE: When this interface is implemented, the environment attribute must be the FIRST part of the primary key.
 * 
 * @author Audacious Inquiry
 */
public interface HasEnvironment {
	/**
	 * Get the environment for this entity.
	 * @return The environment.
	 */
	int getEnvironment();
	/**
	 * Set the environment for this entity.
	 * @param environment The environment for this entity.
	 */
	void setEnvironment(int environment);
}
