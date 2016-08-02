package edu.rit.se.crashavoidance.network;

import java.io.Serializable;

/**
 * Created by Brett on 8/2/2016.
 */
public class Message implements Serializable {

    public MessageType messageType;
    public byte[] message;

    public Message(MessageType messageType, byte[] message) {
        this.messageType = messageType;
        this.message = message;
    }
}
