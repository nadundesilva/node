package org.microfuse.file.sharer.node.core;

import org.microfuse.file.sharer.bootstrap.BootstrapServer;
import org.microfuse.file.sharer.node.commons.Constants;
import org.microfuse.file.sharer.node.commons.communication.network.NetworkHandlerType;
import org.microfuse.file.sharer.node.commons.communication.routing.strategy.RoutingStrategyType;
import org.microfuse.file.sharer.node.commons.peer.Node;
import org.microfuse.file.sharer.node.core.communication.routing.table.OrdinaryPeerRoutingTable;
import org.microfuse.file.sharer.node.core.communication.routing.table.RoutingTable;
import org.microfuse.file.sharer.node.core.communication.routing.table.SuperPeerRoutingTable;
import org.microfuse.file.sharer.node.core.resource.AggregatedResource;
import org.microfuse.file.sharer.node.core.resource.Resource;
import org.microfuse.file.sharer.node.core.resource.index.ResourceIndex;
import org.microfuse.file.sharer.node.core.resource.index.SuperPeerResourceIndex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Test Case for querying.
 */
public class QueryingTestCase extends BaseTestCase {
    private static final Logger logger = LoggerFactory.getLogger(QueryingTestCase.class);

    private int delay;
    private int fileSharer1Port;
    private String localhostIP;
    private BootstrapServer bootstrapServer;
    private FileSharer[] fileSharers;

