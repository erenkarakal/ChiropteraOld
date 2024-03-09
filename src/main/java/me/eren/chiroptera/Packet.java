package me.eren.chiroptera;

import java.io.*;
import java.util.Map;

public record Packet(byte id, Map<Integer, Serializable> data) implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * @return The serialized packet, or an empty byte array if it fails to serialize.
     */
    public byte[] serialize() {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
             ObjectOutputStream oos = new ObjectOutputStream(bos)) {

            oos.writeObject(this);
            return bos.toByteArray();

        } catch (IOException e) {
            return new byte[]{};
        }
    }

    /**
     * @param byteArray Raw packet data
     * @return The deserialized packet, or an empty packet with ID -1 if it fails to deserialize.
     */
    public static Packet deserialize(byte[] byteArray) {
        try (ByteArrayInputStream bis = new ByteArrayInputStream(byteArray);
             ObjectInputStream ois = new ObjectInputStream(bis)) {

            return (Packet) ois.readObject();

        } catch (IOException | ClassNotFoundException e) {
            return new Packet((byte) -1, null);
        }

    }

}