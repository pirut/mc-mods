package net.minecraft.network.chat;

import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.stream.JsonReader;
import com.mojang.brigadier.Message;
import com.mojang.serialization.JsonOps;
import java.io.StringReader;
import java.lang.reflect.Type;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.chat.contents.DataSource;
import net.minecraft.network.chat.contents.KeybindContents;
import net.minecraft.network.chat.contents.NbtContents;
import net.minecraft.network.chat.contents.PlainTextContents;
import net.minecraft.network.chat.contents.ScoreContents;
import net.minecraft.network.chat.contents.SelectorContents;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.level.ChunkPos;

public interface Component extends Message, FormattedText {
   Style getStyle();

   ComponentContents getContents();

   @Override
   default String getString() {
      return FormattedText.super.getString();
   }

   default String getString(int p_130669_) {
      StringBuilder $$1 = new StringBuilder();
      this.visit(p_130673_ -> {
         int $$3 = p_130669_ - $$1.length();
         if ($$3 <= 0) {
            return STOP_ITERATION;
         } else {
            $$1.append(p_130673_.length() <= $$3 ? p_130673_ : p_130673_.substring(0, $$3));
            return Optional.empty();
         }
      });
      return $$1.toString();
   }

   List<Component> getSiblings();

   @Nullable
   default String tryCollapseToString() {
      return this.getContents() instanceof PlainTextContents $$0 && this.getSiblings().isEmpty() && this.getStyle().isEmpty() ? $$0.text() : null;
   }

   default MutableComponent plainCopy() {
      return MutableComponent.create(this.getContents());
   }

   default MutableComponent copy() {
      return new MutableComponent(this.getContents(), new ArrayList<>(this.getSiblings()), this.getStyle());
   }

   FormattedCharSequence getVisualOrderText();

   @Override
   default <T> Optional<T> visit(FormattedText.StyledContentConsumer<T> p_130679_, Style p_130680_) {
      Style $$2 = this.getStyle().applyTo(p_130680_);
      Optional<T> $$3 = this.getContents().visit(p_130679_, $$2);
      if ($$3.isPresent()) {
         return $$3;
      } else {
         for (Component $$4 : this.getSiblings()) {
            Optional<T> $$5 = $$4.visit(p_130679_, $$2);
            if ($$5.isPresent()) {
               return $$5;
            }
         }

         return Optional.empty();
      }
   }

   @Override
   default <T> Optional<T> visit(FormattedText.ContentConsumer<T> p_130677_) {
      Optional<T> $$1 = this.getContents().visit(p_130677_);
      if ($$1.isPresent()) {
         return $$1;
      } else {
         for (Component $$2 : this.getSiblings()) {
            Optional<T> $$3 = $$2.visit(p_130677_);
            if ($$3.isPresent()) {
               return $$3;
            }
         }

         return Optional.empty();
      }
   }

   default List<Component> toFlatList() {
      return this.toFlatList(Style.EMPTY);
   }

   default List<Component> toFlatList(Style p_178406_) {
      List<Component> $$1 = Lists.newArrayList();
      this.visit((p_178403_, p_178404_) -> {
         if (!p_178404_.isEmpty()) {
            $$1.add(literal(p_178404_).withStyle(p_178403_));
         }

         return Optional.empty();
      }, p_178406_);
      return $$1;
   }

   default boolean contains(Component p_240571_) {
      if (this.equals(p_240571_)) {
         return true;
      } else {
         List<Component> $$1 = this.toFlatList();
         List<Component> $$2 = p_240571_.toFlatList(this.getStyle());
         return Collections.indexOfSubList($$1, $$2) != -1;
      }
   }

   static Component nullToEmpty(@Nullable String p_130675_) {
      return (Component)(p_130675_ != null ? literal(p_130675_) : CommonComponents.EMPTY);
   }

   static MutableComponent literal(String p_237114_) {
      return MutableComponent.create(PlainTextContents.create(p_237114_));
   }

   static MutableComponent translatable(String p_237116_) {
      return MutableComponent.create(new TranslatableContents(p_237116_, null, TranslatableContents.NO_ARGS));
   }

   static MutableComponent translatable(String p_237111_, Object... p_237112_) {
      return MutableComponent.create(new TranslatableContents(p_237111_, null, p_237112_));
   }

   static MutableComponent translatableEscape(String p_304683_, Object... p_304399_) {
      for (int $$2 = 0; $$2 < p_304399_.length; $$2++) {
         Object $$3 = p_304399_[$$2];
         if (!TranslatableContents.isAllowedPrimitiveArgument($$3) && !($$3 instanceof Component)) {
            p_304399_[$$2] = String.valueOf($$3);
         }
      }

      return translatable(p_304683_, p_304399_);
   }

