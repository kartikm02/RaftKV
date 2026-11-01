# Architecture and scope

The implemented prototype is a deterministic event queue ordered by virtual time and insertion order. Seeded randomness drives election timeouts and message delay. Nodes exchange vote requests, vote responses, and heartbeats within a single process; the simulation checks at most one elected leader per term after each event. This is deliberately an early model, not the production/simulation shared deterministic core described in the build plan.

```text
seed -> virtual event queue -> simulated nodes -> trace + election safety check
```

Remaining work includes a shared I/O-free core, persistent term/vote and a checksummed WAL, log replication, commit and application, client sessions, linearizable reads and history checking, snapshots, safe membership changes, transport, a real local cluster, metrics, chaos and benchmarks. No correctness or durability claims extend beyond the tests actually run.
