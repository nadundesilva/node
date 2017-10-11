package org.microfuse.file.sharer.node.core.utils;

import org.microfuse.file.sharer.node.commons.Node;
import org.microfuse.file.sharer.node.commons.messaging.Message;
import org.microfuse.file.sharer.node.commons.messaging.MessageType;
import org.microfuse.file.sharer.node.core.communication.routing.PeerType;
import org.microfuse.file.sharer.node.core.communication.routing.Router;
import org.microfuse.file.sharer.node.core.communication.routing.RouterListener;
import org.microfuse.file.sharer.node.core.communication.routing.table.OrdinaryPeerRoutingTable;
import org.microfuse.file.sharer.node.core.communication.routing.table.RoutingTable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Overlay Network Manager.
 */
public class OverlayNetworkManager implements RouterListener {
    private static final Logger logger = LoggerFactory.getLogger(OverlayNetworkManager.class);

    private Router router;

    public OverlayNetworkManager(Router router) {
        this.router = router;
        this.router.registerListener(this);
    }

    @Override
    public void onMessageReceived(Node fromNode, Message message) {
        switch (message.getType()) {
            case REG_OK:
                handleRegOkMessage(fromNode, message);
                break;
            case UNREG_OK:
                handleUnregOkMessage(fromNode, message);
                break;
            case JOIN:
                handleJoinMessage(fromNode, message);
                break;
            case JOIN_OK:
                handleJoinOkMessage(fromNode, message);
                break;
            case LEAVE:
                handleLeaveMessage(fromNode, message);
                break;
            case LEAVE_OK:
                handleLeaveOkMessage(fromNode, message);
                break;
            case SER_SUPER_PEER_OK:
                handleSerSuperPeerOkMessage(fromNode, message);
                break;
            case JOIN_SUPER_PEER:
                handleJoinSuperPeerMessage(fromNode, message);
                break;
            case JOIN_SUPER_PEER_OK:
                handleJoinSuperPeerOkMessage(fromNode, message);
                break;
            default:
                logger.debug("Message " + message.toString() + " of unrecognized type ignored ");
        }
    }

    /**
     * Register the current node in the bootstrap server.
     */
    public void register() {
        Message regMessage = new Message();
        regMessage.setType(MessageType.REG);
        regMessage.setData(MessageIndexes.REG_IP, ServiceHolder.getConfiguration().getIp());
        regMessage.setData(MessageIndexes.REG_PORT,
                Integer.toString(ServiceHolder.getConfiguration().getPeerListeningPort()));
        regMessage.setData(MessageIndexes.REG_USERNAME, ServiceHolder.getConfiguration().getUsername());
        router.sendMessage(ServiceHolder.getConfiguration().getBootstrapServer(), regMessage);
    }

    /**
     * Unregister the current node from the bootstrap server.
     */
    public void unregister() {
        Message unregMessage = new Message();
        unregMessage.setType(MessageType.UNREG);
        unregMessage.setData(MessageIndexes.UNREG_IP, ServiceHolder.getConfiguration().getIp());
        unregMessage.setData(MessageIndexes.UNREG_PORT,
                Integer.toString(ServiceHolder.getConfiguration().getPeerListeningPort()));
        unregMessage.setData(MessageIndexes.UNREG_USERNAME, ServiceHolder.getConfiguration().getUsername());
        router.sendMessage(ServiceHolder.getConfiguration().getBootstrapServer(), unregMessage);
    }

    /**
     * Join the system.
     *
     * @param nodes The nodes to connect to
     */
    private void join(List<Node> nodes) {
        nodes.forEach(node -> {
            Message joinMessage = new Message();
            joinMessage.setType(MessageType.JOIN);
            joinMessage.setData(MessageIndexes.JOIN_IP, ServiceHolder.getConfiguration().getIp());
            joinMessage.setData(MessageIndexes.JOIN_PORT,
                    Integer.toString(ServiceHolder.getConfiguration().getPeerListeningPort()));
            router.sendMessage(node, joinMessage);
        });
    }

