package io.raftkv.storage;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static java.nio.file.StandardOpenOption.*;
import static org.junit.jupiter.api.Assertions.*;

class StorageTest {
    @TempDir Path dir;
    private LogRecord entry(long index) { return new LogRecord((byte) 1, 1, index, new byte[] {(byte) index}); }
    @Test void batchPersistsAndRecovers() throws Exception {
        Path path = dir.resolve("log");
        try (var wal = new WriteAheadLog(path)) { wal.appendAndSync(List.of(entry(1), entry(2))); }
        try (var wal = new WriteAheadLog(path)) {
            var entries = wal.recover();
            assertEquals(2, entries.size()); assertEquals(2, entries.get(1).index());
            assertArrayEquals(new byte[]{2}, entries.get(1).payload());
            assertThrows(IllegalArgumentException.class, () -> wal.appendAndSync(List.of(entry(4))));
            wal.appendAndSync(List.of(entry(3)));
        }
        try (var wal = new WriteAheadLog(path)) { assertEquals(3, wal.recover().size()); }
    }
    @Test void truncatesTornTail() throws Exception {
        Path path = dir.resolve("log");
        try (var wal = new WriteAheadLog(path)) { wal.appendAndSync(List.of(entry(1), entry(2))); }
        try (var f = FileChannel.open(path, WRITE)) { f.truncate(f.size() - 2); }
        try (var wal = new WriteAheadLog(path)) { assertEquals(1, wal.recover().size()); wal.appendAndSync(List.of(entry(2))); }
        try (var wal = new WriteAheadLog(path)) { assertEquals(2, wal.recover().size()); }
    }
    @Test void rejectsMiddleChecksumFailure() throws Exception {
        Path path = dir.resolve("log");
        try (var wal = new WriteAheadLog(path)) { wal.appendAndSync(List.of(entry(1), entry(2))); }
        try (var f = FileChannel.open(path, WRITE)) { f.write(ByteBuffer.wrap(new byte[]{99}), 8); }
        assertThrows(IOException.class, () -> new WriteAheadLog(path));
    }
    @Test void truncatesTailChecksumFailure() throws Exception {
        Path path = dir.resolve("log");
        try (var wal = new WriteAheadLog(path)) { wal.appendAndSync(List.of(entry(1), entry(2))); }
        try (var f = FileChannel.open(path, WRITE)) { f.write(ByteBuffer.wrap(new byte[]{99}), f.size() - 1); }
        try (var wal = new WriteAheadLog(path)) { assertEquals(1, wal.recover().size()); }
    }
    @Test void persistsTermAndVoteAndRejectsSecondVote() throws Exception {
        var store = new HardStateStore(dir);
        assertEquals(new HardState(0, null), store.load());
        store.save(new HardState(3, "n1"));
        assertEquals(new HardState(3, "n1"), new HardStateStore(dir).load());
        assertThrows(IllegalArgumentException.class, () -> store.save(new HardState(3, "n2")));
        assertThrows(IllegalArgumentException.class, () -> store.save(new HardState(2, null)));
        store.save(new HardState(4, null));
    }
    @Test void payloadIsDefensivelyCopied() {
        byte[] bytes = {1}; var record = new LogRecord((byte) 1, 1, 1, bytes);
        bytes[0] = 2; record.payload()[0] = 3; assertArrayEquals(new byte[]{1}, record.payload());
    }
}
