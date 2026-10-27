package io.github.dirnei.mc.servus.foundation.side;

/**
 * One tank a block offers in its Fluids layer. Its name and colour come from the block
 * entity, since they follow the tank's content.
 *
 * @param role which way sides may move fluid through the tank
 */
public record SideTank(TankRole role) {

    public static final SideTank ANY = new SideTank(TankRole.ANY);
}
