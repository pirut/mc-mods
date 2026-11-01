package net.minecraft.nbt;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.Comparators;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.Dynamic;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.stream.Collectors;
import net.minecraft.SharedConstants;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.StateHolder;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public final class NbtUtils {
   private static final Comparator<ListTag> YXZ_LISTTAG_INT_COMPARATOR = Comparator.<ListTag>comparingInt(listTag -> listTag.getIntOr(1, 0))
      .thenComparingInt(listTag -> listTag.getIntOr(0, 0))
      .thenComparingInt(listTag -> listTag.getIntOr(2, 0));
   private static final Comparator<ListTag> YXZ_LISTTAG_DOUBLE_COMPARATOR = Comparator.<ListTag>comparingDouble(listTag -> listTag.getDoubleOr(1, 0.0))
      .thenComparingDouble(listTag -> listTag.getDoubleOr(0, 0.0))
      .thenComparingDouble(listTag -> listTag.getDoubleOr(2, 0.0));
   private static final Codec<ResourceKey<Block>> BLOCK_NAME_CODEC = ResourceKey.codec(Registries.BLOCK);
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
   public static boolean compareNbt(@Nullable Tag tag, @Nullable Tag other, boolean compareListTag) {
      if (tag == other) {
         return true;
      } else if (tag == null) {
         return true;
      } else if (other == null) {
         return false;
      } else if (!tag.getClass().equals(other.getClass())) {
         return false;
      } else if (tag instanceof CompoundTag compoundTag) {
         CompoundTag compoundTag2 = (CompoundTag)other;
         if (compoundTag2.size() < compoundTag.size()) {
            return false;
         } else {
            for (Entry<String, Tag> entry : compoundTag.entrySet()) {
               Tag tag2 = entry.getValue();
               if (!compareNbt(tag2, compoundTag2.get(entry.getKey()), compareListTag)) {
                  return false;
               }
            }

            return true;
         }
      } else if (tag instanceof ListTag listTag && compareListTag) {
         ListTag listTag2 = (ListTag)other;
         if (listTag.isEmpty()) {
            return listTag2.isEmpty();
         } else if (listTag2.size() < listTag.size()) {
            return false;
         } else {
            for (Tag tag3 : listTag) {
               boolean bl = false;

               for (Tag tag4 : listTag2) {
                  if (compareNbt(tag3, tag4, compareListTag)) {
                     bl = true;
                     break;
                  }
               }

               if (!bl) {
                  return false;
               }
            }

            return true;
         }
      } else {
         return tag.equals(other);
      }
   }

   public static BlockState readBlockState(HolderGetter<Block> blockGetter, CompoundTag tag) {
      Optional<? extends Holder<Block>> optional = tag.<ResourceKey<T>>read("Name", BLOCK_NAME_CODEC).flatMap(blockGetter::get);
      if (optional.isEmpty()) {
         return Blocks.AIR.defaultBlockState();
      } else {
         Block block = optional.get().value();
         BlockState blockState = block.defaultBlockState();
         Optional<CompoundTag> optional2 = tag.getCompound("Properties");
         if (optional2.isPresent()) {
            StateDefinition<Block, BlockState> stateDefinition = block.getStateDefinition();

            for (String string : optional2.get().keySet()) {
               Property<?> property = stateDefinition.getProperty(string);
               if (property != null) {
                  blockState = setValueHelper(blockState, property, string, optional2.get(), tag);
               }
            }
         }

         return blockState;
      }
   }

   private static <S extends StateHolder<?, S>, T extends Comparable<T>> S setValueHelper(
      S stateHolder, Property<T> property, String propertyName, CompoundTag propertiesTag, CompoundTag blockStateTag
   ) {
      Optional<T> optional = propertiesTag.getString(propertyName).flatMap(property::getValue);
      if (optional.isPresent()) {
         return stateHolder.setValue(property, optional.get());
      } else {
         LOGGER.warn(
            "Unable to read property: {} with value: {} for blockstate: {}", new Object[]{propertyName, propertiesTag.get(propertyName), blockStateTag}
         );
         return stateHolder;
      }
   }

   public static CompoundTag writeBlockState(BlockState state) {
      CompoundTag compoundTag = new CompoundTag();
      compoundTag.putString("Name", BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString());
      Map<Property<?>, Comparable<?>> map = state.getValues();
      if (!map.isEmpty()) {
         CompoundTag compoundTag2 = new CompoundTag();

         for (Entry<Property<?>, Comparable<?>> entry : map.entrySet()) {
            Property<?> property = entry.getKey();
            compoundTag2.putString(property.getName(), getName(property, entry.getValue()));
         }

         compoundTag.put("Properties", compoundTag2);
      }

      return compoundTag;
   }

   public static CompoundTag writeFluidState(FluidState state) {
      CompoundTag compoundTag = new CompoundTag();
      compoundTag.putString("Name", BuiltInRegistries.FLUID.getKey(state.getType()).toString());
      Map<Property<?>, Comparable<?>> map = state.getValues();
      if (!map.isEmpty()) {
         CompoundTag compoundTag2 = new CompoundTag();

         for (Entry<Property<?>, Comparable<?>> entry : map.entrySet()) {
            Property<?> property = entry.getKey();
            compoundTag2.putString(property.getName(), getName(property, entry.getValue()));
         }

         compoundTag.put("Properties", compoundTag2);
      }

      return compoundTag;
   }

   private static <T extends Comparable<T>> String getName(Property<T> property, Comparable<?> value) {
      return property.getName((T)value);
   }

   public static String prettyPrint(Tag tag) {
      return prettyPrint(tag, false);
   }

   public static String prettyPrint(Tag tag, boolean prettyPrintArray) {
      return prettyPrint(new StringBuilder(), tag, 0, prettyPrintArray).toString();
   }

   public static StringBuilder prettyPrint(StringBuilder stringBuilder, Tag tag, int indentLevel, boolean prettyPrintArray) {
      return switch (tag) {
         case PrimitiveTag primitiveTag -> stringBuilder.append(primitiveTag);
         case EndTag endTag -> stringBuilder;
         case ByteArrayTag byteArrayTag -> {
            byte[] bs = byteArrayTag.getAsByteArray();
            int i = bs.length;
            indent(indentLevel, stringBuilder).append("byte[").append(i).append("] {\n");
            if (prettyPrintArray) {
               indent(indentLevel + 1, stringBuilder);

               for (int j = 0; j < bs.length; j++) {
                  if (j != 0) {
                     stringBuilder.append(',');
                  }

                  if (j % 16 == 0 && j / 16 > 0) {
                     stringBuilder.append('\n');
                     if (j < bs.length) {
                        indent(indentLevel + 1, stringBuilder);
                     }
                  } else if (j != 0) {
                     stringBuilder.append(' ');
                  }

                  stringBuilder.append(String.format(Locale.ROOT, "0x%02X", bs[j] & 255));
               }
            } else {
               indent(indentLevel + 1, stringBuilder).append(" // Skipped, supply withBinaryBlobs true");
            }

            stringBuilder.append('\n');
            indent(indentLevel, stringBuilder).append('}');
            yield stringBuilder;
         }
         case ListTag listTag -> {
            int i = listTag.size();
            indent(indentLevel, stringBuilder).append("list").append("[").append(i).append("] [");
            if (i != 0) {
               stringBuilder.append('\n');
            }

            for (int j = 0; j < i; j++) {
               if (j != 0) {
                  stringBuilder.append(",\n");
               }

               indent(indentLevel + 1, stringBuilder);
               prettyPrint(stringBuilder, listTag.get(j), indentLevel + 1, prettyPrintArray);
            }

            if (i != 0) {
               stringBuilder.append('\n');
            }

            indent(indentLevel, stringBuilder).append(']');
            yield stringBuilder;
         }
         case IntArrayTag intArrayTag -> {
            int[] is = intArrayTag.getAsIntArray();
            int k = 0;

            for (int l : is) {
               k = Math.max(k, String.format(Locale.ROOT, "%X", l).length());
            }

            int m = is.length;
            indent(indentLevel, stringBuilder).append("int[").append(m).append("] {\n");
            if (prettyPrintArray) {
               indent(indentLevel + 1, stringBuilder);

               for (int n = 0; n < is.length; n++) {
                  if (n != 0) {
                     stringBuilder.append(',');
                  }

                  if (n % 16 == 0 && n / 16 > 0) {
                     stringBuilder.append('\n');
                     if (n < is.length) {
                        indent(indentLevel + 1, stringBuilder);
                     }
                  } else if (n != 0) {
                     stringBuilder.append(' ');
                  }

                  stringBuilder.append(String.format(Locale.ROOT, "0x%0" + k + "X", is[n]));
               }
            } else {
               indent(indentLevel + 1, stringBuilder).append(" // Skipped, supply withBinaryBlobs true");
            }

            stringBuilder.append('\n');
            indent(indentLevel, stringBuilder).append('}');
            yield stringBuilder;
         }
         case CompoundTag compoundTag -> {
            List<String> list = Lists.newArrayList(compoundTag.keySet());
            Collections.sort(list);
            indent(indentLevel, stringBuilder).append('{');
            if (stringBuilder.length() - stringBuilder.lastIndexOf("\n") > 2 * (indentLevel + 1)) {
               stringBuilder.append('\n');
               indent(indentLevel + 1, stringBuilder);
            }

            int m = list.stream().mapToInt(String::length).max().orElse(0);
            String string = Strings.repeat(" ", m);

            for (int o = 0; o < list.size(); o++) {
               if (o != 0) {
                  stringBuilder.append(",\n");
               }

               String string2 = list.get(o);
               indent(indentLevel + 1, stringBuilder)
                  .append('"')
                  .append(string2)
                  .append('"')
                  .append(string, 0, string.length() - string2.length())
                  .append(": ");
               prettyPrint(stringBuilder, compoundTag.get(string2), indentLevel + 1, prettyPrintArray);
            }

            if (!list.isEmpty()) {
               stringBuilder.append('\n');
            }

            indent(indentLevel, stringBuilder).append('}');
            yield stringBuilder;
         }
         case LongArrayTag longArrayTag -> {
            long[] ls = longArrayTag.getAsLongArray();
            long p = 0L;

            for (long q : ls) {
               p = Math.max(p, (long)String.format(Locale.ROOT, "%X", q).length());
            }

            long r = ls.length;
            indent(indentLevel, stringBuilder).append("long[").append(r).append("] {\n");
            if (prettyPrintArray) {
               indent(indentLevel + 1, stringBuilder);

               for (int s = 0; s < ls.length; s++) {
                  if (s != 0) {
                     stringBuilder.append(',');
                  }

                  if (s % 16 == 0 && s / 16 > 0) {
                     stringBuilder.append('\n');
                     if (s < ls.length) {
                        indent(indentLevel + 1, stringBuilder);
                     }
                  } else if (s != 0) {
                     stringBuilder.append(' ');
                  }

                  stringBuilder.append(String.format(Locale.ROOT, "0x%0" + p + "X", ls[s]));
               }
            } else {
               indent(indentLevel + 1, stringBuilder).append(" // Skipped, supply withBinaryBlobs true");
            }

            stringBuilder.append('\n');
            indent(indentLevel, stringBuilder).append('}');
            yield stringBuilder;
         }
         default -> throw new MatchException(null, null);
      };
   }

   private static StringBuilder indent(int indentLevel, StringBuilder stringBuilder) {
      int i = stringBuilder.lastIndexOf("\n") + 1;
      int j = stringBuilder.length() - i;

      for (int k = 0; k < 2 * indentLevel - j; k++) {
         stringBuilder.append(' ');
      }

      return stringBuilder;
   }

   public static Component toPrettyComponent(Tag tag) {
      return new TextComponentTagVisitor("").visit(tag);
   }

   public static String structureToSnbt(CompoundTag tag) {
      return new SnbtPrinterTagVisitor().visit(packStructureTemplate(tag));
   }

   public static CompoundTag snbtToStructure(String text) throws CommandSyntaxException {
      return unpackStructureTemplate(TagParser.parseCompoundFully(text));
   }

   @VisibleForTesting
   static CompoundTag packStructureTemplate(CompoundTag tag) {
      Optional<ListTag> optional = tag.getList("palettes");
      ListTag listTag;
      if (optional.isPresent()) {
         listTag = optional.get().getListOrEmpty(0);
      } else {
         listTag = tag.getListOrEmpty("palette");
      }

      ListTag listTag2 = listTag.compoundStream().map(NbtUtils::packBlockState).map(StringTag::valueOf).collect(Collectors.toCollection(ListTag::new));
      tag.put("palette", listTag2);
      if (optional.isPresent()) {
         ListTag listTag3 = new ListTag();
         optional.get().stream().flatMap(tagx -> tagx.asList().stream()).forEach(listTag3x -> {
            CompoundTag compoundTag = new CompoundTag();

            for (int i = 0; i < listTag3x.size(); i++) {
               compoundTag.putString(listTag2.getString(i).orElseThrow(), packBlockState(listTag3x.getCompound(i).orElseThrow()));
            }

            listTag3.add(compoundTag);
         });
         tag.put("palettes", listTag3);
      }

      Optional<ListTag> optional2 = tag.getList("entities");
      if (optional2.isPresent()) {
         ListTag listTag4 = optional2.get()
            .compoundStream()
            .sorted(Comparator.comparing(compoundTag -> compoundTag.getList("pos"), Comparators.emptiesLast(YXZ_LISTTAG_DOUBLE_COMPARATOR)))
            .collect(Collectors.toCollection(ListTag::new));
         tag.put("entities", listTag4);
      }

      ListTag listTag4 = tag.getList("blocks")
         .stream()
         .flatMap(ListTag::compoundStream)
         .sorted(Comparator.comparing(compoundTag -> compoundTag.getList("pos"), Comparators.emptiesLast(YXZ_LISTTAG_INT_COMPARATOR)))
         .peek(compoundTag -> compoundTag.putString("state", listTag2.getString(compoundTag.getIntOr("state", 0)).orElseThrow()))
         .collect(Collectors.toCollection(ListTag::new));
      tag.put("data", listTag4);
      tag.remove("blocks");
      return tag;
   }

   @VisibleForTesting
   static CompoundTag unpackStructureTemplate(CompoundTag tag) {
      ListTag listTag = tag.getListOrEmpty("palette");
      Map<String, Tag> map = listTag.stream()
         .flatMap(tagx -> tagx.asString().stream())
         .collect(ImmutableMap.toImmutableMap(Function.identity(), NbtUtils::unpackBlockState));
      Optional<ListTag> optional = tag.getList("palettes");
      if (optional.isPresent()) {
         tag.put(
            "palettes",
            optional.get()
               .compoundStream()
               .map(
                  compoundTagx -> map.keySet()
                     .stream()
                     .map(stringx -> compoundTagx.getString(stringx).orElseThrow())
                     .map(NbtUtils::unpackBlockState)
                     .collect(Collectors.toCollection(ListTag::new))
               )
               .collect(Collectors.toCollection(ListTag::new))
         );
         tag.remove("palette");
      } else {
         tag.put("palette", map.values().stream().collect(Collectors.toCollection(ListTag::new)));
      }

      Optional<ListTag> optional2 = tag.getList("data");
      if (optional2.isPresent()) {
         Object2IntMap<String> object2IntMap = new Object2IntOpenHashMap();
         object2IntMap.defaultReturnValue(-1);

         for (int i = 0; i < listTag.size(); i++) {
            object2IntMap.put(listTag.getString(i).orElseThrow(), i);
         }

         ListTag listTag2 = optional2.get();

         for (int j = 0; j < listTag2.size(); j++) {
            CompoundTag compoundTag = listTag2.getCompound(j).orElseThrow();
            String string = compoundTag.getString("state").orElseThrow();
            int k = object2IntMap.getInt(string);
            if (k == -1) {
               throw new IllegalStateException("Entry " + string + " missing from palette");
            }

            compoundTag.putInt("state", k);
         }

         tag.put("blocks", listTag2);
         tag.remove("data");
      }

      return tag;
   }

   @VisibleForTesting
   static String packBlockState(CompoundTag tag) {
      StringBuilder stringBuilder = new StringBuilder(tag.getString("Name").orElseThrow());
      tag.getCompound("Properties")
         .ifPresent(
            compoundTag -> {
               String string = compoundTag.entrySet()
                  .stream()
                  .sorted(Entry.comparingByKey())
                  .map(entry -> entry.getKey() + ":" + entry.getValue().asString().orElseThrow())
                  .collect(Collectors.joining(","));
               stringBuilder.append('{').append(string).append('}');
            }
         );
      return stringBuilder.toString();
   }

   @VisibleForTesting
   static CompoundTag unpackBlockState(String blockStateText) {
      CompoundTag compoundTag = new CompoundTag();
      int i = blockStateText.indexOf(123);
      String string;
      if (i >= 0) {
         string = blockStateText.substring(0, i);
         CompoundTag compoundTag2 = new CompoundTag();
         if (i + 2 <= blockStateText.length()) {
            String string2 = blockStateText.substring(i + 1, blockStateText.indexOf(125, i));
            COMMA_SPLITTER.split(string2).forEach(string2x -> {
               List<String> list = COLON_SPLITTER.splitToList(string2x);
               if (list.size() == 2) {
                  compoundTag2.putString(list.get(0), list.get(1));
               } else {
                  LOGGER.error("Something went wrong parsing: '{}' -- incorrect gamedata!", blockStateText);
               }
            });
            compoundTag.put("Properties", compoundTag2);
         }
      } else {
         string = blockStateText;
      }

      compoundTag.putString("Name", string);
      return compoundTag;
   }

   public static CompoundTag addCurrentDataVersion(CompoundTag tag) {
      int i = SharedConstants.getCurrentVersion().dataVersion().version();
      return addDataVersion(tag, i);
   }

   public static CompoundTag addDataVersion(CompoundTag tag, int dataVersion) {
      tag.putInt("DataVersion", dataVersion);
      return tag;
   }

   public static Dynamic<Tag> addCurrentDataVersion(Dynamic<Tag> tag) {
      int i = SharedConstants.getCurrentVersion().dataVersion().version();
      return addDataVersion(tag, i);
   }

   public static Dynamic<Tag> addDataVersion(Dynamic<Tag> tag, int dataVersion) {
      return tag.set("DataVersion", tag.createInt(dataVersion));
   }

   public static void addCurrentDataVersion(ValueOutput output) {
      int i = SharedConstants.getCurrentVersion().dataVersion().version();
      addDataVersion(output, i);
   }

   public static void addDataVersion(ValueOutput output, int dataVersion) {
      output.putInt("DataVersion", dataVersion);
   }

   public static int getDataVersion(CompoundTag tag) {
      return getDataVersion(tag, -1);
   }

   public static int getDataVersion(CompoundTag tag, int defaultValue) {
      return tag.getIntOr("DataVersion", defaultValue);
   }

   public static int getDataVersion(Dynamic<?> tag, int defaultValue) {
      return tag.get("DataVersion").asInt(defaultValue);
   }
}
