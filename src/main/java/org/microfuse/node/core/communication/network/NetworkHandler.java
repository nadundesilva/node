package org.microfuse.node.core.communication.network;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * The Network Handler SuperClass.
 *
 * All types of network handlers should extend this abstract class.
 */
public abstract class NetworkHandler {
    private static final Logger logger = LoggerFactory.getLogger(NetworkHandler.class);

    private List<NetworkHandlerListener> listenersList;

    public NetworkHandler() {
        clearListeners();
    }

    /**
     * Get the name of the network handler.
     *
     * @return The name of the network handler
     */
    public abstract String getName();

    /**
     * Start listening to messages from other devices.
     */
    public abstract void startListening();

    /**
     * Send a message to the specified node.
     *
     * @param toAddress The address to which the message should be sent
     * @param message   The message to be sent
     */
    public abstract void sendMessage(String toAddress, String message);

    /**
     * Runs tasks to be run when an error occurs in sending a message.
     *
     * @param toAddress The address to which the message should be sent
     * @param message   The message
     */
    protected void onMessageSendFailed(String toAddress, String message) {
        logger.debug("Failed to send message to " + toAddress + ": " + message);

        for (NetworkHandlerListener listener : listenersList) {
            listener.onMessageSendFailed(toAddress, message);
        }
    }

    /**
     * Run tasks to be run when a message is received.
     *
     * @param fromAddress The address from which the message was received
     * @param message     The message received
     */
    protected void onMessageReceived(String fromAddress, String message) {
        logger.debug("Message received from " + fromAddress + ": " + message);

        for (NetworkHandlerListener listener : listenersList) {
            listener.onMessageReceived(fromAddress, message);
        }
    }

    /**
     * Register a new listener.
     *
     * @param listener The new listener to be registered
     */
    public void registerListener(NetworkHandlerListener listener) {
         if (listenersList.add(listener)) {
             logger.debug("Registered network handler listener " + listener.getClass());
         } else {
             logger.debug("Failed to register network handler listener " + listener.getClass());
         }
    }

    /**
     * Unregister an existing listener.
     *
     * @param listener The listener to be removed
     */
    public void unregisterListener(NetworkHandlerListener listener) {
        if (listenersList.remove(listener)) {
            logger.debug("Unregistered network handler listener " + listener.getClass());
        } else {
            logger.debug("Failed to unregister network handler listener " + listener.getClass());
        }
    }

    /**
     * Unregister all existing listener.
     */
    public void clearListeners() {
        listenersList = new ArrayList<>();
        logger.debug("Cleared network handler listeners");
    }
}

