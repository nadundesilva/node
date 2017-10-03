package org.microfuse.file.sharer.node.core.communication.routing.strategy;

import org.microfuse.file.sharer.node.commons.Node;
import org.microfuse.file.sharer.node.commons.messaging.Message;
import org.microfuse.file.sharer.node.core.communication.routing.table.RoutingTable;

import java.util.Set;

/**
 * Routing strategy base class.
 */
public interface RoutingStrategy {
    /**
     * Get the name of the routing strategy.
     *
     * @return The name of the routing strategy
     */
    String getName();

    /**
     * Get the forwarding node based on the routing strategy.
     * The message is forwarded to all the nodes returned from this message.
     *
     * @param routingTable The routing table in the router
     * @param fromNode     The node from which this node received the message
     * @param message      The message to be routed
     * @return The forwarding nodes list
     */
    Set<Node> getForwardingNodes(RoutingTable routingTable, Node fromNode, Message message);
}
