package io.github.dirnei.mc.servus.delicacies.aquarium;

import java.util.Optional;

/**
 * Balance numbers of the aquarium. Pure Java. Loaded from {@code data/servus_delicacies/aquarium/aquarium.json}; every
 * field is optional there and falls back to {@link #DEFAULTS}.
 *
 * @param cycleTicks        ticks between two cycles
 * @param waterPerFish      mB of water one fish needs
 * @param breedRate         logistic growth rate per cycle
 * @param feedPerFish       feed items one fish eats per cycle
 * @param starveAfter       unfed cycles in a row the fish survive
 * @param starveLoss        share of the stock lost per cycle after that
 * @param dirtPerFish       dirt one fish adds per cycle
 * @param dirtPerFeed       dirt one eaten feed item adds
 * @param dirtPerBucket     dirt one bucket of water holds at zero quality
 * @param dyingBelow        quality below which fish die
 * @param dirtyLoss         share of the stock lost per cycle in dirty water
 * @param filterRate        dirt a filter removes per powered cycle
 * @param fePerTankBlock    FE per tick for each tank block
 * @param fePerLid          FE per tick for each lid slab
 * @param energyBufferTicks ticks of power the buffer holds
 * @param keepDefault       keep level of a new aquarium
 * @param drawnCap          most fish drawn in one aquarium
 */
public record AquariumStats(int cycleTicks, int waterPerFish, double breedRate, double feedPerFish, int starveAfter,
                            double starveLoss, double dirtPerFish, double dirtPerFeed, double dirtPerBucket, double dyingBelow,
                            double dirtyLoss, int filterRate, int fePerTankBlock, int fePerLid, int energyBufferTicks,
                            int keepDefault, int drawnCap) {

    public static final AquariumStats DEFAULTS = new AquariumStats(1200, 4000, 0.12, 0.1, 3, 0.05, 0.5, 1, 10, 0.2, 0.1,
            40, 2, 1, 4, 8, 16);

    /** Partial stats as written in JSON; {@link AquariumStatsManager#CODEC} reads them. */
    public record Partial(Optional<Integer> cycleTicks, Optional<Integer> waterPerFish, Optional<Double> breedRate,
                          Optional<Double> feedPerFish, Optional<Integer> starveAfter, Optional<Double> starveLoss,
                          Optional<Double> dirtPerFish, Optional<Double> dirtPerFeed, Optional<Double> dirtPerBucket,
                          Optional<Double> dyingBelow, Optional<Double> dirtyLoss, Optional<Integer> filterRate,
                          Optional<Integer> fePerTankBlock, Optional<Integer> fePerLid, Optional<Integer> energyBufferTicks,
                          Optional<Integer> keepDefault, Optional<Integer> drawnCap) {

        public AquariumStats over(AquariumStats defaults) {
            return new AquariumStats(
                    Math.max(1, cycleTicks.orElse(defaults.cycleTicks())),
                    Math.max(1, waterPerFish.orElse(defaults.waterPerFish())),
                    Math.max(0, breedRate.orElse(defaults.breedRate())),
                    Math.max(0, feedPerFish.orElse(defaults.feedPerFish())),
                    Math.max(0, starveAfter.orElse(defaults.starveAfter())),
                    share(starveLoss.orElse(defaults.starveLoss())),
                    Math.max(0, dirtPerFish.orElse(defaults.dirtPerFish())),
                    Math.max(0, dirtPerFeed.orElse(defaults.dirtPerFeed())),
                    Math.max(0.001, dirtPerBucket.orElse(defaults.dirtPerBucket())),
                    share(dyingBelow.orElse(defaults.dyingBelow())),
                    share(dirtyLoss.orElse(defaults.dirtyLoss())),
                    Math.max(0, filterRate.orElse(defaults.filterRate())),
                    Math.max(0, fePerTankBlock.orElse(defaults.fePerTankBlock())),
                    Math.max(0, fePerLid.orElse(defaults.fePerLid())),
                    Math.max(1, energyBufferTicks.orElse(defaults.energyBufferTicks())),
                    Math.max(0, keepDefault.orElse(defaults.keepDefault())),
                    Math.max(0, drawnCap.orElse(defaults.drawnCap())));
        }

        private static double share(double value) {
            return Math.max(0, Math.min(1, value));
        }
    }
}
