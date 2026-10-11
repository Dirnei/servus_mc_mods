package io.github.dirnei.mc.servus.foundation.datagen.model;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemDisplayContext;
import org.jspecify.annotations.Nullable;

/**
 * Writes one block or item model in the vanilla JSON format: parent, textures,
 * elements with faces and rotation, display transforms and ambient occlusion.
 *
 * @param <T> the concrete builder, returned by the chained setters
 */
public abstract class ModelBuilder<T extends ModelBuilder<T>> extends ModelFile {

    /** Face texture rotation in degrees. */
    public enum FaceRotation {
        ZERO(0), CLOCKWISE_90(90), UPSIDE_DOWN(180), COUNTERCLOCKWISE_90(270);

        private final int degrees;

        FaceRotation(int degrees) {
            this.degrees = degrees;
        }
    }

    private @Nullable ModelFile parent;
    private final Map<String, String> textures = new LinkedHashMap<>();
    private final List<ElementBuilder> elements = new ArrayList<>();
    private @Nullable Boolean ambientOcclusion;
    private @Nullable TransformsBuilder transforms;
    private @Nullable JsonObject loader;

    protected ModelBuilder(Identifier location) {
        super(location);
    }

    @SuppressWarnings("unchecked")
    protected T self() {
        return (T) this;
    }

    public T parent(ModelFile parent) {
        this.parent = parent;
        return self();
    }

    /** {@code texture} is a texture location or a {@code #key} reference. */
    public T texture(String key, String texture) {
        textures.put(key, texture.startsWith("#") ? texture : Identifier.parse(texture).toString());
        return self();
    }

    public T texture(String key, Identifier texture) {
        textures.put(key, texture.toString());
        return self();
    }

    /**
     * Kept for the existing model code. Since 26.1 the render layer of each
     * quad follows the transparency of its texture, so nothing is written.
     */
    public T renderType(String renderType) {
        return self();
    }

    /** See {@link #renderType(String)}. */
    public T renderType(Identifier renderType) {
        return self();
    }

    public T ao(boolean ambientOcclusion) {
        this.ambientOcclusion = ambientOcclusion;
        return self();
    }

    /** A custom loader block, written as is together with the {@code loader} key. */
    public T customLoader(Identifier loaderId, JsonObject data) {
        JsonObject json = data.deepCopy();
        json.addProperty("loader", loaderId.toString());
        this.loader = json;
        return self();
    }

    public ElementBuilder element() {
        ElementBuilder element = new ElementBuilder();
        elements.add(element);
        return element;
    }

    public TransformsBuilder transforms() {
        if (transforms == null) {
            transforms = new TransformsBuilder();
        }
        return transforms;
    }

    public JsonObject toJson() {
        JsonObject root = new JsonObject();
        if (loader != null) {
            loader.entrySet().forEach(entry -> root.add(entry.getKey(), entry.getValue()));
        }
        if (parent != null) {
            root.addProperty("parent", parent.getLocation().toString());
        }
        if (ambientOcclusion != null) {
            root.addProperty("ambientocclusion", ambientOcclusion);
        }
        if (transforms != null && !transforms.transforms.isEmpty()) {
            root.add("display", transforms.toJson());
        }
        if (!textures.isEmpty()) {
            JsonObject textureJson = new JsonObject();
            textures.forEach(textureJson::addProperty);
            root.add("textures", textureJson);
        }
        if (!elements.isEmpty()) {
            JsonArray elementJson = new JsonArray();
            elements.forEach(element -> elementJson.add(element.toJson()));
            root.add("elements", elementJson);
        }
        return root;
    }

    private static JsonArray vec(float x, float y, float z) {
        JsonArray array = new JsonArray();
        array.add(num(x));
        array.add(num(y));
        array.add(num(z));
        return array;
    }

    /** Whole numbers as ints, so the JSON reads like hand written models. */
    static Number num(float value) {
        return value == (int) value ? (Number) (int) value : (Number) value;
    }

    /** One cuboid of the model. */
    public class ElementBuilder {

        private float[] from = {0, 0, 0};
        private float[] to = {16, 16, 16};
        private final Map<Direction, FaceBuilder> faces = new EnumMap<>(Direction.class);
        private final List<Direction> faceOrder = new ArrayList<>();
        private @Nullable RotationBuilder rotation;
        private boolean shade = true;

        public ElementBuilder from(float x, float y, float z) {
            from = new float[] {x, y, z};
            return this;
        }

        public ElementBuilder to(float x, float y, float z) {
            to = new float[] {x, y, z};
            return this;
        }

        public ElementBuilder shade(boolean shade) {
            this.shade = shade;
            return this;
        }

        /** The face on {@code direction}, created on first use. */
        public FaceBuilder face(Direction direction) {
            return faces.computeIfAbsent(direction, d -> {
                faceOrder.add(d);
                return new FaceBuilder(d);
            });
        }

        public ElementBuilder allFaces(BiConsumer<Direction, FaceBuilder> action) {
            for (Direction direction : Direction.values()) {
                action.accept(direction, face(direction));
            }
            return this;
        }

        public ElementBuilder allFacesExcept(BiConsumer<Direction, FaceBuilder> action, Set<Direction> except) {
            for (Direction direction : Direction.values()) {
                if (!except.contains(direction)) {
                    action.accept(direction, face(direction));
                }
            }
            return this;
        }

