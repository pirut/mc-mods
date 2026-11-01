package net.minecraft.util.datafix.fixes;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.DataFixUtils;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Dynamic;
import it.unimi.dsi.fastutil.ints.Int2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntListIterator;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap.Entry;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.BitSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import net.minecraft.util.CrudeIncrementalIntIdentityHashBiMap;
import net.minecraft.util.datafix.ExtraDataFixUtils;
import net.minecraft.util.datafix.PackedBitStorage;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public class ChunkPalettedStorageFix extends DataFix {
   private static final int NORTH_WEST_MASK = 128;
   private static final int WEST_MASK = 64;
   private static final int SOUTH_WEST_MASK = 32;
   private static final int SOUTH_MASK = 16;
   private static final int SOUTH_EAST_MASK = 8;
   private static final int EAST_MASK = 4;
   private static final int NORTH_EAST_MASK = 2;
   private static final int NORTH_MASK = 1;
   static final Logger LOGGER = LogUtils.getLogger();
   private static final int SIZE = 4096;

   public ChunkPalettedStorageFix(Schema outputSchema, boolean changesType) {
      super(outputSchema, changesType);
   }

   public static String getName(Dynamic<?> data) {
      return data.get("Name").asString("");
   }

   public static String getProperty(Dynamic<?> data, String key) {
      return data.get("Properties").get(key).asString("");
   }

   public static int idFor(CrudeIncrementalIntIdentityHashBiMap<Dynamic<?>> palette, Dynamic<?> data) {
      int i = palette.getId(data);
      if (i == -1) {
         i = palette.add(data);
      }

      return i;
   }

   private Dynamic<?> fix(Dynamic<?> dynamic) {
      Optional<? extends Dynamic<?>> optional = dynamic.get("Level").result();
      return optional.isPresent() && optional.get().get("Sections").asStreamOpt().result().isPresent()
         ? dynamic.set("Level", new ChunkPalettedStorageFix.UpgradeChunk((Dynamic<?>)optional.get()).write())
         : dynamic;
   }

   public TypeRewriteRule makeRule() {
      Type<?> type = this.getInputSchema().getType(References.CHUNK);
      Type<?> type2 = this.getOutputSchema().getType(References.CHUNK);
      return this.writeFixAndRead("ChunkPalettedStorageFix", type, type2, this::fix);
   }

   public static int getSideMask(boolean west, boolean east, boolean north, boolean south) {
      int i = 0;
      if (north) {
         if (east) {
            i |= 2;
         } else if (west) {
            i |= 128;
         } else {
            i |= 1;
         }
      } else if (south) {
         if (west) {
            i |= 32;
         } else if (east) {
            i |= 8;
         } else {
            i |= 16;
         }
      } else if (east) {
         i |= 4;
      } else if (west) {
         i |= 64;
      }

      return i;
   }

   static class DataLayer {
      private static final int SIZE = 2048;
      private static final int NIBBLE_SIZE = 4;
      private final byte[] data;

      public DataLayer() {
         this.data = new byte[2048];
      }

      public DataLayer(byte[] data) {
         this.data = data;
         if (data.length != 2048) {
            throw new IllegalArgumentException("ChunkNibbleArrays should be 2048 bytes not: " + data.length);
         }
      }

      public int get(int x, int y, int z) {
         int i = this.getPosition(y << 8 | z << 4 | x);
         return this.isFirst(y << 8 | z << 4 | x) ? this.data[i] & 15 : this.data[i] >> 4 & 15;
      }

      private boolean isFirst(int packedPos) {
         return (packedPos & 1) == 0;
      }

      private int getPosition(int packedPos) {
         return packedPos >> 1;
      }
   }

   public static enum Direction {
      DOWN(ChunkPalettedStorageFix.Direction.AxisDirection.NEGATIVE, ChunkPalettedStorageFix.Direction.Axis.Y),
      UP(ChunkPalettedStorageFix.Direction.AxisDirection.POSITIVE, ChunkPalettedStorageFix.Direction.Axis.Y),
      NORTH(ChunkPalettedStorageFix.Direction.AxisDirection.NEGATIVE, ChunkPalettedStorageFix.Direction.Axis.Z),
      SOUTH(ChunkPalettedStorageFix.Direction.AxisDirection.POSITIVE, ChunkPalettedStorageFix.Direction.Axis.Z),
      WEST(ChunkPalettedStorageFix.Direction.AxisDirection.NEGATIVE, ChunkPalettedStorageFix.Direction.Axis.X),
      EAST(ChunkPalettedStorageFix.Direction.AxisDirection.POSITIVE, ChunkPalettedStorageFix.Direction.Axis.X);

      private final ChunkPalettedStorageFix.Direction.Axis axis;
      private final ChunkPalettedStorageFix.Direction.AxisDirection axisDirection;

      private Direction(final ChunkPalettedStorageFix.Direction.AxisDirection axisDirection, final ChunkPalettedStorageFix.Direction.Axis axis) {
         this.axis = axis;
         this.axisDirection = axisDirection;
      }

      public ChunkPalettedStorageFix.Direction.AxisDirection getAxisDirection() {
         return this.axisDirection;
      }

      public ChunkPalettedStorageFix.Direction.Axis getAxis() {
         return this.axis;
      }

      public static enum Axis {
         X,
         Y,
         Z;
      }

      public static enum AxisDirection {
         POSITIVE(1),
         NEGATIVE(-1);

         private final int step;

         private AxisDirection(final int step) {
            this.step = step;
         }

         public int getStep() {
            return this.step;
         }
      }
   }

   static class MappingConstants {
      static final BitSet VIRTUAL = new BitSet(256);
      static final BitSet FIX = new BitSet(256);
      static final Dynamic<?> PUMPKIN = ExtraDataFixUtils.blockState("minecraft:pumpkin");
      static final Dynamic<?> SNOWY_PODZOL = ExtraDataFixUtils.blockState("minecraft:podzol", Map.of("snowy", "true"));
      static final Dynamic<?> SNOWY_GRASS = ExtraDataFixUtils.blockState("minecraft:grass_block", Map.of("snowy", "true"));
      static final Dynamic<?> SNOWY_MYCELIUM = ExtraDataFixUtils.blockState("minecraft:mycelium", Map.of("snowy", "true"));
      static final Dynamic<?> UPPER_SUNFLOWER = ExtraDataFixUtils.blockState("minecraft:sunflower", Map.of("half", "upper"));
      static final Dynamic<?> UPPER_LILAC = ExtraDataFixUtils.blockState("minecraft:lilac", Map.of("half", "upper"));
      static final Dynamic<?> UPPER_TALL_GRASS = ExtraDataFixUtils.blockState("minecraft:tall_grass", Map.of("half", "upper"));
      static final Dynamic<?> UPPER_LARGE_FERN = ExtraDataFixUtils.blockState("minecraft:large_fern", Map.of("half", "upper"));
      static final Dynamic<?> UPPER_ROSE_BUSH = ExtraDataFixUtils.blockState("minecraft:rose_bush", Map.of("half", "upper"));
      static final Dynamic<?> UPPER_PEONY = ExtraDataFixUtils.blockState("minecraft:peony", Map.of("half", "upper"));
      static final Map<String, Dynamic<?>> FLOWER_POT_MAP = (Map<String, Dynamic<?>>)DataFixUtils.make(Maps.newHashMap(), hashMap -> {
         hashMap.put("minecraft:air0", ExtraDataFixUtils.blockState("minecraft:flower_pot"));
         hashMap.put("minecraft:red_flower0", ExtraDataFixUtils.blockState("minecraft:potted_poppy"));
         hashMap.put("minecraft:red_flower1", ExtraDataFixUtils.blockState("minecraft:potted_blue_orchid"));
         hashMap.put("minecraft:red_flower2", ExtraDataFixUtils.blockState("minecraft:potted_allium"));
         hashMap.put("minecraft:red_flower3", ExtraDataFixUtils.blockState("minecraft:potted_azure_bluet"));
         hashMap.put("minecraft:red_flower4", ExtraDataFixUtils.blockState("minecraft:potted_red_tulip"));
         hashMap.put("minecraft:red_flower5", ExtraDataFixUtils.blockState("minecraft:potted_orange_tulip"));
         hashMap.put("minecraft:red_flower6", ExtraDataFixUtils.blockState("minecraft:potted_white_tulip"));
         hashMap.put("minecraft:red_flower7", ExtraDataFixUtils.blockState("minecraft:potted_pink_tulip"));
         hashMap.put("minecraft:red_flower8", ExtraDataFixUtils.blockState("minecraft:potted_oxeye_daisy"));
         hashMap.put("minecraft:yellow_flower0", ExtraDataFixUtils.blockState("minecraft:potted_dandelion"));
         hashMap.put("minecraft:sapling0", ExtraDataFixUtils.blockState("minecraft:potted_oak_sapling"));
         hashMap.put("minecraft:sapling1", ExtraDataFixUtils.blockState("minecraft:potted_spruce_sapling"));
         hashMap.put("minecraft:sapling2", ExtraDataFixUtils.blockState("minecraft:potted_birch_sapling"));
         hashMap.put("minecraft:sapling3", ExtraDataFixUtils.blockState("minecraft:potted_jungle_sapling"));
         hashMap.put("minecraft:sapling4", ExtraDataFixUtils.blockState("minecraft:potted_acacia_sapling"));
         hashMap.put("minecraft:sapling5", ExtraDataFixUtils.blockState("minecraft:potted_dark_oak_sapling"));
         hashMap.put("minecraft:red_mushroom0", ExtraDataFixUtils.blockState("minecraft:potted_red_mushroom"));
         hashMap.put("minecraft:brown_mushroom0", ExtraDataFixUtils.blockState("minecraft:potted_brown_mushroom"));
         hashMap.put("minecraft:deadbush0", ExtraDataFixUtils.blockState("minecraft:potted_dead_bush"));
         hashMap.put("minecraft:tallgrass2", ExtraDataFixUtils.blockState("minecraft:potted_fern"));
         hashMap.put("minecraft:cactus0", ExtraDataFixUtils.blockState("minecraft:potted_cactus"));
      });
      static final Map<String, Dynamic<?>> SKULL_MAP = (Map<String, Dynamic<?>>)DataFixUtils.make(Maps.newHashMap(), hashMap -> {
         mapSkull(hashMap, 0, "skeleton", "skull");
         mapSkull(hashMap, 1, "wither_skeleton", "skull");
         mapSkull(hashMap, 2, "zombie", "head");
         mapSkull(hashMap, 3, "player", "head");
         mapSkull(hashMap, 4, "creeper", "head");
         mapSkull(hashMap, 5, "dragon", "head");
      });
      static final Map<String, Dynamic<?>> DOOR_MAP = (Map<String, Dynamic<?>>)DataFixUtils.make(Maps.newHashMap(), hashMap -> {
         mapDoor(hashMap, "oak_door");
         mapDoor(hashMap, "iron_door");
         mapDoor(hashMap, "spruce_door");
         mapDoor(hashMap, "birch_door");
         mapDoor(hashMap, "jungle_door");
         mapDoor(hashMap, "acacia_door");
         mapDoor(hashMap, "dark_oak_door");
      });
      static final Map<String, Dynamic<?>> NOTE_BLOCK_MAP = (Map<String, Dynamic<?>>)DataFixUtils.make(Maps.newHashMap(), hashMap -> {
         for (int i = 0; i < 26; i++) {
            hashMap.put("true" + i, ExtraDataFixUtils.blockState("minecraft:note_block", Map.of("powered", "true", "note", String.valueOf(i))));
            hashMap.put("false" + i, ExtraDataFixUtils.blockState("minecraft:note_block", Map.of("powered", "false", "note", String.valueOf(i))));
         }
      });
      private static final Int2ObjectMap<String> DYE_COLOR_MAP = (Int2ObjectMap<String>)DataFixUtils.make(
         new Int2ObjectOpenHashMap(), int2ObjectOpenHashMap -> {
            int2ObjectOpenHashMap.put(0, "white");
            int2ObjectOpenHashMap.put(1, "orange");
            int2ObjectOpenHashMap.put(2, "magenta");
            int2ObjectOpenHashMap.put(3, "light_blue");
            int2ObjectOpenHashMap.put(4, "yellow");
            int2ObjectOpenHashMap.put(5, "lime");
            int2ObjectOpenHashMap.put(6, "pink");
            int2ObjectOpenHashMap.put(7, "gray");
            int2ObjectOpenHashMap.put(8, "light_gray");
            int2ObjectOpenHashMap.put(9, "cyan");
            int2ObjectOpenHashMap.put(10, "purple");
            int2ObjectOpenHashMap.put(11, "blue");
            int2ObjectOpenHashMap.put(12, "brown");
            int2ObjectOpenHashMap.put(13, "green");
            int2ObjectOpenHashMap.put(14, "red");
            int2ObjectOpenHashMap.put(15, "black");
         }
      );
      static final Map<String, Dynamic<?>> BED_BLOCK_MAP = (Map<String, Dynamic<?>>)DataFixUtils.make(Maps.newHashMap(), hashMap -> {
         ObjectIterator var1 = DYE_COLOR_MAP.int2ObjectEntrySet().iterator();

         while (var1.hasNext()) {
            Entry<String> entry = (Entry<String>)var1.next();
            if (!Objects.equals(entry.getValue(), "red")) {
               addBeds(hashMap, entry.getIntKey(), (String)entry.getValue());
            }
         }
      });
      static final Map<String, Dynamic<?>> BANNER_BLOCK_MAP = (Map<String, Dynamic<?>>)DataFixUtils.make(Maps.newHashMap(), hashMap -> {
         ObjectIterator var1 = DYE_COLOR_MAP.int2ObjectEntrySet().iterator();

         while (var1.hasNext()) {
            Entry<String> entry = (Entry<String>)var1.next();
            if (!Objects.equals(entry.getValue(), "white")) {
               addBanners(hashMap, 15 - entry.getIntKey(), (String)entry.getValue());
            }
         }
      });
      static final Dynamic<?> AIR = ExtraDataFixUtils.blockState("minecraft:air");

      private MappingConstants() {
      }

      private static void mapSkull(Map<String, Dynamic<?>> map, int id, String skullType, String suffix) {
         map.put(id + "north", ExtraDataFixUtils.blockState("minecraft:" + skullType + "_wall_" + suffix, Map.of("facing", "north")));
         map.put(id + "east", ExtraDataFixUtils.blockState("minecraft:" + skullType + "_wall_" + suffix, Map.of("facing", "east")));
         map.put(id + "south", ExtraDataFixUtils.blockState("minecraft:" + skullType + "_wall_" + suffix, Map.of("facing", "south")));
         map.put(id + "west", ExtraDataFixUtils.blockState("minecraft:" + skullType + "_wall_" + suffix, Map.of("facing", "west")));

         for (int i = 0; i < 16; i++) {
            map.put("" + id + i, ExtraDataFixUtils.blockState("minecraft:" + skullType + "_" + suffix, Map.of("rotation", String.valueOf(i))));
         }
      }

      private static void mapDoor(Map<String, Dynamic<?>> map, String doorId) {
         String string = "minecraft:" + doorId;
         map.put(
            "minecraft:" + doorId + "eastlowerleftfalsefalse",
            ExtraDataFixUtils.blockState(string, Map.of("facing", "east", "half", "lower", "hinge", "left", "open", "false", "powered", "false"))
         );
         map.put(
            "minecraft:" + doorId + "eastlowerleftfalsetrue",
            ExtraDataFixUtils.blockState(string, Map.of("facing", "east", "half", "lower", "hinge", "left", "open", "false", "powered", "true"))
         );
         map.put(
            "minecraft:" + doorId + "eastlowerlefttruefalse",
            ExtraDataFixUtils.blockState(string, Map.of("facing", "east", "half", "lower", "hinge", "left", "open", "true", "powered", "false"))
         );
         map.put(
            "minecraft:" + doorId + "eastlowerlefttruetrue",
            ExtraDataFixUtils.blockState(string, Map.of("facing", "east", "half", "lower", "hinge", "left", "open", "true", "powered", "true"))
         );
         map.put(
            "minecraft:" + doorId + "eastlowerrightfalsefalse",
            ExtraDataFixUtils.blockState(string, Map.of("facing", "east", "half", "lower", "hinge", "right", "open", "false", "powered", "false"))
         );
         map.put(
            "minecraft:" + doorId + "eastlowerrightfalsetrue",
            ExtraDataFixUtils.blockState(string, Map.of("facing", "east", "half", "lower", "hinge", "right", "open", "false", "powered", "true"))
         );
         map.put(
            "minecraft:" + doorId + "eastlowerrighttruefalse",
            ExtraDataFixUtils.blockState(string, Map.of("facing", "east", "half", "lower", "hinge", "right", "open", "true", "powered", "false"))
         );
         map.put(
            "minecraft:" + doorId + "eastlowerrighttruetrue",
            ExtraDataFixUtils.blockState(string, Map.of("facing", "east", "half", "lower", "hinge", "right", "open", "true", "powered", "true"))
         );
         map.put(
            "minecraft:" + doorId + "eastupperleftfalsefalse",
            ExtraDataFixUtils.blockState(string, Map.of("facing", "east", "half", "upper", "hinge", "left", "open", "false", "powered", "false"))
         );
         map.put(
            "minecraft:" + doorId + "eastupperleftfalsetrue",
            ExtraDataFixUtils.blockState(string, Map.of("facing", "east", "half", "upper", "hinge", "left", "open", "false", "powered", "true"))
         );
         map.put(
            "minecraft:" + doorId + "eastupperlefttruefalse",
            ExtraDataFixUtils.blockState(string, Map.of("facing", "east", "half", "upper", "hinge", "left", "open", "true", "powered", "false"))
         );
         map.put(
            "minecraft:" + doorId + "eastupperlefttruetrue",
            ExtraDataFixUtils.blockState(string, Map.of("facing", "east", "half", "upper", "hinge", "left", "open", "true", "powered", "true"))
         );
         map.put(
            "minecraft:" + doorId + "eastupperrightfalsefalse",
            ExtraDataFixUtils.blockState(string, Map.of("facing", "east", "half", "upper", "hinge", "right", "open", "false", "powered", "false"))
         );
         map.put(
            "minecraft:" + doorId + "eastupperrightfalsetrue",
            ExtraDataFixUtils.blockState(string, Map.of("facing", "east", "half", "upper", "hinge", "right", "open", "false", "powered", "true"))
         );
         map.put(
            "minecraft:" + doorId + "eastupperrighttruefalse",
            ExtraDataFixUtils.blockState(string, Map.of("facing", "east", "half", "upper", "hinge", "right", "open", "true", "powered", "false"))
         );
         map.put(
            "minecraft:" + doorId + "eastupperrighttruetrue",
            ExtraDataFixUtils.blockState(string, Map.of("facing", "east", "half", "upper", "hinge", "right", "open", "true", "powered", "true"))
         );
         map.put(
            "minecraft:" + doorId + "northlowerleftfalsefalse",
            ExtraDataFixUtils.blockState(string, Map.of("facing", "north", "half", "lower", "hinge", "left", "open", "false", "powered", "false"))
         );
         map.put(
            "minecraft:" + doorId + "northlowerleftfalsetrue",
            ExtraDataFixUtils.blockState(string, Map.of("facing", "north", "half", "lower", "hinge", "left", "open", "false", "powered", "true"))
         );
         map.put(
            "minecraft:" + doorId + "northlowerlefttruefalse",
            ExtraDataFixUtils.blockState(string, Map.of("facing", "north", "half", "lower", "hinge", "left", "open", "true", "powered", "false"))
         );
         map.put(
            "minecraft:" + doorId + "northlowerlefttruetrue",
            ExtraDataFixUtils.blockState(string, Map.of("facing", "north", "half", "lower", "hinge", "left", "open", "true", "powered", "true"))
         );
         map.put(
            "minecraft:" + doorId + "northlowerrightfalsefalse",
            ExtraDataFixUtils.blockState(string, Map.of("facing", "north", "half", "lower", "hinge", "right", "open", "false", "powered", "false"))
         );
         map.put(
            "minecraft:" + doorId + "northlowerrightfalsetrue",
            ExtraDataFixUtils.blockState(string, Map.of("facing", "north", "half", "lower", "hinge", "right", "open", "false", "powered", "true"))
         );
         map.put(
            "minecraft:" + doorId + "northlowerrighttruefalse",
            ExtraDataFixUtils.blockState(string, Map.of("facing", "north", "half", "lower", "hinge", "right", "open", "true", "powered", "false"))
         );
         map.put(
            "minecraft:" + doorId + "northlowerrighttruetrue",
            ExtraDataFixUtils.blockState(string, Map.of("facing", "north", "half", "lower", "hinge", "right", "open", "true", "powered", "true"))
         );
         map.put(
            "minecraft:" + doorId + "northupperleftfalsefalse",
            ExtraDataFixUtils.blockState(string, Map.of("facing", "north", "half", "upper", "hinge", "left", "open", "false", "powered", "false"))
         );
         map.put(
            "minecraft:" + doorId + "northupperleftfalsetrue",
            ExtraDataFixUtils.blockState(string, Map.of("facing", "north", "half", "upper", "hinge", "left", "open", "false", "powered", "true"))
         );
         map.put(
            "minecraft:" + doorId + "northupperlefttruefalse",
            ExtraDataFixUtils.blockState(string, Map.of("facing", "north", "half", "upper", "hinge", "left", "open", "true", "powered", "false"))
         );
         map.put(
            "minecraft:" + doorId + "northupperlefttruetrue",
            ExtraDataFixUtils.blockState(string, Map.of("facing", "north", "half", "upper", "hinge", "left", "open", "true", "powered", "true"))
         );
         map.put(
            "minecraft:" + doorId + "northupperrightfalsefalse",
            ExtraDataFixUtils.blockState(string, Map.of("facing", "north", "half", "upper", "hinge", "right", "open", "false", "powered", "false"))
         );
         map.put(
            "minecraft:" + doorId + "northupperrightfalsetrue",
            ExtraDataFixUtils.blockState(string, Map.of("facing", "north", "half", "upper", "hinge", "right", "open", "false", "powered", "true"))
         );
         map.put(
            "minecraft:" + doorId + "northupperrighttruefalse",
            ExtraDataFixUtils.blockState(string, Map.of("facing", "north", "half", "upper", "hinge", "right", "open", "true", "powered", "false"))
         );
         map.put(
            "minecraft:" + doorId + "northupperrighttruetrue",
            ExtraDataFixUtils.blockState(string, Map.of("facing", "north", "half", "upper", "hinge", "right", "open", "true", "powered", "true"))
         );
         map.put(
            "minecraft:" + doorId + "southlowerleftfalsefalse",
            ExtraDataFixUtils.blockState(string, Map.of("facing", "south", "half", "lower", "hinge", "left", "open", "false", "powered", "false"))
         );
         map.put(
            "minecraft:" + doorId + "southlowerleftfalsetrue",
            ExtraDataFixUtils.blockState(string, Map.of("facing", "south", "half", "lower", "hinge", "left", "open", "false", "powered", "true"))
         );
         map.put(
            "minecraft:" + doorId + "southlowerlefttruefalse",
            ExtraDataFixUtils.blockState(string, Map.of("facing", "south", "half", "lower", "hinge", "left", "open", "true", "powered", "false"))
         );
         map.put(
            "minecraft:" + doorId + "southlowerlefttruetrue",
            ExtraDataFixUtils.blockState(string, Map.of("facing", "south", "half", "lower", "hinge", "left", "open", "true", "powered", "true"))
         );
         map.put(
            "minecraft:" + doorId + "southlowerrightfalsefalse",
            ExtraDataFixUtils.blockState(string, Map.of("facing", "south", "half", "lower", "hinge", "right", "open", "false", "powered", "false"))
         );
         map.put(
            "minecraft:" + doorId + "southlowerrightfalsetrue",
            ExtraDataFixUtils.blockState(string, Map.of("facing", "south", "half", "lower", "hinge", "right", "open", "false", "powered", "true"))
         );
         map.put(
            "minecraft:" + doorId + "southlowerrighttruefalse",
            ExtraDataFixUtils.blockState(string, Map.of("facing", "south", "half", "lower", "hinge", "right", "open", "true", "powered", "false"))
         );
         map.put(
            "minecraft:" + doorId + "southlowerrighttruetrue",
            ExtraDataFixUtils.blockState(string, Map.of("facing", "south", "half", "lower", "hinge", "right", "open", "true", "powered", "true"))
         );
         map.put(
            "minecraft:" + doorId + "southupperleftfalsefalse",
            ExtraDataFixUtils.blockState(string, Map.of("facing", "south", "half", "upper", "hinge", "left", "open", "false", "powered", "false"))
         );
         map.put(
            "minecraft:" + doorId + "southupperleftfalsetrue",
            ExtraDataFixUtils.blockState(string, Map.of("facing", "south", "half", "upper", "hinge", "left", "open", "false", "powered", "true"))
         );
         map.put(
            "minecraft:" + doorId + "southupperlefttruefalse",
            ExtraDataFixUtils.blockState(string, Map.of("facing", "south", "half", "upper", "hinge", "left", "open", "true", "powered", "false"))
         );
         map.put(
            "minecraft:" + doorId + "southupperlefttruetrue",
            ExtraDataFixUtils.blockState(string, Map.of("facing", "south", "half", "upper", "hinge", "left", "open", "true", "powered", "true"))
         );
         map.put(
            "minecraft:" + doorId + "southupperrightfalsefalse",
            ExtraDataFixUtils.blockState(string, Map.of("facing", "south", "half", "upper", "hinge", "right", "open", "false", "powered", "false"))
         );
         map.put(
            "minecraft:" + doorId + "southupperrightfalsetrue",
            ExtraDataFixUtils.blockState(string, Map.of("facing", "south", "half", "upper", "hinge", "right", "open", "false", "powered", "true"))
         );
         map.put(
            "minecraft:" + doorId + "southupperrighttruefalse",
            ExtraDataFixUtils.blockState(string, Map.of("facing", "south", "half", "upper", "hinge", "right", "open", "true", "powered", "false"))
         );
         map.put(
            "minecraft:" + doorId + "southupperrighttruetrue",
            ExtraDataFixUtils.blockState(string, Map.of("facing", "south", "half", "upper", "hinge", "right", "open", "true", "powered", "true"))
         );
         map.put(
            "minecraft:" + doorId + "westlowerleftfalsefalse",
            ExtraDataFixUtils.blockState(string, Map.of("facing", "west", "half", "lower", "hinge", "left", "open", "false", "powered", "false"))
         );
         map.put(
            "minecraft:" + doorId + "westlowerleftfalsetrue",
            ExtraDataFixUtils.blockState(string, Map.of("facing", "west", "half", "lower", "hinge", "left", "open", "false", "powered", "true"))
         );
         map.put(
            "minecraft:" + doorId + "westlowerlefttruefalse",
            ExtraDataFixUtils.blockState(string, Map.of("facing", "west", "half", "lower", "hinge", "left", "open", "true", "powered", "false"))
         );
         map.put(
            "minecraft:" + doorId + "westlowerlefttruetrue",
            ExtraDataFixUtils.blockState(string, Map.of("facing", "west", "half", "lower", "hinge", "left", "open", "true", "powered", "true"))
         );
         map.put(
            "minecraft:" + doorId + "westlowerrightfalsefalse",
            ExtraDataFixUtils.blockState(string, Map.of("facing", "west", "half", "lower", "hinge", "right", "open", "false", "powered", "false"))
         );
         map.put(
            "minecraft:" + doorId + "westlowerrightfalsetrue",
            ExtraDataFixUtils.blockState(string, Map.of("facing", "west", "half", "lower", "hinge", "right", "open", "false", "powered", "true"))
         );
         map.put(
            "minecraft:" + doorId + "westlowerrighttruefalse",
            ExtraDataFixUtils.blockState(string, Map.of("facing", "west", "half", "lower", "hinge", "right", "open", "true", "powered", "false"))
         );
         map.put(
            "minecraft:" + doorId + "westlowerrighttruetrue",
            ExtraDataFixUtils.blockState(string, Map.of("facing", "west", "half", "lower", "hinge", "right", "open", "true", "powered", "true"))
         );
         map.put(
            "minecraft:" + doorId + "westupperleftfalsefalse",
            ExtraDataFixUtils.blockState(string, Map.of("facing", "west", "half", "upper", "hinge", "left", "open", "false", "powered", "false"))
         );
         map.put(
            "minecraft:" + doorId + "westupperleftfalsetrue",
            ExtraDataFixUtils.blockState(string, Map.of("facing", "west", "half", "upper", "hinge", "left", "open", "false", "powered", "true"))
         );
         map.put(
            "minecraft:" + doorId + "westupperlefttruefalse",
            ExtraDataFixUtils.blockState(string, Map.of("facing", "west", "half", "upper", "hinge", "left", "open", "true", "powered", "false"))
         );
         map.put(
            "minecraft:" + doorId + "westupperlefttruetrue",
            ExtraDataFixUtils.blockState(string, Map.of("facing", "west", "half", "upper", "hinge", "left", "open", "true", "powered", "true"))
         );
         map.put(
            "minecraft:" + doorId + "westupperrightfalsefalse",
            ExtraDataFixUtils.blockState(string, Map.of("facing", "west", "half", "upper", "hinge", "right", "open", "false", "powered", "false"))
         );
         map.put(
            "minecraft:" + doorId + "westupperrightfalsetrue",
            ExtraDataFixUtils.blockState(string, Map.of("facing", "west", "half", "upper", "hinge", "right", "open", "false", "powered", "true"))
         );
         map.put(
            "minecraft:" + doorId + "westupperrighttruefalse",
            ExtraDataFixUtils.blockState(string, Map.of("facing", "west", "half", "upper", "hinge", "right", "open", "true", "powered", "false"))
         );
         map.put(
            "minecraft:" + doorId + "westupperrighttruetrue",
            ExtraDataFixUtils.blockState(string, Map.of("facing", "west", "half", "upper", "hinge", "right", "open", "true", "powered", "true"))
         );
      }

      private static void addBeds(Map<String, Dynamic<?>> map, int id, String bedColor) {
         map.put(
            "southfalsefoot" + id,
            ExtraDataFixUtils.blockState("minecraft:" + bedColor + "_bed", Map.of("facing", "south", "occupied", "false", "part", "foot"))
         );
         map.put(
            "westfalsefoot" + id, ExtraDataFixUtils.blockState("minecraft:" + bedColor + "_bed", Map.of("facing", "west", "occupied", "false", "part", "foot"))
         );
         map.put(
            "northfalsefoot" + id,
            ExtraDataFixUtils.blockState("minecraft:" + bedColor + "_bed", Map.of("facing", "north", "occupied", "false", "part", "foot"))
         );
         map.put(
            "eastfalsefoot" + id, ExtraDataFixUtils.blockState("minecraft:" + bedColor + "_bed", Map.of("facing", "east", "occupied", "false", "part", "foot"))
         );
         map.put(
            "southfalsehead" + id,
            ExtraDataFixUtils.blockState("minecraft:" + bedColor + "_bed", Map.of("facing", "south", "occupied", "false", "part", "head"))
         );
         map.put(
            "westfalsehead" + id, ExtraDataFixUtils.blockState("minecraft:" + bedColor + "_bed", Map.of("facing", "west", "occupied", "false", "part", "head"))
         );
         map.put(
            "northfalsehead" + id,
            ExtraDataFixUtils.blockState("minecraft:" + bedColor + "_bed", Map.of("facing", "north", "occupied", "false", "part", "head"))
         );
         map.put(
            "eastfalsehead" + id, ExtraDataFixUtils.blockState("minecraft:" + bedColor + "_bed", Map.of("facing", "east", "occupied", "false", "part", "head"))
         );
         map.put(
            "southtruehead" + id, ExtraDataFixUtils.blockState("minecraft:" + bedColor + "_bed", Map.of("facing", "south", "occupied", "true", "part", "head"))
         );
         map.put(
            "westtruehead" + id, ExtraDataFixUtils.blockState("minecraft:" + bedColor + "_bed", Map.of("facing", "west", "occupied", "true", "part", "head"))
         );
         map.put(
            "northtruehead" + id, ExtraDataFixUtils.blockState("minecraft:" + bedColor + "_bed", Map.of("facing", "north", "occupied", "true", "part", "head"))
         );
         map.put(
            "easttruehead" + id, ExtraDataFixUtils.blockState("minecraft:" + bedColor + "_bed", Map.of("facing", "east", "occupied", "true", "part", "head"))
         );
      }

      private static void addBanners(Map<String, Dynamic<?>> map, int id, String bannerColor) {
         for (int i = 0; i < 16; i++) {
            map.put(i + "_" + id, ExtraDataFixUtils.blockState("minecraft:" + bannerColor + "_banner", Map.of("rotation", String.valueOf(i))));
         }

         map.put("north_" + id, ExtraDataFixUtils.blockState("minecraft:" + bannerColor + "_wall_banner", Map.of("facing", "north")));
         map.put("south_" + id, ExtraDataFixUtils.blockState("minecraft:" + bannerColor + "_wall_banner", Map.of("facing", "south")));
         map.put("west_" + id, ExtraDataFixUtils.blockState("minecraft:" + bannerColor + "_wall_banner", Map.of("facing", "west")));
         map.put("east_" + id, ExtraDataFixUtils.blockState("minecraft:" + bannerColor + "_wall_banner", Map.of("facing", "east")));
      }

      static {
         FIX.set(2);
         FIX.set(3);
         FIX.set(110);
         FIX.set(140);
         FIX.set(144);
         FIX.set(25);
         FIX.set(86);
         FIX.set(26);
         FIX.set(176);
         FIX.set(177);
         FIX.set(175);
         FIX.set(64);
         FIX.set(71);
         FIX.set(193);
         FIX.set(194);
         FIX.set(195);
         FIX.set(196);
         FIX.set(197);
         VIRTUAL.set(54);
         VIRTUAL.set(146);
         VIRTUAL.set(25);
         VIRTUAL.set(26);
         VIRTUAL.set(51);
         VIRTUAL.set(53);
         VIRTUAL.set(67);
         VIRTUAL.set(108);
         VIRTUAL.set(109);
         VIRTUAL.set(114);
         VIRTUAL.set(128);
         VIRTUAL.set(134);
         VIRTUAL.set(135);
         VIRTUAL.set(136);
         VIRTUAL.set(156);
         VIRTUAL.set(163);
         VIRTUAL.set(164);
         VIRTUAL.set(180);
         VIRTUAL.set(203);
         VIRTUAL.set(55);
         VIRTUAL.set(85);
         VIRTUAL.set(113);
         VIRTUAL.set(188);
         VIRTUAL.set(189);
         VIRTUAL.set(190);
         VIRTUAL.set(191);
         VIRTUAL.set(192);
         VIRTUAL.set(93);
         VIRTUAL.set(94);
         VIRTUAL.set(101);
         VIRTUAL.set(102);
         VIRTUAL.set(160);
         VIRTUAL.set(106);
         VIRTUAL.set(107);
         VIRTUAL.set(183);
         VIRTUAL.set(184);
         VIRTUAL.set(185);
         VIRTUAL.set(186);
         VIRTUAL.set(187);
         VIRTUAL.set(132);
         VIRTUAL.set(139);
         VIRTUAL.set(199);
      }
   }

   static class Section {
      private final CrudeIncrementalIntIdentityHashBiMap<Dynamic<?>> palette = CrudeIncrementalIntIdentityHashBiMap.create(32);
      private final List<Dynamic<?>> listTag;
      private final Dynamic<?> section;
      private final boolean hasData;
      final Int2ObjectMap<IntList> toFix = new Int2ObjectLinkedOpenHashMap();
      final IntList update = new IntArrayList();
      public final int y;
      private final Set<Dynamic<?>> seen = Sets.newIdentityHashSet();
      private final int[] buffer = new int[4096];

      public Section(Dynamic<?> section) {
         this.listTag = Lists.newArrayList();
         this.section = section;
         this.y = section.get("Y").asInt(0);
         this.hasData = section.get("Blocks").result().isPresent();
      }

      public Dynamic<?> getBlock(int index) {
         if (index >= 0 && index <= 4095) {
            Dynamic<?> dynamic = this.palette.byId(this.buffer[index]);
            return dynamic == null ? ChunkPalettedStorageFix.MappingConstants.AIR : dynamic;
         } else {
            return ChunkPalettedStorageFix.MappingConstants.AIR;
         }
      }

      public void setBlock(int index, Dynamic<?> block) {
         if (this.seen.add(block)) {
            this.listTag.add("%%FILTER_ME%%".equals(ChunkPalettedStorageFix.getName(block)) ? ChunkPalettedStorageFix.MappingConstants.AIR : block);
         }

         this.buffer[index] = ChunkPalettedStorageFix.idFor(this.palette, block);
      }

      public int upgrade(int sides) {
         if (!this.hasData) {
            return sides;
         } else {
            ByteBuffer byteBuffer = (ByteBuffer)this.section.get("Blocks").asByteBufferOpt().result().get();
            ChunkPalettedStorageFix.DataLayer dataLayer = this.section
               .get("Data")
               .asByteBufferOpt()
               .map(byteBufferx -> new ChunkPalettedStorageFix.DataLayer(DataFixUtils.toArray(byteBufferx)))
               .result()
               .orElseGet(ChunkPalettedStorageFix.DataLayer::new);
            ChunkPalettedStorageFix.DataLayer dataLayer2 = this.section
               .get("Add")
               .asByteBufferOpt()
               .map(byteBufferx -> new ChunkPalettedStorageFix.DataLayer(DataFixUtils.toArray(byteBufferx)))
               .result()
               .orElseGet(ChunkPalettedStorageFix.DataLayer::new);
            this.seen.add(ChunkPalettedStorageFix.MappingConstants.AIR);
            ChunkPalettedStorageFix.idFor(this.palette, ChunkPalettedStorageFix.MappingConstants.AIR);
            this.listTag.add(ChunkPalettedStorageFix.MappingConstants.AIR);

            for (int i = 0; i < 4096; i++) {
               int j = i & 15;
               int k = i >> 8 & 15;
               int l = i >> 4 & 15;
               int m = dataLayer2.get(j, k, l) << 12 | (byteBuffer.get(i) & 255) << 4 | dataLayer.get(j, k, l);
               if (ChunkPalettedStorageFix.MappingConstants.FIX.get(m >> 4)) {
                  this.addFix(m >> 4, i);
               }

               if (ChunkPalettedStorageFix.MappingConstants.VIRTUAL.get(m >> 4)) {
                  int n = ChunkPalettedStorageFix.getSideMask(j == 0, j == 15, l == 0, l == 15);
                  if (n == 0) {
                     this.update.add(i);
                  } else {
                     sides |= n;
                  }
               }

               this.setBlock(i, BlockStateData.getTag(m));
            }

            return sides;
         }
      }

      private void addFix(int index, int value) {
         IntList intList = (IntList)this.toFix.get(index);
         if (intList == null) {
            intList = new IntArrayList();
            this.toFix.put(index, intList);
         }

         intList.add(value);
      }

      public Dynamic<?> write() {
         Dynamic<?> dynamic = this.section;
         if (!this.hasData) {
            return dynamic;
         } else {
            dynamic = dynamic.set("Palette", dynamic.createList(this.listTag.stream()));
            int i = Math.max(4, DataFixUtils.ceillog2(this.seen.size()));
            PackedBitStorage packedBitStorage = new PackedBitStorage(i, 4096);

            for (int j = 0; j < this.buffer.length; j++) {
               packedBitStorage.set(j, this.buffer[j]);
            }

            dynamic = dynamic.set("BlockStates", dynamic.createLongList(Arrays.stream(packedBitStorage.getRaw())));
            dynamic = dynamic.remove("Blocks");
            dynamic = dynamic.remove("Data");
            return dynamic.remove("Add");
         }
      }
   }

   static final class UpgradeChunk {
      private int sides;
      private final ChunkPalettedStorageFix.Section[] sections = new ChunkPalettedStorageFix.Section[16];
      private final Dynamic<?> level;
      private final int x;
      private final int z;
      private final Int2ObjectMap<Dynamic<?>> blockEntities = new Int2ObjectLinkedOpenHashMap(16);

      public UpgradeChunk(Dynamic<?> level) {
         this.level = level;
         this.x = level.get("xPos").asInt(0) << 4;
         this.z = level.get("zPos").asInt(0) << 4;
         level.get("TileEntities")
            .asStreamOpt()
            .ifSuccess(
               stream -> stream.forEach(
                  dynamicx -> {
                     int ix = dynamicx.get("x").asInt(0) - this.x & 15;
                     int jx = dynamicx.get("y").asInt(0);
                     int k = dynamicx.get("z").asInt(0) - this.z & 15;
                     int l = jx << 8 | k << 4 | ix;
                     if (this.blockEntities.put(l, dynamicx) != null) {
                        ChunkPalettedStorageFix.LOGGER
                           .warn("In chunk: {}x{} found a duplicate block entity at position: [{}, {}, {}]", new Object[]{this.x, this.z, ix, jx, k});
                     }
                  }
               )
            );
         boolean bl = level.get("convertedFromAlphaFormat").asBoolean(false);
         level.get("Sections").asStreamOpt().ifSuccess(stream -> stream.forEach(dynamicx -> {
            ChunkPalettedStorageFix.Section sectionx = new ChunkPalettedStorageFix.Section(dynamicx);
            this.sides = sectionx.upgrade(this.sides);
            this.sections[sectionx.y] = sectionx;
         }));

         for (ChunkPalettedStorageFix.Section section : this.sections) {
            if (section != null) {
               ObjectIterator var7 = section.toFix.int2ObjectEntrySet().iterator();

               while (var7.hasNext()) {
                  Entry<IntList> entry = (Entry<IntList>)var7.next();
                  int i = section.y << 12;
                  switch (entry.getIntKey()) {
                     case 2:
                        IntListIterator var30 = ((IntList)entry.getValue()).iterator();

                        while (var30.hasNext()) {
                           int j = (Integer)var30.next();
                           j |= i;
                           Dynamic<?> dynamic = this.getBlock(j);
                           if ("minecraft:grass_block".equals(ChunkPalettedStorageFix.getName(dynamic))) {
                              String string = ChunkPalettedStorageFix.getName(this.getBlock(relative(j, ChunkPalettedStorageFix.Direction.UP)));
                              if ("minecraft:snow".equals(string) || "minecraft:snow_layer".equals(string)) {
                                 this.setBlock(j, ChunkPalettedStorageFix.MappingConstants.SNOWY_GRASS);
                              }
                           }
                        }
                        break;
                     case 3:
                        IntListIterator var29 = ((IntList)entry.getValue()).iterator();

                        while (var29.hasNext()) {
                           int j = (Integer)var29.next();
                           j |= i;
                           Dynamic<?> dynamic = this.getBlock(j);
                           if ("minecraft:podzol".equals(ChunkPalettedStorageFix.getName(dynamic))) {
                              String string = ChunkPalettedStorageFix.getName(this.getBlock(relative(j, ChunkPalettedStorageFix.Direction.UP)));
                              if ("minecraft:snow".equals(string) || "minecraft:snow_layer".equals(string)) {
                                 this.setBlock(j, ChunkPalettedStorageFix.MappingConstants.SNOWY_PODZOL);
                              }
                           }
                        }
                        break;
                     case 25:
                        IntListIterator var28 = ((IntList)entry.getValue()).iterator();

                        while (var28.hasNext()) {
                           int j = (Integer)var28.next();
                           j |= i;
                           Dynamic<?> dynamic = this.removeBlockEntity(j);
                           if (dynamic != null) {
                              String string = Boolean.toString(dynamic.get("powered").asBoolean(false))
                                 + (byte)Math.min(Math.max(dynamic.get("note").asInt(0), 0), 24);
                              this.setBlock(
                                 j,
                                 ChunkPalettedStorageFix.MappingConstants.NOTE_BLOCK_MAP
                                    .getOrDefault(string, ChunkPalettedStorageFix.MappingConstants.NOTE_BLOCK_MAP.get("false0"))
                              );
                           }
                        }
                        break;
                     case 26:
                        IntListIterator var27 = ((IntList)entry.getValue()).iterator();

                        while (var27.hasNext()) {
                           int j = (Integer)var27.next();
                           j |= i;
                           Dynamic<?> dynamic = this.getBlockEntity(j);
                           Dynamic<?> dynamic2 = this.getBlock(j);
                           if (dynamic != null) {
                              int k = dynamic.get("color").asInt(0);
                              if (k != 14 && k >= 0 && k < 16) {
                                 String string2 = ChunkPalettedStorageFix.getProperty(dynamic2, "facing")
                                    + ChunkPalettedStorageFix.getProperty(dynamic2, "occupied")
                                    + ChunkPalettedStorageFix.getProperty(dynamic2, "part")
                                    + k;
                                 if (ChunkPalettedStorageFix.MappingConstants.BED_BLOCK_MAP.containsKey(string2)) {
                                    this.setBlock(j, ChunkPalettedStorageFix.MappingConstants.BED_BLOCK_MAP.get(string2));
                                 }
                              }
                           }
                        }
                        break;
                     case 64:
                     case 71:
                     case 193:
                     case 194:
                     case 195:
                     case 196:
                     case 197:
                        IntListIterator var26 = ((IntList)entry.getValue()).iterator();

                        while (var26.hasNext()) {
                           int j = (Integer)var26.next();
                           j |= i;
                           Dynamic<?> dynamic = this.getBlock(j);
                           if (ChunkPalettedStorageFix.getName(dynamic).endsWith("_door")) {
                              Dynamic<?> dynamic2 = this.getBlock(j);
                              if ("lower".equals(ChunkPalettedStorageFix.getProperty(dynamic2, "half"))) {
                                 int k = relative(j, ChunkPalettedStorageFix.Direction.UP);
                                 Dynamic<?> dynamic3 = this.getBlock(k);
                                 String string4 = ChunkPalettedStorageFix.getName(dynamic2);
                                 if (string4.equals(ChunkPalettedStorageFix.getName(dynamic3))) {
                                    String string5 = ChunkPalettedStorageFix.getProperty(dynamic2, "facing");
                                    String string6 = ChunkPalettedStorageFix.getProperty(dynamic2, "open");
                                    String string7 = bl ? "left" : ChunkPalettedStorageFix.getProperty(dynamic3, "hinge");
                                    String string8 = bl ? "false" : ChunkPalettedStorageFix.getProperty(dynamic3, "powered");
                                    this.setBlock(
                                       j, ChunkPalettedStorageFix.MappingConstants.DOOR_MAP.get(string4 + string5 + "lower" + string7 + string6 + string8)
                                    );
                                    this.setBlock(
                                       k, ChunkPalettedStorageFix.MappingConstants.DOOR_MAP.get(string4 + string5 + "upper" + string7 + string6 + string8)
                                    );
                                 }
                              }
                           }
                        }
                        break;
                     case 86:
                        IntListIterator var25 = ((IntList)entry.getValue()).iterator();

                        while (var25.hasNext()) {
                           int j = (Integer)var25.next();
                           j |= i;
                           Dynamic<?> dynamic = this.getBlock(j);
                           if ("minecraft:carved_pumpkin".equals(ChunkPalettedStorageFix.getName(dynamic))) {
                              String string = ChunkPalettedStorageFix.getName(this.getBlock(relative(j, ChunkPalettedStorageFix.Direction.DOWN)));
                              if ("minecraft:grass_block".equals(string) || "minecraft:dirt".equals(string)) {
                                 this.setBlock(j, ChunkPalettedStorageFix.MappingConstants.PUMPKIN);
                              }
                           }
                        }
                        break;
                     case 110:
                        IntListIterator var24 = ((IntList)entry.getValue()).iterator();

                        while (var24.hasNext()) {
                           int j = (Integer)var24.next();
                           j |= i;
                           Dynamic<?> dynamic = this.getBlock(j);
                           if ("minecraft:mycelium".equals(ChunkPalettedStorageFix.getName(dynamic))) {
                              String string = ChunkPalettedStorageFix.getName(this.getBlock(relative(j, ChunkPalettedStorageFix.Direction.UP)));
                              if ("minecraft:snow".equals(string) || "minecraft:snow_layer".equals(string)) {
                                 this.setBlock(j, ChunkPalettedStorageFix.MappingConstants.SNOWY_MYCELIUM);
                              }
                           }
                        }
                        break;
                     case 140:
                        IntListIterator var23 = ((IntList)entry.getValue()).iterator();

                        while (var23.hasNext()) {
                           int j = (Integer)var23.next();
                           j |= i;
                           Dynamic<?> dynamic = this.removeBlockEntity(j);
                           if (dynamic != null) {
                              String string = dynamic.get("Item").asString("") + dynamic.get("Data").asInt(0);
                              this.setBlock(
                                 j,
                                 ChunkPalettedStorageFix.MappingConstants.FLOWER_POT_MAP
                                    .getOrDefault(string, ChunkPalettedStorageFix.MappingConstants.FLOWER_POT_MAP.get("minecraft:air0"))
                              );
                           }
                        }
                        break;
                     case 144:
                        IntListIterator var22 = ((IntList)entry.getValue()).iterator();

                        while (var22.hasNext()) {
                           int j = (Integer)var22.next();
                           j |= i;
                           Dynamic<?> dynamic = this.getBlockEntity(j);
                           if (dynamic != null) {
                              String string = String.valueOf(dynamic.get("SkullType").asInt(0));
                              String string3 = ChunkPalettedStorageFix.getProperty(this.getBlock(j), "facing");
                              String string2;
                              if (!"up".equals(string3) && !"down".equals(string3)) {
                                 string2 = string + string3;
                              } else {
                                 string2 = string + dynamic.get("Rot").asInt(0);
                              }

                              dynamic.remove("SkullType");
                              dynamic.remove("facing");
                              dynamic.remove("Rot");
                              this.setBlock(
                                 j,
                                 ChunkPalettedStorageFix.MappingConstants.SKULL_MAP
                                    .getOrDefault(string2, ChunkPalettedStorageFix.MappingConstants.SKULL_MAP.get("0north"))
                              );
                           }
                        }
                        break;
                     case 175:
                        IntListIterator var21 = ((IntList)entry.getValue()).iterator();

                        while (var21.hasNext()) {
                           int j = (Integer)var21.next();
                           j |= i;
                           Dynamic<?> dynamic = this.getBlock(j);
                           if ("upper".equals(ChunkPalettedStorageFix.getProperty(dynamic, "half"))) {
                              Dynamic<?> dynamic2 = this.getBlock(relative(j, ChunkPalettedStorageFix.Direction.DOWN));
                              String string3 = ChunkPalettedStorageFix.getName(dynamic2);
                              switch (string3) {
                                 case "minecraft:sunflower":
                                    this.setBlock(j, ChunkPalettedStorageFix.MappingConstants.UPPER_SUNFLOWER);
                                    break;
                                 case "minecraft:lilac":
                                    this.setBlock(j, ChunkPalettedStorageFix.MappingConstants.UPPER_LILAC);
                                    break;
                                 case "minecraft:tall_grass":
                                    this.setBlock(j, ChunkPalettedStorageFix.MappingConstants.UPPER_TALL_GRASS);
                                    break;
                                 case "minecraft:large_fern":
                                    this.setBlock(j, ChunkPalettedStorageFix.MappingConstants.UPPER_LARGE_FERN);
                                    break;
                                 case "minecraft:rose_bush":
                                    this.setBlock(j, ChunkPalettedStorageFix.MappingConstants.UPPER_ROSE_BUSH);
                                    break;
                                 case "minecraft:peony":
                                    this.setBlock(j, ChunkPalettedStorageFix.MappingConstants.UPPER_PEONY);
                              }
                           }
                        }
                        break;
                     case 176:
                     case 177:
                        IntListIterator var10 = ((IntList)entry.getValue()).iterator();

                        while (var10.hasNext()) {
                           int j = (Integer)var10.next();
                           j |= i;
                           Dynamic<?> dynamic = this.getBlockEntity(j);
                           Dynamic<?> dynamic2 = this.getBlock(j);
                           if (dynamic != null) {
                              int k = dynamic.get("Base").asInt(0);
                              if (k != 15 && k >= 0 && k < 16) {
                                 String string2 = ChunkPalettedStorageFix.getProperty(dynamic2, entry.getIntKey() == 176 ? "rotation" : "facing") + "_" + k;
                                 if (ChunkPalettedStorageFix.MappingConstants.BANNER_BLOCK_MAP.containsKey(string2)) {
                                    this.setBlock(j, ChunkPalettedStorageFix.MappingConstants.BANNER_BLOCK_MAP.get(string2));
                                 }
                              }
                           }
                        }
                  }
               }
            }
         }
      }

      @Nullable
      private Dynamic<?> getBlockEntity(int index) {
         return (Dynamic<?>)this.blockEntities.get(index);
      }

      @Nullable
      private Dynamic<?> removeBlockEntity(int index) {
         return (Dynamic<?>)this.blockEntities.remove(index);
      }

      public static int relative(int data, ChunkPalettedStorageFix.Direction direction) {
         return switch (direction.getAxis()) {
            case X -> {
               int i = (data & 15) + direction.getAxisDirection().getStep();
               yield i >= 0 && i <= 15 ? data & -16 | i : -1;
            }
            case Y -> {
               int i = (data >> 8) + direction.getAxisDirection().getStep();
               yield i >= 0 && i <= 255 ? data & 0xFF | i << 8 : -1;
            }
            case Z -> {
               int i = (data >> 4 & 15) + direction.getAxisDirection().getStep();
               yield i >= 0 && i <= 15 ? data & -241 | i << 4 : -1;
            }
         };
      }

      private void setBlock(int index, Dynamic<?> block) {
         if (index >= 0 && index <= 65535) {
            ChunkPalettedStorageFix.Section section = this.getSection(index);
            if (section != null) {
               section.setBlock(index & 4095, block);
            }
         }
      }

      @Nullable
      private ChunkPalettedStorageFix.Section getSection(int index) {
         int i = index >> 12;
         return i < this.sections.length ? this.sections[i] : null;
      }

      public Dynamic<?> getBlock(int index) {
         if (index >= 0 && index <= 65535) {
            ChunkPalettedStorageFix.Section section = this.getSection(index);
            return section == null ? ChunkPalettedStorageFix.MappingConstants.AIR : section.getBlock(index & 4095);
         } else {
            return ChunkPalettedStorageFix.MappingConstants.AIR;
         }
      }

      public Dynamic<?> write() {
         Dynamic<?> dynamic = this.level;
         if (this.blockEntities.isEmpty()) {
            dynamic = dynamic.remove("TileEntities");
         } else {
            dynamic = dynamic.set("TileEntities", dynamic.createList(this.blockEntities.values().stream()));
         }

         Dynamic<?> dynamic2 = dynamic.emptyMap();
         List<Dynamic<?>> list = Lists.newArrayList();

         for (ChunkPalettedStorageFix.Section section : this.sections) {
            if (section != null) {
               list.add(section.write());
               dynamic2 = dynamic2.set(String.valueOf(section.y), dynamic2.createIntList(Arrays.stream(section.update.toIntArray())));
            }
         }

         Dynamic<?> dynamic3 = dynamic.emptyMap();
         dynamic3 = dynamic3.set("Sides", dynamic3.createByte((byte)this.sides));
         dynamic3 = dynamic3.set("Indices", dynamic2);
         return dynamic.set("UpgradeData", dynamic3).set("Sections", dynamic3.createList(list.stream()));
      }
   }
}
