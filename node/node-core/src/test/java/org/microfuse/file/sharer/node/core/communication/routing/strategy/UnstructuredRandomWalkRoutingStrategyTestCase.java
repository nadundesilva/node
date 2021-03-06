package org.microfuse.file.sharer.node.core.communication.routing.strategy;

import org.microfuse.file.sharer.node.commons.peer.Node;
import org.microfuse.file.sharer.node.core.BaseTestCase;
import org.microfuse.file.sharer.node.core.communication.messaging.Message;
import org.microfuse.file.sharer.node.core.communication.routing.table.OrdinaryPeerRoutingTable;
import org.microfuse.file.sharer.node.core.communication.routing.table.RoutingTable;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.HashSet;
import java.util.Set;

/**
 * Test Case for
 * org.microfuse.file.sharer.node.core.communication.routing.strategy.UnstructuredRandomWalkRoutingStrategy class.
 */
public class UnstructuredRandomWalkRoutingStrategyTestCase extends BaseTestCase {
    private static final Logger logger = LoggerFactory.getLogger(UnstructuredRandomWalkRoutingStrategyTestCase.class);

    private RoutingTable routingTable;
    private UnstructuredRandomWalkRoutingStrategy unstructuredRandomWalkRoutingStrategy;
    private Message message;
    private Node fromNode;
    private Node node1;
    private Node node2;
    private Node node3;

    @BeforeMethod
    public void initializeMethod() {
        logger.info("Initializing Unstructured Random Walk Routing Strategy Test");

        unstructuredRandomWalkRoutingStrategy = new UnstructuredRandomWalkRoutingStrategy(serviceHolder);

        routingTable = Mockito.spy(new OrdinaryPeerRoutingTable(serviceHolder));
        message = Message.parse("0047 SER 129.82.62.142 5070 0 \"Lord of the Rings\" 0");

        fromNode = Mockito.mock(Node.class);
        node1 = Mockito.mock(Node.class);
        Mockito.when(node1.isActive()).thenReturn(true);
        node2 = Mockito.mock(Node.class);
        Mockito.when(node2.isActive()).thenReturn(true);
        node3 = Mockito.mock(Node.class);
        Mockito.when(node3.isActive()).thenReturn(true);

        Set<Node> unstructuredNetworkNodes = new HashSet<>();
        unstructuredNetworkNodes.add(fromNode);
        unstructuredNetworkNodes.add(node1);
        unstructuredNetworkNodes.add(node2);
        unstructuredNetworkNodes.add(node3);
        Mockito.when(routingTable.getAllUnstructuredNetworkNodes())
                .thenReturn(unstructuredNetworkNodes);
    }

    @Test(priority = 1)
    public void testName() {
        logger.info("Running Unstructured Random Walk Routing Strategy Test 01 - Get name");

        Assert.assertNotNull(unstructuredRandomWalkRoutingStrategy.getName());
    }

    @Test(priority = 2)
    public void testGetForwardingNodes() {
        logger.info("Running Unstructured Random Walk Routing Strategy Test 02 - Get forwarding nodes");

        Set<Node> forwardingNodes = unstructuredRandomWalkRoutingStrategy.getForwardingNodes(routingTable,
                fromNode, message);

        Assert.assertEquals(forwardingNodes.size(), 1);
        Assert.assertTrue(forwardingNodes.contains(node1) || forwardingNodes.contains(node2) ||
                forwardingNodes.contains(node3));
    }

    @Test(priority = 3)
    public void testGetForwardingNodesInStartingNode() {
        logger.info("Running Unstructured Random Walk Routing Strategy Test 03 - Get forwarding nodes " +
                "in the starting node");

        Set<Node> forwardingNodes = unstructuredRandomWalkRoutingStrategy.getForwardingNodes(routingTable,
                null, message);

        Assert.assertEquals(forwardingNodes.size(), 1);
        Assert.assertTrue(forwardingNodes.contains(fromNode) || forwardingNodes.contains(node1) ||
                forwardingNodes.contains(node2) || forwardingNodes.contains(node3));
    }

    @Test(priority = 4)
    public void testGetForwardingNodesWithDeadNodes() {
        logger.info("Running Unstructured Random Walk Routing Strategy Test 04 - Get forwarding nodes " +
                "with dead nodes");

        Mockito.when(node1.isActive()).thenReturn(false);

        Set<Node> forwardingNodes = unstructuredRandomWalkRoutingStrategy.getForwardingNodes(routingTable,
                fromNode, message);

        Assert.assertEquals(forwardingNodes.size(), 1);
        Assert.assertTrue(forwardingNodes.contains(node2) || forwardingNodes.contains(node3));
    }
}