   static MutableComponent translatableWithFallback(String p_265747_, @Nullable String p_265287_) {
      return MutableComponent.create(new TranslatableContents(p_265747_, p_265287_, TranslatableContents.NO_ARGS));
   }

   static MutableComponent translatableWithFallback(String p_265449_, @Nullable String p_265281_, Object... p_265785_) {
      return MutableComponent.create(new TranslatableContents(p_265449_, p_265281_, p_265785_));
   }

   static MutableComponent empty() {
      return MutableComponent.create(PlainTextContents.EMPTY);
   }

   static MutableComponent keybind(String p_237118_) {
      return MutableComponent.create(new KeybindContents(p_237118_));
   }

   static MutableComponent nbt(String p_237106_, boolean p_237107_, Optional<Component> p_237108_, DataSource p_237109_) {
      return MutableComponent.create(new NbtContents(p_237106_, p_237107_, p_237108_, p_237109_));
   }

   static MutableComponent score(String p_237100_, String p_237101_) {
      return MutableComponent.create(new ScoreContents(p_237100_, p_237101_));
   }

   static MutableComponent selector(String p_237103_, Optional<Component> p_237104_) {
      return MutableComponent.create(new SelectorContents(p_237103_, p_237104_));
   }

   static Component translationArg(Date p_304461_) {
      return literal(p_304461_.toString());
   }

   static Component translationArg(Message p_304620_) {
      return (Component)(p_304620_ instanceof Component $$1 ? $$1 : literal(p_304620_.getString()));
   }

   static Component translationArg(UUID p_304730_) {
      return literal(p_304730_.toString());
   }

   static Component translationArg(ResourceLocation p_304849_) {
      return literal(p_304849_.toString());
   }

   static Component translationArg(ChunkPos p_304675_) {
      return literal(p_304675_.toString());
   }

   static Component translationArg(URI p_352152_) {
      return literal(p_352152_.toString());
   }

   public static class Serializer {
      private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();

      private Serializer() {
      }

      static MutableComponent deserialize(JsonElement p_130720_, HolderLookup.Provider p_330752_) {
         return (MutableComponent)ComponentSerialization.CODEC
            .parse(p_330752_.createSerializationContext(JsonOps.INSTANCE), p_130720_)
            .getOrThrow(JsonParseException::new);
      }

      static JsonElement serialize(Component p_304983_, HolderLookup.Provider p_331501_) {
         return (JsonElement)ComponentSerialization.CODEC
            .encodeStart(p_331501_.createSerializationContext(JsonOps.INSTANCE), p_304983_)
            .getOrThrow(JsonParseException::new);
      }

      public static String toJson(Component p_130704_, HolderLookup.Provider p_330782_) {
         return GSON.toJson(serialize(p_130704_, p_330782_));
      }

      @Nullable
      public static MutableComponent fromJson(String p_130702_, HolderLookup.Provider p_331632_) {
         JsonElement $$2 = JsonParser.parseString(p_130702_);
         return $$2 == null ? null : deserialize($$2, p_331632_);
      }

      @Nullable
      public static MutableComponent fromJson(@Nullable JsonElement p_130692_, HolderLookup.Provider p_330725_) {
         return p_130692_ == null ? null : deserialize(p_130692_, p_330725_);
      }

      @Nullable
      public static MutableComponent fromJsonLenient(String p_130715_, HolderLookup.Provider p_331029_) {
         JsonReader $$2 = new JsonReader(new StringReader(p_130715_));
         $$2.setLenient(true);
         JsonElement $$3 = JsonParser.parseReader($$2);
         return $$3 == null ? null : deserialize($$3, p_331029_);
      }
   }

   public static class SerializerAdapter implements JsonDeserializer<MutableComponent>, JsonSerializer<Component> {
      private final HolderLookup.Provider registries;

      public SerializerAdapter(HolderLookup.Provider p_330454_) {
         this.registries = p_330454_;
      }

      public MutableComponent deserialize(JsonElement p_304644_, Type p_304861_, JsonDeserializationContext p_304563_) throws JsonParseException {
         return Component.Serializer.deserialize(p_304644_, this.registries);
      }

      public JsonElement serialize(Component p_304915_, Type p_304423_, JsonSerializationContext p_304514_) {
         return Component.Serializer.serialize(p_304915_, this.registries);
      }
   }
}
