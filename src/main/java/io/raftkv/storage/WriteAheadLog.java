package io.raftkv.storage;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import java.util.*;
import java.util.zip.CRC32C;
import static java.nio.file.StandardOpenOption.*;

/** Single-file WAL prototype. appendAndSync returns only after forcing data to disk. */
public final class WriteAheadLog implements AutoCloseable {
    private static final int FIXED = 1 + Long.BYTES * 2;
    private static final int HEADER = Integer.BYTES * 2;
    private static final int MAX_RECORD = 16 * 1024 * 1024;
    private final FileChannel channel;
    private long lastIndex;
    private boolean failed;
    public WriteAheadLog(Path path) throws IOException {
        channel = FileChannel.open(path, CREATE, READ, WRITE);
        try { recover(); } catch (IOException | RuntimeException e) { channel.close(); throw e; }
    }
    public synchronized List<LogRecord> recover() throws IOException {
        if (failed) throw new IOException("WAL is unusable after an I/O failure");
        var records = new ArrayList<LogRecord>();
        long size = channel.size(), position = 0, previous = 0;
        while (position < size) {
            long remaining = size - position;
            if (remaining < HEADER) { truncateTail(position); break; }
            ByteBuffer header = read(position, HEADER);
            int length = header.getInt(), expectedCrc = header.getInt();
            if (length < FIXED || length > MAX_RECORD) throw new IOException("invalid record length at " + position);
            if (remaining - HEADER < length) { truncateTail(position); break; }
            ByteBuffer body = read(position + HEADER, length);
            CRC32C crc = new CRC32C(); crc.update(body.duplicate());
            if ((int) crc.getValue() != expectedCrc) {
                if (remaining == HEADER + (long) length) { truncateTail(position); break; }
                throw new IOException("WAL checksum failure before tail at " + position);
            }
            byte type = body.get(); long term = body.getLong(), index = body.getLong();
            byte[] payload = new byte[body.remaining()]; body.get(payload);
            if (index != previous + 1 || term < 0) throw new IOException("invalid log sequence at " + position);
            records.add(new LogRecord(type, term, index, payload));
            previous = index; position += HEADER + length;
        }
        lastIndex = previous;
        channel.position(channel.size());
        return List.copyOf(records);
    }
    /** Writes a batch in order and performs one fsync. Failure poisons the instance. */
    public synchronized void appendAndSync(List<LogRecord> entries) throws IOException {
        if (failed) throw new IOException("WAL is unusable after an I/O failure");
        long next = lastIndex;
        var encoded = new ArrayList<ByteBuffer>();
        for (LogRecord record : entries) {
            if (record.index() != ++next) throw new IllegalArgumentException("non-contiguous index");
            byte[] payload = record.payload();
            if (payload.length > MAX_RECORD - FIXED) throw new IllegalArgumentException("record too large");
            ByteBuffer body = ByteBuffer.allocate(FIXED + payload.length);
            body.put(record.type()).putLong(record.term()).putLong(record.index()).put(payload).flip();
            CRC32C crc = new CRC32C(); crc.update(body.duplicate());
            ByteBuffer frame = ByteBuffer.allocate(HEADER + body.remaining());
            frame.putInt(body.remaining()).putInt((int) crc.getValue()).put(body).flip(); encoded.add(frame);
        }
        try {
            for (ByteBuffer frame : encoded) while (frame.hasRemaining()) channel.write(frame);
            if (!encoded.isEmpty()) channel.force(true);
            lastIndex = next;
        } catch (IOException e) { failed = true; throw e; }
    }
    private ByteBuffer read(long offset, int size) throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(size);
        while (buffer.hasRemaining()) {
            int n = channel.read(buffer, offset + buffer.position());
            if (n < 0) throw new EOFException("unexpected EOF");
            if (n == 0) throw new IOException("unable to read WAL");
        }
        return buffer.flip();
    }
    private void truncateTail(long position) throws IOException { channel.truncate(position); channel.force(true); }
    @Override public synchronized void close() throws IOException { channel.close(); }
}
