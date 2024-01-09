package themixray.minecraft;

import themixray.Main;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MinecraftServer {
    public InetSocketAddress host;
    public int protocol_version;

    public List<MinecraftPlayer> connected;

    public MinecraftServer(InetSocketAddress host,
                           int protocol_version) {
        this.host = host;
        this.protocol_version = protocol_version;
        this.connected = new ArrayList<>();
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
