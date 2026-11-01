package io.raftkv.sim;

public final class Main {
    public static void main(String[] args) {
        long seed = 1, duration = 1000;
        for (String arg : args) {
            if (arg.startsWith("--seed=")) seed = Long.parseLong(arg.substring(7));
            else if (arg.startsWith("--duration=")) duration = Long.parseLong(arg.substring(11));
            else throw new IllegalArgumentException("Unknown argument: " + arg);
        }
        Simulation sim = new Simulation(seed, 3);
        System.out.printf("seed=%d duration_ms=%d%n", seed, duration);
        System.out.print(sim.run(duration));
        System.out.println("current_leaders=" + sim.currentLeaders());
    }
}
