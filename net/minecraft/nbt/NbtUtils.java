package net.minecraft.nbt;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.StateHolder;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.material.FluidState;
import org.slf4j.Logger;

public final class NbtUtils {
   private static final Comparator<ListTag> YXZ_LISTTAG_INT_COMPARATOR = Comparator.<ListTag>comparingInt(p_178074_ -> p_178074_.getInt(1))
      .thenComparingInt(p_178070_ -> p_178070_.getInt(0))
      .thenComparingInt(p_178066_ -> p_178066_.getInt(2));
   private static final Comparator<ListTag> YXZ_LISTTAG_DOUBLE_COMPARATOR = Comparator.<ListTag>comparingDouble(p_178060_ -> p_178060_.getDouble(1))
      .thenComparingDouble(p_178056_ -> p_178056_.getDouble(0))
      .thenComparingDouble(p_178042_ -> p_178042_.getDouble(2));
   public static final String SNBT_DATA_TAG = "data";
   private static final char PROPERTIES_START = '{';
   private static final char PROPERTIES_END = '}';
   private static final String ELEMENT_SEPARATOR = ",";
   private static final char KEY_VALUE_SEPARATOR = ':';
   private static final Splitter COMMA_SPLITTER = Splitter.on(",");
   private static final Splitter COLON_SPLITTER = Splitter.on(':').limit(2);
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final int INDENT = 2;
   private static final int NOT_FOUND = -1;

   private NbtUtils() {
   }

   @VisibleForTesting
   public static boolean compareNbt(@Nullable Tag p_129236_, @Nullable Tag p_129237_, boolean p_129238_) {
      if (p_129236_ == p_129237_) {
         return true;
      } else if (p_129236_ == null) {
         return true;
      } else if (p_129237_ == null) {
         return false;
      } else if (!p_129236_.getClass().equals(p_129237_.getClass())) {
         return false;
      } else if (p_129236_ instanceof CompoundTag $$3) {
         CompoundTag $$4 = (CompoundTag)p_129237_;
         if ($$4.size() < $$3.size()) {
            return false;
         } else {
            for (String $$5 : $$3.getAllKeys()) {
               Tag $$6 = $$3.get($$5);
               if (!compareNbt($$6, $$4.get($$5), p_129238_)) {
                  return false;
               }
            }

            return true;
         }
      } else if (p_129236_ instanceof ListTag $$7 && p_129238_) {
         ListTag $$8 = (ListTag)p_129237_;
         if ($$7.isEmpty()) {
            return $$8.isEmpty();
         } else if ($$8.size() < $$7.size()) {
            return false;
         } else {
            for (Tag $$9 : $$7) {
               boolean $$10 = false;

               for (Tag $$11 : $$8) {
                  if (compareNbt($$9, $$11, p_129238_)) {
                     $$10 = true;
                     break;
                  }
               }

               if (!$$10) {
                  return false;
               }
            }

            return true;
         }
      } else {
         return p_129236_.equals(p_129237_);
      }
   }

   public static IntArrayTag createUUID(UUID p_129227_) {
      return new IntArrayTag(UUIDUtil.uuidToIntArray(p_129227_));
   }

   public static UUID loadUUID(Tag p_129234_) {
      if (p_129234_.getType() != IntArrayTag.TYPE) {
         throw new IllegalArgumentException(
            "Expected UUID-Tag to be of type " + IntArrayTag.TYPE.getName() + ", but found " + p_129234_.getType().getName() + "."
         );
      } else {
         int[] $$1 = ((IntArrayTag)p_129234_).getAsIntArray();
         if ($$1.length != 4) {
            throw new IllegalArgumentException("Expected UUID-Array to be of length 4, but found " + $$1.length + ".");
         } else {
            return UUIDUtil.uuidFromIntArray($$1);
         }
      }
   }

   public static Optional<BlockPos> readBlockPos(CompoundTag p_129240_, String p_326507_) {
      int[] $$2 = p_129240_.getIntArray(p_326507_);
      return $$2.length == 3 ? Optional.of(new BlockPos($$2[0], $$2[1], $$2[2])) : Optional.empty();
   }

   public static Tag writeBlockPos(BlockPos p_129225_) {
      return new IntArrayTag(new int[]{p_129225_.getX(), p_129225_.getY(), p_129225_.getZ()});
   }

