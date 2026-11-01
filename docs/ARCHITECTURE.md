# Architecture and scope

This repository is an early prototype, not a complete Raft implementation. The deterministic election model receives timer and message events through an injected runtime. The simulator supplies seeded randomness, virtual-time scheduling, message delay, loss and duplication, and simple node isolation. Nodes are not connected to the storage or key-value components yet. The event loop is single-threaded.

The storage prototype provides atomically replaced term/vote metadata and a single-file, CRC32C-framed WAL with batch fsync. It is not a segmented log, has no snapshot support or suffix replacement, and has not been tested for power-loss behavior on different filesystems. WAL recovery truncates incomplete final records and final records with bad checksums. A checksum failure in a complete non-final record fails recovery. A damaged length field can make corruption indistinguishable from a torn tail; this format cannot always classify arbitrary corruption safely. Do not rely on this storage for valuable data.

The independent ordered byte-key state machine supports PUT, DELETE, CAS, reads, and last-response session retry suppression with sequential request numbers. The state machine is in-memory and is not replicated or snapshotted. Its get operation has no cluster consistency guarantee. Client registration, persistence and replay of sessions are not implemented.

Remaining work includes durable consensus integration, PreVote, CheckQuorum, replication, commit and apply, ReadIndex, history checking, snapshots, membership changes, transport, a real local cluster, metrics, chaos and benchmarks. No correctness or durability claims extend beyond the tests actually run.
