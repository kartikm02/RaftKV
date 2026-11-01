# Phase status

- Phase 0: partial. Java/Gradle, CI, deterministic event queue, replayable election messages and an injected runtime are present. Protobuf, gRPC ADR, JMH, jqwik and a fully shared production core remain.
- Phase 1: partial. Simulator supports delay, loss, duplication and node isolation; it checks election safety. Standalone hard-state and single-file WAL prototypes have focused recovery tests. They are not connected to elections. PreVote, CheckQuorum, crash/restart, disk fault simulation, segmented WAL, property/fuzz testing and the 10,000-seed fault suite remain.
- Phase 2: partial. A standalone sequential byte-key state machine supports PUT, DELETE, CAS and cached last-session responses. Replication, durability integration, log invariants, client API and the 10,000-seed test remain.
- Phases 3–7: not implemented. There is no linearizability checker, snapshot, membership protocol, local cluster, chaos suite, observability, bug-injection evidence or performance benchmark.

A phase is complete only after its plan's exit criteria are demonstrated. See BENCHMARKS.md before making performance or resume claims.
