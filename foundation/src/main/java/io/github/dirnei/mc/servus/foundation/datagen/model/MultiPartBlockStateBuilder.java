package io.github.dirnei.mc.servus.foundation.datagen.model;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.properties.Property;
import org.jspecify.annotations.Nullable;

/**
 * A {@code multipart} block state file: parts with models that apply when
 * their conditions hold. Conditions on one part (or group) must all hold
 * unless {@code useOr} is set; nested groups combine the same way.
 */
public final class MultiPartBlockStateBuilder {

    private final Block owner;
    private final List<PartBuilder> parts = new ArrayList<>();

    MultiPartBlockStateBuilder(Block owner) {
        this.owner = owner;
    }

    /** Starts a part; finish its models with {@code addModel()}. */
    public ConfiguredModel.Builder<PartBuilder> part() {
        return new ConfiguredModel.Builder<>(models -> {
            PartBuilder part = new PartBuilder(models);
            parts.add(part);
            return part;
        });
    }

    JsonObject toJson() {
        JsonArray array = new JsonArray();
        parts.forEach(part -> array.add(part.toJson()));
        JsonObject root = new JsonObject();
        root.add("multipart", array);
        return root;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static JsonObject conditionsJson(Map<Property<?>, List<Comparable<?>>> conditions, boolean useOr) {
        JsonObject json = new JsonObject();
        conditions.forEach((property, values) -> json.addProperty(property.getName(),
                values.stream().map(value -> ((Property) property).getName(value)).collect(Collectors.joining("|"))));
        if (!useOr) {
            return json;
        }
        JsonArray or = new JsonArray();
        json.entrySet().forEach(entry -> {
            JsonObject single = new JsonObject();
            single.add(entry.getKey(), entry.getValue());
            or.add(single);
        });
        JsonObject root = new JsonObject();
        root.add("OR", or);
        return root;
    }

    private static JsonObject groupsJson(List<PartBuilder.ConditionGroup> groups, boolean useOr) {
        JsonArray array = new JsonArray();
        groups.forEach(group -> array.add(group.toJson()));
        JsonObject root = new JsonObject();
        root.add(useOr ? "OR" : "AND", array);
        return root;
    }

    /** One part of the multipart file. */
    public final class PartBuilder {

        private final List<ConfiguredModel> models;
        private final Map<Property<?>, List<Comparable<?>>> conditions = new LinkedHashMap<>();
        private final List<ConditionGroup> groups = new ArrayList<>();
        private boolean useOr;

        PartBuilder(List<ConfiguredModel> models) {
            this.models = models;
        }

        public PartBuilder useOr() {
            useOr = true;
            return this;
        }

        @SafeVarargs
        public final <T extends Comparable<T>> PartBuilder condition(Property<T> property, T... values) {
            check(property, values);
            if (!groups.isEmpty()) {
                throw new IllegalStateException("A part has either conditions or nested groups");
            }
            conditions.put(property, new ArrayList<>(Arrays.asList(values)));
            return this;
        }

        public ConditionGroup nestedGroup() {
            if (!conditions.isEmpty()) {
                throw new IllegalStateException("A part has either conditions or nested groups");
            }
            ConditionGroup group = new ConditionGroup(null);
            groups.add(group);
            return group;
        }

        public MultiPartBlockStateBuilder end() {
            return MultiPartBlockStateBuilder.this;
        }

        private void check(Property<?> property, Object[] values) {
            if (values.length == 0) {
                throw new IllegalArgumentException("No values for " + property.getName());
            }
            if (!owner.getStateDefinition().getProperties().contains(property)) {
                throw new IllegalArgumentException(property + " is not a property of " + owner);
            }
        }

        JsonObject toJson() {
            JsonObject json = new JsonObject();
            if (!conditions.isEmpty()) {
                json.add("when", conditionsJson(conditions, useOr));
            } else if (!groups.isEmpty()) {
                json.add("when", groupsJson(groups, useOr));
            }
            json.add("apply", ConfiguredModel.toJson(models));
            return json;
        }

        /** A group of conditions inside a part, possibly nested. */
        public final class ConditionGroup {

            private final @Nullable ConditionGroup parent;
            private final Map<Property<?>, List<Comparable<?>>> groupConditions = new LinkedHashMap<>();
            private final List<ConditionGroup> nested = new ArrayList<>();
            private boolean groupOr;

            ConditionGroup(@Nullable ConditionGroup parent) {
                this.parent = parent;
            }

            @SafeVarargs
            public final <T extends Comparable<T>> ConditionGroup condition(Property<T> property, T... values) {
                check(property, values);
                if (!nested.isEmpty()) {
                    throw new IllegalStateException("A group has either conditions or nested groups");
                }
                groupConditions.put(property, new ArrayList<>(Arrays.asList(values)));
                return this;
            }

            public ConditionGroup nestedGroup() {
                if (!groupConditions.isEmpty()) {
                    throw new IllegalStateException("A group has either conditions or nested groups");
                }
                ConditionGroup group = new ConditionGroup(this);
                nested.add(group);
                return group;
            }

            public ConditionGroup endNestedGroup() {
                if (parent == null) {
                    throw new IllegalStateException("Not a nested group, use end()");
                }
                return parent;
            }

            public PartBuilder end() {
                if (parent != null) {
                    throw new IllegalStateException("A nested group, use endNestedGroup()");
                }
                return PartBuilder.this;
            }

            public ConditionGroup useOr() {
                groupOr = true;
                return this;
            }

            JsonObject toJson() {
                if (!groupConditions.isEmpty()) {
                    return conditionsJson(groupConditions, groupOr);
                }
                if (!nested.isEmpty()) {
                    return groupsJson(nested, groupOr);
                }
                return new JsonObject();
            }
        }
    }
}