   public static BlockState readBlockState(HolderGetter<Block> p_256363_, CompoundTag p_250775_) {
      if (!p_250775_.contains("Name", 8)) {
         return Blocks.AIR.defaultBlockState();
      } else {
         ResourceLocation $$2 = ResourceLocation.parse(p_250775_.getString("Name"));
         Optional<? extends Holder<Block>> $$3 = p_256363_.get(ResourceKey.create(Registries.BLOCK, $$2));
         if ($$3.isEmpty()) {
            return Blocks.AIR.defaultBlockState();
         } else {
            Block $$4 = $$3.get().value();
            BlockState $$5 = $$4.defaultBlockState();
            if (p_250775_.contains("Properties", 10)) {
               CompoundTag $$6 = p_250775_.getCompound("Properties");
               StateDefinition<Block, BlockState> $$7 = $$4.getStateDefinition();

               for (String $$8 : $$6.getAllKeys()) {
                  Property<?> $$9 = $$7.getProperty($$8);
                  if ($$9 != null) {
                     $$5 = setValueHelper($$5, $$9, $$8, $$6, p_250775_);
                  }
               }
            }

            return $$5;
         }
      }
   }

   private static <S extends StateHolder<?, S>, T extends Comparable<T>> S setValueHelper(
      S p_129205_, Property<T> p_129206_, String p_129207_, CompoundTag p_129208_, CompoundTag p_129209_
   ) {
      Optional<T> $$5 = p_129206_.getValue(p_129208_.getString(p_129207_));
      if ($$5.isPresent()) {
         return p_129205_.setValue(p_129206_, $$5.get());
      } else {
         LOGGER.warn("Unable to read property: {} with value: {} for blockstate: {}", new Object[]{p_129207_, p_129208_.getString(p_129207_), p_129209_});
         return p_129205_;
      }
   }

   public static CompoundTag writeBlockState(BlockState p_129203_) {
      CompoundTag $$1 = new CompoundTag();
      $$1.putString("Name", BuiltInRegistries.BLOCK.getKey(p_129203_.getBlock()).toString());
      Map<Property<?>, Comparable<?>> $$2 = p_129203_.getValues();
      if (!$$2.isEmpty()) {
         CompoundTag $$3 = new CompoundTag();

         for (Entry<Property<?>, Comparable<?>> $$4 : $$2.entrySet()) {
            Property<?> $$5 = $$4.getKey();
            $$3.putString($$5.getName(), getName($$5, $$4.getValue()));
         }

         $$1.put("Properties", $$3);
      }

      return $$1;
   }

   public static CompoundTag writeFluidState(FluidState p_178023_) {
      CompoundTag $$1 = new CompoundTag();
      $$1.putString("Name", BuiltInRegistries.FLUID.getKey(p_178023_.getType()).toString());
      Map<Property<?>, Comparable<?>> $$2 = p_178023_.getValues();
      if (!$$2.isEmpty()) {
         CompoundTag $$3 = new CompoundTag();

         for (Entry<Property<?>, Comparable<?>> $$4 : $$2.entrySet()) {
            Property<?> $$5 = $$4.getKey();
            $$3.putString($$5.getName(), getName($$5, $$4.getValue()));
         }

         $$1.put("Properties", $$3);
      }

      return $$1;
   }

   private static <T extends Comparable<T>> String getName(Property<T> p_129211_, Comparable<?> p_129212_) {
      return p_129211_.getName((T)p_129212_);
   }

   public static String prettyPrint(Tag p_178058_) {
      return prettyPrint(p_178058_, false);
   }

   public static String prettyPrint(Tag p_178051_, boolean p_178052_) {
      return prettyPrint(new StringBuilder(), p_178051_, 0, p_178052_).toString();
   }

