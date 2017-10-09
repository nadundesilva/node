package org.microfuse.file.sharer.node.core.communication.routing;

import com.google.common.collect.Lists;
import org.microfuse.file.sharer.node.commons.Node;
import org.microfuse.file.sharer.node.commons.messaging.Message;
import org.microfuse.file.sharer.node.commons.messaging.MessageType;
import org.microfuse.file.sharer.node.core.communication.network.NetworkHandler;
import org.microfuse.file.sharer.node.core.communication.network.NetworkHandlerListener;
import org.microfuse.file.sharer.node.core.communication.routing.strategy.RoutingStrategy;
import org.microfuse.file.sharer.node.core.communication.routing.table.OrdinaryPeerRoutingTable;
import org.microfuse.file.sharer.node.core.communication.routing.table.RoutingTable;
import org.microfuse.file.sharer.node.core.communication.routing.table.SuperPeerRoutingTable;
import org.microfuse.file.sharer.node.core.resource.OwnedResource;
import org.microfuse.file.sharer.node.core.utils.Constants;
import org.microfuse.file.sharer.node.core.utils.MessageConstants;
import org.microfuse.file.sharer.node.core.utils.MessageIndexes;
import org.microfuse.file.sharer.node.core.utils.ServiceHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Router class.
 * <p>
 * This governs how the messages are routed through the P2P network.
 */
public class Router implements NetworkHandlerListener {
    private static final Logger logger = LoggerFactory.getLogger(Router.class);

    private List<RouterListener> listenersList;
    private RoutingTable routingTable;

    private RoutingStrategy routingStrategy;
    private NetworkHandler networkHandler;

    private final ReadWriteLock listenersListLock;
    private final ReadWriteLock routingTableLock;
    private final ReadWriteLock routingStrategyLock;
    private final ReadWriteLock networkHandlerLock;

    public Router(NetworkHandler networkHandler, RoutingStrategy routingStrategy) {
        listenersListLock = new ReentrantReadWriteLock();
        routingTableLock = new ReentrantReadWriteLock();
        routingStrategyLock = new ReentrantReadWriteLock();
        networkHandlerLock = new ReentrantReadWriteLock();
        try {
            routingTable = PeerType.getRoutingTableClass(ServiceHolder.getPeerType()).newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            logger.error("Failed to instantiate routing table for " + ServiceHolder.getPeerType().getValue()
                    + ". Using the routing table for " + PeerType.ORDINARY_PEER.getValue() + " instead", e);
            ServiceHolder.demoteToOrdinaryPeer();
            routingTable = new OrdinaryPeerRoutingTable();
        }
        this.routingStrategy = routingStrategy;
        this.networkHandler = networkHandler;
        this.listenersList = new ArrayList<>();
        this.networkHandler.registerListener(this);
    }

    @Override
    public void onMessageReceived(String fromAddress, int fromPort, Message message) {
        logger.debug("Received message " + message.toString() + " from node " + fromAddress
                + ":" + fromAddress);
        MessageType messageType = message.getType();
        if (messageType != null && messageType == MessageType.SER) {
            Node node;
            routingTableLock.readLock().lock();
            try {
                node = routingTable.getUnstructuredNetworkRoutingTableNode(fromAddress, fromPort);
            } finally {
                routingTableLock.readLock().unlock();
            }
            if (node == null) {
                node = new Node();
                node.setIp(fromAddress);
                node.setPort(fromPort);
                node.setAlive(true);
            }
            route(node, message);
        } else {
            runTasksOnMessageReceived(message);
        }
    }

    @Override
    public void onMessageSendFailed(String toAddress, int toPort, Message message) {
        logger.debug("Sending message " + message.toString() + " to node " + toAddress + ":" + toPort + " failed");
        // Marking the node as inactive
        Node receivingNode;
        routingTableLock.readLock().lock();
        try {
            receivingNode = routingTable.getUnstructuredNetworkRoutingTableNode(toAddress, toPort);
            if (receivingNode == null && ServiceHolder.getPeerType() == PeerType.SUPER_PEER) {
                SuperPeerRoutingTable superPeerRoutingTable = (SuperPeerRoutingTable) routingTable;
                receivingNode = superPeerRoutingTable.getSuperPeerNetworkRoutingTableNode(toAddress, toPort);
                if (receivingNode == null) {
                    receivingNode = superPeerRoutingTable.getAssignedOrdinaryNetworkRoutingTableNode(toAddress, toPort);
                }
            }
        } finally {
            routingTableLock.readLock().unlock();
        }
        if (receivingNode != null) {
            receivingNode.setAlive(false);
        }
    }

    /**
     * Send a message directly to a node.
     *
     * @param toNode  The node to which the message needs to be sent
     * @param message The message to be sent
     */
    public void sendMessage(Node toNode, Message message) {
        networkHandlerLock.readLock().lock();
        try {
            logger.debug("Sending message " + message.toString() + " to node " + toNode.toString());
            networkHandler.sendMessage(toNode.getIp(), toNode.getPort(), message);
        } finally {
            networkHandlerLock.readLock().unlock();
        }
    }

