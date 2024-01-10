package themixray.minecraft;

import themixray.packets.InputPacketContainer;
import themixray.packets.OutputPacketContainer;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.Socket;

public abstract class MinecraftConnection {
    protected InetSocketAddress host;
    protected int compression_threshold;

    protected Socket sock;
    protected DataOutputStream output;
    protected DataInputStream input;

    public MinecraftConnection() {
        this.compression_threshold = -1;
    }

    public void connect(InetSocketAddress host, Proxy proxy) {
        try {
            this.host = host;

            this.sock = proxy != null ? new Socket(proxy) : new Socket();
            this.sock.connect(host);

            this.output = new DataOutputStream(sock.getOutputStream());
            this.input = new DataInputStream(sock.getInputStream());
        } catch (Throwable error) {
            onError(error);
        }
    }

    protected abstract void onError(Throwable error);

    protected void runThread(Runnable runnable) {
        new Thread(() -> {
            try {
                runnable.run();
            } catch (Throwable error) {
                onError(error);
            }
        }).start();
    }

    public void close() {
        try {
            sock.close();
        } catch (Throwable error) {}
    }

    public InetSocketAddress getHost() {
        return host;
    }

    public int getCompressionThreshold() {
        return compression_threshold;
    }

    public void setCompressionThreshold(int compression_threshold) {
        this.compression_threshold = compression_threshold;
    }

    public DataOutputStream getOutput() {
        return output;
    }

    public DataInputStream getInput() {
        return input;
    }

    public void writePacket(OutputPacketContainer packet) {
        try {
            packet.sendCompressedPacket(this);
        } catch (Throwable error) {
            onError(error);
        }
    }

    public void writePacket(WritePacket packet) {
        try {
            packet.run().sendCompressedPacket(this);
        } catch (Throwable error) {
            onError(error);
        }
    }

    public InputPacketContainer readPacket() {
        try {
            return new InputPacketContainer(this);
        } catch (Throwable error) {
            onError(error);
            return null;
        }
    }

    public interface Runnable {
        public void run() throws Throwable;
    }

    public interface WritePacket {
        public OutputPacketContainer run() throws Throwable;
    }
}
