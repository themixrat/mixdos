package themixray.minecraft;

import com.diogonunes.jcolor.Attribute;
import themixray.JSON;
import themixray.Main;
import themixray.packets.InputPacketContainer;
import themixray.packets.OutputPacketContainer;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.diogonunes.jcolor.Ansi.colorize;

public class MinecraftServer {
    public InetSocketAddress host;
    public int protocol_version;

    public List<MinecraftPlayer> connected;

    public MinecraftServer(InetSocketAddress host,
                           int protocol_version) {
        this.host = host;
        this.connected = new ArrayList<>();
        this.protocol_version = protocol_version;
    }

    public MinecraftServer(InetSocketAddress host) {
        this.host = host;
        this.connected = new ArrayList<>();
        this.protocol_version = ((Long) ((Map<String,Object>)fetchStatus(host).get("version")).get("protocol")).intValue();
    }

    public static long fetchPing(InetSocketAddress host, Proxy proxy) {
        try {
            Socket sock = proxy != null ? new Socket(proxy) : new Socket();
            sock.connect(host);

            DataOutputStream output = new DataOutputStream(sock.getOutputStream());
            DataInputStream input = new DataInputStream(sock.getInputStream());

            OutputPacketContainer handshake = new OutputPacketContainer((byte) 0x00);

            handshake.writeVarInt(0); // Protocol version
            handshake.writeString(host.getHostString()); // Server address
            handshake.writeShort((short) host.getPort()); // Server port as unsigned short
            handshake.writeVarInt(1); // Next state (1 for status)

            handshake.sendPacket(output);

            OutputPacketContainer ping = new OutputPacketContainer((byte) 0x01);
            ping.writeLong(System.currentTimeMillis());
            ping.sendPacket(output);

            InputPacketContainer pong = new InputPacketContainer(input);

            if (pong.getPacketId() == 0x01) {
                long start = pong.readLong();
                sock.close();
                return System.currentTimeMillis() - start;
            }
        } catch (Exception e) {}
        return -1;
    }

    public static Map<String,Object> fetchStatus(InetSocketAddress host) {
        try {
            Socket sock = new Socket();
            sock.connect(host);

            DataOutputStream output = new DataOutputStream(sock.getOutputStream());
            DataInputStream input = new DataInputStream(sock.getInputStream());

            OutputPacketContainer handshake = new OutputPacketContainer((byte) 0x00);

            handshake.writeVarInt(0); // Protocol version
            handshake.writeString(host.getHostString()); // Server address
            handshake.writeShort((short) host.getPort()); // Server port as unsigned short
            handshake.writeVarInt(1); // Next state (1 for status)

            handshake.sendPacket(output);

            OutputPacketContainer status = new OutputPacketContainer((byte) 0x00);
//            status.writeVarInt(0);
            status.sendPacket(output);

            InputPacketContainer packet = new InputPacketContainer(input);

            int length = packet.getPacketSize();
            int packetId = packet.getPacketId();

            if (packetId == 0x00) {
                Map<String,Object> data = (Map<String, Object>)
                        JSON.load(packet.readString());
                sock.close();
                return data;
            }
        } catch (IOException e) {
            if (Main.debug_mode)
                e.printStackTrace();
        }
        return null;
    }

    public MinecraftPlayer connectPlayer(String name, Proxy proxy) {
        return connectPlayer(name,UUID.nameUUIDFromBytes(name.getBytes()),proxy);
    }

    public MinecraftPlayer connectPlayer(String name, UUID uuid, Proxy proxy) {
        try {
            Socket sock = proxy != null ? new Socket(proxy) : new Socket();
            sock.connect(host);

            MinecraftPlayer player = new MinecraftPlayer(
                    this, sock, uuid, name);
            player.startConnection();
            connected.add(player);
            return player;
        } catch (IOException e) {
            if (Main.debug_mode)
                e.printStackTrace();
            return null;
        }
    }
}