    /**
     * Leave the system.
     */
    public void leave() {
        router.getRoutingTable().getAll().forEach(node -> {
            Message leaveMessage = new Message();
            leaveMessage.setType(MessageType.LEAVE);
            leaveMessage.setData(MessageIndexes.LEAVE_IP, ServiceHolder.getConfiguration().getIp());
            leaveMessage.setData(MessageIndexes.LEAVE_PORT,
                    Integer.toString(ServiceHolder.getConfiguration().getPeerListeningPort()));
            router.sendMessage(node, leaveMessage);
        });
    }

    /**
     * Heartbeat to all nodes.
     */
    public void heartBeat() {
        router.enableHeartBeat();
    }

    /**
     * Enable heart beating.
     */
    public void enableHeartBeat() {
        router.disableHeartBeat();
    }

    /**
     * Search for a super peer in the system.
     */
    private void searchForSuperPeer() {
        RoutingTable routingTable = router.getRoutingTable();
        if (routingTable instanceof OrdinaryPeerRoutingTable) {
            OrdinaryPeerRoutingTable ordinaryPeerRoutingTable = (OrdinaryPeerRoutingTable) routingTable;
            if (ordinaryPeerRoutingTable.getAssignedSuperPeer() != null) {
                Message searchSuperPeerMessage = new Message();
                searchSuperPeerMessage.setType(MessageType.SER_SUPER_PEER);
                searchSuperPeerMessage.setData(MessageIndexes.SER_SUPER_PEER_SOURCE_IP,
                        ServiceHolder.getConfiguration().getIp());
                searchSuperPeerMessage.setData(MessageIndexes.SER_SUPER_PEER_SOURCE_PORT,
                        Integer.toString(ServiceHolder.getConfiguration().getPeerListeningPort()));
                searchSuperPeerMessage.setData(MessageIndexes.SER_SUPER_PEER_HOP_COUNT,
                        Integer.toString(Constants.INITIAL_HOP_COUNT));
                router.route(searchSuperPeerMessage);
            }
        }
    }

    /**
     * Self assign current node as super peer.
     */
    private void selfAssignSuperPeer() {

    }

    /**
     * Handle REG_OK type messages.
     *
     * @param fromNode The node from which the message was received
     * @param message  The message received
     */
    private void handleRegOkMessage(Node fromNode, Message message) {
        switch (message.getData(MessageIndexes.REG_OK_NODES_COUNT)) {
            case MessageConstants.REG_OK_NODE_COUNT_VALUE_ERROR:
                logger.error("Unknown error in registering with bootstrap server "
                        + ServiceHolder.getConfiguration().getBootstrapServerIP() + ":"
                        + ServiceHolder.getConfiguration().getPeerListeningPort());
                break;
            case MessageConstants.REG_OK_NODE_COUNT_VALUE_ERROR_ALREADY_REGISTERED:
                logger.warn("Current node already registered to bootstrap server "
                        + ServiceHolder.getConfiguration().getBootstrapServerIP() + ":"
                        + ServiceHolder.getConfiguration().getPeerListeningPort());
                break;
            case MessageConstants.REG_OK_NODE_COUNT_VALUE_ERROR_ALREADY_OCCUPIED:
                logger.warn("Already registered to bootstrap server "
                        + ServiceHolder.getConfiguration().getBootstrapServerIP() + ":"
                        + ServiceHolder.getConfiguration().getPeerListeningPort());

                // Retrying
                ServiceHolder.getConfiguration()
                        .setPeerListeningPort(ServiceHolder.getConfiguration().getPeerListeningPort() + 1);
                logger.debug("Changing peer listening port to "
                        + ServiceHolder.getConfiguration().getPeerListeningPort() + " and retrying");
                register();
                break;
            case MessageConstants.REG_OK_NODE_COUNT_VALUE_ERROR_FULL:
                logger.warn("Bootstrap server "
                        + ServiceHolder.getConfiguration().getBootstrapServerIP() + ":"
                        + ServiceHolder.getConfiguration().getPeerListeningPort() + " full");
                break;
            default:
                int nodesCount = Integer.parseInt(message.getData(MessageIndexes.REG_OK_NODES_COUNT));
                if (nodesCount > 0) {
                    List<Node> nodesList = new ArrayList<>();
                    if (nodesCount <= 2) {
                        for (int i = 0; i < nodesCount * 2; i += 2) {
                            int messageIndex = MessageIndexes.REG_OK_IP_PORT_START + i;
                            Node node = new Node();
                            node.setIp(message.getData(messageIndex));
                            node.setPort(message.getData(messageIndex));
                            nodesList.add(node);
                        }
                    } else {
                        for (int i = 0; i < 2; i++) {
                            int messageIndex = MessageIndexes.REG_OK_IP_PORT_START
                                    + ThreadLocalRandom.current().nextInt(0, nodesCount);
                            Node node = new Node();
                            node.setIp(message.getData(messageIndex));
                            node.setPort(message.getData(messageIndex));
                            nodesList.add(node);
                        }
                    }

                    if (nodesList.size() > 0) {
                        join(nodesList);
                    } else {
                        selfAssignSuperPeer();
                    }
                }
        }
    }

