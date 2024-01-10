package themixray.minecraft;

import com.diogonunes.jcolor.Attribute;
import themixray.JSON;
import themixray.packets.InputPacketContainer;
import themixray.Main;
import themixray.packets.OutputPacketContainer;

import java.net.Proxy;
import java.net.Socket;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.*;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import static com.diogonunes.jcolor.Ansi.colorize;

public class MinecraftPlayer extends MinecraftConnection {
    public MinecraftServer server;
    public Proxy proxy;
    public String name;
    public UUID uuid;

    public boolean connected = false;

    public MinecraftPlayer(MinecraftServer server, Proxy proxy, UUID uuid, String name) {
        this.server = server;
        this.uuid = uuid;
        this.name = name;
        this.proxy = proxy;

        compression_threshold = -1;
    }

    @Override
    protected void onError(Throwable error) {
        if (Main.debug_mode)
            error.printStackTrace();
        disconnect(error.getMessage());
    }

    public void disconnect(String message) {
        try {
            if (sock.isConnected() && !sock.isClosed())
                sock.close();
            if (connected)
                System.out.println(colorize(" - "+name+" disconnected: "+message, Attribute.BRIGHT_RED_TEXT()));
            startConnection();
        } catch (Exception e) {}
    }

    public void startConnection() {
        runThread(() -> {
            connect(server.host, proxy);

            sendHandshake();
            sendLoginStartPacket();

            runThread(this::readPackets);
        });
    }

    private void sendChat() {
        try {
            if (Main.chat_on_join != null) {
                for (int i = 0; i < Main.chat_on_join.size(); i++) {
                    String v = Main.chat_on_join.get(i);
                    long d = Main.chat_delays.get(i);

                    Thread.sleep(d);

                    if (!v.startsWith("/")) sendChatMessagePacket(v);
                    else sendChatCommandPacket(v.substring(1));
                }
            }
        } catch (Exception e) {
            onError(e);
        }
    }

    private void sendHandshake() {
        OutputPacketContainer packet = new OutputPacketContainer((byte) 0x00);

        packet.writeVarInt(server.protocol_version); // Protocol version
        packet.writeString(server.host.getHostString()); // Server address
        packet.writeShort((short) server.host.getPort()); // Server port as unsigned short
        packet.writeVarInt(2); // Next state (2 for login)

        writePacket(packet);
    }

    private void sendLoginStartPacket() {
        OutputPacketContainer packet = new OutputPacketContainer((byte) 0x00);

        packet.writeString(name);
        packet.writeUUID(uuid);

        writePacket(packet);
    }

    private void sendAcknowledgedPacket() {
        OutputPacketContainer packet = new OutputPacketContainer((byte) 0x03);
        packet.writeVarInt(0);
        writePacket(packet);
    }

    private void sendKeepAlivePacket(long id) {
        OutputPacketContainer packet = new OutputPacketContainer((byte) 0x03);
        packet.writeLong(id);
        writePacket(packet);
    }

    private void sendConfirmTeleportationPacket(int id) {
        OutputPacketContainer packet = new OutputPacketContainer((byte) 0x00);
        packet.writeVarInt(id);
        writePacket(packet);
    }

    private void sendChatCommandPacket(String command) {
        OutputPacketContainer packet = new OutputPacketContainer((byte) 0x04);

        packet.writeString(command); // Command
        packet.writeLong(System.currentTimeMillis()); // Timestamp
        packet.writeLong(0); // Salt
        packet.writeVarInt(0); // Array length
        packet.writeVarInt(0); // Message Count
        packet.writeBytes(Arrays.copyOf(new BitSet().toByteArray(), -Math.floorDiv(-20, 8))); // Acknowledged

        writePacket(packet);
    }

    private void sendChatMessagePacket(String message) {
//        OutputPacketContainer packet = new OutputPacketContainer((byte) 0x05);
//
//        packet.writeString(message); // Message
//        packet.writeLong(System.currentTimeMillis()); // Timestamp
//        packet.writeLong(0); // Salt
//        packet.writeBoolean(false); // Has Signature
//        packet.writeVarInt(0); // Message Count
//        packet.writeBytes(Arrays.copyOf(new BitSet().toByteArray(), -Math.floorDiv(-20, 8))); // Acknowledged
//
//        writePacket(packet);
    }

    private void readPackets() {
        while (sock.isConnected() && !sock.isClosed()) {
//            System.out.println("packet reading");
            InputPacketContainer packet = readPacket();

            int length = packet.getPacketSize();
            int packetId = packet.getPacketId();

            if (packetId == 0x02) {
                UUID uuid = new UUID(packet.readLong(), packet.readLong());
                String username = packet.readString();

                System.out.println(colorize(" + "+username + " [" + uuid + "] connected", Attribute.BRIGHT_GREEN_TEXT()));
//                sendAcknowledgedPacket();

                sendChat();

                connected = true;
            } else if (packetId == 0x03) {
                if (!connected) compression_threshold = packet.readVarInt();
                else sendKeepAlivePacket(packet.readLong());
            } else if (packetId == 0x3E) {
                double x = packet.readDouble();
                double y = packet.readDouble();
                double z = packet.readDouble();
                double yaw = packet.readFloat();
                double pitch = packet.readFloat();
                byte flags = packet.readByte();
                int id = packet.readVarInt();

                sendConfirmTeleportationPacket(id);
            } else if (packetId == 0x00) {
                String json = packet.readString();
                disconnect(json);
                break;
            }
        }

        disconnect("timed out");
    }
}