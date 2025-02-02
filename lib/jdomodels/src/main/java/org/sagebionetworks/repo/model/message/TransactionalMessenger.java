package org.sagebionetworks.repo.model.message;

import java.util.List;

import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.ObservableEntity;

/**
 * Sends messages as part of a transaction.
 * @author John
 *
 */
public interface TransactionalMessenger {
	
	/**
	 * Send a delete message after the current thread's transaction commits.
	 * 
	 * @param objectId
	 * @param objectType
	 */
	void sendDeleteMessageAfterCommit(String objectId, ObjectType objectType);
	
	/**
	 * Send a change message after the current thread's transaction commits.
	 * Note: The ChangeMessage.userId will be set from the thread local context for this case.
	 * 
	 * @Deprecated Use: {@link TransactionalMessenger#sendMessageAfterCommit(MessageToSend)}
	 * @param objectId
	 * @param objectType
	 * @param changeType
	 */
	void sendMessageAfterCommit(String objectId, ObjectType objectType, ChangeType changeType);

	
	/**
	 * Send a message after a commit.
	 * @param toSend
	 */
	void sendMessageAfterCommit(MessageToSend toSend);

	/**
	 * Send a change message fashioned after the passed entity
	 * after the current transaction commits.
	 */
	void sendMessageAfterCommit(ObservableEntity entity, ChangeType changeType);
	
	/**
	 * Send the passed message after the current transaction commits.
	 */
	void sendMessageAfterCommit(ChangeMessage message);
	
	/**
	 * Publishes the given message to its observers directly after the current transaction commits. If the timestamp is not set it will be automatically be assigned.
	 * 
	 * In case of a {@link LocalStackChangeMesssage} the change number is automatically populated with a -1 if not supplied.
	 * 
	 * @param message A message is that is local to the current stack (e.g. does not migrate between stack and will not be re-played)
	 */
	void publishMessageAfterCommit(LocalStackMessage message);

	/**
	 * Register an observer that will be notified when there is a message after a commit.
	 * 
	 * @param observer
	 */
	void registerObserver(TransactionalMessengerObserver observer);
	
	
	/**
	 * Remove an observer.
	 * @param observer
	 * @return true if observer was registered.
	 */
	boolean removeObserver(TransactionalMessengerObserver observer);
	
	/**
	 * Get an immutable list of all TransactionalMessengerObservers
	 * @return
	 */
	List<TransactionalMessengerObserver> getAllObservers();
	
	/**
	 * Register a batch of change messages as sent.  Each ChangeMessage in the batch must 
	 * have an ObjectType that matches the provided type.
	 * 
	 * @param type The ObjectType of the batch.
	 * @param batch
	 */
	void registerMessagesSent(ObjectType type, List<ChangeMessage> batch);
	
	/**
	 * List messages that have been created but not registered as sent (see {@link #registerMessageSent(long)}).
	 * This is used to detect messages that need to be sent either for the first time or re-sent on a new stacks.
	 * 
	 * @param limit
	 * @return
	 */
	List<ChangeMessage> listUnsentMessages(long limit);
}
