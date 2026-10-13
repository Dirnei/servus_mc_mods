package io.github.dirnei.mc.servus.storage.shuttle;

/**
 * Where a shuttle is heading. Field runs drive TO_FIELD, work a rail while
 * WORKING, fetch seeds TO_SEEDS or fertilizer TO_SUPPLY, put the cargo away
 * TO_STORE and wait at home in WAIT_STORE while nothing has room for it.
 */
enum Leg { TO_SOURCE, TO_TARGET, TO_STATION, TO_TERMINAL, WAITING_ROOM, HOME, TO_FIELD, WORKING, TO_SEEDS, TO_SUPPLY, TO_STORE, WAIT_STORE }
