package org.microfuse.file.sharer.node.core.communication.routing.strategy;

import org.microfuse.file.sharer.node.commons.peer.Node;
import org.microfuse.file.sharer.node.core.BaseTestCase;
import org.microfuse.file.sharer.node.core.communication.messaging.Message;
import org.microfuse.file.sharer.node.core.communication.routing.table.OrdinaryPeerRoutingTable;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.HashSet;
import java.util.Set;

/**
 * Test Case for org.microfuse.file.sharer.node.core.communication.routing.strategy.UnstructuredFloodingRoutingStrategy
 * class.
 */
public class UnstructuredFloodingRoutingStrategyTestCase extends BaseTestCase {
    private static final Logger logger = LoggerFactory.getLogger(UnstructuredFloodingRoutingStrategyTestCase.class);

    private OrdinaryPeerRoutingTable routingTable;
    private UnstructuredFloodingRoutingStrategy unstructuredFloodingRoutingStrategy;
    private Message message;
    private Node fromNode;
    private Node node1;
    private Node node2;
    private Node node3;

    @BeforeMethod
    public void initializeMethod() {
        logger.info("Initializing Unstructured Flooding Routing Strategy Test");

        unstructuredFloodingRoutingStrategy = new UnstructuredFloodingRoutingStrategy(serviceHolder);

        routingTable = Mockito.spy(new OrdinaryPeerRoutingTable(serviceHolder));
        message = Message.parse("0047 SER 129.82.62.142 5070 0 \"Lord of the Rings\" 0");

        fromNode = Mockito.mock(Node.class);
        node1 = Mockito.mock(Node.class);
        node2 = Mockito.mock(Node.class);
        node3 = Mockito.mock(Node.class);

        Set<Node> unstructuredNetworkNodes = new HashSet<>();
        unstructuredNetworkNodes.add(fromNode);
        Mockito.when(fromNode.isActive()).thenReturn(true);
        unstructuredNetworkNodes.add(node1);
        Mockito.when(node1.isActive()).thenReturn(true);
        unstructuredNetworkNodes.add(node2);
        Mockito.when(node2.isActive()).thenReturn(true);
        unstructuredNetworkNodes.add(node3);
        Mockito.when(node3.isActive()).thenReturn(true);
        Mockito.when(routingTable.getAllUnstructuredNetworkNodes())
                .thenReturn(unstructuredNetworkNodes);
    }

    @Test(priority = 1)
    public void testName() {
        logger.info("Running Unstructured Flooding Routing Strategy Test 01 - Get name");

        Assert.assertNotNull(unstructuredFloodingRoutingStrategy.getName());
    }

    @Test(priority = 2)
    public void testGetForwardingNodes() {
        logger.info("Running Unstructured Flooding Routing Strategy Test 02 - Get forwarding nodes");

        Set<Node> forwardingNodes = unstructuredFloodingRoutingStrategy.getForwardingNodes(routingTable,
                fromNode, message);

        Assert.assertEquals(forwardingNodes.size(), 3);
        Assert.assertTrue(forwardingNodes.contains(node1));
        Assert.assertTrue(forwardingNodes.contains(node2));
        Assert.assertTrue(forwardingNodes.contains(node3));
    }

    @Test(priority = 3)
    public void testGetForwardingNodesInStartingNode() {
        logger.info("Running Unstructured Flooding Routing Strategy Test 03 - Get forwarding nodes " +
                "in the starting node");

        Set<Node> forwardingNodes = unstructuredFloodingRoutingStrategy.getForwardingNodes(routingTable,
                null, message);

        Assert.assertEquals(forwardingNodes.size(), 4);
        Assert.assertTrue(forwardingNodes.contains(fromNode));
        Assert.assertTrue(forwardingNodes.contains(node1));
        Assert.assertTrue(forwardingNodes.contains(node2));
        Assert.assertTrue(forwardingNodes.contains(node3));
    }

    @Test(priority = 4)
    public void testGetForwardingNodesWithDeadNodes() {
        logger.info("Running Unstructured Flooding Routing Strategy Test 04 - Get forwarding nodes " +
                "with dead nodes");

        Mockito.when(node1.isActive()).thenReturn(false);

        Set<Node> forwardingNodes = unstructuredFloodingRoutingStrategy.getForwardingNodes(routingTable,
                fromNode, message);

        Assert.assertEquals(forwardingNodes.size(), 2);
        Assert.assertTrue(forwardingNodes.contains(node2));
        Assert.assertTrue(forwardingNodes.contains(node3));
    }
}
