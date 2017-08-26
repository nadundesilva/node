package org.microfuse.node.core.communication.routing;

import com.google.gson.Gson;
import org.microfuse.node.commons.Node;
import org.microfuse.node.commons.messaging.Message;
import org.microfuse.node.commons.messaging.MessageType;
import org.microfuse.node.core.Manager;
import org.microfuse.node.core.communication.network.NetworkHandler;
import org.microfuse.node.core.communication.network.NetworkHandlerListener;
import org.microfuse.node.core.communication.routing.strategy.RoutingStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Router class.
 *
 * This governs how the messages are routed through the P2P network.
 */
public class Router implements NetworkHandlerListener {
    private static final Logger logger = LoggerFactory.getLogger(Router.class);

    private List<RouterListener> listenersList;
    private RoutingTable routingTable;

    private RoutingStrategy routingStrategy;
    private NetworkHandler networkHandler;

    public Router(NetworkHandler networkHandler, RoutingStrategy routingStrategy) {
        routingTable = new RoutingTable();
        this.networkHandler = networkHandler;
        this.routingStrategy = routingStrategy;
    }

    @Override
    public void onMessageReceived(String fromAddress, String message) {
        Message msg = new Gson().fromJson(message, Message.class);

        if (msg.getType() == MessageType.DESTINATION_NOT_FOUND) {
            int msgCount = routingTable.incrementBackwardRoutingMessageCount(msg.popPathNode(), -1);
            if (msgCount == 0) {
                routeToSource(msg);
            }
        } else {
            routeToSource(msg);
        }
    }

    @Override
    public void onMessageSendFailed(String toAddress, String message) {
        Message msg = new Gson().fromJson(message, Message.class);
        int msgCount = routingTable.incrementBackwardRoutingMessageCount(msg.peekPathNode(), -1);

        if (msgCount == 0) {
            msg.setType(MessageType.DESTINATION_NOT_FOUND);
            routeToSource(msg);
        }
    }

    /**
     * Route a message through the P2P network.
     *
     * @param fromNode The node from which the message was received
     * @param message  The message to be sent
     */
    public void route(Node fromNode, Message message) {
        MessageType messageType = message.getType();

        if (messageType == MessageType.REQUEST) {
            routeToDestination(fromNode, message);
        } else if (messageType == MessageType.ACKNOWLEDGEMENT || messageType == MessageType.DESTINATION_NOT_FOUND) {
            routeToSource(message);
        }
    }

    /**
     * Route the message to the destination of the message.
     *
     * @param fromNode The node from which the message was received
     * @param message  The message to be routed
     */
    private void routeToDestination(Node fromNode, Message message) {
        int timeToLive = message.getTimeToLive();
        message.setTimeToLive(--timeToLive);

        int destinationNodeID = message.getDestinationNodeID();
        if (Manager.getConfigurationInstance().getNodeID() == destinationNodeID) {
            for (RouterListener routerListener : listenersList) {
                routerListener.onMessageReceived(message.getContent());
            }
        } else {
            Node destinationNode = routingTable.getRoutingTableNode(destinationNodeID);
            if (destinationNode != null) {
                message.pushPathNode(routingTable.putBackwardRoutingTableEntry(fromNode));
                String outputMessage = new Gson().toJson(message);

                logger.debug("Routing message to node " + destinationNode.getNodeID() + ": " + outputMessage);
                networkHandler.sendMessage(destinationNode.getAddress(), outputMessage);
            } else if (timeToLive > 0) {
                List<Node> forwardingNodes = routingStrategy.getForwardingNode(routingTable, fromNode, message);
                int pathKey = routingTable.putBackwardRoutingTableEntry(fromNode);

                for (Node forwardNode : forwardingNodes) {
                    Message clonedMessage = message.clone();
                    clonedMessage.pushPathNode(pathKey);
                    String outputMessage = new Gson().toJson(clonedMessage);

                    logger.debug("Routing message to " + forwardNode + ": " + outputMessage);
                    networkHandler.sendMessage(forwardNode.getAddress(), outputMessage);
                }
            }
        }
    }

    /**
     * Route the message to the source of the message.
     *
     * @param message The message to be routed
     */
    private void routeToSource(Message message) {
        Node destinationNode = routingTable.removeBackwardRoutingTableEntry(message.popPathNode());

        if (destinationNode != null) {
            String outputMessage = new Gson().toJson(message);

            logger.debug("Routing message to node " + destinationNode.getNodeID() + ": " + outputMessage);
            networkHandler.sendMessage(destinationNode.getAddress(), outputMessage);
        } else {
            logger.debug("Dropping unrecognized acknowledgement message: " + message.getContent());
        }
    }

    /**
     * Get the routing strategy used by this router.
     *
     * @return The routing strategy used by the router
     */
    public RoutingStrategy getRoutingStrategy() {
        return routingStrategy;
    }

    /**
     * Set the routing strategy currently used by this router.
     *
     * @param routingStrategy The routing strategy used by this router
     */
    public void setRoutingStrategy(RoutingStrategy routingStrategy) {
        this.routingStrategy = routingStrategy;
        logger.debug("Changed routing strategy to " + routingStrategy.getName());
    }

    /**
     * Get the network handler used by this router.
     *
     * @return The network handler used by this router
     */
    public NetworkHandler getNetworkHandler() {
        return networkHandler;
    }

    /**
     * Set a new Network handler.
     *
     * @param networkHandler The new network handler
     */
    public void setNetworkHandler(NetworkHandler networkHandler) {
        this.networkHandler = networkHandler;
    }

    /**
     * Register a new listener.
     *
     * @param listener The new listener to be registered
     */
    public void registerListener(RouterListener listener) {
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
    public void unregisterListener(RouterListener listener) {
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
