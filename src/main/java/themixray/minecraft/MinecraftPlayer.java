package themixray.minecraft;

import themixray.packets.InputPacketContainer;
import themixray.Main;
import themixray.packets.OutputPacketContainer;

import java.net.Socket;
import java.util.UUID;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class MinecraftPlayer {
    public Socket sock;

    public DataOutputStream output;
    public DataInputStream input;

    public MinecraftServer server;

    public UUID uuid;
    public String name;

    public int compressionThreshold = -1;

    public MinecraftPlayer(MinecraftServer server, Socket sock, UUID uuid, String name) throws IOException {
        this.server = server;
        this.uuid = uuid;
        this.name = name;

        this.output = new DataOutputStream(sock.getOutputStream());
        this.input = new DataInputStream(sock.getInputStream());
    }

    public void startConnection() {
        new Thread(() -> {
            try {
                sendHandshake();
                sendLoginStartPacket();
                readPackets();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void sendHandshake() {
        OutputPacketContainer packet = new OutputPacketContainer((byte) 0x00);

        packet.writeVarInt(server.protocol_version); // Protocol version
        packet.writeString(server.host.getHostString()); // Server address
        packet.writeShort((short) server.host.getPort()); // Server port as unsigned short
        packet.writeVarInt(2); // Next state (2 for login)

        packet.sendCompressedPacket(this, compressionThreshold);
    }

    private void sendLoginStartPacket() {
        OutputPacketContainer packet = new OutputPacketContainer((byte) 0x00);

        packet.writeString(name);
        if (uuid != null)
            packet.writeUUID(uuid);

        packet.sendCompressedPacket(this, compressionThreshold);
    }

    private void sendAcknowledgedResponse() {
        OutputPacketContainer packet = new OutputPacketContainer((byte) 0x03);
        packet.writeVarInt(0);
        packet.sendCompressedPacket(this, compressionThreshold);
    }

    private void readPackets() {
        InputPacketContainer packet;
        try {
            packet = new InputPacketContainer(input, compressionThreshold);
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        int length = packet.getPacketSize();
        int packetId = packet.getPacketId();

        if (packetId == 0x02) { // Packet ID for login success
            UUID uuid = new UUID(packet.readLong(), packet.readLong()); // UUID is received
            String username = packet.readString(); // Username is received as String

//            System.out.println("UUID: " + uuid + ", Username: " + username);
            System.out.println(name+": Connection Successful!");

            sendAcknowledgedResponse();
        } else if (packetId == 0x03) { // Packet ID for login success
            int threshold = packet.readVarInt();
//            System.out.println("threshold: " + threshold);
            compressionThreshold = threshold;
//            sendAcknowledgedResponse();
//            return true;
        } else if (packetId == 0x00) {
//            System.out.println("Login Failed!");
//            System.out.println((String)((List<Object>)((Map<String,Object>)JSON.load(readString(input))).get("with")).get(0));
        }

        readPackets();
    }
}