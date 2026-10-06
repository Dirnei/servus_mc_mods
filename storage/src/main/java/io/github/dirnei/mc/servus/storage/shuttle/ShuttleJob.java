package io.github.dirnei.mc.servus.storage.shuttle;

/** Something a shuttle does: fetch a pull or carry a put-away. Both share one queue. */
public sealed interface ShuttleJob permits PickJob, PutAwayJob {
}