    /**
     * Route a message through the P2P network.
     *
     * @param message  The message to be sent
     */
    public void route(Message message) {
        logger.debug("Routing new message " + message.toString() + " over the network.");
        route(null, message);
    }

    /**
     * Route a message through the P2P network.
     *
     * @param fromNode The node from which the message was received by this node
     * @param message  The message to be sent
     */
    private void route(Node fromNode, Message message) {
        MessageType messageType = message.getType();

        if (messageType != null && messageType == MessageType.SER) {
            // Checking owned resources
            Set<OwnedResource> ownedResources = ServiceHolder.getResourceIndex()
                    .findResources(message.getData(MessageIndexes.SER_FILE_NAME));
            if (ownedResources.size() > 0) {
                logger.debug("Resource requested by \"" + message.toString() + "\" found in owned resources");
                Message serOkMessage = new Message();
                serOkMessage.setType(MessageType.SER_OK);

                List<String> serOkData = Lists.newArrayList(
                        Integer.toString(ownedResources.size()),
                        ServiceHolder.getConfiguration().getIp(),
                        Integer.toString(ServiceHolder.getConfiguration().getPeerListeningPort()),
                        Integer.toString(Constants.INITIAL_HOP_COUNT)
                );
                ownedResources.forEach(resource -> serOkData.add(resource.getName()));

                serOkMessage.setData(serOkData);
                networkHandlerLock.readLock().lock();
                try {
                    logger.debug("Sending search request success message \"" + message.toString() + "\" back to "
                            + message.getData(MessageIndexes.SER_SOURCE_IP) + ":"
                            + message.getData(MessageIndexes.SER_SOURCE_PORT));
                    networkHandler.sendMessage(message.getData(MessageIndexes.SER_SOURCE_IP),
                            Integer.parseInt(message.getData(MessageIndexes.SER_SOURCE_PORT)), serOkMessage);
                } finally {
                    networkHandlerLock.readLock().unlock();
                }
            } else {
                logger.debug("Resource requested by \"" + message.toString() + "\" not found in owned resources");

                // Updating the hop count
                Integer hopCount = Integer.parseInt(message.getData(MessageIndexes.SER_HOP_COUNT));
                hopCount++;
                message.setData(MessageIndexes.SER_HOP_COUNT, hopCount.toString());
                logger.debug("Increased hop count of message" + message.toString());

                if (hopCount <= ServiceHolder.getConfiguration().getTimeToLive()) {
                    logger.debug("The hop count of the message " + message.toString() + " is lower than time to live"
                            + ServiceHolder.getConfiguration().getTimeToLive());

                    Set<Node> forwardingNodes;
                    routingTableLock.readLock().lock();
                    try {
                        routingStrategyLock.readLock().lock();
                        try {
                            forwardingNodes = routingStrategy.getForwardingNodes(routingTable, fromNode, message);
                        } finally {
                            routingStrategyLock.readLock().unlock();
                        }
                    } finally {
                        routingTableLock.readLock().unlock();
                    }

                    forwardingNodes.stream().parallel()
                            .forEach(forwardingNode -> {
                                Message clonedMessage = message.clone();
                                networkHandlerLock.readLock().lock();
                                try {
                                    logger.debug("Forwarding message " + clonedMessage.toString()
                                            + " to " + forwardingNode.toString());
                                    networkHandler.sendMessage(
                                            forwardingNode.getIp(), forwardingNode.getPort(), clonedMessage
                                    );
                                } finally {
                                    networkHandlerLock.readLock().unlock();
                                }
                            });
                } else {
                    logger.debug("Sending search failed back to search request source node "
                            + message.getData(MessageIndexes.SER_SOURCE_IP) + ":"
                            + message.getData(MessageIndexes.SER_SOURCE_PORT)
                            + " since the hop count of the message " + message.toString()
                            + " is higher than time to live " + ServiceHolder.getConfiguration().getTimeToLive());

                    // Unable to find resource
                    Message serOkMessage = new Message();
                    serOkMessage.setType(MessageType.SER_OK);
                    serOkMessage.setData(Lists.newArrayList(MessageConstants.SER_OK_NOT_FOUND_FILE_COUNT,
                            MessageConstants.SER_OK_NOT_FOUND_IP, MessageConstants.SER_OK_NOT_FOUND_PORT));
                    networkHandlerLock.readLock().lock();
                    try {
                        networkHandler.sendMessage(message.getData(MessageIndexes.SER_SOURCE_IP),
                                Integer.parseInt(message.getData(MessageIndexes.SER_SOURCE_PORT)), serOkMessage);
                    } finally {
                        networkHandlerLock.readLock().unlock();
                    }
                }
            }
        } else {
            logger.warn("Not routing message of type " + messageType + ". The Router will only route messages of type "
                    + MessageType.SER + ".");
        }
    }

