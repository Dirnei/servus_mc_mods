package io.github.dirnei.mc.servus.foundation.datagen.model;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.List;
import java.util.function.Function;

/** A model with its rotation in a block state file. */
public final class ConfiguredModel {

    public final ModelFile model;
    public final int rotationX;
    public final int rotationY;
    public final boolean uvLock;

    public ConfiguredModel(ModelFile model, int rotationX, int rotationY, boolean uvLock) {
        this.model = model;
        this.rotationX = rotationX;
        this.rotationY = rotationY;
        this.uvLock = uvLock;
    }

    public ConfiguredModel(ModelFile model) {
        this(model, 0, 0, false);
    }

    public static Builder<ConfiguredModel[]> builder() {
        return new Builder<>(models -> models.toArray(ConfiguredModel[]::new));
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
        return json;
    }

    /** A single model as an object, several as an array. */
    static JsonElement toJson(List<ConfiguredModel> models) {
        if (models.size() == 1) {
            return models.get(0).toJson();
        }
        JsonArray array = new JsonArray();
        models.forEach(model -> array.add(model.toJson()));
        return array;
    }

    /**
     * Builds one configured model.
     *
     * @param <T> what {@link #build()} or {@link #addModel()} hands back
     */
    public static final class Builder<T> {

        private final Function<List<ConfiguredModel>, T> callback;
        private ModelFile model;
        private int rotationX;
        private int rotationY;
        private boolean uvLock;

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

        public T build() {
            if (model == null) {
                throw new IllegalStateException("No model set");
            }
            return callback.apply(List.of(new ConfiguredModel(model, rotationX, rotationY, uvLock)));
        }

        /** Same as {@link #build()}, the name the multipart builder uses. */
        public T addModel() {
            return build();
        }
    }
}
