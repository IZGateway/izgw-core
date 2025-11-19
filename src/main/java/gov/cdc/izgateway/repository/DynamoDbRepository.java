package gov.cdc.izgateway.repository;

import java.lang.reflect.InvocationTargetException;
import java.util.Iterator;
import java.util.List;
import java.util.ServiceConfigurationError;

import gov.cdc.izgateway.logging.markers.Markers2;
import gov.cdc.izgateway.model.DbAudit;
import gov.cdc.izgateway.model.DynamoDbEntity;
import gov.cdc.izgateway.model.HasEnvironment;
import gov.cdc.izgateway.utils.SystemUtils;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Expression;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.mapper.BeanTableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.PutItemEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;

/**
 * A DynamoDbRepository for a given type of database entity.
 * @author Audacious Inquiry
 *
 * @param <T>
 */
@Slf4j
public abstract class DynamoDbRepository<T extends DynamoDbEntity & DbAudit> implements IRepository<T> {
	
	private static String serverName = null;
	/**
	 * The table name for the repository of entities.  Because we are using a single table 
	 * design, there is only one table name.
	 */
	private final Class<T> entityClass;
	private final DynamoDbTable<T> table;
	
	/**
	 * Set the server name to use for audit fields.
	 * @param name The server name to set for audit fields.
	 */
	public static void setServerName(String name) {
		serverName = name;
	}
	/**
	 * Get the server name to use for audit fields.
	 * @return The server name to use for audit fields.
	 */
	public static String getServerName() {
		return serverName;
	}
	/**
	 * Create a new DynamoDbRepository for a given entity type.
	 * @param entityClass	The class representing the entity to create the repository for.
	 * @param client	The DynamoDbEnhancedClient to use to create the repository.
	 * @param tableName The name of the DynamoDB table to use.
	 */
	protected DynamoDbRepository(Class<T> entityClass, DynamoDbEnhancedClient client, String tableName) {
		this.entityClass = entityClass;
		// This creates the schema from the class, but does not get subclass attributes.
		BeanTableSchema<T> schema = TableSchema.fromBean(entityClass);
		table = client.table(tableName, schema);
		log.info("Table initialized for {}", entityClass.getSimpleName());
	}
	
	/**
	 * Find the specified entity by its primary identifier
	 * @param primaryId	The primary identifier
	 * @return	The found entity.
	 */
	public T find(String primaryId) {
		Key key = Key.builder().partitionValue(entityClass.getSimpleName()).sortValue(primaryId).build();
		Iterator<T> i = table.query(QueryConditional.keyEqualTo(key)).items().iterator();
		if (!i.hasNext()) {
			return null;
		}
		return i.next();
	}
	
	/**
	 * This is a protected method that supports finding by a partial sort key.
	 * It is intended to be called by methods which are better named in the 
	 * extending interface to collect entities with particular properties.
	 * 
	 * @param partialKey	The partial key
	 * @return	The found entities.
	 */
	protected List<T> findByType(String partialKey) {
		Key key = Key.builder().partitionValue(entityClass.getSimpleName()).sortValue(partialKey).build();
		return table.query(QueryConditional.sortBeginsWith(key)).items().stream().toList();
	}
	
	/**
	 * Find all items for the current environment.
	 * NOTE: This method assumes that the sort key begins with the environment id followed by a '#'.
	 * @return	All entities of the specified type stored in the database for the current environment
	 */
	public List<T> findAllForEnvironment() {
		if (HasEnvironment.class.isAssignableFrom(entityClass)) {
			return findByType(SystemUtils.getDestType() + "#");
		} 
		return findAll();
	}
	
	/**
	 * Find all items.
	 * @return	All entities of the specified type stored in the database 
	 */
	public List<T> findAll() {
		Key key = Key.builder().partitionValue(entityClass.getSimpleName()).build();
		return table.query(QueryConditional.keyEqualTo(key)).items().stream().toList();
	}
	
	/**
	 * Create a new entity of the specified type.
	 * @return The new entity.
	 */
	public T createEntity() {
		try {
			return entityClass.getDeclaredConstructor().newInstance();
		} catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException
				| NoSuchMethodException | SecurityException e) {
			String msg = "Unexpected exception creating " + entityClass.getName();
			log.error(Markers2.append(e), msg);
			throw new ServiceConfigurationError(msg, e);
		}
	}
	
	/**
	 * Delete the specified entity.  Override this method to make it public in derived classes. 
	 * @param primaryId	The primary key of the entity to delete.
	 */
	protected void delete(String primaryId) {
		if (primaryId == null) {
			throw new NullPointerException("Entity cannot be null");
		}
		Key key = Key.builder().partitionValue(entityClass.getSimpleName()).sortValue(primaryId).build();
		table.deleteItem(key);
	}
	
	/**
	 * Delete the specified entity.  
	 * @param entity	The entity to delete.
	 */
	public void delete(T entity) {
		if (entity == null) {
			throw new NullPointerException("Entity cannot be null");
		}
		// Note: Log before we act so that if the delete fails we still have a record of it.
		Key key = Key.builder().partitionValue(entity.getEntityType()).sortValue(entity.getSortKey()).build();
		String type = entity.getClass().getSimpleName();
		log.info(Markers2.append(type, entity), "Deleted {}", type);
		table.deleteItem(key);
	}
	
	/**
	 * Create or update the specified entity.  Override this method to make it public in derived classes.  
	 * @param entity	The entity to create.
	 * @return	The saved entity.
	 */
	protected T saveAndFlush(T entity) {
		if (entity == null) {
			throw new NullPointerException("Entity cannot be null");
		}
		
		String type = entity.getClass().getSimpleName();
		log.info(Markers2.append(type, entity), "Updated {}", type);
		table.putItem(entity);
        return entity;
	}
	
	protected T saveIfNotExists(T entity) {
		Expression ex = Expression.builder().expression(
			"attribute_not_exists(" + DynamoDbEntity.ENTITY_TYPE + ") AND attribute_not_exists(" + DynamoDbEntity.SORT_KEY + ")"
		).build();
		PutItemEnhancedRequest<T> request = PutItemEnhancedRequest.builder(entityClass).item(entity).conditionExpression(ex).build();
		try {
			// Note: Log before we act so that if the create fails we still have a record of it.
			String type = entity.getClass().getSimpleName();
			log.info(Markers2.append(type, entity), "Created {}", type);
			table.putItem(request);
			return entity;
		} catch (ConditionalCheckFailedException e) {
			return null;
		} catch (Exception e) {
			log.warn(Markers2.append(e), "Conditional Create on {}:{} failed", entity.getEntityType(), entity.getSortKey());
			return null;
		}
	}
}
