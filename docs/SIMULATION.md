# Simulation

Run `./gradlew sim -PsimArgs="--seed=42 --duration=1000"`. Reusing the seed and duration produces the same trace. Time is virtual milliseconds. The queue is ordered by timestamp then insertion sequence. Current tests run 1,000 healthy three-node election scenarios and assert that a leader is present after 2,000 virtual milliseconds; election safety is checked after each event. The event queue models delay but does not inject loss, duplicates, partitions, crashes, or disk faults. The run does not check log or linearizability invariants.
