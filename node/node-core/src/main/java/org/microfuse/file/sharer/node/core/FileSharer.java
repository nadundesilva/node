package org.microfuse.file.sharer.node.core;

import org.microfuse.file.sharer.node.commons.Constants;
import org.microfuse.file.sharer.node.core.utils.ServiceHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Node Manager class.
 */
public class FileSharer {
    private static final Logger logger = LoggerFactory.getLogger(FileSharer.class);

    private ServiceHolder serviceHolder;

    public FileSharer() {
        serviceHolder = new ServiceHolder();
    }

    /**
     * Start the current node.
     */
    public void start() {
        Thread thread = new Thread(this::startInCurrentThread);
        thread.setPriority(Thread.MAX_PRIORITY);
        thread.start();
    }

    /**
     * Restarts the current node.
     */
    public void restart() {
        Thread thread = new Thread(() -> {
            shutdownInCurrentThread();
            try {
                Thread.sleep(Constants.CONTINUOUS_TASK_INTERVAL);
            } catch (InterruptedException ignored) {
            }
            startInCurrentThread();
        });
        thread.setPriority(Thread.MAX_PRIORITY);
        thread.start();
    }

    /**
     * Shutdown the file sharer.
     */
    public void shutdown() {
        Thread thread = new Thread(this::shutdownInCurrentThread);
        thread.setPriority(Thread.MAX_PRIORITY);
        thread.start();
    }

    /**
     * Query the file sharer system for a file.
     */
    public ServiceHolder getServiceHolder() {
        return serviceHolder;
    }

    /**
     * Start the file sharer in the same thread.
     */
    private void startInCurrentThread() {
        logger.info("Starting Node");
        serviceHolder.getOverlayNetworkManager().register();
        serviceHolder.getOverlayNetworkManager().enableHeartBeat();
        serviceHolder.getOverlayNetworkManager().enableGossiping();
    }

    /**
     * Shutdown the file sharer in the same thread.
     */
    private void shutdownInCurrentThread() {
        logger.debug("Shutting down the Node");
        serviceHolder.getOverlayNetworkManager().unregister();
        serviceHolder.getOverlayNetworkManager().leave();

        // Waiting with a time out to leave the network
        try {
            Thread.sleep(Constants.CONTINUOUS_TASK_INTERVAL);
        } catch (InterruptedException ignored) {
        }

        serviceHolder.getOverlayNetworkManager().disableGossiping();
        serviceHolder.getOverlayNetworkManager().disableHeartBeat();
        serviceHolder.clear();
    }
}
