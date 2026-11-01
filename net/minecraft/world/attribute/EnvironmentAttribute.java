package net.minecraft.world.attribute;

import com.mojang.serialization.Codec;
import java.util.Objects;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.Util;
import org.jspecify.annotations.Nullable;

public class EnvironmentAttribute<Value> {
   private final AttributeType<Value> type;
   private final Value defaultValue;
   private final AttributeRange<Value> valueRange;
   private final boolean isSyncable;
   private final boolean isPositional;
   private final boolean isSpatiallyInterpolated;

   EnvironmentAttribute(
      AttributeType<Value> p_458131_, Value p_458143_, AttributeRange<Value> p_458004_, boolean p_458087_, boolean p_457785_, boolean p_457872_
   ) {
      this.type = p_458131_;
      this.defaultValue = p_458143_;
      this.valueRange = p_458004_;
      this.isSyncable = p_458087_;
      this.isPositional = p_457785_;
      this.isSpatiallyInterpolated = p_457872_;
   }

   public static <Value> EnvironmentAttribute.Builder<Value> builder(AttributeType<Value> p_457822_) {
      return new EnvironmentAttribute.Builder<>(p_457822_);
   }

   public AttributeType<Value> type() {
      return this.type;
   }

   public Value defaultValue() {
      return this.defaultValue;
   }

   public Codec<Value> valueCodec() {
      return this.type.valueCodec().validate(this.valueRange::validate);
   }

   public Value sanitizeValue(Value p_458023_) {
      return this.valueRange.sanitize(p_458023_);
   }

   public boolean isSyncable() {
      return this.isSyncable;
   }

   public boolean isPositional() {
      return this.isPositional;
   }

   public boolean isSpatiallyInterpolated() {
      return this.isSpatiallyInterpolated;
   }

   @Override
   public String toString() {
      return Util.getRegisteredName(BuiltInRegistries.ENVIRONMENT_ATTRIBUTE, this);
   }

   public static class Builder<Value> {
      private final AttributeType<Value> type;
      @Nullable
      private Value defaultValue;
      private AttributeRange<Value> valueRange = AttributeRange.any();
      private boolean isSyncable = false;
      private boolean isPositional = true;
      private boolean isSpatiallyInterpolated = false;

      public Builder(AttributeType<Value> p_457611_) {
         this.type = p_457611_;
      }

      public EnvironmentAttribute.Builder<Value> defaultValue(Value p_457855_) {
         this.defaultValue = p_457855_;
         return this;
      }

      public EnvironmentAttribute.Builder<Value> valueRange(AttributeRange<Value> p_457972_) {
         this.valueRange = p_457972_;
         return this;
      }

      public EnvironmentAttribute.Builder<Value> syncable() {
         this.isSyncable = true;
         return this;
      }

      public EnvironmentAttribute.Builder<Value> notPositional() {
         this.isPositional = false;
         return this;
      }

      public EnvironmentAttribute.Builder<Value> spatiallyInterpolated() {
         this.isSpatiallyInterpolated = true;
         return this;
      }

      public EnvironmentAttribute<Value> build() {
         return new EnvironmentAttribute<>(
            this.type,
            Objects.requireNonNull(this.defaultValue, "Missing default value"),
            this.valueRange,
            this.isSyncable,
            this.isPositional,
            this.isSpatiallyInterpolated
         );
      }
   }
}