    /**
     * Promote the current node to an ordinary peer.
     */
    public void promoteToSuperPeer() {
        routingTableLock.writeLock().lock();
        try {
            if (routingTable instanceof OrdinaryPeerRoutingTable) {
                SuperPeerRoutingTable superPeerRoutingTable = new SuperPeerRoutingTable();
                superPeerRoutingTable.setBootstrapServer(routingTable.getBootstrapServer());
                superPeerRoutingTable.addAllUnstructuredNetworkRoutingTableEntry(
                        routingTable.getAllUnstructuredNetworkRoutingTableNodes());
                routingTable = superPeerRoutingTable;
                logger.debug("Changed routing table to super peer routing table.");
            }
        } finally {
            routingTableLock.writeLock().unlock();
        }
        logger.debug("Promoted router to super peer router.");
    }

    /**
     * Demote the current node to a super peer.
     */
    public void demoteToOrdinaryPeer() {
        routingTableLock.writeLock().lock();
        try {
            if (routingTable instanceof SuperPeerRoutingTable) {
                OrdinaryPeerRoutingTable ordinaryPeerRoutingTable = new OrdinaryPeerRoutingTable();
                ordinaryPeerRoutingTable.setBootstrapServer(routingTable.getBootstrapServer());
                ordinaryPeerRoutingTable.addAllUnstructuredNetworkRoutingTableEntry(
                        routingTable.getAllUnstructuredNetworkRoutingTableNodes());
                routingTable = ordinaryPeerRoutingTable;
                logger.debug("Changed routing table to ordinary peer routing table.");
            }
        } finally {
            routingTableLock.writeLock().unlock();
        }
        logger.debug("Demoted router to ordinary peer router.");
    }

    /**
     * Change the network handler used by the router.
     *
     * @param networkHandler The network handler to be used
     */
    public void changeNetworkHandler(NetworkHandler networkHandler) {
        networkHandlerLock.writeLock().lock();
        try {
            this.networkHandler = networkHandler;
            logger.info("Network handler changed to " + this.networkHandler.getName());
        } finally {
            networkHandlerLock.writeLock().unlock();
        }
    }

    /**
     * Change the routing strategy used by the router.
     *
     * @param routingStrategy The routing strategy to be used
     */
    public void changeRoutingStrategy(RoutingStrategy routingStrategy) {
        routingStrategyLock.writeLock().lock();
        try {
            this.routingStrategy = routingStrategy;
            logger.info("Routing strategy changed to " + this.routingStrategy.getName());
        } finally {
            routingStrategyLock.writeLock().unlock();
        }
    }

    /**
     * Run tasks to be run when a message intended for this node is received.
     *
     * @param message The message that was received
     */
    public void runTasksOnMessageReceived(Message message) {
        listenersListLock.readLock().lock();
        try {
            listenersList.stream().parallel()
                    .forEach(routerListener -> routerListener.onMessageReceived(message));
        } finally {
            listenersListLock.readLock().unlock();
        }
    }

    /**
     * Get the routing table used by this router.
     *
     * @return The routing table
     */
    public RoutingTable getRoutingTable() {
        RoutingTable requestedRoutingTable;
        routingTableLock.readLock().lock();
        try {
            requestedRoutingTable = routingTable;
        } finally {
            routingTableLock.readLock().unlock();
        }
        return requestedRoutingTable;
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
     * Get the network handler used by this router.
     *
     * @return The network handler used by this router
     */
    public NetworkHandler getNetworkHandler() {
        return networkHandler;
    }

    /**
     * Register a new listener.
     *
     * @param listener The new listener to be registered
     */
    public boolean registerListener(RouterListener listener) {
        boolean isSuccessful;
        listenersListLock.writeLock().lock();
        try {
            isSuccessful = listenersList.add(listener);
            if (isSuccessful) {
                logger.debug("Registered router listener " + listener.getClass());
            } else {
                logger.debug("Failed to register router listener " + listener.getClass());
            }
        } finally {
            listenersListLock.writeLock().unlock();
        }
        return isSuccessful;
    }

    /**
     * Unregister an existing listener.
     *
     * @param listener The listener to be removed
     */
    public boolean unregisterListener(RouterListener listener) {
        boolean isSuccessful;
        listenersListLock.writeLock().lock();
        try {
            isSuccessful = listenersList.remove(listener);
            if (isSuccessful) {
                logger.debug("Unregistered router listener " + listener.getClass());
            } else {
                logger.debug("Failed to unregister router listener " + listener.getClass());
            }
        } finally {
            listenersListLock.writeLock().unlock();
        }
        return isSuccessful;
    }

    /**
     * Unregister all existing listener.
     */
    public void clearListeners() {
        listenersListLock.writeLock().lock();
        try {
            listenersList.clear();
            logger.debug("Cleared network handler listeners");
        } finally {
            listenersListLock.writeLock().lock();
        }
    }
}
