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
        MinecraftConnection conn = new MinecraftConnection() {
            @Override
            protected void onError(Throwable error) {

            }
        };

        conn.connect(host,null);

        conn.writePacket(() -> {
            OutputPacketContainer packet = new OutputPacketContainer((byte) 0x00);

            packet.writeVarInt(0); // Protocol version
            packet.writeString(host.getHostString()); // Server address
            packet.writeShort((short) host.getPort()); // Server port as unsigned short
            packet.writeVarInt(1); // Next state (1 for status)

            return packet;
        });

        conn.writePacket(() -> new OutputPacketContainer((byte) 0x00));

        InputPacketContainer packet = conn.readPacket();

        if (packet == null) return null;

        if (packet.getPacketId() == 0x00) {
            Map<String,Object> data = (Map<String, Object>) JSON.load(packet.readString());
            conn.close();
            return data;
        }

        return null;
    }

    public MinecraftPlayer connectPlayer(String name, Proxy proxy) {
        return connectPlayer(name,UUID.nameUUIDFromBytes(name.getBytes()),proxy);
    }

    public MinecraftPlayer connectPlayer(String name, UUID uuid, Proxy proxy) {
        MinecraftPlayer player = new MinecraftPlayer(
                this, proxy, uuid, name);
        player.startConnection();
        connected.add(player);
        return player;
    }
}