    /**
     * Handle UNREG_OK type messages.
     *
     * @param fromNode The node from which the message was received
     * @param message  The message received
     */
    private void handleUnregOkMessage(Node fromNode, Message message) {
        if (Objects.equals(message.getData(MessageIndexes.UNREG_OK_VALUE), MessageConstants.UNREG_OK_VALUE_SUCCESS)) {
            logger.info("Successfully unregistered from bootstrap server");
        } else if (Objects.equals(message.getData(MessageIndexes.UNREG_OK_VALUE),
                MessageConstants.UNREG_OK_VALUE_ERROR)) {
            logger.warn("Failed to create unstructured connection with " + fromNode.toString());
        } else {
            logger.debug("Unknown value " + message.getData(MessageIndexes.UNREG_OK_VALUE) + " in message \""
                    + message.toString() + "\"");
        }
    }

    /**
     * Handle JOIN type message.
     *
     * @param fromNode The node from which the message was received
     * @param message  The message received
     */
    private void handleJoinMessage(Node fromNode, Message message) {
        Node node = new Node();
        node.setIp(message.getData(MessageIndexes.JOIN_IP));
        node.setPort(message.getData(MessageIndexes.JOIN_PORT));
        node.setAlive(true);

        boolean isSuccessful = router.getRoutingTable().addUnstructuredNetworkRoutingTableEntry(node);

        Message replyMessage = new Message();
        replyMessage.setType(MessageType.JOIN_OK);
        replyMessage.setData(
                MessageIndexes.JOIN_OK_VALUE,
                (isSuccessful ? MessageConstants.JOIN_OK_VALUE_SUCCESS : MessageConstants.JOIN_OK_VALUE_ERROR)
        );
        replyMessage.setData(MessageIndexes.JOIN_OK_IP, ServiceHolder.getConfiguration().getIp());
        replyMessage.setData(MessageIndexes.JOIN_OK_PORT,
                Integer.toString(ServiceHolder.getConfiguration().getPeerListeningPort()));
        router.sendMessage(node, replyMessage);
    }

    /**
     * Handle JOIN_OK type message.
     *
     * @param fromNode The node from which the message was received
     * @param message  The message received
     */
    private void handleJoinOkMessage(Node fromNode, Message message) {
        if (Objects.equals(message.getData(MessageIndexes.JOIN_OK_VALUE), MessageConstants.JOIN_OK_VALUE_SUCCESS)) {
            Node node = new Node();
            node.setIp(message.getData(MessageIndexes.JOIN_OK_IP));
            node.setPort(message.getData(MessageIndexes.JOIN_OK_PORT));

            router.getRoutingTable().addUnstructuredNetworkRoutingTableEntry(node);
            searchForSuperPeer();
        } else if (Objects.equals(message.getData(MessageIndexes.JOIN_OK_VALUE),
                MessageConstants.JOIN_OK_VALUE_ERROR)) {
            logger.warn("Failed to create unstructured connection with " + fromNode.toString());
        } else {
            logger.debug("Unknown value " + message.getData(MessageIndexes.JOIN_OK_VALUE) + " in message \""
                    + message.toString() + "\"");
        }
    }

