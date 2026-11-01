package io.raftkv.statemachine;

import java.util.*;

/** Sequential deterministic state machine. Replication, snapshots, and session registration are not implemented. */
public final class KeyValueStateMachine {
    public enum Operation { PUT, DELETE, CAS }
    public record Command(String clientId, long sequence, Operation operation, byte[] key, byte[] expected, byte[] value) {
        public Command {
            Objects.requireNonNull(clientId); Objects.requireNonNull(operation); Objects.requireNonNull(key);
            if (clientId.isBlank() || sequence < 1) throw new IllegalArgumentException("invalid session");
            if ((operation == Operation.PUT || operation == Operation.CAS) && value == null) throw new IllegalArgumentException("value required");
            if (operation != Operation.CAS && expected != null) throw new IllegalArgumentException("unexpected comparison value");
            if (operation == Operation.DELETE && value != null) throw new IllegalArgumentException("unexpected value");
            key = key.clone(); expected = copy(expected); value = copy(value);
        }
        @Override public byte[] key() { return key.clone(); }
        @Override public byte[] expected() { return copy(expected); }
        @Override public byte[] value() { return copy(value); }
    }
    public record Result(boolean applied, byte[] previous) {
        public Result { previous = copy(previous); }
        @Override public byte[] previous() { return copy(previous); }
    }
    private record Session(long sequence, Command command, Result result) { }
    private final NavigableMap<byte[], byte[]> values = new TreeMap<>(Arrays::compareUnsigned);
    private final Map<String, Session> sessions = new HashMap<>();
    public byte[] get(byte[] key) { return copy(values.get(Objects.requireNonNull(key))); }
    public Result apply(Command command) {
        Objects.requireNonNull(command);
        Session old = sessions.get(command.clientId());
        if (old != null && command.sequence() == old.sequence()) {
            if (!sameCommand(old.command(), command)) throw new IllegalArgumentException("sequence reused for a different command");
            return old.result();
        }
        long expectedSequence = old == null ? 1 : old.sequence() + 1;
        if (command.sequence() != expectedSequence) throw new IllegalArgumentException("out-of-order request");
        byte[] key = command.key(), previous = values.get(key);
        boolean applied = true;
        switch (command.operation()) {
            case PUT -> values.put(key, command.value());
            case DELETE -> values.remove(key);
            case CAS -> {
                applied = Arrays.equals(previous, command.expected());
                if (applied) values.put(key, command.value());
            }
        }
        Result result = new Result(applied, previous);
        sessions.put(command.clientId(), new Session(command.sequence(), command, result));
        return result;
    }
    private static boolean sameCommand(Command a, Command b) {
        return a.operation() == b.operation() && Arrays.equals(a.key(), b.key()) && Arrays.equals(a.expected(), b.expected()) && Arrays.equals(a.value(), b.value());
    }
    private static byte[] copy(byte[] value) { return value == null ? null : value.clone(); }
}
