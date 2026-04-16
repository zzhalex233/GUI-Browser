package com.zzhalex233.guibrowser.client.session;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.Objects;
import java.util.UUID;

import javax.annotation.Nullable;

public abstract class GuiSessionSourceKey {

    private GuiSessionSourceKey() {
    }

    public abstract JsonObject toJson();

    @Nullable
    public static GuiSessionSourceKey fromJson(JsonElement element) {
        if (element == null || !element.isJsonObject()) {
            return null;
        }
        JsonObject obj = element.getAsJsonObject();
        String type = obj.has("type") ? obj.get("type").getAsString() : null;
        if ("block".equals(type)) {
            return BlockKey.fromJson(obj);
        }
        if ("entity".equals(type)) {
            return EntityKey.fromJson(obj);
        }
        return null;
    }

    public static BlockKey fromBlockSource(GuiSessionSource.BlockSource source) {
        return new BlockKey(
            source.getPos().getX(),
            source.getPos().getY(),
            source.getPos().getZ(),
            source.getDimensionId()
        );
    }

    @Nullable
    public static GuiSessionSourceKey fromSource(@Nullable GuiSessionSource source) {
        if (source instanceof GuiSessionSource.BlockSource) {
            return fromBlockSource((GuiSessionSource.BlockSource) source);
        }
        // EntitySource cannot produce a persistent key without a live entity reference
        return null;
    }

    public static final class BlockKey extends GuiSessionSourceKey {
        private final int x;
        private final int y;
        private final int z;
        private final int dimensionId;

        public BlockKey(int x, int y, int z, int dimensionId) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.dimensionId = dimensionId;
        }

        public int getX() { return x; }
        public int getY() { return y; }
        public int getZ() { return z; }
        public int getDimensionId() { return dimensionId; }

        public GuiSessionSource.BlockSource toBlockSource() {
            return new GuiSessionSource.BlockSource(
                new net.minecraft.util.math.BlockPos(x, y, z), dimensionId
            );
        }

        @Override
        public JsonObject toJson() {
            JsonObject obj = new JsonObject();
            obj.addProperty("type", "block");
            obj.addProperty("x", x);
            obj.addProperty("y", y);
            obj.addProperty("z", z);
            obj.addProperty("dim", dimensionId);
            return obj;
        }

        @Nullable
        static BlockKey fromJson(JsonObject obj) {
            if (!obj.has("x") || !obj.has("y") || !obj.has("z") || !obj.has("dim")) {
                return null;
            }
            return new BlockKey(
                obj.get("x").getAsInt(),
                obj.get("y").getAsInt(),
                obj.get("z").getAsInt(),
                obj.get("dim").getAsInt()
            );
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof BlockKey)) return false;
            BlockKey that = (BlockKey) o;
            return x == that.x && y == that.y && z == that.z && dimensionId == that.dimensionId;
        }

        @Override
        public int hashCode() {
            return Objects.hash(x, y, z, dimensionId);
        }

        @Override
        public String toString() {
            return x + ", " + y + ", " + z + " (dim " + dimensionId + ")";
        }
    }

    public static final class EntityKey extends GuiSessionSourceKey {
        private final String entityUUID;
        private final String entityClassName;

        public EntityKey(String entityUUID, String entityClassName) {
            this.entityUUID = Objects.requireNonNull(entityUUID);
            this.entityClassName = Objects.requireNonNull(entityClassName);
        }

        public String getEntityUUID() { return entityUUID; }
        public String getEntityClassName() { return entityClassName; }

        @Override
        public JsonObject toJson() {
            JsonObject obj = new JsonObject();
            obj.addProperty("type", "entity");
            obj.addProperty("uuid", entityUUID);
            obj.addProperty("className", entityClassName);
            return obj;
        }

        @Nullable
        static EntityKey fromJson(JsonObject obj) {
            if (!obj.has("uuid") || !obj.has("className")) {
                return null;
            }
            return new EntityKey(
                obj.get("uuid").getAsString(),
                obj.get("className").getAsString()
            );
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof EntityKey)) return false;
            EntityKey that = (EntityKey) o;
            return entityUUID.equals(that.entityUUID);
        }

        @Override
        public int hashCode() {
            return entityUUID.hashCode();
        }

        @Override
        public String toString() {
            return entityClassName + " (" + entityUUID.substring(0, 8) + "...)";
        }
    }
}