   public static StringBuilder prettyPrint(StringBuilder p_178027_, Tag p_178028_, int p_178029_, boolean p_178030_) {
      switch (p_178028_.getId()) {
         case 0:
            break;
         case 1:
         case 2:
         case 3:
         case 4:
         case 5:
         case 6:
         case 8:
            p_178027_.append(p_178028_);
            break;
         case 7:
            ByteArrayTag $$4 = (ByteArrayTag)p_178028_;
            byte[] $$5 = $$4.getAsByteArray();
            int $$6 = $$5.length;
            indent(p_178029_, p_178027_).append("byte[").append($$6).append("] {\n");
            if (p_178030_) {
               indent(p_178029_ + 1, p_178027_);

               for (int $$7 = 0; $$7 < $$5.length; $$7++) {
                  if ($$7 != 0) {
                     p_178027_.append(',');
                  }

                  if ($$7 % 16 == 0 && $$7 / 16 > 0) {
                     p_178027_.append('\n');
                     if ($$7 < $$5.length) {
                        indent(p_178029_ + 1, p_178027_);
                     }
                  } else if ($$7 != 0) {
                     p_178027_.append(' ');
                  }

                  p_178027_.append(String.format(Locale.ROOT, "0x%02X", $$5[$$7] & 255));
               }
            } else {
               indent(p_178029_ + 1, p_178027_).append(" // Skipped, supply withBinaryBlobs true");
            }

            p_178027_.append('\n');
            indent(p_178029_, p_178027_).append('}');
            break;
         case 9:
            ListTag $$8 = (ListTag)p_178028_;
            int $$9 = $$8.size();
            int $$10 = $$8.getElementType();
            String $$11 = $$10 == 0 ? "undefined" : TagTypes.getType($$10).getPrettyName();
            indent(p_178029_, p_178027_).append("list<").append($$11).append(">[").append($$9).append("] [");
            if ($$9 != 0) {
               p_178027_.append('\n');
            }

            for (int $$12 = 0; $$12 < $$9; $$12++) {
               if ($$12 != 0) {
                  p_178027_.append(",\n");
               }

               indent(p_178029_ + 1, p_178027_);
               prettyPrint(p_178027_, $$8.get($$12), p_178029_ + 1, p_178030_);
            }

            if ($$9 != 0) {
               p_178027_.append('\n');
            }

            indent(p_178029_, p_178027_).append(']');
            break;
         case 10:
            CompoundTag $$19 = (CompoundTag)p_178028_;
            List<String> $$20 = Lists.newArrayList($$19.getAllKeys());
            Collections.sort($$20);
            indent(p_178029_, p_178027_).append('{');
            if (p_178027_.length() - p_178027_.lastIndexOf("\n") > 2 * (p_178029_ + 1)) {
               p_178027_.append('\n');
               indent(p_178029_ + 1, p_178027_);
            }

            int $$21 = $$20.stream().mapToInt(String::length).max().orElse(0);
            String $$22 = Strings.repeat(" ", $$21);

            for (int $$23 = 0; $$23 < $$20.size(); $$23++) {
               if ($$23 != 0) {
                  p_178027_.append(",\n");
               }

               String $$24 = $$20.get($$23);
               indent(p_178029_ + 1, p_178027_).append('"').append($$24).append('"').append($$22, 0, $$22.length() - $$24.length()).append(": ");
               prettyPrint(p_178027_, $$19.get($$24), p_178029_ + 1, p_178030_);
            }

            if (!$$20.isEmpty()) {
               p_178027_.append('\n');
            }

            indent(p_178029_, p_178027_).append('}');
            break;
         case 11:
            IntArrayTag $$13 = (IntArrayTag)p_178028_;
            int[] $$14 = $$13.getAsIntArray();
            int $$15 = 0;

            for (int $$16 : $$14) {
               $$15 = Math.max($$15, String.format(Locale.ROOT, "%X", $$16).length());
            }

            int $$17 = $$14.length;
            indent(p_178029_, p_178027_).append("int[").append($$17).append("] {\n");
            if (p_178030_) {
               indent(p_178029_ + 1, p_178027_);

               for (int $$18 = 0; $$18 < $$14.length; $$18++) {
                  if ($$18 != 0) {
                     p_178027_.append(',');
                  }

                  if ($$18 % 16 == 0 && $$18 / 16 > 0) {
                     p_178027_.append('\n');
                     if ($$18 < $$14.length) {
                        indent(p_178029_ + 1, p_178027_);
                     }
                  } else if ($$18 != 0) {
                     p_178027_.append(' ');
                  }

                  p_178027_.append(String.format(Locale.ROOT, "0x%0" + $$15 + "X", $$14[$$18]));
               }
            } else {
               indent(p_178029_ + 1, p_178027_).append(" // Skipped, supply withBinaryBlobs true");
            }

            p_178027_.append('\n');
            indent(p_178029_, p_178027_).append('}');
            break;
         case 12:
            LongArrayTag $$25 = (LongArrayTag)p_178028_;
            long[] $$26 = $$25.getAsLongArray();
            long $$27 = 0L;

            for (long $$28 : $$26) {
               $$27 = Math.max($$27, (long)String.format(Locale.ROOT, "%X", $$28).length());
            }

            long $$29 = $$26.length;
            indent(p_178029_, p_178027_).append("long[").append($$29).append("] {\n");
            if (p_178030_) {
               indent(p_178029_ + 1, p_178027_);

               for (int $$30 = 0; $$30 < $$26.length; $$30++) {
                  if ($$30 != 0) {
                     p_178027_.append(',');
                  }

                  if ($$30 % 16 == 0 && $$30 / 16 > 0) {
                     p_178027_.append('\n');
                     if ($$30 < $$26.length) {
                        indent(p_178029_ + 1, p_178027_);
                     }
                  } else if ($$30 != 0) {
                     p_178027_.append(' ');
                  }

                  p_178027_.append(String.format(Locale.ROOT, "0x%0" + $$27 + "X", $$26[$$30]));
               }
            } else {
               indent(p_178029_ + 1, p_178027_).append(" // Skipped, supply withBinaryBlobs true");
            }

            p_178027_.append('\n');
            indent(p_178029_, p_178027_).append('}');
            break;
         default:
            p_178027_.append("<UNKNOWN :(>");
      }

      return p_178027_;
   }

