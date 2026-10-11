package io.github.dirnei.mc.servus.foundation.datagen.model;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/** A model with its rotation in a block state file. */
public final class ConfiguredModel {

    public final ModelFile model;
    public final int rotationX;
    public final int rotationY;
    public final boolean uvLock;
    public final int weight;

    public ConfiguredModel(ModelFile model, int rotationX, int rotationY, boolean uvLock, int weight) {
        this.model = model;
        this.rotationX = rotationX;
        this.rotationY = rotationY;
        this.uvLock = uvLock;
        this.weight = weight;
    }

    public ConfiguredModel(ModelFile model) {
        this(model, 0, 0, false, 1);
    }

    public static Builder<ConfiguredModel[]> builder() {
        return new Builder<>(models -> models.toArray(ConfiguredModel[]::new));
    }

    public static ConfiguredModel[] allYRotations(ModelFile model, int x, boolean uvlock) {
        return new ConfiguredModel[] {new ConfiguredModel(model, x, 0, uvlock, 1), new ConfiguredModel(model, x, 90, uvlock, 1),
                new ConfiguredModel(model, x, 180, uvlock, 1), new ConfiguredModel(model, x, 270, uvlock, 1)};
    }

    JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("model", model.getLocation().toString());
        if (rotationX != 0) {
            json.addProperty("x", rotationX);
        }
        if (rotationY != 0) {
            json.addProperty("y", rotationY);
        }
        if (uvLock) {
            json.addProperty("uvlock", true);
        }
        if (weight != 1) {
            json.addProperty("weight", weight);
        }
        return json;
    }

    static JsonElement toJson(List<ConfiguredModel> models) {
        if (models.size() == 1) {
            return models.get(0).toJson();
        }
        JsonArray array = new JsonArray();
        models.forEach(model -> array.add(model.toJson()));
        return array;
    }

    /**
     * Builds one or more configured models.
     *
     * @param <T> what {@link #build()} or {@link #addModel()} hands back
     */
    public static final class Builder<T> {

        private final Function<List<ConfiguredModel>, T> callback;
        private final List<ConfiguredModel> done = new ArrayList<>();
        private ModelFile model;
        private int rotationX;
        private int rotationY;
        private boolean uvLock;
        private int weight = 1;

        Builder(Function<List<ConfiguredModel>, T> callback) {
            this.callback = callback;
        }

        public Builder<T> modelFile(ModelFile model) {
            this.model = model;
            return this;
        }

        public Builder<T> rotationX(int rotationX) {
            this.rotationX = rotationX;
            return this;
        }

        public Builder<T> rotationY(int rotationY) {
            this.rotationY = rotationY;
            return this;
        }

        public Builder<T> uvLock(boolean uvLock) {
            this.uvLock = uvLock;
            return this;
        }

        public Builder<T> weight(int weight) {
            this.weight = weight;
            return this;
        }

        /** Finishes this model and starts another one (weighted random variants). */
        public Builder<T> nextModel() {
            done.add(current());
            model = null;
            rotationX = 0;
            rotationY = 0;
            uvLock = false;
            weight = 1;
            return this;
        }

        private ConfiguredModel current() {
            if (model == null) {
                throw new IllegalStateException("No model set");
            }
            return new ConfiguredModel(model, rotationX, rotationY, uvLock, weight);
        }

        public T build() {
            List<ConfiguredModel> all = new ArrayList<>(done);
            all.add(current());
            return callback.apply(all);
        }

        /** Same as {@link #build()}, the name the multipart builder uses. */
        public T addModel() {
            return build();
        }
    }
}
