package org.microfuse.file.sharer.node.core.communication.network;

import org.microfuse.file.sharer.node.commons.messaging.Message;
import org.microfuse.file.sharer.node.core.utils.Constants;
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

/**
 * A Socket based network handler.
 * <p>
 * Uses TCP sockets to communicate with other nodes.
 */
public class TCPSocketNetworkHandler extends NetworkHandler {
    private static final Logger logger = LoggerFactory.getLogger(TCPSocketNetworkHandler.class);

    @Override
    public String getName() {
        return NetworkHandlerType.TCP_SOCKET.getValue();
    }

    @Override
    public void startListening() {
        new Thread(() -> {
            while (true) {
                int portNumber = ServiceHolder.getConfiguration().getPeerListeningPort();
                while (!restartRequired) {
                    try (
                            ServerSocket serverSocket = new ServerSocket(portNumber);
                            Socket clientSocket = serverSocket.accept();
                            BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream(),
                                    Constants.DEFAULT_CHARSET))
                    ) {
                        try {
                            StringBuilder message = new StringBuilder();
                            String inputLine;
                            while ((inputLine = in.readLine()) != null) {
                                message.append(inputLine);
                            }
                            onMessageReceived(
                                    clientSocket.getRemoteSocketAddress().toString(),
                                    clientSocket.getPort(),
                                    Message.parse(message.toString())
                            );
                        } catch (IOException e) {
                            logger.debug("Failed to receive message from "
                                    + clientSocket.getRemoteSocketAddress().toString(), e);
                        }
                    } catch (IOException e) {
                        logger.debug("Failed to establish socket connection", e);
                    }
                }
            }
        }).start();
    }

    @Override
    public void sendMessage(String ip, int port, Message message) {
        try (
                Socket echoSocket = new Socket(ip, port);
                PrintWriter out = new PrintWriter(new OutputStreamWriter(echoSocket.getOutputStream(),
                        Constants.DEFAULT_CHARSET), true)
        ) {
            out.write(message.toString());
        } catch (IOException e) {
            logger.debug("Message sent to " + ip + ":" + port + " : " + message, e);
        }
    }
}
