package net.minecraft.world.item.component;

import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import io.netty.buffer.ByteBuf;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import net.minecraft.ChatFormatting;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.RegistryOps;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import org.slf4j.Logger;

public final class TypedEntityData<IdType> implements TooltipProvider {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final String TYPE_TAG = "id";
   final IdType type;
   final CompoundTag tag;

   public static <T> Codec<TypedEntityData<T>> codec(final Codec<T> p_435769_) {
      return new Codec<TypedEntityData<T>>() {
         public <V> DataResult<Pair<TypedEntityData<T>, V>> decode(DynamicOps<V> p_436802_, V p_436757_) {
            return CustomData.COMPOUND_TAG_CODEC
               .decode(p_436802_, p_436757_)
               .flatMap(
                  p_466655_ -> {
                     CompoundTag compoundtag = ((CompoundTag)p_466655_.getFirst()).copy();
                     Tag tag = compoundtag.remove("id");
                     return tag == null
                        ? DataResult.error(() -> "Expected 'id' field in " + p_436757_)
                        : p_435769_.parse(asNbtOps((DynamicOps<T>)p_436802_), tag)
                           .map(p_466658_ -> Pair.of(new TypedEntityData<>(p_466658_, compoundtag), p_466655_.getSecond()));
                  }
               );
         }

         public <V> DataResult<V> encode(TypedEntityData<T> p_436720_, DynamicOps<V> p_436614_, V p_436650_) {
            return p_435769_.encodeStart(asNbtOps((DynamicOps<T>)p_436614_), p_436720_.type).flatMap(p_436684_ -> {
               CompoundTag compoundtag = p_436720_.tag.copy();
               compoundtag.put("id", p_436684_);
               return CustomData.COMPOUND_TAG_CODEC.encode(compoundtag, p_436614_, p_436650_);
            });
         }

         private static <T> DynamicOps<Tag> asNbtOps(DynamicOps<T> p_436750_) {
            return (DynamicOps<Tag>)(p_436750_ instanceof RegistryOps<T> registryops ? registryops.withParent(NbtOps.INSTANCE) : NbtOps.INSTANCE);
         }
      };
   }

   public static <B extends ByteBuf, T> StreamCodec<B, TypedEntityData<T>> streamCodec(StreamCodec<B, T> p_435057_) {
      return StreamCodec.composite(
         p_435057_,
         (Function<TypedEntityData<T>, T>)(TypedEntityData::type),
         ByteBufCodecs.COMPOUND_TAG,
         TypedEntityData::tag,
         (BiFunction<T, CompoundTag, TypedEntityData<T>>)(TypedEntityData::new)
      );
   }

   TypedEntityData(IdType p_434806_, CompoundTag p_435415_) {
      this.type = p_434806_;
      this.tag = stripId(p_435415_);
   }

   public static <T> TypedEntityData<T> of(T p_435505_, CompoundTag p_435677_) {
      return new TypedEntityData<>(p_435505_, p_435677_);
   }

   private static CompoundTag stripId(CompoundTag p_434779_) {
      if (p_434779_.contains("id")) {
         CompoundTag compoundtag = p_434779_.copy();
         compoundtag.remove("id");
         return compoundtag;
      } else {
         return p_434779_;
      }
   }

   public IdType type() {
      return this.type;
   }

   public boolean contains(String p_434145_) {
      return this.tag.contains(p_434145_);
   }

   @Override
   public boolean equals(Object p_435892_) {
      if (p_435892_ == this) {
         return true;
      } else {
         return p_435892_ instanceof TypedEntityData<?> typedentitydata ? this.type == typedentitydata.type && this.tag.equals(typedentitydata.tag) : false;
      }
   }

   @Override
   public int hashCode() {
      return 31 * this.type.hashCode() + this.tag.hashCode();
   }

   @Override
   public String toString() {
      return this.type + " " + this.tag;
   }

   public void loadInto(Entity p_434831_) {
      try (ProblemReporter.ScopedCollector problemreporter$scopedcollector = new ProblemReporter.ScopedCollector(p_434831_.problemPath(), LOGGER)) {
         TagValueOutput tagvalueoutput = TagValueOutput.createWithContext(problemreporter$scopedcollector, p_434831_.registryAccess());
         p_434831_.saveWithoutId(tagvalueoutput);
         CompoundTag compoundtag = tagvalueoutput.buildResult();
         UUID uuid = p_434831_.getUUID();
         compoundtag.merge(this.getUnsafe());
         p_434831_.load(TagValueInput.create(problemreporter$scopedcollector, p_434831_.registryAccess(), compoundtag));
         p_434831_.setUUID(uuid);
      }
   }

   public boolean loadInto(BlockEntity p_433312_, HolderLookup.Provider p_434810_) {
      boolean $$6;
      try (ProblemReporter.ScopedCollector problemreporter$scopedcollector = new ProblemReporter.ScopedCollector(p_433312_.problemPath(), LOGGER)) {
         TagValueOutput tagvalueoutput = TagValueOutput.createWithContext(problemreporter$scopedcollector, p_434810_);
         p_433312_.saveCustomOnly(tagvalueoutput);
         CompoundTag compoundtag = tagvalueoutput.buildResult();
         CompoundTag compoundtag1 = compoundtag.copy();
         compoundtag.merge(this.getUnsafe());
         if (!compoundtag.equals(compoundtag1)) {
            try {
               p_433312_.loadCustomOnly(TagValueInput.create(problemreporter$scopedcollector, p_434810_, compoundtag));
               p_433312_.setChanged();
               return true;
            } catch (Exception var12) {
               LOGGER.warn("Failed to apply custom data to block entity at {}", p_433312_.getBlockPos(), var12);

               try {
                  p_433312_.loadCustomOnly(TagValueInput.create(problemreporter$scopedcollector.forChild(() -> "(rollback)"), p_434810_, compoundtag1));
               } catch (Exception var11) {
                  LOGGER.warn("Failed to rollback block entity at {} after failure", p_433312_.getBlockPos(), var11);
               }
            }
         }

         $$6 = false;
      }

      return $$6;
   }

   private CompoundTag tag() {
      return this.tag;
   }

   @Deprecated
   public CompoundTag getUnsafe() {
      return this.tag;
   }

   public CompoundTag copyTagWithoutId() {
      return this.tag.copy();
   }

   @Override
   public void addToTooltip(Item.TooltipContext p_435732_, Consumer<Component> p_433356_, TooltipFlag p_435305_, DataComponentGetter p_434480_) {
      if (this.type.getClass() == EntityType.class) {
         EntityType<?> entitytype = (EntityType<?>)this.type;
         if (p_435732_.isPeaceful() && !entitytype.isAllowedInPeaceful()) {
            p_433356_.accept(Component.translatable("item.spawn_egg.peaceful").withStyle(ChatFormatting.RED));
         }
      }
   }
}
