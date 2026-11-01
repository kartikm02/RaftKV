# RaftKV

RaftKV is an in-progress Java 25 learning project for building a Raft-replicated key-value store from scratch. This repository currently contains an **election simulation prototype**. It does not yet contain a working key-value store, network server, durable storage, local cluster, or AWS deployment. Do not use it to store data or expose it to a network.

## Run

Install a Java 25 JDK, then run:

```sh
./gradlew test
./gradlew sim -PsimArgs="--seed=42 --duration=1000"
```

The simulator executes a three-node election in virtual time using a seed, an ordered event queue, randomized election timers, vote messages, and heartbeats. It checks election safety after every delivered event and emits a reproducible trace. The current model assumes a healthy network with variable delivery delays; it has **no log, persistence, crash recovery, partitions, PreVote, CheckQuorum, or client operations**. It is not a complete implementation of Raft.

See [SIMULATION.md](docs/SIMULATION.md), [ARCHITECTURE.md](docs/ARCHITECTURE.md), [SECURITY.md](SECURITY.md) and [BENCHMARKS.md](docs/BENCHMARKS.md).
