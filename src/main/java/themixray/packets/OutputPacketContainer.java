package themixray.packets;

import themixray.Main;
import themixray.minecraft.MinecraftPlayer;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.zip.Deflater;

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

    public void sendPacket(MinecraftPlayer player) {
        sendPacket(player.output);
    }

    public void sendCompressedPacket(MinecraftPlayer player, int threshold) {
        sendCompressedPacket(player.output, threshold);
    }

    public void sendPacket(DataOutputStream output) {
        byte[] message = buffer.toByteArray();
        try {
            writeVarInt(output, message.length);
            output.write(message);
            output.flush();

            Main.logOutputPacket(message.length, packet_id, message);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void sendCompressedPacket(DataOutputStream output, int threshold) {
        if (threshold == -1) {
            sendPacket(output);
            return;
        }

        byte[] message = buffer.toByteArray();

        try {
            if (message.length < threshold) {
                writeVarInt(output, message.length + 1);
                writeVarInt(output, 0);
                output.write(message);
            } else {
                byte[] compressed = zlibCompress(message);
                writeVarInt(output, compressed.length + 1);
                writeVarInt(output, message.length);
                output.write(compressed);
            }
            output.flush();

            Main.logOutputPacket(message.length, packet_id, message);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static byte[] zlibCompress(byte[] data) {
        Deflater compressor = new Deflater();
        compressor.setLevel(Deflater.BEST_COMPRESSION);
        compressor.setInput(data);
        compressor.finish();
        ByteArrayOutputStream bos = new ByteArrayOutputStream(data.length);
        byte[] buf = new byte[1024];
        while (!compressor.finished()) {
            int count = compressor.deflate(buf);
            bos.write(buf, 0, count);
        }
        try {
            bos.close();
        } catch (IOException e) {}
        return bos.toByteArray();
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
