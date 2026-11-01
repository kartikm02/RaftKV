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
    @Test void invalidClusterSizeRejected() { assertThrows(IllegalArgumentException.class, () -> new Simulation(1, 4)); }
    @Test void runCanBeResumed() {
        Simulation resumed = new Simulation(7, 3);
        resumed.run(500);
        assertEquals(new Simulation(7, 3).run(1000), resumed.run(1000));
    }
}
