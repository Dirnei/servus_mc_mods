package io.github.dirnei.mc.servus.foundation.data.packet;

/** Why a delivery failed. */
public enum DeliveryReason {
    /** No route of data ducts links source and target. */
    NO_ROUTE,
    /** A packet's route broke and no other route was left. */
    ROUTE_BROKEN,
    /** The target is gone or takes nothing. */
    NO_TARGET
}