   private static StringBuilder indent(int p_178020_, StringBuilder p_178021_) {
      int $$2 = p_178021_.lastIndexOf("\n") + 1;
      int $$3 = p_178021_.length() - $$2;

      for (int $$4 = 0; $$4 < 2 * p_178020_ - $$3; $$4++) {
         p_178021_.append(' ');
      }

      return p_178021_;
   }

   public static Component toPrettyComponent(Tag p_178062_) {
      return new TextComponentTagVisitor("").visit(p_178062_);
   }

   public static String structureToSnbt(CompoundTag p_178064_) {
      return new SnbtPrinterTagVisitor().visit(packStructureTemplate(p_178064_));
   }

   public static CompoundTag snbtToStructure(String p_178025_) throws CommandSyntaxException {
      return unpackStructureTemplate(TagParser.parseTag(p_178025_));
   }

   @VisibleForTesting
   static CompoundTag packStructureTemplate(CompoundTag p_178068_) {
      boolean $$1 = p_178068_.contains("palettes", 9);
      ListTag $$2;
      if ($$1) {
         $$2 = p_178068_.getList("palettes", 9).getList(0);
      } else {
         $$2 = p_178068_.getList("palette", 10);
      }

      ListTag $$4 = $$2.stream()
         .map(CompoundTag.class::cast)
         .map(NbtUtils::packBlockState)
         .map(StringTag::valueOf)
         .collect(Collectors.toCollection(ListTag::new));
      p_178068_.put("palette", $$4);
      if ($$1) {
         ListTag $$5 = new ListTag();
         ListTag $$6 = p_178068_.getList("palettes", 9);
         $$6.stream().map(ListTag.class::cast).forEach(p_178049_ -> {
            CompoundTag $$3x = new CompoundTag();

            for (int $$4x = 0; $$4x < p_178049_.size(); $$4x++) {
               $$3x.putString($$4.getString($$4x), packBlockState(p_178049_.getCompound($$4x)));
            }

            $$5.add($$3x);
         });
         p_178068_.put("palettes", $$5);
      }

      if (p_178068_.contains("entities", 9)) {
         ListTag $$7 = p_178068_.getList("entities", 10);
         ListTag $$8 = $$7.stream()
            .map(CompoundTag.class::cast)
            .sorted(Comparator.comparing(p_178080_ -> p_178080_.getList("pos", 6), YXZ_LISTTAG_DOUBLE_COMPARATOR))
            .collect(Collectors.toCollection(ListTag::new));
         p_178068_.put("entities", $$8);
      }

      ListTag $$9 = p_178068_.getList("blocks", 10)
         .stream()
         .map(CompoundTag.class::cast)
         .sorted(Comparator.comparing(p_178078_ -> p_178078_.getList("pos", 3), YXZ_LISTTAG_INT_COMPARATOR))
         .peek(p_178045_ -> p_178045_.putString("state", $$4.getString(p_178045_.getInt("state"))))
         .collect(Collectors.toCollection(ListTag::new));
      p_178068_.put("data", $$9);
      p_178068_.remove("blocks");
      return p_178068_;
   }

