package org.microfuse.file.sharer.node.core.communication.messaging;

import org.microfuse.file.sharer.node.commons.communication.messaging.UDPMessageType;

import java.util.Objects;

/**
 * UDP message class.
 * Used by the UDP Network Handler
 */
public class UDPMessage implements Cloneable {
    private UDPMessageType type;
    private String sourceIP;
    private int sourcePort;
    private long sequenceNumber;
    private Message message;
    private int usedRetriesCount;

    private static final Character MESSAGE_DATA_SEPARATOR = ' ';

    public UDPMessage() {
        type = UDPMessageType.ERROR;
        sequenceNumber = -1;
        usedRetriesCount = -1;
    }

    public UDPMessageType getType() {
        return type;
    }

    public void setType(UDPMessageType type) {
        this.type = type;
    }

    public String getSourceIP() {
        return sourceIP;
    }

    public void setSourceIP(String sourceIP) {
        this.sourceIP = sourceIP;
    }

    public int getSourcePort() {
        return sourcePort;
    }

    public void setSourcePort(int sourcePort) {
        this.sourcePort = sourcePort;
    }

    public long getSequenceNumber() {
        return sequenceNumber;
    }

    public void setSequenceNumber(long sequenceNumber) {
        this.sequenceNumber = sequenceNumber;
    }

    public Message getMessage() {
        return message;
    }

    public void setMessage(Message message) {
        this.message = message;
    }

    public int getUsedRetriesCount() {
        return usedRetriesCount;
    }

    public void setUsedRetriesCount(int usedRetriesCount) {
        this.usedRetriesCount = usedRetriesCount;
    }

    @Override
    public UDPMessage clone() {
        UDPMessage clone;
        try {
            clone = (UDPMessage) super.clone();
        } catch (CloneNotSupportedException e) {
            clone = new UDPMessage();
        }
        clone.setType(type);
        clone.setSourceIP(sourceIP);
        clone.setSourcePort(sourcePort);
        clone.setSequenceNumber(sequenceNumber);
        clone.setMessage((message != null ? message.clone() : null));
        clone.setUsedRetriesCount(usedRetriesCount);
        return clone;
    }

    /**
     * Parse a UDP Message object from a string.
     *
     * @param messageString The string which contains the UDP message
     * @return The relevant UDP message objects representing the message string
     */
    public static UDPMessage parse(String messageString) {
        UDPMessage udpMessage = new UDPMessage();

        // Getting the UDP message type
        int firstSeparatorIndex = messageString.indexOf(MESSAGE_DATA_SEPARATOR);
        udpMessage.setType(UDPMessageType.valueOf(messageString.substring(0, firstSeparatorIndex)));

        // Getting the message IP
        int secondSeparatorIndex = messageString.indexOf(MESSAGE_DATA_SEPARATOR, firstSeparatorIndex + 1);
        udpMessage.setSourceIP(messageString.substring(firstSeparatorIndex + 1, secondSeparatorIndex));

        // Getting the message sourcePort
        int thirdSeparatorIndex = messageString.indexOf(MESSAGE_DATA_SEPARATOR, secondSeparatorIndex + 1);
        udpMessage.setSourcePort(
                Integer.parseInt(messageString.substring(secondSeparatorIndex + 1, thirdSeparatorIndex)));

        // Getting the sequence number
        int forthSeparatorIndex = messageString.indexOf(MESSAGE_DATA_SEPARATOR, thirdSeparatorIndex + 1);
        if (forthSeparatorIndex < 0) {
            forthSeparatorIndex = messageString.length();
        }
        udpMessage.setSequenceNumber(
                Long.parseLong(messageString.substring(thirdSeparatorIndex + 1, forthSeparatorIndex)));

        // Getting the message delivered by the UDP layer
        if (forthSeparatorIndex < messageString.length()) {
            udpMessage.setMessage(Message.parse(messageString.substring(forthSeparatorIndex + 1)));
        }

        return udpMessage;
    }

    @Override
    public String toString() {
        return type.toString() + MESSAGE_DATA_SEPARATOR + sourceIP + MESSAGE_DATA_SEPARATOR + sourcePort
                + MESSAGE_DATA_SEPARATOR + sequenceNumber
                + (message != null ? MESSAGE_DATA_SEPARATOR + message.toString() : "");
    }

    @Override
    public boolean equals(Object object) {
        if (object != null && object instanceof UDPMessage) {
            UDPMessage messageObject = (UDPMessage) object;
            return Objects.equals(messageObject.getType(), getType())
                    && Objects.equals(messageObject.getSourceIP(), getSourceIP())
                    && Objects.equals(messageObject.getSourcePort(), getSourcePort())
                    && Objects.equals(messageObject.getSequenceNumber(), getSequenceNumber());
        }
        return false;
    }

    @Override
    public int hashCode() {
        return (getType().toString() + MESSAGE_DATA_SEPARATOR + getSourceIP() + MESSAGE_DATA_SEPARATOR
                + getSourcePort() + MESSAGE_DATA_SEPARATOR + getSequenceNumber()).hashCode();
    }
}