    @BeforeMethod
    public void initializeMethod() {
        logger.info("Initializing Querying Test");

        delay = 1000;
        fileSharer1Port = 29871;
        localhostIP = "127.0.0.1";
        bootstrapServer = new BootstrapServer();

        fileSharers = new FileSharer[10];
        for (int i = 0; i < fileSharers.length; i++) {
            fileSharers[i] = new FileSharer();
            fileSharers[i].getServiceHolder().getConfiguration().setIp(localhostIP);
            fileSharers[i].getServiceHolder().getConfiguration().setPeerListeningPort(fileSharer1Port + i);
            fileSharers[i].getServiceHolder().getConfiguration().setTimeToLive(10);
            fileSharers[i].getServiceHolder().getConfiguration().setMaxAssignedOrdinaryPeerCount(2);
            fileSharers[i].getServiceHolder().getConfiguration().setNetworkHandlerType(NetworkHandlerType.TCP_SOCKET);
        }

        // Registering resources
        ResourceIndex resourceIndices1 = fileSharers[0].getServiceHolder().getResourceIndex();
        resourceIndices1.addOwnedResource("Lord of the Rings 2", null);
        resourceIndices1.addOwnedResource("Cars", null);
        resourceIndices1.addOwnedResource("Iron Man", null);

        ResourceIndex resourceIndices2 = fileSharers[1].getServiceHolder().getResourceIndex();
        resourceIndices2.addOwnedResource("Lord of the Rings", null);
        resourceIndices2.addOwnedResource("Iron Man 2", null);
        resourceIndices2.addOwnedResource("Spider Man", null);

        ResourceIndex resourceIndices3 = fileSharers[2].getServiceHolder().getResourceIndex();
        resourceIndices3.addOwnedResource("Hotel Transylvania", null);
        resourceIndices3.addOwnedResource("How to train your Dragon", null);
        resourceIndices3.addOwnedResource("The Bounty Hunter", null);

        ResourceIndex resourceIndices4 = fileSharers[3].getServiceHolder().getResourceIndex();
        resourceIndices4.addOwnedResource("Leap Year", null);
        resourceIndices4.addOwnedResource("Amazing Spider Man", null);
        resourceIndices4.addOwnedResource("Two weeks Notice", null);

        ResourceIndex resourceIndices5 = fileSharers[4].getServiceHolder().getResourceIndex();
        resourceIndices5.addOwnedResource("Me Before You", null);
        resourceIndices5.addOwnedResource("Endless Love", null);
        resourceIndices5.addOwnedResource("Life as we know it", null);

        ResourceIndex resourceIndices6 = fileSharers[5].getServiceHolder().getResourceIndex();
        resourceIndices6.addOwnedResource("How do you know", null);
        resourceIndices6.addOwnedResource("The Last Song", null);
        resourceIndices6.addOwnedResource("Thor", null);

        ResourceIndex resourceIndices7 = fileSharers[6].getServiceHolder().getResourceIndex();
        resourceIndices7.addOwnedResource("X-Men Origins", null);
        resourceIndices7.addOwnedResource("Cars", null);
        resourceIndices7.addOwnedResource("Captain America", null);

        ResourceIndex resourceIndices8 = fileSharers[7].getServiceHolder().getResourceIndex();
        resourceIndices8.addOwnedResource("22 Jump Street", null);
        resourceIndices8.addOwnedResource("Iron Man 3", null);
        resourceIndices8.addOwnedResource("Lord of the Rings", null);

        ResourceIndex resourceIndices9 = fileSharers[8].getServiceHolder().getResourceIndex();
        resourceIndices9.addOwnedResource("James Bond Sky fall", null);
        resourceIndices9.addOwnedResource("Suicide Squad", null);
        resourceIndices9.addOwnedResource("Fast and Furious", null);

        ResourceIndex resourceIndices10 = fileSharers[9].getServiceHolder().getResourceIndex();
        resourceIndices10.addOwnedResource("Teenage Mutant Ninja Turtles", null);
        resourceIndices10.addOwnedResource("Underworld", null);
        resourceIndices10.addOwnedResource("Despicable Me 3", null);

        bootstrapServer.startInThread();
        waitFor(delay);

        for (int i = 0; i < fileSharers.length; i++) {
            fileSharers[i].getServiceHolder().getConfiguration().setMaxAssignedOrdinaryPeerCount(5);
            fileSharers[i].start();
            waitFor(delay);
            fileSharers[i].getServiceHolder().getOverlayNetworkManager().disableHeartBeat();
            fileSharers[i].getServiceHolder().getOverlayNetworkManager().disableGossiping();
        }

        // Fixing the network
        {
            fileSharers[0].getServiceHolder().promoteToSuperPeer();
            waitFor(delay);
            RoutingTable routingTables = fileSharers[0].getServiceHolder().getRouter().getRoutingTable();
            Assert.assertTrue(routingTables instanceof SuperPeerRoutingTable);
            SuperPeerRoutingTable superPeerRoutingTable = (SuperPeerRoutingTable) routingTables;

            superPeerRoutingTable.clear();
            superPeerRoutingTable.addUnstructuredNetworkRoutingTableEntry(localhostIP, fileSharer1Port + 1);
            superPeerRoutingTable.addUnstructuredNetworkRoutingTableEntry(localhostIP, fileSharer1Port + 2);
            superPeerRoutingTable.addSuperPeerNetworkRoutingTableEntry(localhostIP, fileSharer1Port + 3);
            superPeerRoutingTable.addSuperPeerNetworkRoutingTableEntry(localhostIP, fileSharer1Port + 6);
            superPeerRoutingTable.addAssignedOrdinaryNetworkRoutingTableEntry(localhostIP, fileSharer1Port + 1);
            superPeerRoutingTable.addAssignedOrdinaryNetworkRoutingTableEntry(localhostIP, fileSharer1Port + 2);

            fileSharers[0].getServiceHolder().getOverlayNetworkManager().gossip();
        }
        {
            fileSharers[1].getServiceHolder().demoteToOrdinaryPeer();
            waitFor(delay);
            RoutingTable routingTables = fileSharers[1].getServiceHolder().getRouter().getRoutingTable();
            Assert.assertTrue(routingTables instanceof OrdinaryPeerRoutingTable);
            OrdinaryPeerRoutingTable ordinaryPeerRoutingTable = (OrdinaryPeerRoutingTable) routingTables;

            ordinaryPeerRoutingTable.clear();
            ordinaryPeerRoutingTable.addUnstructuredNetworkRoutingTableEntry(localhostIP, fileSharer1Port);
            ordinaryPeerRoutingTable.addUnstructuredNetworkRoutingTableEntry(localhostIP, fileSharer1Port + 2);
            ordinaryPeerRoutingTable.setAssignedSuperPeer(localhostIP, fileSharer1Port);
        }
        {
            fileSharers[2].getServiceHolder().demoteToOrdinaryPeer();
            waitFor(delay);
            RoutingTable routingTables = fileSharers[2].getServiceHolder().getRouter().getRoutingTable();
            Assert.assertTrue(routingTables instanceof OrdinaryPeerRoutingTable);
            OrdinaryPeerRoutingTable ordinaryPeerRoutingTable = (OrdinaryPeerRoutingTable) routingTables;

            ordinaryPeerRoutingTable.clear();
            ordinaryPeerRoutingTable.addUnstructuredNetworkRoutingTableEntry(localhostIP, fileSharer1Port);
            ordinaryPeerRoutingTable.addUnstructuredNetworkRoutingTableEntry(localhostIP, fileSharer1Port + 1);
            ordinaryPeerRoutingTable.addUnstructuredNetworkRoutingTableEntry(localhostIP, fileSharer1Port + 3);
            ordinaryPeerRoutingTable.addUnstructuredNetworkRoutingTableEntry(localhostIP, fileSharer1Port + 5);
            ordinaryPeerRoutingTable.addUnstructuredNetworkRoutingTableEntry(localhostIP, fileSharer1Port + 8);
            ordinaryPeerRoutingTable.setAssignedSuperPeer(localhostIP, fileSharer1Port);
        }
        {
            fileSharers[3].getServiceHolder().promoteToSuperPeer();
            waitFor(delay);
            RoutingTable routingTables = fileSharers[3].getServiceHolder().getRouter().getRoutingTable();
            Assert.assertTrue(routingTables instanceof SuperPeerRoutingTable);
            SuperPeerRoutingTable superPeerRoutingTable = (SuperPeerRoutingTable) routingTables;

            superPeerRoutingTable.clear();
            superPeerRoutingTable.addUnstructuredNetworkRoutingTableEntry(localhostIP, fileSharer1Port);
            superPeerRoutingTable.addUnstructuredNetworkRoutingTableEntry(localhostIP, fileSharer1Port + 2);
            superPeerRoutingTable.addUnstructuredNetworkRoutingTableEntry(localhostIP, fileSharer1Port + 4);
            superPeerRoutingTable.addUnstructuredNetworkRoutingTableEntry(localhostIP, fileSharer1Port + 7);
            superPeerRoutingTable.addUnstructuredNetworkRoutingTableEntry(localhostIP, fileSharer1Port + 9);
            superPeerRoutingTable.addSuperPeerNetworkRoutingTableEntry(localhostIP, fileSharer1Port);
            superPeerRoutingTable.addSuperPeerNetworkRoutingTableEntry(localhostIP, fileSharer1Port + 6);
            superPeerRoutingTable.addAssignedOrdinaryNetworkRoutingTableEntry(localhostIP, fileSharer1Port + 4);
            superPeerRoutingTable.addAssignedOrdinaryNetworkRoutingTableEntry(localhostIP, fileSharer1Port + 5);

            ResourceIndex resourceIndex = fileSharers[3].getServiceHolder().getResourceIndex();
            Assert.assertTrue(resourceIndex instanceof SuperPeerResourceIndex);
            SuperPeerResourceIndex superPeerResourceIndex = (SuperPeerResourceIndex) resourceIndex;
            superPeerResourceIndex.addAllAggregatedResources(
                    fileSharers[4].getServiceHolder().getResourceIndex().getAllOwnedResources().stream()
                            .map(Resource::getName)
                            .collect(Collectors.toList()),
                    fileSharers[4].getServiceHolder().getConfiguration().getIp(),
                    fileSharers[4].getServiceHolder().getConfiguration().getPeerListeningPort()
            );
            superPeerResourceIndex.addAllAggregatedResources(
                    fileSharers[5].getServiceHolder().getResourceIndex().getAllOwnedResources().stream()
                            .map(Resource::getName)
                            .collect(Collectors.toList()),
                    fileSharers[5].getServiceHolder().getConfiguration().getIp(),
                    fileSharers[5].getServiceHolder().getConfiguration().getPeerListeningPort()
            );
        }
        {
            fileSharers[4].getServiceHolder().demoteToOrdinaryPeer();
            waitFor(delay);
            RoutingTable routingTables = fileSharers[4].getServiceHolder().getRouter().getRoutingTable();
            Assert.assertTrue(routingTables instanceof OrdinaryPeerRoutingTable);
            OrdinaryPeerRoutingTable ordinaryPeerRoutingTable = (OrdinaryPeerRoutingTable) routingTables;

            ordinaryPeerRoutingTable.clear();
            ordinaryPeerRoutingTable.addUnstructuredNetworkRoutingTableEntry(localhostIP, fileSharer1Port + 3);
            ordinaryPeerRoutingTable.addUnstructuredNetworkRoutingTableEntry(localhostIP, fileSharer1Port + 5);
            ordinaryPeerRoutingTable.setAssignedSuperPeer(localhostIP, fileSharer1Port + 3);
        }
        {
            fileSharers[5].getServiceHolder().demoteToOrdinaryPeer();
            waitFor(delay);
            RoutingTable routingTables = fileSharers[5].getServiceHolder().getRouter().getRoutingTable();
            Assert.assertTrue(routingTables instanceof OrdinaryPeerRoutingTable);
            OrdinaryPeerRoutingTable ordinaryPeerRoutingTable = (OrdinaryPeerRoutingTable) routingTables;

            ordinaryPeerRoutingTable.clear();
            ordinaryPeerRoutingTable.addUnstructuredNetworkRoutingTableEntry(localhostIP, fileSharer1Port + 2);
            ordinaryPeerRoutingTable.addUnstructuredNetworkRoutingTableEntry(localhostIP, fileSharer1Port + 4);
            ordinaryPeerRoutingTable.addUnstructuredNetworkRoutingTableEntry(localhostIP, fileSharer1Port + 7);
            ordinaryPeerRoutingTable.addUnstructuredNetworkRoutingTableEntry(localhostIP, fileSharer1Port + 8);
            ordinaryPeerRoutingTable.setAssignedSuperPeer(localhostIP, fileSharer1Port + 3);
        }
        {
            fileSharers[6].getServiceHolder().promoteToSuperPeer();
            waitFor(delay);
            RoutingTable routingTables = fileSharers[6].getServiceHolder().getRouter().getRoutingTable();
            Assert.assertTrue(routingTables instanceof SuperPeerRoutingTable);
            SuperPeerRoutingTable superPeerRoutingTable = (SuperPeerRoutingTable) routingTables;

            superPeerRoutingTable.clear();
            superPeerRoutingTable.addUnstructuredNetworkRoutingTableEntry(localhostIP, fileSharer1Port + 7);
            superPeerRoutingTable.addUnstructuredNetworkRoutingTableEntry(localhostIP, fileSharer1Port + 2);
            superPeerRoutingTable.addUnstructuredNetworkRoutingTableEntry(localhostIP, fileSharer1Port + 3);
            superPeerRoutingTable.addSuperPeerNetworkRoutingTableEntry(localhostIP, fileSharer1Port);
            superPeerRoutingTable.addSuperPeerNetworkRoutingTableEntry(localhostIP, fileSharer1Port + 3);
            superPeerRoutingTable.addSuperPeerNetworkRoutingTableEntry(localhostIP, fileSharer1Port + 9);
            superPeerRoutingTable.addAssignedOrdinaryNetworkRoutingTableEntry(localhostIP, fileSharer1Port + 7);
            superPeerRoutingTable.addAssignedOrdinaryNetworkRoutingTableEntry(localhostIP, fileSharer1Port + 8);

            ResourceIndex resourceIndex = fileSharers[6].getServiceHolder().getResourceIndex();
            Assert.assertTrue(resourceIndex instanceof SuperPeerResourceIndex);
            SuperPeerResourceIndex superPeerResourceIndex = (SuperPeerResourceIndex) resourceIndex;
            superPeerResourceIndex.addAllAggregatedResources(
                    fileSharers[7].getServiceHolder().getResourceIndex().getAllOwnedResources().stream()
                            .map(Resource::getName)
                            .collect(Collectors.toList()),
                    fileSharers[7].getServiceHolder().getConfiguration().getIp(),
                    fileSharers[7].getServiceHolder().getConfiguration().getPeerListeningPort()
            );
            superPeerResourceIndex.addAllAggregatedResources(
                    fileSharers[8].getServiceHolder().getResourceIndex().getAllOwnedResources().stream()
                            .map(Resource::getName)
                            .collect(Collectors.toList()),
                    fileSharers[8].getServiceHolder().getConfiguration().getIp(),
                    fileSharers[8].getServiceHolder().getConfiguration().getPeerListeningPort()
            );
        }
        {
            fileSharers[7].getServiceHolder().demoteToOrdinaryPeer();
            waitFor(delay);
            RoutingTable routingTables = fileSharers[7].getServiceHolder().getRouter().getRoutingTable();
            Assert.assertTrue(routingTables instanceof OrdinaryPeerRoutingTable);
            OrdinaryPeerRoutingTable ordinaryPeerRoutingTable = (OrdinaryPeerRoutingTable) routingTables;

            ordinaryPeerRoutingTable.clear();
            ordinaryPeerRoutingTable.addUnstructuredNetworkRoutingTableEntry(localhostIP, fileSharer1Port + 3);
            ordinaryPeerRoutingTable.addUnstructuredNetworkRoutingTableEntry(localhostIP, fileSharer1Port + 5);
            ordinaryPeerRoutingTable.addUnstructuredNetworkRoutingTableEntry(localhostIP, fileSharer1Port + 6);
            ordinaryPeerRoutingTable.addUnstructuredNetworkRoutingTableEntry(localhostIP, fileSharer1Port + 8);
            ordinaryPeerRoutingTable.addUnstructuredNetworkRoutingTableEntry(localhostIP, fileSharer1Port + 9);
            ordinaryPeerRoutingTable.setAssignedSuperPeer(localhostIP, fileSharer1Port + 6);
        }
        {
            fileSharers[8].getServiceHolder().demoteToOrdinaryPeer();
            waitFor(delay);
            RoutingTable routingTables = fileSharers[8].getServiceHolder().getRouter().getRoutingTable();
            Assert.assertTrue(routingTables instanceof OrdinaryPeerRoutingTable);
            OrdinaryPeerRoutingTable ordinaryPeerRoutingTable = (OrdinaryPeerRoutingTable) routingTables;

            ordinaryPeerRoutingTable.clear();
            ordinaryPeerRoutingTable.addUnstructuredNetworkRoutingTableEntry(localhostIP, fileSharer1Port + 2);
            ordinaryPeerRoutingTable.addUnstructuredNetworkRoutingTableEntry(localhostIP, fileSharer1Port + 5);
            ordinaryPeerRoutingTable.addUnstructuredNetworkRoutingTableEntry(localhostIP, fileSharer1Port + 7);
            ordinaryPeerRoutingTable.setAssignedSuperPeer(localhostIP, fileSharer1Port + 6);
        }
        {
            fileSharers[9].getServiceHolder().promoteToSuperPeer();
            waitFor(delay);
            RoutingTable routingTables = fileSharers[9].getServiceHolder().getRouter().getRoutingTable();
            Assert.assertTrue(routingTables instanceof SuperPeerRoutingTable);
            SuperPeerRoutingTable superPeerRoutingTable = (SuperPeerRoutingTable) routingTables;

            superPeerRoutingTable.clear();
            superPeerRoutingTable.addUnstructuredNetworkRoutingTableEntry(localhostIP, fileSharer1Port + 2);
            superPeerRoutingTable.addUnstructuredNetworkRoutingTableEntry(localhostIP, fileSharer1Port + 3);
            superPeerRoutingTable.addUnstructuredNetworkRoutingTableEntry(localhostIP, fileSharer1Port + 7);
            superPeerRoutingTable.addSuperPeerNetworkRoutingTableEntry(localhostIP, fileSharer1Port + 6);
        }
    }

