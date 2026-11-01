package net.minecraft.world.item.component;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import java.util.function.Consumer;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;

public final class CustomData {
   public static final CustomData EMPTY = new CustomData(new CompoundTag());
   public static final Codec<CompoundTag> COMPOUND_TAG_CODEC = Codec.withAlternative(CompoundTag.CODEC, TagParser.FLATTENED_CODEC);
   public static final Codec<CustomData> CODEC = COMPOUND_TAG_CODEC.xmap(CustomData::new, p_331996_ -> p_331996_.tag);
   @Deprecated
   public static final StreamCodec<ByteBuf, CustomData> STREAM_CODEC = ByteBufCodecs.COMPOUND_TAG.map(CustomData::new, p_331280_ -> p_331280_.tag);
   private final CompoundTag tag;

   private CustomData(CompoundTag p_331863_) {
      this.tag = p_331863_;
   }

   public static CustomData of(CompoundTag p_330724_) {
      return new CustomData(p_330724_.copy());
   }

   public boolean matchedBy(CompoundTag p_330550_) {
      return NbtUtils.compareNbt(p_330550_, this.tag, true);
   }

   public static void update(DataComponentType<CustomData> p_331895_, ItemStack p_332185_, Consumer<CompoundTag> p_331274_) {
      CustomData customdata = p_332185_.getOrDefault(p_331895_, EMPTY).update(p_331274_);
      if (customdata.tag.isEmpty()) {
         p_332185_.remove(p_331895_);
      } else {
         p_332185_.set(p_331895_, customdata);
      }
   }

   public static void set(DataComponentType<CustomData> p_330462_, ItemStack p_332015_, CompoundTag p_331019_) {
      if (!p_331019_.isEmpty()) {
         p_332015_.set(p_330462_, of(p_331019_));
      } else {
         p_332015_.remove(p_330462_);
      }
   }

   public CustomData update(Consumer<CompoundTag> p_331451_) {
      CompoundTag compoundtag = this.tag.copy();
      p_331451_.accept(compoundtag);
      return new CustomData(compoundtag);
   }

   public boolean isEmpty() {
      return this.tag.isEmpty();
   }

   public CompoundTag copyTag() {
      return this.tag.copy();
   }

   public boolean contains(String key) {
      return this.tag.contains(key);
   }

   @Override
   public boolean equals(Object p_331195_) {
      if (p_331195_ == this) {
         return true;
      } else {
         return p_331195_ instanceof CustomData customdata ? this.tag.equals(customdata.tag) : false;
      }
   }

   @Override
   public int hashCode() {
      return this.tag.hashCode();
   }

   @Override
   public String toString() {
      return this.tag.toString();
   }
}
