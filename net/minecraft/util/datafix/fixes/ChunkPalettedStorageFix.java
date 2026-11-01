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
import javax.annotation.Nullable;
import net.minecraft.util.CrudeIncrementalIntIdentityHashBiMap;
import net.minecraft.util.datafix.PackedBitStorage;
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
   static final BitSet VIRTUAL = new BitSet(256);
   static final BitSet FIX = new BitSet(256);
   static final Dynamic<?> PUMPKIN = BlockStateData.parse("{Name:'minecraft:pumpkin'}");
   static final Dynamic<?> SNOWY_PODZOL = BlockStateData.parse("{Name:'minecraft:podzol',Properties:{snowy:'true'}}");
   static final Dynamic<?> SNOWY_GRASS = BlockStateData.parse("{Name:'minecraft:grass_block',Properties:{snowy:'true'}}");
   static final Dynamic<?> SNOWY_MYCELIUM = BlockStateData.parse("{Name:'minecraft:mycelium',Properties:{snowy:'true'}}");
   static final Dynamic<?> UPPER_SUNFLOWER = BlockStateData.parse("{Name:'minecraft:sunflower',Properties:{half:'upper'}}");
   static final Dynamic<?> UPPER_LILAC = BlockStateData.parse("{Name:'minecraft:lilac',Properties:{half:'upper'}}");
   static final Dynamic<?> UPPER_TALL_GRASS = BlockStateData.parse("{Name:'minecraft:tall_grass',Properties:{half:'upper'}}");
   static final Dynamic<?> UPPER_LARGE_FERN = BlockStateData.parse("{Name:'minecraft:large_fern',Properties:{half:'upper'}}");
   static final Dynamic<?> UPPER_ROSE_BUSH = BlockStateData.parse("{Name:'minecraft:rose_bush',Properties:{half:'upper'}}");
   static final Dynamic<?> UPPER_PEONY = BlockStateData.parse("{Name:'minecraft:peony',Properties:{half:'upper'}}");
   static final Map<String, Dynamic<?>> FLOWER_POT_MAP = (Map<String, Dynamic<?>>)DataFixUtils.make(Maps.newHashMap(), p_15111_ -> {
      p_15111_.put("minecraft:air0", BlockStateData.parse("{Name:'minecraft:flower_pot'}"));
      p_15111_.put("minecraft:red_flower0", BlockStateData.parse("{Name:'minecraft:potted_poppy'}"));
      p_15111_.put("minecraft:red_flower1", BlockStateData.parse("{Name:'minecraft:potted_blue_orchid'}"));
      p_15111_.put("minecraft:red_flower2", BlockStateData.parse("{Name:'minecraft:potted_allium'}"));
      p_15111_.put("minecraft:red_flower3", BlockStateData.parse("{Name:'minecraft:potted_azure_bluet'}"));
      p_15111_.put("minecraft:red_flower4", BlockStateData.parse("{Name:'minecraft:potted_red_tulip'}"));
      p_15111_.put("minecraft:red_flower5", BlockStateData.parse("{Name:'minecraft:potted_orange_tulip'}"));
      p_15111_.put("minecraft:red_flower6", BlockStateData.parse("{Name:'minecraft:potted_white_tulip'}"));
      p_15111_.put("minecraft:red_flower7", BlockStateData.parse("{Name:'minecraft:potted_pink_tulip'}"));
      p_15111_.put("minecraft:red_flower8", BlockStateData.parse("{Name:'minecraft:potted_oxeye_daisy'}"));
      p_15111_.put("minecraft:yellow_flower0", BlockStateData.parse("{Name:'minecraft:potted_dandelion'}"));
      p_15111_.put("minecraft:sapling0", BlockStateData.parse("{Name:'minecraft:potted_oak_sapling'}"));
      p_15111_.put("minecraft:sapling1", BlockStateData.parse("{Name:'minecraft:potted_spruce_sapling'}"));
      p_15111_.put("minecraft:sapling2", BlockStateData.parse("{Name:'minecraft:potted_birch_sapling'}"));
      p_15111_.put("minecraft:sapling3", BlockStateData.parse("{Name:'minecraft:potted_jungle_sapling'}"));
      p_15111_.put("minecraft:sapling4", BlockStateData.parse("{Name:'minecraft:potted_acacia_sapling'}"));
      p_15111_.put("minecraft:sapling5", BlockStateData.parse("{Name:'minecraft:potted_dark_oak_sapling'}"));
      p_15111_.put("minecraft:red_mushroom0", BlockStateData.parse("{Name:'minecraft:potted_red_mushroom'}"));
      p_15111_.put("minecraft:brown_mushroom0", BlockStateData.parse("{Name:'minecraft:potted_brown_mushroom'}"));
      p_15111_.put("minecraft:deadbush0", BlockStateData.parse("{Name:'minecraft:potted_dead_bush'}"));
      p_15111_.put("minecraft:tallgrass2", BlockStateData.parse("{Name:'minecraft:potted_fern'}"));
      p_15111_.put("minecraft:cactus0", BlockStateData.getTag(2240));
   });
   static final Map<String, Dynamic<?>> SKULL_MAP = (Map<String, Dynamic<?>>)DataFixUtils.make(Maps.newHashMap(), p_15108_ -> {
      mapSkull(p_15108_, 0, "skeleton", "skull");
      mapSkull(p_15108_, 1, "wither_skeleton", "skull");
      mapSkull(p_15108_, 2, "zombie", "head");
      mapSkull(p_15108_, 3, "player", "head");
      mapSkull(p_15108_, 4, "creeper", "head");
      mapSkull(p_15108_, 5, "dragon", "head");
   });
   static final Map<String, Dynamic<?>> DOOR_MAP = (Map<String, Dynamic<?>>)DataFixUtils.make(Maps.newHashMap(), p_15105_ -> {
      mapDoor(p_15105_, "oak_door", 1024);
      mapDoor(p_15105_, "iron_door", 1136);
      mapDoor(p_15105_, "spruce_door", 3088);
      mapDoor(p_15105_, "birch_door", 3104);
      mapDoor(p_15105_, "jungle_door", 3120);
      mapDoor(p_15105_, "acacia_door", 3136);
      mapDoor(p_15105_, "dark_oak_door", 3152);
   });
   static final Map<String, Dynamic<?>> NOTE_BLOCK_MAP = (Map<String, Dynamic<?>>)DataFixUtils.make(Maps.newHashMap(), p_15102_ -> {
      for (int $$1 = 0; $$1 < 26; $$1++) {
         p_15102_.put("true" + $$1, BlockStateData.parse("{Name:'minecraft:note_block',Properties:{powered:'true',note:'" + $$1 + "'}}"));
         p_15102_.put("false" + $$1, BlockStateData.parse("{Name:'minecraft:note_block',Properties:{powered:'false',note:'" + $$1 + "'}}"));
      }
   });
   private static final Int2ObjectMap<String> DYE_COLOR_MAP = (Int2ObjectMap<String>)DataFixUtils.make(new Int2ObjectOpenHashMap(), p_15070_ -> {
      p_15070_.put(0, "white");
      p_15070_.put(1, "orange");
      p_15070_.put(2, "magenta");
      p_15070_.put(3, "light_blue");
      p_15070_.put(4, "yellow");
      p_15070_.put(5, "lime");
      p_15070_.put(6, "pink");
      p_15070_.put(7, "gray");
      p_15070_.put(8, "light_gray");
      p_15070_.put(9, "cyan");
      p_15070_.put(10, "purple");
      p_15070_.put(11, "blue");
      p_15070_.put(12, "brown");
      p_15070_.put(13, "green");
      p_15070_.put(14, "red");
      p_15070_.put(15, "black");
   });
   static final Map<String, Dynamic<?>> BED_BLOCK_MAP = (Map<String, Dynamic<?>>)DataFixUtils.make(Maps.newHashMap(), p_15095_ -> {
      ObjectIterator var1 = DYE_COLOR_MAP.int2ObjectEntrySet().iterator();

      while (var1.hasNext()) {
         Entry<String> $$1 = (Entry<String>)var1.next();
         if (!Objects.equals($$1.getValue(), "red")) {
            addBeds(p_15095_, $$1.getIntKey(), (String)$$1.getValue());
         }
      }
   });
   static final Map<String, Dynamic<?>> BANNER_BLOCK_MAP = (Map<String, Dynamic<?>>)DataFixUtils.make(Maps.newHashMap(), p_15072_ -> {
      ObjectIterator var1 = DYE_COLOR_MAP.int2ObjectEntrySet().iterator();

      while (var1.hasNext()) {
         Entry<String> $$1 = (Entry<String>)var1.next();
         if (!Objects.equals($$1.getValue(), "white")) {
            addBanners(p_15072_, 15 - $$1.getIntKey(), (String)$$1.getValue());
         }
      }
   });
   static final Dynamic<?> AIR = BlockStateData.getTag(0);
   private static final int SIZE = 4096;

   public ChunkPalettedStorageFix(Schema p_15058_, boolean p_15059_) {
      super(p_15058_, p_15059_);
   }

   private static void mapSkull(Map<String, Dynamic<?>> p_15078_, int p_15079_, String p_15080_, String p_15081_) {
      p_15078_.put(p_15079_ + "north", BlockStateData.parse("{Name:'minecraft:" + p_15080_ + "_wall_" + p_15081_ + "',Properties:{facing:'north'}}"));
      p_15078_.put(p_15079_ + "east", BlockStateData.parse("{Name:'minecraft:" + p_15080_ + "_wall_" + p_15081_ + "',Properties:{facing:'east'}}"));
      p_15078_.put(p_15079_ + "south", BlockStateData.parse("{Name:'minecraft:" + p_15080_ + "_wall_" + p_15081_ + "',Properties:{facing:'south'}}"));
      p_15078_.put(p_15079_ + "west", BlockStateData.parse("{Name:'minecraft:" + p_15080_ + "_wall_" + p_15081_ + "',Properties:{facing:'west'}}"));

      for (int $$4 = 0; $$4 < 16; $$4++) {
         p_15078_.put("" + p_15079_ + $$4, BlockStateData.parse("{Name:'minecraft:" + p_15080_ + "_" + p_15081_ + "',Properties:{rotation:'" + $$4 + "'}}"));
      }
   }

   private static void mapDoor(Map<String, Dynamic<?>> p_15083_, String p_15084_, int p_15085_) {
      p_15083_.put(
         "minecraft:" + p_15084_ + "eastlowerleftfalsefalse",
         BlockStateData.parse("{Name:'minecraft:" + p_15084_ + "',Properties:{facing:'east',half:'lower',hinge:'left',open:'false',powered:'false'}}")
      );
      p_15083_.put(
         "minecraft:" + p_15084_ + "eastlowerleftfalsetrue",
         BlockStateData.parse("{Name:'minecraft:" + p_15084_ + "',Properties:{facing:'east',half:'lower',hinge:'left',open:'false',powered:'true'}}")
      );
      p_15083_.put(
         "minecraft:" + p_15084_ + "eastlowerlefttruefalse",
         BlockStateData.parse("{Name:'minecraft:" + p_15084_ + "',Properties:{facing:'east',half:'lower',hinge:'left',open:'true',powered:'false'}}")
      );
      p_15083_.put(
         "minecraft:" + p_15084_ + "eastlowerlefttruetrue",
         BlockStateData.parse("{Name:'minecraft:" + p_15084_ + "',Properties:{facing:'east',half:'lower',hinge:'left',open:'true',powered:'true'}}")
      );
      p_15083_.put("minecraft:" + p_15084_ + "eastlowerrightfalsefalse", BlockStateData.getTag(p_15085_));
      p_15083_.put(
         "minecraft:" + p_15084_ + "eastlowerrightfalsetrue",
         BlockStateData.parse("{Name:'minecraft:" + p_15084_ + "',Properties:{facing:'east',half:'lower',hinge:'right',open:'false',powered:'true'}}")
      );
      p_15083_.put("minecraft:" + p_15084_ + "eastlowerrighttruefalse", BlockStateData.getTag(p_15085_ + 4));
      p_15083_.put(
         "minecraft:" + p_15084_ + "eastlowerrighttruetrue",
         BlockStateData.parse("{Name:'minecraft:" + p_15084_ + "',Properties:{facing:'east',half:'lower',hinge:'right',open:'true',powered:'true'}}")
      );
      p_15083_.put("minecraft:" + p_15084_ + "eastupperleftfalsefalse", BlockStateData.getTag(p_15085_ + 8));
      p_15083_.put("minecraft:" + p_15084_ + "eastupperleftfalsetrue", BlockStateData.getTag(p_15085_ + 10));
      p_15083_.put(
         "minecraft:" + p_15084_ + "eastupperlefttruefalse",
         BlockStateData.parse("{Name:'minecraft:" + p_15084_ + "',Properties:{facing:'east',half:'upper',hinge:'left',open:'true',powered:'false'}}")
      );
      p_15083_.put(
         "minecraft:" + p_15084_ + "eastupperlefttruetrue",
         BlockStateData.parse("{Name:'minecraft:" + p_15084_ + "',Properties:{facing:'east',half:'upper',hinge:'left',open:'true',powered:'true'}}")
      );
      p_15083_.put("minecraft:" + p_15084_ + "eastupperrightfalsefalse", BlockStateData.getTag(p_15085_ + 9));
      p_15083_.put("minecraft:" + p_15084_ + "eastupperrightfalsetrue", BlockStateData.getTag(p_15085_ + 11));
      p_15083_.put(
         "minecraft:" + p_15084_ + "eastupperrighttruefalse",
         BlockStateData.parse("{Name:'minecraft:" + p_15084_ + "',Properties:{facing:'east',half:'upper',hinge:'right',open:'true',powered:'false'}}")
      );
      p_15083_.put(
         "minecraft:" + p_15084_ + "eastupperrighttruetrue",
         BlockStateData.parse("{Name:'minecraft:" + p_15084_ + "',Properties:{facing:'east',half:'upper',hinge:'right',open:'true',powered:'true'}}")
      );
      p_15083_.put(
         "minecraft:" + p_15084_ + "northlowerleftfalsefalse",
         BlockStateData.parse("{Name:'minecraft:" + p_15084_ + "',Properties:{facing:'north',half:'lower',hinge:'left',open:'false',powered:'false'}}")
      );
      p_15083_.put(
         "minecraft:" + p_15084_ + "northlowerleftfalsetrue",
         BlockStateData.parse("{Name:'minecraft:" + p_15084_ + "',Properties:{facing:'north',half:'lower',hinge:'left',open:'false',powered:'true'}}")
      );
      p_15083_.put(
         "minecraft:" + p_15084_ + "northlowerlefttruefalse",
         BlockStateData.parse("{Name:'minecraft:" + p_15084_ + "',Properties:{facing:'north',half:'lower',hinge:'left',open:'true',powered:'false'}}")
      );
      p_15083_.put(
         "minecraft:" + p_15084_ + "northlowerlefttruetrue",
         BlockStateData.parse("{Name:'minecraft:" + p_15084_ + "',Properties:{facing:'north',half:'lower',hinge:'left',open:'true',powered:'true'}}")
      );
      p_15083_.put("minecraft:" + p_15084_ + "northlowerrightfalsefalse", BlockStateData.getTag(p_15085_ + 3));
      p_15083_.put(
         "minecraft:" + p_15084_ + "northlowerrightfalsetrue",
         BlockStateData.parse("{Name:'minecraft:" + p_15084_ + "',Properties:{facing:'north',half:'lower',hinge:'right',open:'false',powered:'true'}}")
      );
      p_15083_.put("minecraft:" + p_15084_ + "northlowerrighttruefalse", BlockStateData.getTag(p_15085_ + 7));
      p_15083_.put(
         "minecraft:" + p_15084_ + "northlowerrighttruetrue",
         BlockStateData.parse("{Name:'minecraft:" + p_15084_ + "',Properties:{facing:'north',half:'lower',hinge:'right',open:'true',powered:'true'}}")
      );
      p_15083_.put(
         "minecraft:" + p_15084_ + "northupperleftfalsefalse",
         BlockStateData.parse("{Name:'minecraft:" + p_15084_ + "',Properties:{facing:'north',half:'upper',hinge:'left',open:'false',powered:'false'}}")
      );
      p_15083_.put(
         "minecraft:" + p_15084_ + "northupperleftfalsetrue",
         BlockStateData.parse("{Name:'minecraft:" + p_15084_ + "',Properties:{facing:'north',half:'upper',hinge:'left',open:'false',powered:'true'}}")
      );
      p_15083_.put(
         "minecraft:" + p_15084_ + "northupperlefttruefalse",
         BlockStateData.parse("{Name:'minecraft:" + p_15084_ + "',Properties:{facing:'north',half:'upper',hinge:'left',open:'true',powered:'false'}}")
      );
      p_15083_.put(
         "minecraft:" + p_15084_ + "northupperlefttruetrue",
         BlockStateData.parse("{Name:'minecraft:" + p_15084_ + "',Properties:{facing:'north',half:'upper',hinge:'left',open:'true',powered:'true'}}")
      );
      p_15083_.put(
         "minecraft:" + p_15084_ + "northupperrightfalsefalse",
         BlockStateData.parse("{Name:'minecraft:" + p_15084_ + "',Properties:{facing:'north',half:'upper',hinge:'right',open:'false',powered:'false'}}")
      );
      p_15083_.put(
         "minecraft:" + p_15084_ + "northupperrightfalsetrue",
         BlockStateData.parse("{Name:'minecraft:" + p_15084_ + "',Properties:{facing:'north',half:'upper',hinge:'right',open:'false',powered:'true'}}")
      );
      p_15083_.put(
         "minecraft:" + p_15084_ + "northupperrighttruefalse",
         BlockStateData.parse("{Name:'minecraft:" + p_15084_ + "',Properties:{facing:'north',half:'upper',hinge:'right',open:'true',powered:'false'}}")
      );
      p_15083_.put(
         "minecraft:" + p_15084_ + "northupperrighttruetrue",
         BlockStateData.parse("{Name:'minecraft:" + p_15084_ + "',Properties:{facing:'north',half:'upper',hinge:'right',open:'true',powered:'true'}}")
      );
      p_15083_.put(
         "minecraft:" + p_15084_ + "southlowerleftfalsefalse",
         BlockStateData.parse("{Name:'minecraft:" + p_15084_ + "',Properties:{facing:'south',half:'lower',hinge:'left',open:'false',powered:'false'}}")
      );
      p_15083_.put(
         "minecraft:" + p_15084_ + "southlowerleftfalsetrue",
         BlockStateData.parse("{Name:'minecraft:" + p_15084_ + "',Properties:{facing:'south',half:'lower',hinge:'left',open:'false',powered:'true'}}")
      );
      p_15083_.put(
         "minecraft:" + p_15084_ + "southlowerlefttruefalse",
         BlockStateData.parse("{Name:'minecraft:" + p_15084_ + "',Properties:{facing:'south',half:'lower',hinge:'left',open:'true',powered:'false'}}")
      );
      p_15083_.put(
         "minecraft:" + p_15084_ + "southlowerlefttruetrue",
         BlockStateData.parse("{Name:'minecraft:" + p_15084_ + "',Properties:{facing:'south',half:'lower',hinge:'left',open:'true',powered:'true'}}")
      );
      p_15083_.put("minecraft:" + p_15084_ + "southlowerrightfalsefalse", BlockStateData.getTag(p_15085_ + 1));
      p_15083_.put(
         "minecraft:" + p_15084_ + "southlowerrightfalsetrue",
         BlockStateData.parse("{Name:'minecraft:" + p_15084_ + "',Properties:{facing:'south',half:'lower',hinge:'right',open:'false',powered:'true'}}")
      );
      p_15083_.put("minecraft:" + p_15084_ + "southlowerrighttruefalse", BlockStateData.getTag(p_15085_ + 5));
      p_15083_.put(
         "minecraft:" + p_15084_ + "southlowerrighttruetrue",
         BlockStateData.parse("{Name:'minecraft:" + p_15084_ + "',Properties:{facing:'south',half:'lower',hinge:'right',open:'true',powered:'true'}}")
      );
      p_15083_.put(
         "minecraft:" + p_15084_ + "southupperleftfalsefalse",
         BlockStateData.parse("{Name:'minecraft:" + p_15084_ + "',Properties:{facing:'south',half:'upper',hinge:'left',open:'false',powered:'false'}}")
      );
      p_15083_.put(
         "minecraft:" + p_15084_ + "southupperleftfalsetrue",
         BlockStateData.parse("{Name:'minecraft:" + p_15084_ + "',Properties:{facing:'south',half:'upper',hinge:'left',open:'false',powered:'true'}}")
      );
      p_15083_.put(
         "minecraft:" + p_15084_ + "southupperlefttruefalse",
         BlockStateData.parse("{Name:'minecraft:" + p_15084_ + "',Properties:{facing:'south',half:'upper',hinge:'left',open:'true',powered:'false'}}")
      );
      p_15083_.put(
         "minecraft:" + p_15084_ + "southupperlefttruetrue",
         BlockStateData.parse("{Name:'minecraft:" + p_15084_ + "',Properties:{facing:'south',half:'upper',hinge:'left',open:'true',powered:'true'}}")
      );
      p_15083_.put(
         "minecraft:" + p_15084_ + "southupperrightfalsefalse",
         BlockStateData.parse("{Name:'minecraft:" + p_15084_ + "',Properties:{facing:'south',half:'upper',hinge:'right',open:'false',powered:'false'}}")
      );
      p_15083_.put(
         "minecraft:" + p_15084_ + "southupperrightfalsetrue",
         BlockStateData.parse("{Name:'minecraft:" + p_15084_ + "',Properties:{facing:'south',half:'upper',hinge:'right',open:'false',powered:'true'}}")
      );
      p_15083_.put(
         "minecraft:" + p_15084_ + "southupperrighttruefalse",
         BlockStateData.parse("{Name:'minecraft:" + p_15084_ + "',Properties:{facing:'south',half:'upper',hinge:'right',open:'true',powered:'false'}}")
      );
      p_15083_.put(
         "minecraft:" + p_15084_ + "southupperrighttruetrue",
         BlockStateData.parse("{Name:'minecraft:" + p_15084_ + "',Properties:{facing:'south',half:'upper',hinge:'right',open:'true',powered:'true'}}")
      );
      p_15083_.put(
         "minecraft:" + p_15084_ + "westlowerleftfalsefalse",
         BlockStateData.parse("{Name:'minecraft:" + p_15084_ + "',Properties:{facing:'west',half:'lower',hinge:'left',open:'false',powered:'false'}}")
      );
      p_15083_.put(
         "minecraft:" + p_15084_ + "westlowerleftfalsetrue",
         BlockStateData.parse("{Name:'minecraft:" + p_15084_ + "',Properties:{facing:'west',half:'lower',hinge:'left',open:'false',powered:'true'}}")
      );
      p_15083_.put(
         "minecraft:" + p_15084_ + "westlowerlefttruefalse",
         BlockStateData.parse("{Name:'minecraft:" + p_15084_ + "',Properties:{facing:'west',half:'lower',hinge:'left',open:'true',powered:'false'}}")
      );
      p_15083_.put(
         "minecraft:" + p_15084_ + "westlowerlefttruetrue",
         BlockStateData.parse("{Name:'minecraft:" + p_15084_ + "',Properties:{facing:'west',half:'lower',hinge:'left',open:'true',powered:'true'}}")
      );
      p_15083_.put("minecraft:" + p_15084_ + "westlowerrightfalsefalse", BlockStateData.getTag(p_15085_ + 2));
      p_15083_.put(
         "minecraft:" + p_15084_ + "westlowerrightfalsetrue",
         BlockStateData.parse("{Name:'minecraft:" + p_15084_ + "',Properties:{facing:'west',half:'lower',hinge:'right',open:'false',powered:'true'}}")
      );
      p_15083_.put("minecraft:" + p_15084_ + "westlowerrighttruefalse", BlockStateData.getTag(p_15085_ + 6));
      p_15083_.put(
         "minecraft:" + p_15084_ + "westlowerrighttruetrue",
         BlockStateData.parse("{Name:'minecraft:" + p_15084_ + "',Properties:{facing:'west',half:'lower',hinge:'right',open:'true',powered:'true'}}")
      );
      p_15083_.put(
         "minecraft:" + p_15084_ + "westupperleftfalsefalse",
         BlockStateData.parse("{Name:'minecraft:" + p_15084_ + "',Properties:{facing:'west',half:'upper',hinge:'left',open:'false',powered:'false'}}")
      );
      p_15083_.put(
         "minecraft:" + p_15084_ + "westupperleftfalsetrue",
         BlockStateData.parse("{Name:'minecraft:" + p_15084_ + "',Properties:{facing:'west',half:'upper',hinge:'left',open:'false',powered:'true'}}")
      );
      p_15083_.put(
         "minecraft:" + p_15084_ + "westupperlefttruefalse",
         BlockStateData.parse("{Name:'minecraft:" + p_15084_ + "',Properties:{facing:'west',half:'upper',hinge:'left',open:'true',powered:'false'}}")
      );
      p_15083_.put(
         "minecraft:" + p_15084_ + "westupperlefttruetrue",
         BlockStateData.parse("{Name:'minecraft:" + p_15084_ + "',Properties:{facing:'west',half:'upper',hinge:'left',open:'true',powered:'true'}}")
      );
      p_15083_.put(
         "minecraft:" + p_15084_ + "westupperrightfalsefalse",
         BlockStateData.parse("{Name:'minecraft:" + p_15084_ + "',Properties:{facing:'west',half:'upper',hinge:'right',open:'false',powered:'false'}}")
      );
      p_15083_.put(
         "minecraft:" + p_15084_ + "westupperrightfalsetrue",
         BlockStateData.parse("{Name:'minecraft:" + p_15084_ + "',Properties:{facing:'west',half:'upper',hinge:'right',open:'false',powered:'true'}}")
      );
      p_15083_.put(
         "minecraft:" + p_15084_ + "westupperrighttruefalse",
         BlockStateData.parse("{Name:'minecraft:" + p_15084_ + "',Properties:{facing:'west',half:'upper',hinge:'right',open:'true',powered:'false'}}")
      );
      p_15083_.put(
         "minecraft:" + p_15084_ + "westupperrighttruetrue",
         BlockStateData.parse("{Name:'minecraft:" + p_15084_ + "',Properties:{facing:'west',half:'upper',hinge:'right',open:'true',powered:'true'}}")
      );
   }

   private static void addBeds(Map<String, Dynamic<?>> p_15074_, int p_15075_, String p_15076_) {
      p_15074_.put(
         "southfalsefoot" + p_15075_, BlockStateData.parse("{Name:'minecraft:" + p_15076_ + "_bed',Properties:{facing:'south',occupied:'false',part:'foot'}}")
      );
      p_15074_.put(
         "westfalsefoot" + p_15075_, BlockStateData.parse("{Name:'minecraft:" + p_15076_ + "_bed',Properties:{facing:'west',occupied:'false',part:'foot'}}")
      );
      p_15074_.put(
         "northfalsefoot" + p_15075_, BlockStateData.parse("{Name:'minecraft:" + p_15076_ + "_bed',Properties:{facing:'north',occupied:'false',part:'foot'}}")
      );
      p_15074_.put(
         "eastfalsefoot" + p_15075_, BlockStateData.parse("{Name:'minecraft:" + p_15076_ + "_bed',Properties:{facing:'east',occupied:'false',part:'foot'}}")
      );
      p_15074_.put(
         "southfalsehead" + p_15075_, BlockStateData.parse("{Name:'minecraft:" + p_15076_ + "_bed',Properties:{facing:'south',occupied:'false',part:'head'}}")
      );
      p_15074_.put(
         "westfalsehead" + p_15075_, BlockStateData.parse("{Name:'minecraft:" + p_15076_ + "_bed',Properties:{facing:'west',occupied:'false',part:'head'}}")
      );
      p_15074_.put(
         "northfalsehead" + p_15075_, BlockStateData.parse("{Name:'minecraft:" + p_15076_ + "_bed',Properties:{facing:'north',occupied:'false',part:'head'}}")
      );
      p_15074_.put(
         "eastfalsehead" + p_15075_, BlockStateData.parse("{Name:'minecraft:" + p_15076_ + "_bed',Properties:{facing:'east',occupied:'false',part:'head'}}")
      );
      p_15074_.put(
         "southtruehead" + p_15075_, BlockStateData.parse("{Name:'minecraft:" + p_15076_ + "_bed',Properties:{facing:'south',occupied:'true',part:'head'}}")
      );
      p_15074_.put(
         "westtruehead" + p_15075_, BlockStateData.parse("{Name:'minecraft:" + p_15076_ + "_bed',Properties:{facing:'west',occupied:'true',part:'head'}}")
      );
      p_15074_.put(
         "northtruehead" + p_15075_, BlockStateData.parse("{Name:'minecraft:" + p_15076_ + "_bed',Properties:{facing:'north',occupied:'true',part:'head'}}")
      );
      p_15074_.put(
         "easttruehead" + p_15075_, BlockStateData.parse("{Name:'minecraft:" + p_15076_ + "_bed',Properties:{facing:'east',occupied:'true',part:'head'}}")
      );
   }

   private static void addBanners(Map<String, Dynamic<?>> p_15097_, int p_15098_, String p_15099_) {
      for (int $$3 = 0; $$3 < 16; $$3++) {
         p_15097_.put($$3 + "_" + p_15098_, BlockStateData.parse("{Name:'minecraft:" + p_15099_ + "_banner',Properties:{rotation:'" + $$3 + "'}}"));
      }

      p_15097_.put("north_" + p_15098_, BlockStateData.parse("{Name:'minecraft:" + p_15099_ + "_wall_banner',Properties:{facing:'north'}}"));
      p_15097_.put("south_" + p_15098_, BlockStateData.parse("{Name:'minecraft:" + p_15099_ + "_wall_banner',Properties:{facing:'south'}}"));
      p_15097_.put("west_" + p_15098_, BlockStateData.parse("{Name:'minecraft:" + p_15099_ + "_wall_banner',Properties:{facing:'west'}}"));
      p_15097_.put("east_" + p_15098_, BlockStateData.parse("{Name:'minecraft:" + p_15099_ + "_wall_banner',Properties:{facing:'east'}}"));
   }

   public static String getName(Dynamic<?> p_15065_) {
      return p_15065_.get("Name").asString("");
   }

   public static String getProperty(Dynamic<?> p_15067_, String p_15068_) {
      return p_15067_.get("Properties").get(p_15068_).asString("");
   }

   public static int idFor(CrudeIncrementalIntIdentityHashBiMap<Dynamic<?>> p_15062_, Dynamic<?> p_15063_) {
      int $$2 = p_15062_.getId(p_15063_);
      if ($$2 == -1) {
         $$2 = p_15062_.add(p_15063_);
      }

      return $$2;
   }

   private Dynamic<?> fix(Dynamic<?> p_15093_) {
      Optional<? extends Dynamic<?>> $$1 = p_15093_.get("Level").result();
      return $$1.isPresent() && $$1.get().get("Sections").asStreamOpt().result().isPresent()
         ? p_15093_.set("Level", new ChunkPalettedStorageFix.UpgradeChunk((Dynamic<?>)$$1.get()).write())
         : p_15093_;
   }

   public TypeRewriteRule makeRule() {
      Type<?> $$0 = this.getInputSchema().getType(References.CHUNK);
      Type<?> $$1 = this.getOutputSchema().getType(References.CHUNK);
      return this.writeFixAndRead("ChunkPalettedStorageFix", $$0, $$1, this::fix);
   }

   public static int getSideMask(boolean p_15087_, boolean p_15088_, boolean p_15089_, boolean p_15090_) {
      int $$4 = 0;
      if (p_15089_) {
         if (p_15088_) {
            $$4 |= 2;
         } else if (p_15087_) {
            $$4 |= 128;
         } else {
            $$4 |= 1;
         }
      } else if (p_15090_) {
         if (p_15087_) {
            $$4 |= 32;
         } else if (p_15088_) {
            $$4 |= 8;
         } else {
            $$4 |= 16;
         }
      } else if (p_15088_) {
         $$4 |= 4;
      } else if (p_15087_) {
         $$4 |= 64;
      }

      return $$4;
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

   static class DataLayer {
      private static final int SIZE = 2048;
      private static final int NIBBLE_SIZE = 4;
      private final byte[] data;

      public DataLayer() {
         this.data = new byte[2048];
      }

      public DataLayer(byte[] p_15132_) {
         this.data = p_15132_;
         if (p_15132_.length != 2048) {
            throw new IllegalArgumentException("ChunkNibbleArrays should be 2048 bytes not: " + p_15132_.length);
         }
      }

      public int get(int p_15136_, int p_15137_, int p_15138_) {
         int $$3 = this.getPosition(p_15137_ << 8 | p_15138_ << 4 | p_15136_);
         return this.isFirst(p_15137_ << 8 | p_15138_ << 4 | p_15136_) ? this.data[$$3] & 15 : this.data[$$3] >> 4 & 15;
      }

      private boolean isFirst(int p_15134_) {
         return (p_15134_ & 1) == 0;
      }

      private int getPosition(int p_15140_) {
         return p_15140_ >> 1;
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

      private Direction(ChunkPalettedStorageFix.Direction.AxisDirection p_15154_, ChunkPalettedStorageFix.Direction.Axis p_15155_) {
         this.axis = p_15155_;
         this.axisDirection = p_15154_;
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

         private AxisDirection(int p_15180_) {
            this.step = p_15180_;
         }

         public int getStep() {
            return this.step;
         }
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

      public Section(Dynamic<?> p_15195_) {
         this.listTag = Lists.newArrayList();
         this.section = p_15195_;
         this.y = p_15195_.get("Y").asInt(0);
         this.hasData = p_15195_.get("Blocks").result().isPresent();
      }

      public Dynamic<?> getBlock(int p_15198_) {
         if (p_15198_ >= 0 && p_15198_ <= 4095) {
            Dynamic<?> $$1 = this.palette.byId(this.buffer[p_15198_]);
            return $$1 == null ? ChunkPalettedStorageFix.AIR : $$1;
         } else {
            return ChunkPalettedStorageFix.AIR;
         }
      }

      public void setBlock(int p_15203_, Dynamic<?> p_15204_) {
         if (this.seen.add(p_15204_)) {
            this.listTag.add("%%FILTER_ME%%".equals(ChunkPalettedStorageFix.getName(p_15204_)) ? ChunkPalettedStorageFix.AIR : p_15204_);
         }

         this.buffer[p_15203_] = ChunkPalettedStorageFix.idFor(this.palette, p_15204_);
      }

      public int upgrade(int p_15210_) {
         if (!this.hasData) {
            return p_15210_;
         } else {
            ByteBuffer $$1 = (ByteBuffer)this.section.get("Blocks").asByteBufferOpt().result().get();
            ChunkPalettedStorageFix.DataLayer $$2 = this.section
               .get("Data")
               .asByteBufferOpt()
               .map(p_15214_ -> new ChunkPalettedStorageFix.DataLayer(DataFixUtils.toArray(p_15214_)))
               .result()
               .orElseGet(ChunkPalettedStorageFix.DataLayer::new);
            ChunkPalettedStorageFix.DataLayer $$3 = this.section
               .get("Add")
               .asByteBufferOpt()
               .map(p_15208_ -> new ChunkPalettedStorageFix.DataLayer(DataFixUtils.toArray(p_15208_)))
               .result()
               .orElseGet(ChunkPalettedStorageFix.DataLayer::new);
            this.seen.add(ChunkPalettedStorageFix.AIR);
            ChunkPalettedStorageFix.idFor(this.palette, ChunkPalettedStorageFix.AIR);
            this.listTag.add(ChunkPalettedStorageFix.AIR);

            for (int $$4 = 0; $$4 < 4096; $$4++) {
               int $$5 = $$4 & 15;
               int $$6 = $$4 >> 8 & 15;
               int $$7 = $$4 >> 4 & 15;
               int $$8 = $$3.get($$5, $$6, $$7) << 12 | ($$1.get($$4) & 255) << 4 | $$2.get($$5, $$6, $$7);
               if (ChunkPalettedStorageFix.FIX.get($$8 >> 4)) {
                  this.addFix($$8 >> 4, $$4);
               }

               if (ChunkPalettedStorageFix.VIRTUAL.get($$8 >> 4)) {
                  int $$9 = ChunkPalettedStorageFix.getSideMask($$5 == 0, $$5 == 15, $$7 == 0, $$7 == 15);
                  if ($$9 == 0) {
                     this.update.add($$4);
                  } else {
                     p_15210_ |= $$9;
                  }
               }

               this.setBlock($$4, BlockStateData.getTag($$8));
            }

            return p_15210_;
         }
      }

      private void addFix(int p_15200_, int p_15201_) {
         IntList $$2 = (IntList)this.toFix.get(p_15200_);
         if ($$2 == null) {
            $$2 = new IntArrayList();
            this.toFix.put(p_15200_, $$2);
         }

         $$2.add(p_15201_);
      }

      public Dynamic<?> write() {
         Dynamic<?> $$0 = this.section;
         if (!this.hasData) {
            return $$0;
         } else {
            $$0 = $$0.set("Palette", $$0.createList(this.listTag.stream()));
            int $$1 = Math.max(4, DataFixUtils.ceillog2(this.seen.size()));
            PackedBitStorage $$2 = new PackedBitStorage($$1, 4096);

            for (int $$3 = 0; $$3 < this.buffer.length; $$3++) {
               $$2.set($$3, this.buffer[$$3]);
            }

            $$0 = $$0.set("BlockStates", $$0.createLongList(Arrays.stream($$2.getRaw())));
            $$0 = $$0.remove("Blocks");
            $$0 = $$0.remove("Data");
            return $$0.remove("Add");
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

      public UpgradeChunk(Dynamic<?> p_15222_) {
         this.level = p_15222_;
         this.x = p_15222_.get("xPos").asInt(0) << 4;
         this.z = p_15222_.get("zPos").asInt(0) << 4;
         p_15222_.get("TileEntities")
            .asStreamOpt()
            .ifSuccess(
               p_15241_ -> p_15241_.forEach(
                  p_145228_ -> {
                     int $$1x = p_145228_.get("x").asInt(0) - this.x & 15;
                     int $$2x = p_145228_.get("y").asInt(0);
                     int $$3x = p_145228_.get("z").asInt(0) - this.z & 15;
                     int $$4x = $$2x << 8 | $$3x << 4 | $$1x;
                     if (this.blockEntities.put($$4x, p_145228_) != null) {
                        ChunkPalettedStorageFix.LOGGER
                           .warn("In chunk: {}x{} found a duplicate block entity at position: [{}, {}, {}]", new Object[]{this.x, this.z, $$1x, $$2x, $$3x});
                     }
                  }
               )
            );
         boolean $$1 = p_15222_.get("convertedFromAlphaFormat").asBoolean(false);
         p_15222_.get("Sections").asStreamOpt().ifSuccess(p_15235_ -> p_15235_.forEach(p_145226_ -> {
            ChunkPalettedStorageFix.Section $$1x = new ChunkPalettedStorageFix.Section((Dynamic<?>)p_145226_);
            this.sides = $$1x.upgrade(this.sides);
            this.sections[$$1x.y] = $$1x;
         }));

         for (ChunkPalettedStorageFix.Section $$2 : this.sections) {
            if ($$2 != null) {
               ObjectIterator var7 = $$2.toFix.entrySet().iterator();

               while (var7.hasNext()) {
                  java.util.Map.Entry<Integer, IntList> $$3 = (java.util.Map.Entry<Integer, IntList>)var7.next();
                  int $$4 = $$2.y << 12;
                  switch ($$3.getKey()) {
                     case 2:
                        IntListIterator var30 = $$3.getValue().iterator();

                        while (var30.hasNext()) {
                           int $$5 = (Integer)var30.next();
                           $$5 |= $$4;
                           Dynamic<?> $$6 = this.getBlock($$5);
                           if ("minecraft:grass_block".equals(ChunkPalettedStorageFix.getName($$6))) {
                              String $$7 = ChunkPalettedStorageFix.getName(this.getBlock(relative($$5, ChunkPalettedStorageFix.Direction.UP)));
                              if ("minecraft:snow".equals($$7) || "minecraft:snow_layer".equals($$7)) {
                                 this.setBlock($$5, ChunkPalettedStorageFix.SNOWY_GRASS);
                              }
                           }
                        }
                        break;
                     case 3:
                        IntListIterator var29 = $$3.getValue().iterator();

                        while (var29.hasNext()) {
                           int $$8 = (Integer)var29.next();
                           $$8 |= $$4;
                           Dynamic<?> $$9 = this.getBlock($$8);
                           if ("minecraft:podzol".equals(ChunkPalettedStorageFix.getName($$9))) {
                              String $$10 = ChunkPalettedStorageFix.getName(this.getBlock(relative($$8, ChunkPalettedStorageFix.Direction.UP)));
                              if ("minecraft:snow".equals($$10) || "minecraft:snow_layer".equals($$10)) {
                                 this.setBlock($$8, ChunkPalettedStorageFix.SNOWY_PODZOL);
                              }
                           }
                        }
                        break;
                     case 25:
                        IntListIterator var28 = $$3.getValue().iterator();

                        while (var28.hasNext()) {
                           int $$14 = (Integer)var28.next();
                           $$14 |= $$4;
                           Dynamic<?> $$15 = this.removeBlockEntity($$14);
                           if ($$15 != null) {
                              String $$16 = Boolean.toString($$15.get("powered").asBoolean(false)) + (byte)Math.min(Math.max($$15.get("note").asInt(0), 0), 24);
                              this.setBlock(
                                 $$14, ChunkPalettedStorageFix.NOTE_BLOCK_MAP.getOrDefault($$16, ChunkPalettedStorageFix.NOTE_BLOCK_MAP.get("false0"))
                              );
                           }
                        }
                        break;
                     case 26:
                        IntListIterator var27 = $$3.getValue().iterator();

                        while (var27.hasNext()) {
                           int $$17 = (Integer)var27.next();
                           $$17 |= $$4;
                           Dynamic<?> $$18 = this.getBlockEntity($$17);
                           Dynamic<?> $$19 = this.getBlock($$17);
                           if ($$18 != null) {
                              int $$20 = $$18.get("color").asInt(0);
                              if ($$20 != 14 && $$20 >= 0 && $$20 < 16) {
                                 String $$21 = ChunkPalettedStorageFix.getProperty($$19, "facing")
                                    + ChunkPalettedStorageFix.getProperty($$19, "occupied")
                                    + ChunkPalettedStorageFix.getProperty($$19, "part")
                                    + $$20;
                                 if (ChunkPalettedStorageFix.BED_BLOCK_MAP.containsKey($$21)) {
                                    this.setBlock($$17, ChunkPalettedStorageFix.BED_BLOCK_MAP.get($$21));
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
                        IntListIterator var26 = $$3.getValue().iterator();

                        while (var26.hasNext()) {
                           int $$39 = (Integer)var26.next();
                           $$39 |= $$4;
                           Dynamic<?> $$40 = this.getBlock($$39);
                           if (ChunkPalettedStorageFix.getName($$40).endsWith("_door")) {
                              Dynamic<?> $$41 = this.getBlock($$39);
                              if ("lower".equals(ChunkPalettedStorageFix.getProperty($$41, "half"))) {
                                 int $$42 = relative($$39, ChunkPalettedStorageFix.Direction.UP);
                                 Dynamic<?> $$43 = this.getBlock($$42);
                                 String $$44 = ChunkPalettedStorageFix.getName($$41);
                                 if ($$44.equals(ChunkPalettedStorageFix.getName($$43))) {
                                    String $$45 = ChunkPalettedStorageFix.getProperty($$41, "facing");
                                    String $$46 = ChunkPalettedStorageFix.getProperty($$41, "open");
                                    String $$47 = $$1 ? "left" : ChunkPalettedStorageFix.getProperty($$43, "hinge");
                                    String $$48 = $$1 ? "false" : ChunkPalettedStorageFix.getProperty($$43, "powered");
                                    this.setBlock($$39, ChunkPalettedStorageFix.DOOR_MAP.get($$44 + $$45 + "lower" + $$47 + $$46 + $$48));
                                    this.setBlock($$42, ChunkPalettedStorageFix.DOOR_MAP.get($$44 + $$45 + "upper" + $$47 + $$46 + $$48));
                                 }
                              }
                           }
                        }
                        break;
                     case 86:
                        IntListIterator var25 = $$3.getValue().iterator();

                        while (var25.hasNext()) {
                           int $$27 = (Integer)var25.next();
                           $$27 |= $$4;
                           Dynamic<?> $$28 = this.getBlock($$27);
                           if ("minecraft:carved_pumpkin".equals(ChunkPalettedStorageFix.getName($$28))) {
                              String $$29 = ChunkPalettedStorageFix.getName(this.getBlock(relative($$27, ChunkPalettedStorageFix.Direction.DOWN)));
                              if ("minecraft:grass_block".equals($$29) || "minecraft:dirt".equals($$29)) {
                                 this.setBlock($$27, ChunkPalettedStorageFix.PUMPKIN);
                              }
                           }
                        }
                        break;
                     case 110:
                        IntListIterator var24 = $$3.getValue().iterator();

                        while (var24.hasNext()) {
                           int $$11 = (Integer)var24.next();
                           $$11 |= $$4;
                           Dynamic<?> $$12 = this.getBlock($$11);
                           if ("minecraft:mycelium".equals(ChunkPalettedStorageFix.getName($$12))) {
                              String $$13 = ChunkPalettedStorageFix.getName(this.getBlock(relative($$11, ChunkPalettedStorageFix.Direction.UP)));
                              if ("minecraft:snow".equals($$13) || "minecraft:snow_layer".equals($$13)) {
                                 this.setBlock($$11, ChunkPalettedStorageFix.SNOWY_MYCELIUM);
                              }
                           }
                        }
                        break;
                     case 140:
                        IntListIterator var23 = $$3.getValue().iterator();

                        while (var23.hasNext()) {
                           int $$30 = (Integer)var23.next();
                           $$30 |= $$4;
                           Dynamic<?> $$31 = this.removeBlockEntity($$30);
                           if ($$31 != null) {
                              String $$32 = $$31.get("Item").asString("") + $$31.get("Data").asInt(0);
                              this.setBlock(
                                 $$30, ChunkPalettedStorageFix.FLOWER_POT_MAP.getOrDefault($$32, ChunkPalettedStorageFix.FLOWER_POT_MAP.get("minecraft:air0"))
                              );
                           }
                        }
                        break;
                     case 144:
                        IntListIterator var22 = $$3.getValue().iterator();

                        while (var22.hasNext()) {
                           int $$33 = (Integer)var22.next();
                           $$33 |= $$4;
                           Dynamic<?> $$34 = this.getBlockEntity($$33);
                           if ($$34 != null) {
                              String $$35 = String.valueOf($$34.get("SkullType").asInt(0));
                              String $$36 = ChunkPalettedStorageFix.getProperty(this.getBlock($$33), "facing");
                              String $$38;
                              if (!"up".equals($$36) && !"down".equals($$36)) {
                                 $$38 = $$35 + $$36;
                              } else {
                                 $$38 = $$35 + $$34.get("Rot").asInt(0);
                              }

                              $$34.remove("SkullType");
                              $$34.remove("facing");
                              $$34.remove("Rot");
                              this.setBlock($$33, ChunkPalettedStorageFix.SKULL_MAP.getOrDefault($$38, ChunkPalettedStorageFix.SKULL_MAP.get("0north")));
                           }
                        }
                        break;
                     case 175:
                        IntListIterator var21 = $$3.getValue().iterator();

                        while (var21.hasNext()) {
                           int $$49 = (Integer)var21.next();
                           $$49 |= $$4;
                           Dynamic<?> $$50 = this.getBlock($$49);
                           if ("upper".equals(ChunkPalettedStorageFix.getProperty($$50, "half"))) {
                              Dynamic<?> $$51 = this.getBlock(relative($$49, ChunkPalettedStorageFix.Direction.DOWN));
                              String $$52 = ChunkPalettedStorageFix.getName($$51);
                              if ("minecraft:sunflower".equals($$52)) {
                                 this.setBlock($$49, ChunkPalettedStorageFix.UPPER_SUNFLOWER);
                              } else if ("minecraft:lilac".equals($$52)) {
                                 this.setBlock($$49, ChunkPalettedStorageFix.UPPER_LILAC);
                              } else if ("minecraft:tall_grass".equals($$52)) {
                                 this.setBlock($$49, ChunkPalettedStorageFix.UPPER_TALL_GRASS);
                              } else if ("minecraft:large_fern".equals($$52)) {
                                 this.setBlock($$49, ChunkPalettedStorageFix.UPPER_LARGE_FERN);
                              } else if ("minecraft:rose_bush".equals($$52)) {
                                 this.setBlock($$49, ChunkPalettedStorageFix.UPPER_ROSE_BUSH);
                              } else if ("minecraft:peony".equals($$52)) {
                                 this.setBlock($$49, ChunkPalettedStorageFix.UPPER_PEONY);
                              }
                           }
                        }
                        break;
                     case 176:
                     case 177:
                        IntListIterator var10 = $$3.getValue().iterator();

                        while (var10.hasNext()) {
                           int $$22 = (Integer)var10.next();
                           $$22 |= $$4;
                           Dynamic<?> $$23 = this.getBlockEntity($$22);
                           Dynamic<?> $$24 = this.getBlock($$22);
                           if ($$23 != null) {
                              int $$25 = $$23.get("Base").asInt(0);
                              if ($$25 != 15 && $$25 >= 0 && $$25 < 16) {
                                 String $$26 = ChunkPalettedStorageFix.getProperty($$24, $$3.getKey() == 176 ? "rotation" : "facing") + "_" + $$25;
                                 if (ChunkPalettedStorageFix.BANNER_BLOCK_MAP.containsKey($$26)) {
                                    this.setBlock($$22, ChunkPalettedStorageFix.BANNER_BLOCK_MAP.get($$26));
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
      private Dynamic<?> getBlockEntity(int p_15237_) {
         return (Dynamic<?>)this.blockEntities.get(p_15237_);
      }

      @Nullable
      private Dynamic<?> removeBlockEntity(int p_15243_) {
         return (Dynamic<?>)this.blockEntities.remove(p_15243_);
      }

      public static int relative(int p_15227_, ChunkPalettedStorageFix.Direction p_15228_) {
         switch (p_15228_.getAxis()) {
            case X:
               int $$2 = (p_15227_ & 15) + p_15228_.getAxisDirection().getStep();
               return $$2 >= 0 && $$2 <= 15 ? p_15227_ & -16 | $$2 : -1;
            case Y:
               int $$3 = (p_15227_ >> 8) + p_15228_.getAxisDirection().getStep();
               return $$3 >= 0 && $$3 <= 255 ? p_15227_ & 0xFF | $$3 << 8 : -1;
            case Z:
               int $$4 = (p_15227_ >> 4 & 15) + p_15228_.getAxisDirection().getStep();
               return $$4 >= 0 && $$4 <= 15 ? p_15227_ & -241 | $$4 << 4 : -1;
            default:
               return -1;
         }
      }

      private void setBlock(int p_15230_, Dynamic<?> p_15231_) {
         if (p_15230_ >= 0 && p_15230_ <= 65535) {
            ChunkPalettedStorageFix.Section $$2 = this.getSection(p_15230_);
            if ($$2 != null) {
               $$2.setBlock(p_15230_ & 4095, p_15231_);
            }
         }
      }

      @Nullable
      private ChunkPalettedStorageFix.Section getSection(int p_15245_) {
         int $$1 = p_15245_ >> 12;
         return $$1 < this.sections.length ? this.sections[$$1] : null;
      }

      public Dynamic<?> getBlock(int p_15225_) {
         if (p_15225_ >= 0 && p_15225_ <= 65535) {
            ChunkPalettedStorageFix.Section $$1 = this.getSection(p_15225_);
            return $$1 == null ? ChunkPalettedStorageFix.AIR : $$1.getBlock(p_15225_ & 4095);
         } else {
            return ChunkPalettedStorageFix.AIR;
         }
      }

      public Dynamic<?> write() {
         Dynamic<?> $$0 = this.level;
         if (this.blockEntities.isEmpty()) {
            $$0 = $$0.remove("TileEntities");
         } else {
            $$0 = $$0.set("TileEntities", $$0.createList(this.blockEntities.values().stream()));
         }

         Dynamic<?> $$1 = $$0.emptyMap();
         List<Dynamic<?>> $$2 = Lists.newArrayList();

         for (ChunkPalettedStorageFix.Section $$3 : this.sections) {
            if ($$3 != null) {
               $$2.add($$3.write());
               $$1 = $$1.set(String.valueOf($$3.y), $$1.createIntList(Arrays.stream($$3.update.toIntArray())));
            }
         }

         Dynamic<?> $$4 = $$0.emptyMap();
         $$4 = $$4.set("Sides", $$4.createByte((byte)this.sides));
         $$4 = $$4.set("Indices", $$1);
         return $$0.set("UpgradeData", $$4).set("Sections", $$4.createList($$2.stream()));
      }
   }
}
