package net.minecraft.client.data.models.model;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class TextureMapping {
   private final Map<TextureSlot, Identifier> slots = Maps.newHashMap();
   private final Set<TextureSlot> forcedSlots = Sets.newHashSet();

   public TextureMapping put(TextureSlot p_388242_, Identifier p_469902_) {
      this.slots.put(p_388242_, p_469902_);
      return this;
   }

   public TextureMapping putForced(TextureSlot p_388207_, Identifier p_469280_) {
      this.slots.put(p_388207_, p_469280_);
      this.forcedSlots.add(p_388207_);
      return this;
   }

   public Stream<TextureSlot> getForced() {
      return this.forcedSlots.stream();
   }

   public TextureMapping copySlot(TextureSlot p_386513_, TextureSlot p_386704_) {
      this.slots.put(p_386704_, this.slots.get(p_386513_));
      return this;
   }

   public TextureMapping copyForced(TextureSlot p_386696_, TextureSlot p_387863_) {
      this.slots.put(p_387863_, this.slots.get(p_386696_));
      this.forcedSlots.add(p_387863_);
      return this;
   }

   public Identifier get(TextureSlot p_388725_) {
      for (TextureSlot textureslot = p_388725_; textureslot != null; textureslot = textureslot.getParent()) {
         Identifier identifier = this.slots.get(textureslot);
         if (identifier != null) {
            return identifier;
         }
      }

      throw new IllegalStateException("Can't find texture for slot " + p_388725_);
   }

   public TextureMapping copyAndUpdate(TextureSlot p_387359_, Identifier p_469727_) {
      TextureMapping texturemapping = new TextureMapping();
      texturemapping.slots.putAll(this.slots);
      texturemapping.forcedSlots.addAll(this.forcedSlots);
      texturemapping.put(p_387359_, p_469727_);
      return texturemapping;
   }

   public static TextureMapping cube(Block p_387253_) {
      Identifier identifier = getBlockTexture(p_387253_);
      return cube(identifier);
   }

   public static TextureMapping defaultTexture(Block p_386481_) {
      Identifier identifier = getBlockTexture(p_386481_);
      return defaultTexture(identifier);
   }

   public static TextureMapping defaultTexture(Identifier p_469994_) {
      return new TextureMapping().put(TextureSlot.TEXTURE, p_469994_);
   }

   public static TextureMapping cube(Identifier p_468883_) {
      return new TextureMapping().put(TextureSlot.ALL, p_468883_);
   }

   public static TextureMapping cross(Block p_388375_) {
      return singleSlot(TextureSlot.CROSS, getBlockTexture(p_388375_));
   }

   public static TextureMapping side(Block p_387192_) {
      return singleSlot(TextureSlot.SIDE, getBlockTexture(p_387192_));
   }

   public static TextureMapping crossEmissive(Block p_388279_) {
      return new TextureMapping().put(TextureSlot.CROSS, getBlockTexture(p_388279_)).put(TextureSlot.CROSS_EMISSIVE, getBlockTexture(p_388279_, "_emissive"));
   }

   public static TextureMapping cross(Identifier p_466925_) {
      return singleSlot(TextureSlot.CROSS, p_466925_);
   }

   public static TextureMapping plant(Block p_386532_) {
      return singleSlot(TextureSlot.PLANT, getBlockTexture(p_386532_));
   }

   public static TextureMapping plantEmissive(Block p_388102_) {
      return new TextureMapping().put(TextureSlot.PLANT, getBlockTexture(p_388102_)).put(TextureSlot.CROSS_EMISSIVE, getBlockTexture(p_388102_, "_emissive"));
   }

   public static TextureMapping plant(Identifier p_467433_) {
      return singleSlot(TextureSlot.PLANT, p_467433_);
   }

   public static TextureMapping rail(Block p_387835_) {
      return singleSlot(TextureSlot.RAIL, getBlockTexture(p_387835_));
   }

   public static TextureMapping rail(Identifier p_469528_) {
      return singleSlot(TextureSlot.RAIL, p_469528_);
   }

   public static TextureMapping wool(Block p_386520_) {
      return singleSlot(TextureSlot.WOOL, getBlockTexture(p_386520_));
   }

   public static TextureMapping flowerbed(Block p_387057_) {
      return new TextureMapping().put(TextureSlot.FLOWERBED, getBlockTexture(p_387057_)).put(TextureSlot.STEM, getBlockTexture(p_387057_, "_stem"));
   }

   public static TextureMapping wool(Identifier p_467378_) {
      return singleSlot(TextureSlot.WOOL, p_467378_);
   }

   public static TextureMapping stem(Block p_387853_) {
      return singleSlot(TextureSlot.STEM, getBlockTexture(p_387853_));
   }

   public static TextureMapping attachedStem(Block p_388790_, Block p_388101_) {
      return new TextureMapping().put(TextureSlot.STEM, getBlockTexture(p_388790_)).put(TextureSlot.UPPER_STEM, getBlockTexture(p_388101_));
   }

   public static TextureMapping pattern(Block p_388667_) {
      return singleSlot(TextureSlot.PATTERN, getBlockTexture(p_388667_));
   }

   public static TextureMapping fan(Block p_387296_) {
      return singleSlot(TextureSlot.FAN, getBlockTexture(p_387296_));
   }

   public static TextureMapping crop(Identifier p_468731_) {
      return singleSlot(TextureSlot.CROP, p_468731_);
   }

   public static TextureMapping pane(Block p_388128_, Block p_387021_) {
      return new TextureMapping().put(TextureSlot.PANE, getBlockTexture(p_388128_)).put(TextureSlot.EDGE, getBlockTexture(p_387021_, "_top"));
   }

   public static TextureMapping singleSlot(TextureSlot p_386734_, Identifier p_468358_) {
      return new TextureMapping().put(p_386734_, p_468358_);
   }

   public static TextureMapping column(Block p_386762_) {
      return new TextureMapping().put(TextureSlot.SIDE, getBlockTexture(p_386762_, "_side")).put(TextureSlot.END, getBlockTexture(p_386762_, "_top"));
   }

   public static TextureMapping cubeTop(Block p_386552_) {
      return new TextureMapping().put(TextureSlot.SIDE, getBlockTexture(p_386552_, "_side")).put(TextureSlot.TOP, getBlockTexture(p_386552_, "_top"));
   }

   public static TextureMapping pottedAzalea(Block p_388232_) {
      return new TextureMapping()
         .put(TextureSlot.PLANT, getBlockTexture(p_388232_, "_plant"))
         .put(TextureSlot.SIDE, getBlockTexture(p_388232_, "_side"))
         .put(TextureSlot.TOP, getBlockTexture(p_388232_, "_top"));
   }

   public static TextureMapping logColumn(Block p_388105_) {
      return new TextureMapping()
         .put(TextureSlot.SIDE, getBlockTexture(p_388105_))
         .put(TextureSlot.END, getBlockTexture(p_388105_, "_top"))
         .put(TextureSlot.PARTICLE, getBlockTexture(p_388105_));
   }

   public static TextureMapping column(Identifier p_468056_, Identifier p_469024_) {
      return new TextureMapping().put(TextureSlot.SIDE, p_468056_).put(TextureSlot.END, p_469024_);
   }

   public static TextureMapping fence(Block p_388148_) {
      return new TextureMapping()
         .put(TextureSlot.TEXTURE, getBlockTexture(p_388148_))
         .put(TextureSlot.SIDE, getBlockTexture(p_388148_, "_side"))
         .put(TextureSlot.TOP, getBlockTexture(p_388148_, "_top"));
   }

   public static TextureMapping customParticle(Block p_387796_) {
      return new TextureMapping().put(TextureSlot.TEXTURE, getBlockTexture(p_387796_)).put(TextureSlot.PARTICLE, getBlockTexture(p_387796_, "_particle"));
   }

   public static TextureMapping cubeBottomTop(Block p_388422_) {
      return new TextureMapping()
         .put(TextureSlot.SIDE, getBlockTexture(p_388422_, "_side"))
         .put(TextureSlot.TOP, getBlockTexture(p_388422_, "_top"))
         .put(TextureSlot.BOTTOM, getBlockTexture(p_388422_, "_bottom"));
   }

   public static TextureMapping cubeBottomTopWithWall(Block p_388122_) {
      Identifier identifier = getBlockTexture(p_388122_);
      return new TextureMapping()
         .put(TextureSlot.WALL, identifier)
         .put(TextureSlot.SIDE, identifier)
         .put(TextureSlot.TOP, getBlockTexture(p_388122_, "_top"))
         .put(TextureSlot.BOTTOM, getBlockTexture(p_388122_, "_bottom"));
   }

   public static TextureMapping columnWithWall(Block p_387356_) {
      Identifier identifier = getBlockTexture(p_387356_);
      return new TextureMapping()
         .put(TextureSlot.TEXTURE, identifier)
         .put(TextureSlot.WALL, identifier)
         .put(TextureSlot.SIDE, identifier)
         .put(TextureSlot.END, getBlockTexture(p_387356_, "_top"));
   }

   public static TextureMapping door(Identifier p_469119_, Identifier p_469964_) {
      return new TextureMapping().put(TextureSlot.TOP, p_469119_).put(TextureSlot.BOTTOM, p_469964_);
   }

   public static TextureMapping door(Block p_388928_) {
      return new TextureMapping().put(TextureSlot.TOP, getBlockTexture(p_388928_, "_top")).put(TextureSlot.BOTTOM, getBlockTexture(p_388928_, "_bottom"));
   }

   public static TextureMapping particle(Block p_386555_) {
      return new TextureMapping().put(TextureSlot.PARTICLE, getBlockTexture(p_386555_));
   }

   public static TextureMapping particle(Identifier p_467839_) {
      return new TextureMapping().put(TextureSlot.PARTICLE, p_467839_);
   }

   public static TextureMapping fire0(Block p_388175_) {
      return new TextureMapping().put(TextureSlot.FIRE, getBlockTexture(p_388175_, "_0"));
   }

   public static TextureMapping fire1(Block p_388429_) {
      return new TextureMapping().put(TextureSlot.FIRE, getBlockTexture(p_388429_, "_1"));
   }

   public static TextureMapping lantern(Block p_388559_) {
      return new TextureMapping().put(TextureSlot.LANTERN, getBlockTexture(p_388559_));
   }

   public static TextureMapping torch(Block p_387340_) {
      return new TextureMapping().put(TextureSlot.TORCH, getBlockTexture(p_387340_));
   }

   public static TextureMapping torch(Identifier p_466986_) {
      return new TextureMapping().put(TextureSlot.TORCH, p_466986_);
   }

   public static TextureMapping trialSpawner(Block p_388385_, String p_387531_, String p_387908_) {
      return new TextureMapping()
         .put(TextureSlot.SIDE, getBlockTexture(p_388385_, p_387531_))
         .put(TextureSlot.TOP, getBlockTexture(p_388385_, p_387908_))
         .put(TextureSlot.BOTTOM, getBlockTexture(p_388385_, "_bottom"));
   }

   public static TextureMapping vault(Block p_387220_, String p_387725_, String p_388640_, String p_387921_, String p_388068_) {
      return new TextureMapping()
         .put(TextureSlot.FRONT, getBlockTexture(p_387220_, p_387725_))
         .put(TextureSlot.SIDE, getBlockTexture(p_387220_, p_388640_))
         .put(TextureSlot.TOP, getBlockTexture(p_387220_, p_387921_))
         .put(TextureSlot.BOTTOM, getBlockTexture(p_387220_, p_388068_));
   }

   public static TextureMapping particleFromItem(Item p_386954_) {
      return new TextureMapping().put(TextureSlot.PARTICLE, getItemTexture(p_386954_));
   }

   public static TextureMapping commandBlock(Block p_387668_) {
      return new TextureMapping()
         .put(TextureSlot.SIDE, getBlockTexture(p_387668_, "_side"))
         .put(TextureSlot.FRONT, getBlockTexture(p_387668_, "_front"))
         .put(TextureSlot.BACK, getBlockTexture(p_387668_, "_back"));
   }

   public static TextureMapping orientableCube(Block p_387647_) {
      return new TextureMapping()
         .put(TextureSlot.SIDE, getBlockTexture(p_387647_, "_side"))
         .put(TextureSlot.FRONT, getBlockTexture(p_387647_, "_front"))
         .put(TextureSlot.TOP, getBlockTexture(p_387647_, "_top"))
         .put(TextureSlot.BOTTOM, getBlockTexture(p_387647_, "_bottom"));
   }

   public static TextureMapping orientableCubeOnlyTop(Block p_388737_) {
      return new TextureMapping()
         .put(TextureSlot.SIDE, getBlockTexture(p_388737_, "_side"))
         .put(TextureSlot.FRONT, getBlockTexture(p_388737_, "_front"))
         .put(TextureSlot.TOP, getBlockTexture(p_388737_, "_top"));
   }

   public static TextureMapping orientableCubeSameEnds(Block p_387915_) {
      return new TextureMapping()
         .put(TextureSlot.SIDE, getBlockTexture(p_387915_, "_side"))
         .put(TextureSlot.FRONT, getBlockTexture(p_387915_, "_front"))
         .put(TextureSlot.END, getBlockTexture(p_387915_, "_end"));
   }

   public static TextureMapping top(Block p_388431_) {
      return new TextureMapping().put(TextureSlot.TOP, getBlockTexture(p_388431_, "_top"));
   }

   public static TextureMapping craftingTable(Block p_388634_, Block p_386697_) {
      return new TextureMapping()
         .put(TextureSlot.PARTICLE, getBlockTexture(p_388634_, "_front"))
         .put(TextureSlot.DOWN, getBlockTexture(p_386697_))
         .put(TextureSlot.UP, getBlockTexture(p_388634_, "_top"))
         .put(TextureSlot.NORTH, getBlockTexture(p_388634_, "_front"))
         .put(TextureSlot.EAST, getBlockTexture(p_388634_, "_side"))
         .put(TextureSlot.SOUTH, getBlockTexture(p_388634_, "_side"))
         .put(TextureSlot.WEST, getBlockTexture(p_388634_, "_front"));
   }

   public static TextureMapping fletchingTable(Block p_388336_, Block p_387560_) {
      return new TextureMapping()
         .put(TextureSlot.PARTICLE, getBlockTexture(p_388336_, "_front"))
         .put(TextureSlot.DOWN, getBlockTexture(p_387560_))
         .put(TextureSlot.UP, getBlockTexture(p_388336_, "_top"))
         .put(TextureSlot.NORTH, getBlockTexture(p_388336_, "_front"))
         .put(TextureSlot.SOUTH, getBlockTexture(p_388336_, "_front"))
         .put(TextureSlot.EAST, getBlockTexture(p_388336_, "_side"))
         .put(TextureSlot.WEST, getBlockTexture(p_388336_, "_side"));
   }

   public static TextureMapping snifferEgg(String p_388037_) {
      return new TextureMapping()
         .put(TextureSlot.PARTICLE, getBlockTexture(Blocks.SNIFFER_EGG, p_388037_ + "_north"))
         .put(TextureSlot.BOTTOM, getBlockTexture(Blocks.SNIFFER_EGG, p_388037_ + "_bottom"))
         .put(TextureSlot.TOP, getBlockTexture(Blocks.SNIFFER_EGG, p_388037_ + "_top"))
         .put(TextureSlot.NORTH, getBlockTexture(Blocks.SNIFFER_EGG, p_388037_ + "_north"))
         .put(TextureSlot.SOUTH, getBlockTexture(Blocks.SNIFFER_EGG, p_388037_ + "_south"))
         .put(TextureSlot.EAST, getBlockTexture(Blocks.SNIFFER_EGG, p_388037_ + "_east"))
         .put(TextureSlot.WEST, getBlockTexture(Blocks.SNIFFER_EGG, p_388037_ + "_west"));
   }

   public static TextureMapping driedGhast(String p_415943_) {
      return new TextureMapping()
         .put(TextureSlot.PARTICLE, getBlockTexture(Blocks.DRIED_GHAST, p_415943_ + "_north"))
         .put(TextureSlot.BOTTOM, getBlockTexture(Blocks.DRIED_GHAST, p_415943_ + "_bottom"))
         .put(TextureSlot.TOP, getBlockTexture(Blocks.DRIED_GHAST, p_415943_ + "_top"))
         .put(TextureSlot.NORTH, getBlockTexture(Blocks.DRIED_GHAST, p_415943_ + "_north"))
         .put(TextureSlot.SOUTH, getBlockTexture(Blocks.DRIED_GHAST, p_415943_ + "_south"))
         .put(TextureSlot.EAST, getBlockTexture(Blocks.DRIED_GHAST, p_415943_ + "_east"))
         .put(TextureSlot.WEST, getBlockTexture(Blocks.DRIED_GHAST, p_415943_ + "_west"))
         .put(TextureSlot.TENTACLES, getBlockTexture(Blocks.DRIED_GHAST, p_415943_ + "_tentacles"));
   }

   public static TextureMapping campfire(Block p_386870_) {
      return new TextureMapping().put(TextureSlot.LIT_LOG, getBlockTexture(p_386870_, "_log_lit")).put(TextureSlot.FIRE, getBlockTexture(p_386870_, "_fire"));
   }

   public static TextureMapping candleCake(Block p_387252_, boolean p_387959_) {
      return new TextureMapping()
         .put(TextureSlot.PARTICLE, getBlockTexture(Blocks.CAKE, "_side"))
         .put(TextureSlot.BOTTOM, getBlockTexture(Blocks.CAKE, "_bottom"))
         .put(TextureSlot.TOP, getBlockTexture(Blocks.CAKE, "_top"))
         .put(TextureSlot.SIDE, getBlockTexture(Blocks.CAKE, "_side"))
         .put(TextureSlot.CANDLE, getBlockTexture(p_387252_, p_387959_ ? "_lit" : ""));
   }

   public static TextureMapping cauldron(Identifier p_469688_) {
      return new TextureMapping()
         .put(TextureSlot.PARTICLE, getBlockTexture(Blocks.CAULDRON, "_side"))
         .put(TextureSlot.SIDE, getBlockTexture(Blocks.CAULDRON, "_side"))
         .put(TextureSlot.TOP, getBlockTexture(Blocks.CAULDRON, "_top"))
         .put(TextureSlot.BOTTOM, getBlockTexture(Blocks.CAULDRON, "_bottom"))
         .put(TextureSlot.INSIDE, getBlockTexture(Blocks.CAULDRON, "_inner"))
         .put(TextureSlot.CONTENT, p_469688_);
   }

   public static TextureMapping sculkShrieker(boolean p_387374_) {
      String s = p_387374_ ? "_can_summon" : "";
      return new TextureMapping()
         .put(TextureSlot.PARTICLE, getBlockTexture(Blocks.SCULK_SHRIEKER, "_bottom"))
         .put(TextureSlot.SIDE, getBlockTexture(Blocks.SCULK_SHRIEKER, "_side"))
         .put(TextureSlot.TOP, getBlockTexture(Blocks.SCULK_SHRIEKER, "_top"))
         .put(TextureSlot.INNER_TOP, getBlockTexture(Blocks.SCULK_SHRIEKER, s + "_inner_top"))
         .put(TextureSlot.BOTTOM, getBlockTexture(Blocks.SCULK_SHRIEKER, "_bottom"));
   }

   public static TextureMapping bars(Block p_436719_) {
      return new TextureMapping().put(TextureSlot.BARS, getBlockTexture(p_436719_)).put(TextureSlot.EDGE, getBlockTexture(p_436719_));
   }

   public static TextureMapping layer0(Item p_386958_) {
      return new TextureMapping().put(TextureSlot.LAYER0, getItemTexture(p_386958_));
   }

   public static TextureMapping layer0(Block p_387628_) {
      return new TextureMapping().put(TextureSlot.LAYER0, getBlockTexture(p_387628_));
   }

   public static TextureMapping layer0(Identifier p_469135_) {
      return new TextureMapping().put(TextureSlot.LAYER0, p_469135_);
   }

   public static TextureMapping layered(Identifier p_468888_, Identifier p_467211_) {
      return new TextureMapping().put(TextureSlot.LAYER0, p_468888_).put(TextureSlot.LAYER1, p_467211_);
   }

   public static TextureMapping layered(Identifier p_469771_, Identifier p_467988_, Identifier p_469178_) {
      return new TextureMapping().put(TextureSlot.LAYER0, p_469771_).put(TextureSlot.LAYER1, p_467988_).put(TextureSlot.LAYER2, p_469178_);
   }

   public static Identifier getBlockTexture(Block p_387523_) {
      Identifier identifier = BuiltInRegistries.BLOCK.getKey(p_387523_);
      return identifier.withPrefix("block/");
   }

   public static Identifier getBlockTexture(Block p_386654_, String p_388891_) {
      Identifier identifier = BuiltInRegistries.BLOCK.getKey(p_386654_);
      return identifier.withPath(p_388162_ -> "block/" + p_388162_ + p_388891_);
   }

   public static Identifier getItemTexture(Item p_388249_) {
      Identifier identifier = BuiltInRegistries.ITEM.getKey(p_388249_);
      return identifier.withPrefix("item/");
   }

   public static Identifier getItemTexture(Item p_386842_, String p_386898_) {
      Identifier identifier = BuiltInRegistries.ITEM.getKey(p_386842_);
      return identifier.withPath(p_387396_ -> "item/" + p_387396_ + p_386898_);
   }

   public TextureMapping copy() {
      TextureMapping texturemapping = new TextureMapping();
      texturemapping.slots.putAll(this.slots);
      texturemapping.forcedSlots.addAll(this.forcedSlots);
      return texturemapping;
   }
}
