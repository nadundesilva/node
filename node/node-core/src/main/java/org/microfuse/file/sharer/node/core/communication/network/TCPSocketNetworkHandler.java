package org.microfuse.file.sharer.node.core.communication.network;

import com.google.common.io.Closeables;
import org.microfuse.file.sharer.node.commons.Constants;
import org.microfuse.file.sharer.node.commons.communication.network.NetworkHandlerType;
import org.microfuse.file.sharer.node.commons.messaging.Message;
import org.microfuse.file.sharer.node.core.utils.ServiceHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

/**
 * A Socket based network handler.
 * <p>
 * Uses TCP sockets to communicate with other nodes.
 */
public class TCPSocketNetworkHandler extends NetworkHandler {
    private static final Logger logger = LoggerFactory.getLogger(TCPSocketNetworkHandler.class);

    private ServerSocket serverSocket;

    @Override
    public String getName() {
        return NetworkHandlerType.TCP_SOCKET.getValue();
    }

    @Override
    public void startListening() {
        if (!running) {
            super.startListening();
            new Thread(() -> {
                while (running) {
                    int portNumber = ServiceHolder.getConfiguration().getPeerListeningPort();
                    Socket clientSocket = null;
                    BufferedReader in = null;
                    try {
                        serverSocket = new ServerSocket(portNumber);
                        serverSocket.setReuseAddress(false);

                        logger.debug("Started listening at " + portNumber + ".");
                        while (running && !restartRequired) {
                            clientSocket = serverSocket.accept();
                            in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream(),
                                    Constants.DEFAULT_CHARSET));

                            StringBuilder message = new StringBuilder();
                            String inputLine;
                            while ((inputLine = in.readLine()) != null) {
                                message.append(inputLine);
                            }
                            onMessageReceived(
                                    clientSocket.getInetAddress().getHostAddress(),
                                    clientSocket.getPort(),
                                    Message.parse(message.toString())
                            );
                        }
                    } catch (IOException e) {
                        logger.debug("Listening stopped", e);
                    } finally {
                        Closeables.closeQuietly(in);
                        try {
                            Closeables.close(clientSocket, true);
                        } catch (IOException ignored) {
                        }
                        closeSocket();
                    }
                }
            }).start();
        } else {
            logger.warn("The TCP network handler is already listening. Ignored request to start again.");
        }
    }

    @Override
    public void restart() {
        if (running) {
            super.restart();
            restartRequired = true;
            try {
                closeSocket();
            } finally {
                restartRequired = false;
            }
        } else {
            logger.warn("The TCP network handler is not listening. Ignored request to restart.");
        }
    }

    @Override
    public void shutdown() {
        running = false;
        closeSocket();
    }

    @Override
    public void sendMessage(String ip, int port, Message message, boolean waitForReply) {
        try (
                Socket sendSocket = new Socket(ip, port);
                PrintWriter out = new PrintWriter(new OutputStreamWriter(sendSocket.getOutputStream(),
                        Constants.DEFAULT_CHARSET), true)
        ) {
            out.write(message.toString());
            int localPort = sendSocket.getLocalPort();

            if (waitForReply) {
                Socket clientSocket = null;
                BufferedReader in = null;
                try (
                        ServerSocket replyServerSocket = new ServerSocket(localPort)
                ) {
                    clientSocket = replyServerSocket.accept();
                    in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream(),
                            Constants.DEFAULT_CHARSET));

                    StringBuilder replyMessage = new StringBuilder();
                    String inputLine;
                    while ((inputLine = in.readLine()) != null) {
                        replyMessage.append(inputLine);
                    }

                    onMessageReceived(ip, port, Message.parse(replyMessage.toString()));
                } finally {
                    Closeables.closeQuietly(in);
                    Closeables.close(clientSocket, true);
                }
            }
        } catch (IOException e) {
            logger.debug("Failed to send message " + message.toString() + " to " + ip + ":" + port, e);
            onMessageSendFailed(ip, port, message);
        }
    }

    /**
     * Close the TCP socket.
     */
    private void closeSocket() {
        // Set reusable to enable a new network handler to use the same port
        if (serverSocket != null) {
            try {
                serverSocket.setReuseAddress(true);
            } catch (SocketException e) {
                logger.warn("Failed to set socket to reusable", e);
            }
        }

        try {
            Closeables.close(serverSocket, true);
        } catch (IOException ignored) {
        }
    }
}
