package net.carcdr.ycrdt;

import java.util.function.Consumer;

/**
 * A collaborative document containing shared types.
 * YDoc is the root container for all Y-CRDT data structures.
 */
public interface YDoc extends AutoCloseable {

    /**
     * Returns the client ID for this document.
     *
     * @return the client ID
     */
    long getClientId();

    /**
     * Returns the globally unique identifier for this document.
     *
     * @return the GUID
     */
    String getGuid();

    // State encoding

    /**
     * Encodes the entire document state as an update.
     *
     * @return the encoded update
     */
    byte[] encodeStateAsUpdate();

    /**
     * Encodes the entire document state as an update within a transaction.
     *
     * @param txn the transaction
     * @return the encoded update
     */
    byte[] encodeStateAsUpdate(YTransaction txn);

    /**
     * Applies an update to this document.
     *
     * @param update the update to apply
     */
    void applyUpdate(byte[] update);

    /**
     * Applies an update to this document within a transaction.
     *
     * @param txn the transaction
     * @param update the update to apply
     */
    void applyUpdate(YTransaction txn, byte[] update);

    /**
     * Encodes the state vector of this document.
     *
     * @return the encoded state vector
     */
    byte[] encodeStateVector();

    /**
     * Encodes the state vector of this document within a transaction.
     *
     * @param txn the transaction
     * @return the encoded state vector
     */
    byte[] encodeStateVector(YTransaction txn);

    /**
     * Encodes the difference between this document and the given state vector.
     *
     * @param stateVector the remote state vector
     * @return the encoded diff
     */
    byte[] encodeDiff(byte[] stateVector);

    /**
     * Encodes the difference between this document and the given state vector
     * within a transaction.
     *
     * @param txn the transaction
     * @param stateVector the remote state vector
     * @return the encoded diff
     */
    byte[] encodeDiff(YTransaction txn, byte[] stateVector);

    // Shared type accessors

    /**
     * Gets or creates a collaborative text with the given name.
     *
     * @param name the name of the text
     * @return the text instance
     */
    YText getText(String name);

    /**
     * Gets or creates a collaborative array with the given name.
     *
     * @param name the name of the array
     * @return the array instance
     */
    YArray getArray(String name);

    /**
     * Gets or creates a collaborative map with the given name.
     *
     * @param name the name of the map
     * @return the map instance
     */
    YMap getMap(String name);

    /**
     * Gets or creates a collaborative XML text with the given name.
     *
     * @param name the name of the XML text
     * @return the XML text instance
     */
    YXmlText getXmlText(String name);

    /**
     * Gets or creates a collaborative XML element with the given name.
     *
     * @param name the name of the XML element
     * @return the XML element instance
     */
    YXmlElement getXmlElement(String name);

    /**
     * Gets or creates a collaborative XML fragment with the given name.
     *
     * @param name the name of the XML fragment
     * @return the XML fragment instance
     */
    YXmlFragment getXmlFragment(String name);

    // Transactions

    /**
     * Begins a new transaction for batching operations.
     *
     * @return the transaction
     */
    YTransaction beginTransaction();

    /**
     * Executes a function within a transaction.
     *
     * @param fn the function to execute
     */
    void transaction(Consumer<YTransaction> fn);

    // Observers

    /**
     * Registers an observer for document updates.
     *
     * @param observer the observer to register
     * @return a subscription handle for unregistering
     */
    YSubscription observeUpdateV1(UpdateObserver observer);

    /**
     * Sets the error handler for observer exceptions.
     *
     * <p>When an observer throws an exception, this handler will be called
     * instead of letting the exception propagate. The default handler prints
     * to stderr for backwards compatibility.</p>
     *
     * @param handler the error handler to use, or null to use the default handler
     * @see ObserverErrorHandler
     * @see DefaultObserverErrorHandler
     */
    void setObserverErrorHandler(ObserverErrorHandler handler);

    /**
     * Gets the current error handler for observer exceptions.
     *
     * @return the current error handler (never null)
     */
    ObserverErrorHandler getObserverErrorHandler();

    // Lifecycle

    /**
     * Closes this document and releases all resources.
     */
    @Override
    void close();

    /**
     * Checks if this document has been closed.
     *
     * @return true if closed, false otherwise
     */
    boolean isClosed();
}
