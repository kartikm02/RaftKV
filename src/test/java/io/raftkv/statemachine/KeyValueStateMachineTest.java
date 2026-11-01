package io.raftkv.statemachine;

import org.junit.jupiter.api.Test;
import static io.raftkv.statemachine.KeyValueStateMachine.*;
import static org.junit.jupiter.api.Assertions.*;

class KeyValueStateMachineTest {
    private static byte[] b(String s) { return s.getBytes(java.nio.charset.StandardCharsets.UTF_8); }
    private static Command cmd(long seq, Operation op, String key, String expected, String value) {
        return new Command("client", seq, op, b(key), expected == null ? null : b(expected), value == null ? null : b(value));
    }
    @Test void operationsAndDefensiveCopies() {
        var kv = new KeyValueStateMachine();
        assertTrue(kv.apply(cmd(1, Operation.CAS, "k", null, "v")).applied());
        assertFalse(kv.apply(cmd(2, Operation.CAS, "k", "wrong", "new")).applied());
        assertTrue(kv.apply(cmd(3, Operation.CAS, "k", "v", "new")).applied());
        byte[] value = kv.get(b("k")); value[0] = 0;
        assertArrayEquals(b("new"), kv.get(b("k")));
        assertArrayEquals(b("new"), kv.apply(cmd(4, Operation.DELETE, "k", null, null)).previous());
        assertNull(kv.get(b("k")));
    }
    @Test void retriesReturnCachedResultAndRejectChangedOrOldRequests() {
        var kv = new KeyValueStateMachine();
        var first = cmd(1, Operation.PUT, "k", null, "first");
        assertNull(kv.apply(first).previous());
        assertNull(kv.apply(first).previous());
        assertThrows(IllegalArgumentException.class, () -> kv.apply(cmd(1, Operation.PUT, "k", null, "other")));
        kv.apply(cmd(2, Operation.PUT, "k", null, "second"));
        assertThrows(IllegalArgumentException.class, () -> kv.apply(first));
        assertThrows(IllegalArgumentException.class, () -> kv.apply(cmd(4, Operation.DELETE, "k", null, null)));
    }
}