    @AfterMethod
    public void cleanUp() {
        logger.info("Cleaning Up Querying Test");

        for (int i = 9; i >= 0; i--) {
            fileSharers[i].shutdown();
            waitFor(delay + Constants.TASK_INTERVAL + Constants.THREAD_DISABLE_TIMEOUT);
        }

        bootstrapServer.shutdown();
        waitFor(delay);
    }

    @Test(priority = 1)
    public void testQueryForExistingResource() {
        logger.info("Running Querying Test 01 - Query for existing resource");

        for (FileSharer fileSharer : fileSharers) {
            fileSharer.getServiceHolder().getConfiguration()
                    .setRoutingStrategyType(RoutingStrategyType.SUPER_PEER_FLOODING);
        }

        fileSharers[5].getServiceHolder().getQueryManager().query("Spider Man");
        waitFor(delay);

        List<AggregatedResource> resources =
                fileSharers[5].getServiceHolder().getQueryManager().getQueryResults("Spider Man");
        Assert.assertEquals(resources.size(), 1);
        Assert.assertEquals(resources.get(0).getName(), "Amazing Spider Man");
        Assert.assertEquals(resources.get(0).getNodeCount(), 1);
        Assert.assertTrue(resources.get(0).getAllNodes().contains(new Node(localhostIP, fileSharer1Port + 3)));
    }