    /**
     * Handle LEAVE type messages.
     *
     * @param fromNode The node from which the message was received
     * @param message  The message received
     */
    private void handleLeaveMessage(Node fromNode, Message message) {
        Node node = new Node();
        node.setIp(message.getData(MessageIndexes.LEAVE_IP));
        node.setPort(message.getData(MessageIndexes.LEAVE_PORT));
        node.setAlive(false);

        boolean isSuccessful = router.getRoutingTable().removeFromAll(node);

        Message replyMessage = new Message();
        replyMessage.setType(MessageType.LEAVE_OK);
        replyMessage.setData(
                MessageIndexes.LEAVE_OK_VALUE,
                (isSuccessful ? MessageConstants.LEAVE_OK_VALUE_SUCCESS : MessageConstants.LEAVE_OK_VALUE_ERROR)
        );
        router.sendMessage(node, replyMessage);
    }

    /**
     * Handle LEAVE_OK type messages.
     *
     * @param fromNode The node from which the message was received
     * @param message  The message received
     */
    private void handleLeaveOkMessage(Node fromNode, Message message) {
        if (Objects.equals(message.getData(MessageIndexes.LEAVE_OK_VALUE), MessageConstants.LEAVE_OK_VALUE_SUCCESS)) {
            router.getRoutingTable().removeFromAll(fromNode);
        } else if (Objects.equals(message.getData(MessageIndexes.LEAVE_OK_VALUE),
                MessageConstants.LEAVE_OK_VALUE_ERROR)) {
            logger.warn("Failed to disconnect unstructured connection with " + fromNode.toString());
        } else {
            logger.debug("Unknown value " + message.getData(MessageIndexes.LEAVE_OK_VALUE) + " in message \""
                    + message.toString() + "\"");
        }
    }

    /**
     * Handle SER_SUPER_PEER_OK type messages.
     *
     * @param fromNode The node from which the message was received
     * @param message  The message received
     */
    private void handleSerSuperPeerOkMessage(Node fromNode, Message message) {
        if (Objects.equals(message.getData(MessageIndexes.SER_SUPER_PEER_OK_IP),
                MessageConstants.SER_SUPER_PEER_OK_NOT_FOUND_IP) ||
                Objects.equals(message.getData(MessageIndexes.SER_SUPER_PEER_OK_PORT),
                        MessageConstants.SER_SUPER_PEER_OK_NOT_FOUND_PORT)) {
            selfAssignSuperPeer();
        } else {
            Message joinMessage = new Message();
            joinMessage.setType(MessageType.JOIN_SUPER_PEER);
            joinMessage.setData(MessageIndexes.JOIN_SUPER_PEER_SOURCE_IP, ServiceHolder.getConfiguration().getIp());
            joinMessage.setData(MessageIndexes.JOIN_SUPER_PEER_SOURCE_PORT,
                    Integer.toString(ServiceHolder.getConfiguration().getPeerListeningPort()));
            router.sendMessage(
                    message.getData(MessageIndexes.SER_SUPER_PEER_OK_IP),
                    Integer.parseInt(message.getData(MessageIndexes.SER_SUPER_PEER_OK_PORT)),
                    joinMessage
            );
        }
    }

    /**
     * Handle JOIN_SUPER_PEER type messages.
     *
     * @param fromNode The node from which the message was received
     * @param message  The message received
     */
    private void handleJoinSuperPeerMessage(Node fromNode, Message message) {
        Message replyMessage = new Message();
        replyMessage.setType(MessageType.JOIN_SUPER_PEER_OK);
        if (ServiceHolder.getPeerType() == PeerType.SUPER_PEER) {

        } else {
            replyMessage.setData(MessageIndexes.JOIN_SUPER_PEER_OK_VALUE,
                    MessageConstants.JOIN_SUPER_PEER_OK_VALUE_ERROR_NOT_SUPER_PEER);
        }
        router.sendMessage(
                message.getData(MessageIndexes.JOIN_SUPER_PEER_SOURCE_IP),
                Integer.parseInt(message.getData(MessageIndexes.JOIN_SUPER_PEER_SOURCE_PORT)),
                replyMessage
        );
    }

    /**
     * Handle JOIN_SUPER_PEER_OK type messages.
     *
     * @param fromNode The node from which the message was received
     * @param message  The message received
     */
    private void handleJoinSuperPeerOkMessage(Node fromNode, Message message) {

    }
}
