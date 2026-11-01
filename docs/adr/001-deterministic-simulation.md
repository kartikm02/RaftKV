# ADR 001: deterministic simulation

Use a seeded random generator and a virtual-time queue with a stable tie-breaker so a failing schedule can be replayed. Eventually the production and simulation runtimes should drive the same I/O-free Raft core; this prototype does not yet meet that architecture. Determinism alone does not establish correctness.
