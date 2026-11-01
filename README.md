# RaftKV

RaftKV is an in-progress Java 25 learning project for building a Raft-replicated key-value store from scratch. This repository currently contains an **election simulation and isolated storage/state-machine prototypes**. It does not yet contain a working key-value store, network server, durable storage, local cluster, or AWS deployment. Do not use it to store data or expose it to a network.

## Run

Install a Java 25 JDK, then run:

```sh
./gradlew test
./gradlew sim -PsimArgs="--seed=42 --duration=1000"
```

The simulator executes a three-node election in virtual time using a seed, an ordered event queue, randomized election timers, vote messages, and heartbeats. It checks election safety after every delivered event and emits a reproducible trace. The simulation supports delay, loss, duplication and isolation through its Java API. It has **no replicated log, integrated persistence, crash recovery, PreVote, CheckQuorum, or client operations**. Standalone WAL, election metadata and in-memory state-machine components are present but are not connected to the simulation or a network server. It is not a complete implementation of Raft.

See [SIMULATION.md](docs/SIMULATION.md), [ARCHITECTURE.md](docs/ARCHITECTURE.md), [SECURITY.md](SECURITY.md) and [BENCHMARKS.md](docs/BENCHMARKS.md), and [phase status](docs/PHASES.md).
