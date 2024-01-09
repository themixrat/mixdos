package themixray.minecraft;

import com.diogonunes.jcolor.Attribute;
import themixray.packets.InputPacketContainer;
import themixray.Main;
import themixray.packets.OutputPacketContainer;

import java.net.Socket;
import java.util.UUID;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import static com.diogonunes.jcolor.Ansi.colorize;

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

        this.sock = sock;

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
                if (Main.debug_mode)
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

    private void sendAcknowledgedPacket() {
        OutputPacketContainer packet = new OutputPacketContainer((byte) 0x03);
        packet.writeVarInt(0);
        packet.sendCompressedPacket(this, compressionThreshold);
    }

    private void sendKeepAlivePacket(long id) {
        OutputPacketContainer packet = new OutputPacketContainer((byte) 0x03);
        packet.writeLong(id);
        packet.sendCompressedPacket(this, compressionThreshold);
    }

    private boolean connected = false;

    private void readPackets() {
        InputPacketContainer packet;
        try {
            packet = new InputPacketContainer(input, compressionThreshold);
        } catch (Exception e) {
            if (Main.debug_mode)
                e.printStackTrace();
            return;
        }

        int length = packet.getPacketSize();
        int packetId = packet.getPacketId();

        if (packetId == 0x02) {
            UUID uuid = new UUID(packet.readLong(), packet.readLong());
            String username = packet.readString();

            System.out.println(colorize(username+" ["+uuid+"] connected", Attribute.GREEN_TEXT()));
            connected = true;

            sendAcknowledgedPacket();
        } else if (packetId == 0x03) {
            if (!connected) {
                compressionThreshold = packet.readVarInt();
            } else {
                sendKeepAlivePacket(packet.readLong());
            }
        }

        readPackets();
    }
}