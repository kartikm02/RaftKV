package io.raftkv.sim;

import java.util.*;

/** A deterministic virtual-time election model. Not yet a durable Raft implementation. */
public final class Simulation {
    private static final long HEARTBEAT = 50;
    private final PriorityQueue<Event> events = new PriorityQueue<>(Comparator.comparingLong(Event::at).thenComparingLong(Event::sequence));
    private final SplittableRandom random;
    private final Node[] nodes;
    private final StringBuilder trace = new StringBuilder();
    private final Map<Long, Integer> leaders = new HashMap<>();
    private long now, sequence;
    private record Event(long at, long sequence, Runnable action) { }
    enum Role { FOLLOWER, CANDIDATE, LEADER }
    enum Kind { VOTE_REQUEST, VOTE_RESPONSE, HEARTBEAT }
    record Message(Kind kind, int from, int to, long term, boolean granted) { }

    public Simulation(long seed, int count) {
        if (count < 3 || count % 2 == 0) throw new IllegalArgumentException("odd node count >= 3 required");
        random = new SplittableRandom(seed);
        nodes = new Node[count];
        for (int i = 0; i < count; i++) nodes[i] = new Node(i);
        for (Node n : nodes) n.resetTimer();
    }
    private void schedule(long delay, Runnable action) { events.add(new Event(now + delay, sequence++, action)); }
    private void send(Message message) { schedule(1 + random.nextInt(10), () -> nodes[message.to].receive(message)); }
    private void log(String s) { trace.append(now).append(' ').append(s).append('\n'); }
    public String run(long untilMillis) {
        if (untilMillis < now) throw new IllegalArgumentException("time cannot go backwards");
        while (!events.isEmpty() && events.peek().at <= untilMillis) {
            Event event = events.remove(); now = event.at; event.action.run(); checkSafety();
        }
        now = untilMillis;
        return trace.toString();
    }
    public int currentLeaders() { int n = 0; for (Node node : nodes) if (node.role == Role.LEADER) n++; return n; }
    private void checkSafety() {
        for (Node n : nodes) if (n.role == Role.LEADER) {
            Integer earlier = leaders.putIfAbsent(n.term, n.id);
            if (earlier != null && earlier != n.id) throw new IllegalStateException("Election safety violated in term " + n.term + "\n" + trace);
        }
    }
    private final class Node {
        final int id; long term, timerGeneration; int votedFor = -1; Role role = Role.FOLLOWER;
        final Set<Integer> votes = new HashSet<>();
        Node(int id) { this.id = id; }
        void resetTimer() {
            long generation = ++timerGeneration;
            schedule(150 + random.nextInt(151), () -> { if (generation == timerGeneration && role != Role.LEADER) startElection(); });
        }
        void stepDown(long newTerm) {
            if (newTerm > term) { term = newTerm; votedFor = -1; }
            role = Role.FOLLOWER; votes.clear();
        }
        void startElection() {
            role = Role.CANDIDATE; term++; votedFor = id; votes.clear(); votes.add(id);
            log("candidate node=" + id + " term=" + term); resetTimer();
            for (Node peer : nodes) if (peer.id != id) send(new Message(Kind.VOTE_REQUEST, id, peer.id, term, false));
        }
        void receive(Message m) {
            if (m.term > term) stepDown(m.term);
            switch (m.kind) {
                case VOTE_REQUEST -> {
                    boolean grant = m.term == term && (votedFor == -1 || votedFor == m.from);
                    if (grant) { votedFor = m.from; resetTimer(); }
                    send(new Message(Kind.VOTE_RESPONSE, id, m.from, term, grant));
                }
                case VOTE_RESPONSE -> {
                    if (m.term == term && role == Role.CANDIDATE && m.granted && votes.add(m.from) && votes.size() > nodes.length / 2) {
                        role = Role.LEADER; ++timerGeneration; log("leader node=" + id + " term=" + term); heartbeat(term);
                    }
                }
                case HEARTBEAT -> {
                    if (m.term == term && m.from != id) { stepDown(term); resetTimer(); }
                }
            }
        }
        void heartbeat(long epoch) {
            if (role != Role.LEADER || term != epoch) return;
            log("heartbeat node=" + id + " term=" + term);
            for (Node peer : nodes) if (peer.id != id) send(new Message(Kind.HEARTBEAT, id, peer.id, term, false));
            schedule(HEARTBEAT, () -> heartbeat(epoch));
        }
    }
}