    @Test(priority = 2)
    public void testQueryForExistingResourceWithHalfMatches() {
        logger.info("Running Querying Test 02 - Query for existing resource with half match");

        for (FileSharer fileSharer : fileSharers) {
            fileSharer.getServiceHolder().getConfiguration()
                    .setRoutingStrategyType(RoutingStrategyType.SUPER_PEER_FLOODING);
        }

        fileSharers[5].getServiceHolder().getQueryManager().query("Spider");
        waitFor(delay);

        List<AggregatedResource> resources =
                fileSharers[5].getServiceHolder().getQueryManager().getQueryResults("Spider");
        Assert.assertEquals(resources.size(), 1);
        Assert.assertEquals(resources.get(0).getName(), "Amazing Spider Man");
        Assert.assertEquals(resources.get(0).getNodeCount(), 1);
        Assert.assertTrue(resources.get(0).getAllNodes().contains(new Node(localhostIP, fileSharer1Port + 3)));
    }

    @Test(priority = 3)
    public void testQueryForExistingResourceWithMultipleCopies() {
        logger.info("Running Querying Test 03 - Query for existing resource with multiple copies");

        for (FileSharer fileSharer : fileSharers) {
            fileSharer.getServiceHolder().getConfiguration()
                    .setRoutingStrategyType(RoutingStrategyType.SUPER_PEER_FLOODING);
        }

        fileSharers[5].getServiceHolder().getQueryManager().query("Cars");
        waitFor(delay);

        List<AggregatedResource> resources =
                fileSharers[5].getServiceHolder().getQueryManager().getQueryResults("Cars");
        Assert.assertEquals(resources.size(), 1);
        Assert.assertEquals(resources.get(0).getName(), "Cars");
        Assert.assertEquals(resources.get(0).getNodeCount(), 2);
        Assert.assertTrue(resources.get(0).getAllNodes().contains(new Node(localhostIP, fileSharer1Port)));
        Assert.assertTrue(resources.get(0).getAllNodes().contains(new Node(localhostIP, fileSharer1Port + 6)));
    }

