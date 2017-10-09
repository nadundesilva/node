package org.microfuse.file.sharer.node.core.utils;

import org.microfuse.file.sharer.node.commons.Configuration;
import org.microfuse.file.sharer.node.commons.messaging.Message;
import org.microfuse.file.sharer.node.commons.messaging.MessageType;
import org.microfuse.file.sharer.node.core.communication.routing.Router;
import org.microfuse.file.sharer.node.core.communication.routing.RouterListener;

/**
 * Query Manager.
 */
public class QueryManager implements RouterListener {
    private Router router;

    public QueryManager(Router router) {
        this.router = router;
        this.router.registerListener(this);
    }

    @Override
    public void onMessageReceived(Message message) {

    }

    /**
     * Query the file sharer system for a file.
     *
     * @param fileName The name of the file to be queried for
     */
    public void query(String fileName) {
        Configuration configuration = ServiceHolder.getConfiguration();
        Message message = new Message();
        message.setType(MessageType.SER);
        message.setData(MessageIndexes.SER_SOURCE_IP, configuration.getIp());
        message.setData(MessageIndexes.SER_SOURCE_IP, Integer.toString(configuration.getPeerListeningPort()));
        message.setData(MessageIndexes.SER_FILE_NAME, fileName);
        message.setData(MessageIndexes.SER_HOP_COUNT, Integer.toOctalString(Constants.INITIAL_HOP_COUNT));
        router.route(message);
    }
}
