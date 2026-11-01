package io.raftkv.storage;

/** An immutable log entry. Payload bytes are defensively copied. */
public record LogRecord(byte type, long term, long index, byte[] payload) {
    public LogRecord {
        if (term < 0 || index < 1) throw new IllegalArgumentException("invalid term or index");
        payload = payload.clone();
    }
    @Override public byte[] payload() { return payload.clone(); }
}
