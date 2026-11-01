package net.minecraft.world.item.component;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.ImmutableList.Builder;
import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.network.Filterable;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.StringUtil;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.jspecify.annotations.Nullable;

public record WrittenBookContent(Filterable<String> title, String author, int generation, List<Filterable<Component>> pages, boolean resolved)
   implements BookContent<Component, WrittenBookContent>,
   TooltipProvider {
   public static final WrittenBookContent EMPTY = new WrittenBookContent(Filterable.passThrough(""), "", 0, List.of(), true);
   public static final int PAGE_LENGTH = 32767;
   public static final int TITLE_LENGTH = 16;
   public static final int TITLE_MAX_LENGTH = 32;
   public static final int MAX_GENERATION = 3;
   public static final int MAX_CRAFTABLE_GENERATION = 2;
   public static final Codec<Component> CONTENT_CODEC = ComponentSerialization.flatRestrictedCodec(32767);
   public static final Codec<List<Filterable<Component>>> PAGES_CODEC = pagesCodec(CONTENT_CODEC);
   public static final Codec<WrittenBookContent> CODEC = RecordCodecBuilder.create(
      p_331461_ -> p_331461_.group(
            Filterable.codec(Codec.string(0, 32)).fieldOf("title").forGetter(WrittenBookContent::title),
            Codec.STRING.fieldOf("author").forGetter(WrittenBookContent::author),
            ExtraCodecs.intRange(0, 3).optionalFieldOf("generation", 0).forGetter(WrittenBookContent::generation),
            PAGES_CODEC.optionalFieldOf("pages", List.of()).forGetter(WrittenBookContent::pages),
            Codec.BOOL.optionalFieldOf("resolved", false).forGetter(WrittenBookContent::resolved)
         )
         .apply(p_331461_, WrittenBookContent::new)
   );
   public static final StreamCodec<RegistryFriendlyByteBuf, WrittenBookContent> STREAM_CODEC = StreamCodec.composite(
      Filterable.streamCodec(ByteBufCodecs.stringUtf8(32)),
      WrittenBookContent::title,
      ByteBufCodecs.STRING_UTF8,
      WrittenBookContent::author,
      ByteBufCodecs.VAR_INT,
      WrittenBookContent::generation,
      Filterable.streamCodec(ComponentSerialization.STREAM_CODEC).apply(ByteBufCodecs.list()),
      WrittenBookContent::pages,
      ByteBufCodecs.BOOL,
      WrittenBookContent::resolved,
      WrittenBookContent::new
   );

   public WrittenBookContent(Filterable<String> title, String author, int generation, List<Filterable<Component>> pages, boolean resolved) {
      if (generation >= 0 && generation <= 3) {
         this.title = title;
         this.author = author;
         this.generation = generation;
         this.pages = pages;
         this.resolved = resolved;
      } else {
         throw new IllegalArgumentException("Generation was " + generation + ", but must be between 0 and 3");
      }
   }

   private static Codec<Filterable<Component>> pageCodec(Codec<Component> p_335677_) {
      return Filterable.codec(p_335677_);
   }

   public static Codec<List<Filterable<Component>>> pagesCodec(Codec<Component> p_335930_) {
      return pageCodec(p_335930_).listOf();
   }

   @Nullable
   public WrittenBookContent tryCraftCopy() {
      return this.generation >= 2 ? null : new WrittenBookContent(this.title, this.author, this.generation + 1, this.pages, this.resolved);
   }

   public static boolean resolveForItem(ItemStack p_394373_, CommandSourceStack p_393899_, @Nullable Player p_393825_) {
      WrittenBookContent writtenbookcontent = p_394373_.get(DataComponents.WRITTEN_BOOK_CONTENT);
      if (writtenbookcontent != null && !writtenbookcontent.resolved()) {
         WrittenBookContent writtenbookcontent1 = writtenbookcontent.resolve(p_393899_, p_393825_);
         if (writtenbookcontent1 != null) {
            p_394373_.set(DataComponents.WRITTEN_BOOK_CONTENT, writtenbookcontent1);
            return true;
         }

         p_394373_.set(DataComponents.WRITTEN_BOOK_CONTENT, writtenbookcontent.markResolved());
      }

      return false;
   }

   @Nullable
   public WrittenBookContent resolve(CommandSourceStack p_330312_, @Nullable Player p_331808_) {
      if (this.resolved) {
         return null;
      } else {
         Builder<Filterable<Component>> builder = ImmutableList.builderWithExpectedSize(this.pages.size());

         for (Filterable<Component> filterable : this.pages) {
            Optional<Filterable<Component>> optional = resolvePage(p_330312_, p_331808_, filterable);
            if (optional.isEmpty()) {
               return null;
            }

            builder.add(optional.get());
         }

         return new WrittenBookContent(this.title, this.author, this.generation, builder.build(), true);
      }
   }

   public WrittenBookContent markResolved() {
      return new WrittenBookContent(this.title, this.author, this.generation, this.pages, true);
   }

   private static Optional<Filterable<Component>> resolvePage(CommandSourceStack p_330620_, @Nullable Player p_330555_, Filterable<Component> p_330218_) {
      return p_330218_.resolve(p_331526_ -> {
         try {
            Component component = ComponentUtils.updateForEntity(p_330620_, p_331526_, p_330555_, 0);
            return isPageTooLarge(component, p_330620_.registryAccess()) ? Optional.empty() : Optional.of(component);
         } catch (Exception var4) {
            return Optional.of(p_331526_);
         }
      });
   }

   private static boolean isPageTooLarge(Component p_331458_, HolderLookup.Provider p_331039_) {
      DataResult<JsonElement> dataresult = ComponentSerialization.CODEC.encodeStart(p_331039_.createSerializationContext(JsonOps.INSTANCE), p_331458_);
      return dataresult.isSuccess() && GsonHelper.encodesLongerThan((JsonElement)dataresult.getOrThrow(), 32767);
   }

   public List<Component> getPages(boolean p_330387_) {
      return Lists.transform(this.pages, p_332134_ -> (Component)p_332134_.get(p_330387_));
   }

   public WrittenBookContent withReplacedPages(List<Filterable<Component>> p_333901_) {
      return new WrittenBookContent(this.title, this.author, this.generation, p_333901_, false);
   }

   @Override
   public void addToTooltip(Item.TooltipContext p_393666_, Consumer<Component> p_393982_, TooltipFlag p_394366_, DataComponentGetter p_400256_) {
      if (!StringUtil.isBlank(this.author)) {
         p_393982_.accept(Component.translatable("book.byAuthor", this.author).withStyle(ChatFormatting.GRAY));
      }

      p_393982_.accept(Component.translatable("book.generation." + this.generation).withStyle(ChatFormatting.GRAY));
   }
}
