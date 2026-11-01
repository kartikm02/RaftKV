package io.raftkv.storage;

/** Persistent election state. A null vote means the node has not voted this term. */
public record HardState(long term, String votedFor) {
    public HardState {
        if (term < 0) throw new IllegalArgumentException("negative term");
        if (votedFor != null && (votedFor.isBlank() || votedFor.length() > 1024))
            throw new IllegalArgumentException("invalid vote");
    }
}
