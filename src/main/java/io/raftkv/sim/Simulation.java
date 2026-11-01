package io.raftkv.sim;

import io.raftkv.core.ElectionNode;
import java.util.*;

/** A deterministic virtual-time election model. Not yet a durable Raft implementation. */
public final class Simulation implements ElectionNode.Runtime {
    private static final long HEARTBEAT = 50;
    private final PriorityQueue<Event> events = new PriorityQueue<>(Comparator.comparingLong(Event::at).thenComparingLong(Event::sequence));
    private final SplittableRandom random;
    private final ElectionNode[] nodes;
    private final StringBuilder trace = new StringBuilder();
    private final Map<Long, Integer> leaders = new HashMap<>();
    private long now, sequence;
    private final Set<Integer> isolated = new HashSet<>();
    private int dropPercent, duplicatePercent;
    private record Event(long at, long sequence, Runnable action) { }

    public Simulation(long seed, int count) {
        if (count < 3 || count % 2 == 0) throw new IllegalArgumentException("odd node count >= 3 required");
        random = new SplittableRandom(seed);
        nodes = new ElectionNode[count];
        for (int i = 0; i < count; i++) nodes[i] = new ElectionNode(i, count, this);
        for (ElectionNode n : nodes) n.start();
    }
    private void schedule(long delay, Runnable action) { events.add(new Event(now + delay, sequence++, action)); }
    @Override public void send(ElectionNode.Message message) {
        if (blocked(message) || random.nextInt(100) < dropPercent) return;
        deliver(message);
        if (random.nextInt(100) < duplicatePercent) deliver(message);
    }
    private boolean blocked(ElectionNode.Message message) {
        return isolated.contains(message.from()) || isolated.contains(message.to());
    }
    private void deliver(ElectionNode.Message message) {
        schedule(1 + random.nextInt(10), () -> { if (!blocked(message)) nodes[message.to()].receive(message); });
    }
    /** Partition a node from every other node. In-flight messages are checked on delivery. */
    public void isolate(int node) {
        if (node < 0 || node >= nodes.length) throw new IllegalArgumentException("invalid node");
        isolated.add(node); trace("isolate node=" + node);
    }
    public void heal() { isolated.clear(); dropPercent = 0; duplicatePercent = 0; trace("network healed"); }
    public void networkFaults(int dropPercent, int duplicatePercent) {
        if (dropPercent < 0 || dropPercent > 100 || duplicatePercent < 0 || duplicatePercent > 100)
            throw new IllegalArgumentException("percent outside 0..100");
        this.dropPercent = dropPercent; this.duplicatePercent = duplicatePercent;
    }
    @Override public void electionTimer(int node, long generation) { schedule(150 + random.nextInt(151), () -> nodes[node].electionTimeout(generation)); }
    @Override public void heartbeatTimer(int node, long term) { schedule(HEARTBEAT, () -> nodes[node].heartbeat(term)); }
    @Override public void trace(String event) { trace.append(now).append(' ').append(event).append('\n'); }
    public String run(long untilMillis) {
        if (untilMillis < now) throw new IllegalArgumentException("time cannot go backwards");
        while (!events.isEmpty() && events.peek().at <= untilMillis) {
            Event event = events.remove(); now = event.at; event.action.run(); checkSafety();
        }
        now = untilMillis; return trace.toString();
    }
    public int currentLeaders() { int n = 0; for (ElectionNode node : nodes) if (node.role() == ElectionNode.Role.LEADER) n++; return n; }
    private void checkSafety() {
        for (int id = 0; id < nodes.length; id++) if (nodes[id].role() == ElectionNode.Role.LEADER) {
            Integer earlier = leaders.putIfAbsent(nodes[id].term(), id);
            if (earlier != null && earlier != id) throw new IllegalStateException("Election safety violated in term " + nodes[id].term() + "\n" + trace);
        }
    }
}
