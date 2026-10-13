package io.github.dirnei.mc.servus.foundation.datagen.model;

import net.minecraft.core.Direction;

/** Block state rotations for models built facing north. */
public final class ModelRotations {

    private ModelRotations() {
    }

    /** Block state rotation Y that turns a north facing model toward the horizontal {@code facing}. */
    public static int rotationY(Direction facing) {
        return ((int) facing.toYRot() + 180) % 360;
    }
}