   @VisibleForTesting
   static CompoundTag unpackStructureTemplate(CompoundTag p_178072_) {
      ListTag $$1 = p_178072_.getList("palette", 8);
      Map<String, Tag> $$2 = $$1.stream()
         .map(StringTag.class::cast)
         .map(StringTag::getAsString)
         .collect(ImmutableMap.toImmutableMap(Function.identity(), NbtUtils::unpackBlockState));
      if (p_178072_.contains("palettes", 9)) {
         p_178072_.put(
            "palettes",
            p_178072_.getList("palettes", 10)
               .stream()
               .map(CompoundTag.class::cast)
               .map(p_178033_ -> $$2.keySet().stream().map(p_178033_::getString).map(NbtUtils::unpackBlockState).collect(Collectors.toCollection(ListTag::new)))
               .collect(Collectors.toCollection(ListTag::new))
         );
         p_178072_.remove("palette");
      } else {
         p_178072_.put("palette", $$2.values().stream().collect(Collectors.toCollection(ListTag::new)));
      }

      if (p_178072_.contains("data", 9)) {
         Object2IntMap<String> $$3 = new Object2IntOpenHashMap();
         $$3.defaultReturnValue(-1);

         for (int $$4 = 0; $$4 < $$1.size(); $$4++) {
            $$3.put($$1.getString($$4), $$4);
         }

         ListTag $$5 = p_178072_.getList("data", 10);

         for (int $$6 = 0; $$6 < $$5.size(); $$6++) {
            CompoundTag $$7 = $$5.getCompound($$6);
            String $$8 = $$7.getString("state");
            int $$9 = $$3.getInt($$8);
            if ($$9 == -1) {
               throw new IllegalStateException("Entry " + $$8 + " missing from palette");
            }

            $$7.putInt("state", $$9);
         }

         p_178072_.put("blocks", $$5);
         p_178072_.remove("data");
      }

      return p_178072_;
   }

   @VisibleForTesting
   static String packBlockState(CompoundTag p_178076_) {
      StringBuilder $$1 = new StringBuilder(p_178076_.getString("Name"));
      if (p_178076_.contains("Properties", 10)) {
         CompoundTag $$2 = p_178076_.getCompound("Properties");
         String $$3 = $$2.getAllKeys().stream().sorted().map(p_178036_ -> p_178036_ + ":" + $$2.get(p_178036_).getAsString()).collect(Collectors.joining(","));
         $$1.append('{').append($$3).append('}');
      }

      return $$1.toString();
   }

   @VisibleForTesting
   static CompoundTag unpackBlockState(String p_178054_) {
      CompoundTag $$1 = new CompoundTag();
      int $$2 = p_178054_.indexOf(123);
      String $$3;
      if ($$2 >= 0) {
         $$3 = p_178054_.substring(0, $$2);
         CompoundTag $$4 = new CompoundTag();
         if ($$2 + 2 <= p_178054_.length()) {
            String $$5 = p_178054_.substring($$2 + 1, p_178054_.indexOf(125, $$2));
            COMMA_SPLITTER.split($$5).forEach(p_178040_ -> {
               List<String> $$3x = COLON_SPLITTER.splitToList(p_178040_);
               if ($$3x.size() == 2) {
                  $$4.putString($$3x.get(0), $$3x.get(1));
               } else {
                  LOGGER.error("Something went wrong parsing: '{}' -- incorrect gamedata!", p_178054_);
               }
            });
            $$1.put("Properties", $$4);
         }
      } else {
         $$3 = p_178054_;
      }

      $$1.putString("Name", $$3);
      return $$1;
   }

   public static CompoundTag addCurrentDataVersion(CompoundTag p_265050_) {
      int $$1 = SharedConstants.getCurrentVersion().getDataVersion().getVersion();
      return addDataVersion(p_265050_, $$1);
   }

   public static CompoundTag addDataVersion(CompoundTag p_265534_, int p_265686_) {
      p_265534_.putInt("DataVersion", p_265686_);
      return p_265534_;
   }

   public static int getDataVersion(CompoundTag p_265397_, int p_265399_) {
      return p_265397_.contains("DataVersion", 99) ? p_265397_.getInt("DataVersion") : p_265399_;
   }
}
