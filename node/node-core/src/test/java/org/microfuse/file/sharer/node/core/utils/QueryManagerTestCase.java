package org.microfuse.file.sharer.node.core.utils;

import org.microfuse.file.sharer.node.commons.Configuration;
import org.microfuse.file.sharer.node.commons.communication.messaging.MessageIndexes;
import org.microfuse.file.sharer.node.commons.communication.messaging.MessageType;
import org.microfuse.file.sharer.node.commons.peer.NodeConstants;
import org.microfuse.file.sharer.node.core.BaseTestCase;
import org.microfuse.file.sharer.node.core.communication.messaging.Message;
import org.microfuse.file.sharer.node.core.communication.network.UDPSocketNetworkHandler;
import org.microfuse.file.sharer.node.core.communication.routing.Router;
import org.microfuse.file.sharer.node.core.communication.routing.strategy.UnstructuredFloodingRoutingStrategy;
import org.mockito.Mockito;
import org.mockito.internal.util.reflection.Whitebox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.List;

/**
 * Test Case for org.microfuse.file.sharer.node.core.utils.NodeManager class.
 */
public class QueryManagerTestCase extends BaseTestCase {
    private static final Logger logger = LoggerFactory.getLogger(QueryManagerTestCase.class);

    private Router router;
    private QueryManager queryManager;

    @BeforeMethod
    public void initializeMethod() {
        logger.info("Initializing Query Manager Test");

        router = Mockito.spy(new Router(
                new UDPSocketNetworkHandler(serviceHolder),
                new UnstructuredFloodingRoutingStrategy(serviceHolder),
                serviceHolder
        ));
        Whitebox.setInternalState(serviceHolder, "router", router);
        queryManager = new QueryManager(serviceHolder);
    }

    @AfterMethod
    public void cleanUp() {
        logger.info("Cleaning Up Query Manager Test");

        router.shutdown();
    }

    @Test(priority = 1)
    public void testConstructor() {
        logger.info("Running Query Manager Test 01 - Constructor");

        Object internalStateRouter = Whitebox.getInternalState(serviceHolder, "router");
        Assert.assertNotNull(internalStateRouter);
        Assert.assertTrue(internalStateRouter == router);

        Object internalStateListenersList = Whitebox.getInternalState(router, "listenersList");
        Assert.assertTrue(internalStateListenersList instanceof List<?>);
        List<?> listenersList = (List<?>) internalStateListenersList;
        Assert.assertEquals(listenersList.size(), 1);
        Assert.assertTrue(listenersList.get(0) == queryManager);
    }

    @Test(priority = 2)
    public void testQuery() {
        logger.info("Running Query Manager Test 02 - Query");

        queryManager.query("Cars");

        Configuration configuration = serviceHolder.getConfiguration();
        Message usedMessage = new Message();
        usedMessage.setType(MessageType.SER);
        usedMessage.setData(MessageIndexes.SER_SOURCE_IP, configuration.getIp());
        usedMessage.setData(MessageIndexes.SER_SOURCE_PORT, Integer.toString(configuration.getPeerListeningPort()));
        usedMessage.setData(MessageIndexes.SER_SEQUENCE_NUMBER, "0");
        usedMessage.setData(MessageIndexes.SER_HOP_COUNT, Integer.toString(NodeConstants.INITIAL_HOP_COUNT + 1));
        usedMessage.setData(MessageIndexes.SER_QUERY, "Cars");

        Mockito.verify(router, Mockito.times(1)).route(usedMessage);
    }
}
