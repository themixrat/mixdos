package themixray.packets;

import themixray.Main;
import themixray.minecraft.MinecraftConnection;
import themixray.minecraft.MinecraftPlayer;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.BitSet;
import java.util.UUID;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

import static themixray.Main.zlibCompress;
import static themixray.Main.zlipDecompress;

public class OutputPacketContainer {
    private ByteArrayOutputStream buffer;
    private DataOutputStream output;
    private byte packet_id;

    public OutputPacketContainer(byte packet_id) {
        this.packet_id = packet_id;

        buffer = new ByteArrayOutputStream();
        output = new DataOutputStream(buffer);

        writeVarInt(packet_id);
    }

    public ByteArrayOutputStream getBuffer() {
        return buffer;
    }

    public DataOutputStream getOutput() {
        return output;
    }

    public byte getPacketId() {
        return packet_id;
    }

    public void writeByte(byte v) {
        try {
            output.writeByte(v);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void writeShort(short v) {
        try {
            output.writeShort(v);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void writeString(String v, Charset charset) {
        byte[] bytes = v.getBytes(charset);
        writeVarInt(bytes.length);
        writeBytes(bytes);
    }

    public void writeString(String v) {
        writeString(v,StandardCharsets.UTF_8);
    }

    public void writeVarInt(int v) {
        writeVarInt(output,v);
    }

    public void writeBitSet(BitSet v) {
        long[] array = v.toLongArray();
        writeVarInt(array.length);
        for (int i = 0; i < array.length; i++)
            writeLong(array[i]);
    }

    public void writeBoolean(boolean v) {
        writeByte((byte) (v ? 0x00 : 0x01));
    }

    public void writeVarLong(long v) {
        writeVarLong(output,v);
    }
    public void writeLong(long v) {
        try {
            output.writeLong(v);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void writeBytes(byte[] v) {
        try {
            output.write(v);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void writeUUID(UUID v) {
        try {
            ByteBuffer bb = ByteBuffer.allocate(16);
            bb.putLong(v.getMostSignificantBits());
            bb.putLong(v.getLeastSignificantBits());

            byte[] bytes = bb.array();

            writeVarInt(bytes.length);
            output.write(bytes);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void sendPacket(MinecraftPlayer player) throws IOException {
        sendPacket(player.getOutput());
    }

    public void sendCompressedPacket(MinecraftPlayer player, int threshold) throws IOException {
        sendCompressedPacket(player.getOutput(), threshold);
    }

    public void sendCompressedPacket(MinecraftConnection conn) throws IOException {
        sendCompressedPacket(conn.getOutput(), conn.getCompressionThreshold());
    }

    public void sendPacket(DataOutputStream output) throws IOException {
        byte[] message = buffer.toByteArray();

        writeVarInt(output, message.length);
        output.write(message);
        output.flush();

        Main.logOutputPacket(message.length, packet_id, message);
    }

    public void sendCompressedPacket(DataOutputStream output, int threshold) throws IOException {
        if (threshold == -1) {
            sendPacket(output);
            return;
        }

        byte[] message = buffer.toByteArray();

        if (message.length < threshold) {
            writeVarInt(output, message.length + 1);
            writeVarInt(output, 0);
            output.write(message);

            Main.logOutputPacket(message.length, packet_id, message);
        } else {
            byte[] compressed = zlibCompress(message);
            writeVarInt(output, compressed.length + 1);
            writeVarInt(output, message.length);
            output.write(compressed);

            Main.logOutputPacket(message.length, packet_id, compressed);
        }
        output.flush();
    }

    private static final int SEGMENT_BITS = 0x7F;
    private static final int CONTINUE_BIT = 0x80;

    private static void writeVarInt(DataOutputStream output, int v) {
        try {
            while (true) {
                if ((v & ~SEGMENT_BITS) == 0) {
                    output.writeByte(v);
                    return;
                }

                output.writeByte((v & SEGMENT_BITS) | CONTINUE_BIT);

                // Note: >>> means that the sign bit is shifted with the rest of the number rather than being left alone
                v >>>= 7;
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void writeVarLong(DataOutputStream output, long v) {
        try {
            while (true) {
                if ((v & ~((long) SEGMENT_BITS)) == 0) {
                    output.writeByte((int) v);
                    return;
                }

                output.writeByte((int) ((v & SEGMENT_BITS) | CONTINUE_BIT));

                // Note: >>> means that the sign bit is shifted with the rest of the number rather than being left alone
                v >>>= 7;
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