    @Test(priority = 4)
    public void testMultipleQueriesForExistingResources() {
        logger.info("Running Querying Test 03 - Multiple queries for existing resources");

        for (FileSharer fileSharer : fileSharers) {
            fileSharer.getServiceHolder().getConfiguration()
                    .setRoutingStrategyType(RoutingStrategyType.SUPER_PEER_FLOODING);
        }

        fileSharers[5].getServiceHolder().getQueryManager().query("Cars");
        fileSharers[5].getServiceHolder().getQueryManager().query("Spider Man");
        fileSharers[5].getServiceHolder().getQueryManager().query("X-Men");
        fileSharers[5].getServiceHolder().getQueryManager().query("Lord");
        fileSharers[5].getServiceHolder().getQueryManager().query("Endless Love");
        fileSharers[5].getServiceHolder().getQueryManager().query("What happens in Rome stays in Rome");
        fileSharers[5].getServiceHolder().getQueryManager().query("Hotel Transylvania");
        fileSharers[5].getServiceHolder().getQueryManager().query("Lord of the Rings");
        fileSharers[5].getServiceHolder().getQueryManager().query("Captain America");
        fileSharers[5].getServiceHolder().getQueryManager().query("Iron Man");
        waitFor(delay * 2);

        {
            List<AggregatedResource> resources =
                    fileSharers[5].getServiceHolder().getQueryManager().getQueryResults("Cars");
            Assert.assertEquals(resources.size(), 1);
            Assert.assertEquals(resources.get(0).getName(), "Cars");
            Assert.assertEquals(resources.get(0).getNodeCount(), 2);
            Assert.assertTrue(resources.get(0).getAllNodes().contains(new Node(localhostIP, fileSharer1Port)));
            Assert.assertTrue(resources.get(0).getAllNodes().contains(new Node(localhostIP, fileSharer1Port + 6)));
        }
        {
            List<AggregatedResource> resources =
                    fileSharers[5].getServiceHolder().getQueryManager().getQueryResults("Spider Man");
            Assert.assertEquals(resources.size(), 1);
            Assert.assertEquals(resources.get(0).getName(), "Amazing Spider Man");
            Assert.assertEquals(resources.get(0).getNodeCount(), 1);
            Assert.assertTrue(resources.get(0).getAllNodes().contains(new Node(localhostIP, fileSharer1Port + 3)));
        }
        {
            List<AggregatedResource> resources =
                    fileSharers[5].getServiceHolder().getQueryManager().getQueryResults("X-Men");
            Assert.assertEquals(resources.size(), 1);
            Assert.assertEquals(resources.get(0).getName(), "X-Men Origins");
            Assert.assertEquals(resources.get(0).getNodeCount(), 1);
            Assert.assertTrue(resources.get(0).getAllNodes().contains(new Node(localhostIP, fileSharer1Port + 6)));
        }
        {
            List<AggregatedResource> resources =
                    fileSharers[5].getServiceHolder().getQueryManager().getQueryResults("Lord");
            Assert.assertEquals(resources.size(), 2);
            for (int i = 0; i < 2; i++) {
                if (Objects.equals(resources.get(i).getName(), "Lord of the Rings")) {
                    Assert.assertEquals(resources.get(i).getNodeCount(), 1);
                    Assert.assertTrue(resources.get(i).getAllNodes()
                            .contains(new Node(localhostIP, fileSharer1Port + 7)));
                } else if (Objects.equals(resources.get(i).getName(), "Lord of the Rings 2")) {
                    Assert.assertEquals(resources.get(i).getNodeCount(), 1);
                    Assert.assertTrue(resources.get(i).getAllNodes().contains(new Node(localhostIP, fileSharer1Port)));
                } else {
                    Assert.fail("Expected either \"Lord of the Rings\" and \"Lord of the Rings 2\"");
                }
            }
        }
        {
            List<AggregatedResource> resources =
                    fileSharers[5].getServiceHolder().getQueryManager().getQueryResults("Endless Love");
            Assert.assertEquals(resources.size(), 1);
            Assert.assertEquals(resources.get(0).getName(), "Endless Love");
            Assert.assertEquals(resources.get(0).getNodeCount(), 1);
            Assert.assertTrue(resources.get(0).getAllNodes().contains(new Node(localhostIP, fileSharer1Port + 4)));
        }
        {
            List<AggregatedResource> resources =
                    fileSharers[5].getServiceHolder().getQueryManager()
                            .getQueryResults("What happens in Rome stays in Rome");
            Assert.assertEquals(resources.size(), 0);
        }
        {
            List<AggregatedResource> resources =
                    fileSharers[5].getServiceHolder().getQueryManager()
                            .getQueryResults("Hotel Transylvania");
            Assert.assertEquals(resources.size(), 1);
            Assert.assertEquals(resources.get(0).getName(), "Hotel Transylvania");
            Assert.assertEquals(resources.get(0).getNodeCount(), 1);
            Assert.assertTrue(resources.get(0).getAllNodes().contains(new Node(localhostIP, fileSharer1Port + 2)));
        }
        {
            List<AggregatedResource> resources =
                    fileSharers[5].getServiceHolder().getQueryManager()
                            .getQueryResults("Lord of the Rings");
            Assert.assertEquals(resources.size(), 2);
            for (int i = 0; i < 2; i++) {
                if (Objects.equals(resources.get(i).getName(), "Lord of the Rings 2")) {
                    Assert.assertEquals(resources.get(i).getNodeCount(), 1);
                    Assert.assertTrue(resources.get(i).getAllNodes().contains(new Node(localhostIP, fileSharer1Port)));
                } else if (Objects.equals(resources.get(i).getName(), "Lord of the Rings")) {
                    Assert.assertEquals(resources.get(i).getNodeCount(), 1);
                    Assert.assertTrue(resources.get(i).getAllNodes().contains(
                            new Node(localhostIP, fileSharer1Port + 7)));
                } else {
                    Assert.fail("Expected either \"Lord of the Rings\" and \"Lord of the Rings 2\"");
                }
            }
        }
        {
            List<AggregatedResource> resources =
                    fileSharers[5].getServiceHolder().getQueryManager()
                            .getQueryResults("Captain America");
            Assert.assertEquals(resources.size(), 1);
            Assert.assertEquals(resources.get(0).getName(), "Captain America");
            Assert.assertEquals(resources.get(0).getNodeCount(), 1);
            Assert.assertTrue(resources.get(0).getAllNodes().contains(new Node(localhostIP, fileSharer1Port + 6)));
        }
        {
            List<AggregatedResource> resources =
                    fileSharers[5].getServiceHolder().getQueryManager()
                            .getQueryResults("Iron Man");
            Assert.assertEquals(resources.size(), 2);
            for (int i = 0; i < 2; i++) {
                if (Objects.equals(resources.get(i).getName(), "Iron Man 3")) {
                    Assert.assertEquals(resources.get(i).getNodeCount(), 1);
                    Assert.assertTrue(resources.get(i).getAllNodes().contains(
                            new Node(localhostIP, fileSharer1Port + 7)));
                } else if (Objects.equals(resources.get(i).getName(), "Iron Man")) {
                    Assert.assertEquals(resources.get(i).getNodeCount(), 1);
                    Assert.assertTrue(resources.get(i).getAllNodes().contains(new Node(localhostIP, fileSharer1Port)));
                } else {
                    Assert.fail("Expected either \"Iron Man 3\" and \"Iron Man\"");
                }
            }
        }
    }
}
