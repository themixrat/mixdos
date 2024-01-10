package themixray.packets;

import themixray.Main;
import themixray.minecraft.MinecraftConnection;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.zip.DataFormatException;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

import static themixray.Main.zlipDecompress;

public class InputPacketContainer {
    private DataInputStream input;
    private BufferedInputStream buffer;

    private byte packet_id;
    private int packet_size;

    public InputPacketContainer(MinecraftConnection conn) throws IOException {
        this(conn.getInput(), conn.getCompressionThreshold());
    }

    public InputPacketContainer(DataInputStream input) throws IOException {
        this(input, -1);
    }

    public InputPacketContainer(DataInputStream input, int compression_threshold) throws IOException {
        byte[] data;

        if (compression_threshold == -1) {
            packet_size = readVarInt(input);
            data = input.readNBytes(packet_size);
        } else {
            int compressed_size = readVarInt(input);
            packet_size = readVarInt(input);

            if (packet_size <= compression_threshold) {
                packet_size = compressed_size;
                data = input.readNBytes(packet_size);
            } else {
                try {
                    data = zlipDecompress(input.readNBytes(packet_size));
                } catch (DataFormatException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        this.buffer = new BufferedInputStream(new ByteArrayInputStream(data));
        this.input = new DataInputStream(buffer);

        this.packet_id = (byte) readVarInt();

        Main.logInputPacket(packet_size, packet_id, data);
    }

    public int getPacketSize() {
        return packet_size;
    }

    public BufferedInputStream getBuffer() {
        return buffer;
    }

    public byte getPacketId() {
        return packet_id;
    }

    public boolean readBoolean() {
        return readByte() == 0x01;
    }

    public String readString() {
        return readString(StandardCharsets.UTF_8);
    }

    public String readString(Charset ch) {
        try {
            return readString(input, ch);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public int readVarInt() {
        try {
            return readVarInt(input);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public byte readByte() {
        try {
            return input.readByte();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public double readDouble() {
        try {
            return input.readDouble();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public float readFloat() {
        try {
            return input.readFloat();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public short readShort() {
        try {
            return input.readShort();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public long readVarLong() {
        try {
            return readVarLong(input);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public long readLong() {
        try {
            return input.readLong();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public byte[] readBytes(int size) {
        try {
            return input.readNBytes(size);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public UUID readUUID() {
        return new UUID(readLong(), readLong());
    }

    private static int readVarInt(DataInputStream in) throws IOException {
        int value = 0;
        int bitOffset = 0;
        byte b;
        do {
            b = in.readByte();
            value |= (b & 127) << bitOffset;
            bitOffset += 7;
        } while ((b & 128) == 128);
        return value;
    }

    private static String readString(DataInputStream in, Charset ch) throws IOException {
        return new String(in.readNBytes(readVarInt(in)), ch);
    }

    private static final int SEGMENT_BITS = 0x7F;
    private static final int CONTINUE_BIT = 0x80;

    private static long readVarLong(DataInputStream in) throws IOException {
        long value = 0;
        int position = 0;
        byte currentByte;

        while (true) {
            currentByte = in.readByte();
            value |= (long) (currentByte & SEGMENT_BITS) << position;

            if ((currentByte & CONTINUE_BIT) == 0) break;

            position += 7;

            if (position >= 64) throw new RuntimeException("VarLong is too big");
        }

        return value;
    }
}
