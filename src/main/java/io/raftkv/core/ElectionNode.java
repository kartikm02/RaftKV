package io.raftkv.core;

import java.util.HashSet;
import java.util.Set;

/** Deterministic in-memory election model. Its caller serializes all events. Not production Raft. */
public final class ElectionNode {
    public enum Role { FOLLOWER, CANDIDATE, LEADER }
    public enum Kind { VOTE_REQUEST, VOTE_RESPONSE, HEARTBEAT }
    public record Message(Kind kind, int from, int to, long term, boolean granted) { }
    /** All side effects are supplied by the runtime; timer generations suppress stale events. */
    public interface Runtime {
        void send(Message message);
        void electionTimer(int node, long generation);
        void heartbeatTimer(int node, long term);
        void trace(String event);
    }
    private final int id, size;
    private final Runtime runtime;
    private long term, timerGeneration;
    private int votedFor = -1;
    private Role role = Role.FOLLOWER;
    private final Set<Integer> votes = new HashSet<>();
    public ElectionNode(int id, int size, Runtime runtime) {
        if (id < 0 || id >= size || size < 3 || size % 2 == 0) throw new IllegalArgumentException("invalid cluster");
        this.id = id; this.size = size; this.runtime = runtime;
    }
    public void start() { resetTimer(); }
    public long term() { return term; }
    public Role role() { return role; }
    public void electionTimeout(long generation) {
        if (generation != timerGeneration || role == Role.LEADER) return;
        role = Role.CANDIDATE; term++; votedFor = id; votes.clear(); votes.add(id);
        runtime.trace("candidate node=" + id + " term=" + term); resetTimer();
        for (int peer = 0; peer < size; peer++) if (peer != id) runtime.send(new Message(Kind.VOTE_REQUEST, id, peer, term, false));
    }
    public void receive(Message m) {
        if (m.to != id || m.from < 0 || m.from >= size || m.from == id) throw new IllegalArgumentException("invalid peer");
        if (m.term > term) stepDown(m.term);
        switch (m.kind) {
            case VOTE_REQUEST -> {
                boolean grant = m.term == term && (votedFor == -1 || votedFor == m.from);
                if (grant) { votedFor = m.from; resetTimer(); }
                runtime.send(new Message(Kind.VOTE_RESPONSE, id, m.from, term, grant));
            }
            case VOTE_RESPONSE -> {
                if (m.term == term && role == Role.CANDIDATE && m.granted && votes.add(m.from) && votes.size() > size / 2) {
                    role = Role.LEADER; ++timerGeneration; runtime.trace("leader node=" + id + " term=" + term); heartbeat(term);
                }
            }
            case HEARTBEAT -> { if (m.term == term) { stepDown(term); resetTimer(); } }
        }
    }
    public void heartbeat(long epoch) {
        if (role != Role.LEADER || term != epoch) return;
        runtime.trace("heartbeat node=" + id + " term=" + term);
        for (int peer = 0; peer < size; peer++) if (peer != id) runtime.send(new Message(Kind.HEARTBEAT, id, peer, term, false));
        runtime.heartbeatTimer(id, epoch);
    }
    private void resetTimer() { runtime.electionTimer(id, ++timerGeneration); }
    private void stepDown(long newTerm) {
        if (newTerm > term) { term = newTerm; votedFor = -1; }
        role = Role.FOLLOWER; votes.clear();
    }
}