        public ElementBuilder faces(BiConsumer<Direction, FaceBuilder> action) {
            faceOrder.forEach(direction -> action.accept(direction, faces.get(direction)));
            return this;
        }

        public RotationBuilder rotation() {
            if (rotation == null) {
                rotation = new RotationBuilder();
            }
            return rotation;
        }

        public T end() {
            return self();
        }

        JsonObject toJson() {
            JsonObject json = new JsonObject();
            json.add("from", vec(from[0], from[1], from[2]));
            json.add("to", vec(to[0], to[1], to[2]));
            if (rotation != null) {
                json.add("rotation", rotation.toJson());
            }
            if (!shade) {
                json.addProperty("shade", false);
            }
            JsonObject faceJson = new JsonObject();
            for (Direction direction : faceOrder) {
                faceJson.add(direction.getSerializedName(), faces.get(direction).toJson());
            }
            json.add("faces", faceJson);
            return json;
        }

        /** One face of the element. */
        public class FaceBuilder {

            private final Direction direction;
            private @Nullable String texture;
            private float @Nullable [] uvs;
            private @Nullable Direction cullface;
            private int tintIndex = -1;
            private FaceRotation rotation = FaceRotation.ZERO;

            FaceBuilder(Direction direction) {
                this.direction = direction;
            }

            /** A {@code #key} reference to one of the model's textures. */
            public FaceBuilder texture(String texture) {
                this.texture = texture;
                return this;
            }

            public FaceBuilder uvs(float u0, float v0, float u1, float v1) {
                this.uvs = new float[] {u0, v0, u1, v1};
                return this;
            }

            public FaceBuilder cullface(@Nullable Direction cullface) {
                this.cullface = cullface;
                return this;
            }

            public FaceBuilder tintindex(int tintIndex) {
                this.tintIndex = tintIndex;
                return this;
            }

            public FaceBuilder rotation(FaceRotation rotation) {
                this.rotation = rotation;
                return this;
            }

            public ElementBuilder end() {
                return ElementBuilder.this;
            }

            JsonObject toJson() {
                JsonObject json = new JsonObject();
                if (uvs != null) {
                    JsonArray array = new JsonArray();
                    for (float uv : uvs) {
                        array.add(num(uv));
                    }
                    json.add("uv", array);
                }
                if (rotation != FaceRotation.ZERO) {
                    json.addProperty("rotation", rotation.degrees);
                }
                json.addProperty("texture", texture == null ? "#missing" : texture);
                if (cullface != null) {
                    json.addProperty("cullface", cullface.getSerializedName());
                }
                if (tintIndex != -1) {
                    json.addProperty("tintindex", tintIndex);
                }
                return json;
            }
        }

        /** Rotation of the whole element around an origin. */
        public class RotationBuilder {

            private float[] origin = {8, 8, 8};
            private Direction.Axis axis = Direction.Axis.Y;
            private float angle;
            private boolean rescale;

            public RotationBuilder origin(float x, float y, float z) {
                origin = new float[] {x, y, z};
                return this;
            }

            public RotationBuilder axis(Direction.Axis axis) {
                this.axis = axis;
                return this;
            }

            public RotationBuilder angle(float angle) {
                this.angle = angle;
                return this;
            }

            public RotationBuilder rescale(boolean rescale) {
                this.rescale = rescale;
                return this;
            }

            public ElementBuilder end() {
                return ElementBuilder.this;
            }

            JsonElement toJson() {
                JsonObject json = new JsonObject();
                json.add("origin", vec(origin[0], origin[1], origin[2]));
                json.addProperty("axis", axis.getSerializedName());
                json.addProperty("angle", num(angle));
                if (rescale) {
                    json.addProperty("rescale", true);
                }
                return json;
            }
        }
    }

    /** The {@code display} block of item models. */
    public class TransformsBuilder {

        private final Map<ItemDisplayContext, TransformVecBuilder> transforms = new LinkedHashMap<>();

        public TransformVecBuilder transform(ItemDisplayContext context) {
            return transforms.computeIfAbsent(context, c -> new TransformVecBuilder());
        }

        public T end() {
            return self();
        }

        JsonObject toJson() {
            JsonObject json = new JsonObject();
            transforms.forEach((context, vec) -> json.add(context.getSerializedName().toLowerCase(Locale.ROOT), vec.toJson()));
            return json;
        }

        /** Rotation, translation and scale for one display context. */
        public class TransformVecBuilder {

            private float[] rotation = {0, 0, 0};
            private float[] translation = {0, 0, 0};
            private float[] scale = {1, 1, 1};

            public TransformVecBuilder rotation(float x, float y, float z) {
                rotation = new float[] {x, y, z};
                return this;
            }

            public TransformVecBuilder translation(float x, float y, float z) {
                translation = new float[] {x, y, z};
                return this;
            }

            public TransformVecBuilder scale(float scale) {
                return scale(scale, scale, scale);
            }

            public TransformVecBuilder scale(float x, float y, float z) {
                scale = new float[] {x, y, z};
                return this;
            }

            public TransformsBuilder end() {
                return TransformsBuilder.this;
            }

            JsonObject toJson() {
                JsonObject json = new JsonObject();
                json.add("rotation", vec(rotation[0], rotation[1], rotation[2]));
                json.add("translation", vec(translation[0], translation[1], translation[2]));
                json.add("scale", vec(scale[0], scale[1], scale[2]));
                return json;
            }
        }
    }
}
