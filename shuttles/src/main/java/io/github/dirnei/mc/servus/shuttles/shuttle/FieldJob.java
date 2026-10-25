package io.github.dirnei.mc.servus.shuttles.shuttle;

import io.github.dirnei.mc.servus.shuttles.field.FieldKind;
import io.github.dirnei.mc.servus.shuttles.shuttle.module.JobNeeds;
import net.minecraft.core.BlockPos;

/**
 * A field run: a shuttle with a Harvester harvests, tills and plants a Crops
 * field, a shuttle with a Lumber module fells and plants a Trees field; both
 * bring what they gather to the head of the field.
 *
 * @param id   the job's id, unique in the world
 * @param head the Field Head of the field
 * @param kind what the field grew when the job was queued
 */
public record FieldJob(int id, BlockPos head, FieldKind kind) implements ShuttleJob {

    @Override
    public JobNeeds needs() {
        return kind.trees() ? JobNeeds.LUMBER : JobNeeds.HARVESTER;
    }
}
