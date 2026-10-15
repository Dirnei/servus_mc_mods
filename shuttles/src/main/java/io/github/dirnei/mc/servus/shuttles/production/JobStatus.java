package io.github.dirnei.mc.servus.shuttles.production;

import java.util.List;

/**
 * Says what a crafting job is doing in plain words: a lang key suffix and its
 * arguments, from facts about the order and its first unfinished step.
 */
public final class JobStatus {

    private JobStatus() {
    }

    /**
     * The first step of the order that is not done.
     *
     * @param machine   the producer's name
     * @param state     waiting_station, delivering, producing or done
     * @param reason    why the step waits, empty when it does not
     * @param verb      crafting, smelting, ... or working
     * @param done      crafts finished
     * @param total     crafts of the step
     * @param outOfFuel whether the producer keeps upkeep and holds none
     * @param sending   the item the step hands straight to the next step's producer, empty when none
     * @param sendingTo the receiving producer's name, empty when none
     * @param noPower   whether the producer is a Crafting Station whose craft waits for the warehouse's power
     */
    public record Step(String machine, String state, String reason, String verb, int done, int total, boolean outOfFuel,
                       String sending, String sendingTo, boolean noPower) {

        public Step(String machine, String state, String reason, String verb, int done, int total, boolean outOfFuel,
                    String sending, String sendingTo) {
            this(machine, state, reason, verb, done, total, outOfFuel, sending, sendingTo, false);
        }

        public Step(String machine, String state, String reason, String verb, int done, int total, boolean outOfFuel) {
            this(machine, state, reason, verb, done, total, outOfFuel, "", "");
        }
    }

    /** A lang key suffix under {@code gui.servus_shuttles.production.job.} and its arguments. */
    public record Status(String key, List<String> args) {
    }

    /**
     * @param orderState   waiting, running, blocked, done or cancelled
     * @param orderReason  why the order waits or is blocked, empty otherwise
     * @param missingName  the first missing raw material, empty when nothing is missing
     * @param missingCount how many of it are missing
     * @param current      the first step that is not done, null when there is none
     */
    public static Status of(String orderState, String orderReason, String missingName, int missingCount, Step current) {
        switch (orderState) {
            case "done" -> {
                return new Status("done", List.of());
            }
            case "cancelled" -> {
                return new Status("cancelled", List.of());
            }
            case "waiting" -> {
                return new Status("waiting_cpu", List.of());
            }
            default -> {
            }
        }
        // The plan keeps what was missing when it was made; it only matters while the order is blocked.
        if (orderState.equals("blocked") && missingCount > 0 && !missingName.isEmpty()) {
            return new Status("missing", List.of(Integer.toString(missingCount), missingName));
        }
        if (current == null) {
            return running(orderReason);
        }
        if (current.outOfFuel()) {
            return new Status("out_of_fuel", List.of(current.machine()));
        }
        if (current.noPower()) {
            return new Status("no_power", List.of(current.machine()));
        }
        if (!current.reason().isEmpty()) {
            return new Status("blocked", List.of(current.reason()));
        }
        if (!current.sending().isEmpty()) {
            return new Status("sending", List.of(current.machine(), current.sending(), current.sendingTo()));
        }
        return switch (current.state()) {
            case "waiting_station" -> new Status("waiting_machine", List.of(current.machine()));
            case "delivering" -> new Status("delivering", List.of(current.machine()));
            case "producing" -> new Status("working", List.of(current.machine(), current.verb(),
                    Integer.toString(current.done()), Integer.toString(current.total())));
            default -> running(orderReason);
        };
    }

    private static Status running(String orderReason) {
        return orderReason.isEmpty() ? new Status("running", List.of()) : new Status("blocked", List.of(orderReason));
    }

    /** Crafts finished from the collected output, capped at the crafts of the step. */
    public static int craftsDone(int collected, int perCraft, int total) {
        if (perCraft <= 0) {
            return 0;
        }
        return Math.max(0, Math.min(total, collected / perCraft));
    }
}
