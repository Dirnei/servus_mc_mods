package io.github.dirnei.mc.servus.storage.network;

/**
 * A block entity whose item capability is a door into the warehouse, not
 * storage: a warehouse never takes it as a foreign inventory, so its slots are
 * no stock, no put-away target and no source for picks.
 */
public interface NoStock {
}
