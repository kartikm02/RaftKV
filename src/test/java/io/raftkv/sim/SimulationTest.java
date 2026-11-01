package io.raftkv.sim;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SimulationTest {
    @Test void sameSeedReplaysExactly() {
        assertEquals(new Simulation(42, 3).run(3000), new Simulation(42, 3).run(3000));
    }
    @Test void healthyClustersElectOneLeaderAcrossSeeds() {
        for (int seed = 0; seed < 1000; seed++) {
            Simulation s = new Simulation(seed, 3);
            s.run(2000);
            assertEquals(1, s.currentLeaders(), "seed=" + seed);
        }
    }
    @Test void electionRecoversAfterPartitionAndLoss() {
        for (int seed = 0; seed < 1000; seed++) {
            Simulation s = new Simulation(seed, 3);
            s.run(1000); s.isolate(0); s.networkFaults(20, 25); s.run(2500);
            s.heal(); s.run(5000);
            assertEquals(1, s.currentLeaders(), "seed=" + seed);
        }
    }
    @Test void faultsReplayExactly() {
        Simulation a = new Simulation(99, 3), b = new Simulation(99, 3);
        a.networkFaults(20, 40); b.networkFaults(20, 40);
        a.run(1000); b.run(1000); a.isolate(1); b.isolate(1);
        assertEquals(a.run(2000), b.run(2000));
    }
    @Test void invalidClusterSizeRejected() { assertThrows(IllegalArgumentException.class, () -> new Simulation(1, 4)); }
    @Test void runCanBeResumed() {
        Simulation resumed = new Simulation(7, 3);
        resumed.run(500);
        assertEquals(new Simulation(7, 3).run(1000), resumed.run(1000));
    }
}
