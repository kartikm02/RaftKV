package io.raftkv.storage;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import static java.nio.file.StandardCopyOption.*;
import static java.nio.file.StandardOpenOption.*;

/** Crash-safe replacement of election metadata on filesystems supporting atomic rename. */
public final class HardStateStore {
    private static final int MAGIC = 0x524b564d;
    private final Path directory, file;
    public HardStateStore(Path directory) throws IOException {
        this.directory = directory.toAbsolutePath();
        Files.createDirectories(this.directory);
        file = this.directory.resolve("hard-state");
    }
    public HardState load() throws IOException {
        if (!Files.exists(file)) return new HardState(0, null);
        try (var in = new DataInputStream(Files.newInputStream(file))) {
            if (in.readInt() != MAGIC) throw new IOException("invalid hard state magic");
            long term = in.readLong();
            String vote = in.readBoolean() ? in.readUTF() : null;
            if (in.read() != -1) throw new IOException("trailing hard state bytes");
            try { return new HardState(term, vote); }
            catch (IllegalArgumentException e) { throw new IOException("invalid hard state", e); }
        }
    }
    public void save(HardState state) throws IOException {
        HardState previous = load();
        if (state.term() < previous.term() || (state.term() == previous.term() && previous.votedFor() != null && !previous.votedFor().equals(state.votedFor())))
            throw new IllegalArgumentException("term or vote would move backwards");
        var bytes = new ByteArrayOutputStream();
        try (var out = new DataOutputStream(bytes)) {
            out.writeInt(MAGIC); out.writeLong(state.term());
            out.writeBoolean(state.votedFor() != null);
            if (state.votedFor() != null) out.writeUTF(state.votedFor());
        }
        Path tmp = Files.createTempFile(directory, ".hard-state-", ".tmp");
        try {
            try (var channel = FileChannel.open(tmp, WRITE, TRUNCATE_EXISTING)) {
                ByteBuffer data = ByteBuffer.wrap(bytes.toByteArray());
                while (data.hasRemaining()) channel.write(data);
                channel.force(true);
            }
            Files.move(tmp, file, ATOMIC_MOVE, REPLACE_EXISTING);
            try (var dir = FileChannel.open(directory, READ)) { dir.force(true); }
        } finally { Files.deleteIfExists(tmp); }
    }
}
