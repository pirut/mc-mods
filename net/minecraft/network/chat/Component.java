package net.minecraft.network.chat;

import com.google.common.collect.Lists;
import com.mojang.brigadier.Message;
import com.mojang.datafixers.util.Either;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.commands.arguments.selector.SelectorPattern;
import net.minecraft.network.chat.contents.KeybindContents;
import net.minecraft.network.chat.contents.NbtContents;
import net.minecraft.network.chat.contents.ObjectContents;
import net.minecraft.network.chat.contents.PlainTextContents;
import net.minecraft.network.chat.contents.ScoreContents;
import net.minecraft.network.chat.contents.SelectorContents;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.network.chat.contents.data.DataSource;
import net.minecraft.network.chat.contents.objects.ObjectInfo;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.level.ChunkPos;
import org.jspecify.annotations.Nullable;

public interface Component extends Message, FormattedText {
   Style getStyle();

   ComponentContents getContents();

   @Override
   default String getString() {
      return FormattedText.super.getString();
   }

   default String getString(int maxLength) {
      StringBuilder stringBuilder = new StringBuilder();
      this.visit(string -> {
         int j = maxLength - stringBuilder.length();
         if (j <= 0) {
            return STOP_ITERATION;
         } else {
            stringBuilder.append(string.length() <= j ? string : string.substring(0, j));
            return Optional.empty();
         }
      });
      return stringBuilder.toString();
   }

   List<Component> getSiblings();

   @Nullable
   default String tryCollapseToString() {
      return this.getContents() instanceof PlainTextContents plainTextContents && this.getSiblings().isEmpty() && this.getStyle().isEmpty()
         ? plainTextContents.text()
         : null;
   }

   default MutableComponent plainCopy() {
      return MutableComponent.create(this.getContents());
   }

   default MutableComponent copy() {
      return new MutableComponent(this.getContents(), new ArrayList<>(this.getSiblings()), this.getStyle());
   }

   FormattedCharSequence getVisualOrderText();

   @Override
   default <T> Optional<T> visit(FormattedText.StyledContentConsumer<T> acceptor, Style style) {
      Style style2 = this.getStyle().applyTo(style);
      Optional<T> optional = this.getContents().visit(acceptor, style2);
      if (optional.isPresent()) {
         return optional;
      } else {
         for (Component component : this.getSiblings()) {
            Optional<T> optional2 = component.visit(acceptor, style2);
            if (optional2.isPresent()) {
               return optional2;
            }
         }

         return Optional.empty();
      }
   }

   @Override
   default <T> Optional<T> visit(FormattedText.ContentConsumer<T> acceptor) {
      Optional<T> optional = this.getContents().visit(acceptor);
      if (optional.isPresent()) {
         return optional;
      } else {
         for (Component component : this.getSiblings()) {
            Optional<T> optional2 = component.visit(acceptor);
            if (optional2.isPresent()) {
               return optional2;
            }
         }

         return Optional.empty();
      }
   }

   default List<Component> toFlatList() {
      return this.toFlatList(Style.EMPTY);
   }

   default List<Component> toFlatList(Style style) {
      List<Component> list = Lists.newArrayList();
      this.visit((stylex, string) -> {
         if (!string.isEmpty()) {
            list.add(literal(string).withStyle(stylex));
         }

         return Optional.empty();
      }, style);
      return list;
   }

   default boolean contains(Component other) {
      if (this.equals(other)) {
         return true;
      } else {
         List<Component> list = this.toFlatList();
         List<Component> list2 = other.toFlatList(this.getStyle());
         return Collections.indexOfSubList(list, list2) != -1;
      }
   }

   static Component nullToEmpty(@Nullable String text) {
      return (Component)(text != null ? literal(text) : CommonComponents.EMPTY);
   }

   static MutableComponent literal(String text) {
      return MutableComponent.create(PlainTextContents.create(text));
   }

   static MutableComponent translatable(String key) {
      return MutableComponent.create(new TranslatableContents(key, null, TranslatableContents.NO_ARGS));
   }

   static MutableComponent translatable(String key, Object... args) {
      return MutableComponent.create(new TranslatableContents(key, null, args));
   }

   static MutableComponent translatableEscape(String key, Object... args) {
      for (int i = 0; i < args.length; i++) {
         Object object = args[i];
         if (!TranslatableContents.isAllowedPrimitiveArgument(object) && !(object instanceof Component)) {
            args[i] = String.valueOf(object);
         }
      }

      return translatable(key, args);
   }

   static MutableComponent translatableWithFallback(String key, @Nullable String fallback) {
      return MutableComponent.create(new TranslatableContents(key, fallback, TranslatableContents.NO_ARGS));
   }

   static MutableComponent translatableWithFallback(String key, @Nullable String fallback, Object... args) {
      return MutableComponent.create(new TranslatableContents(key, fallback, args));
   }

   static MutableComponent empty() {
      return MutableComponent.create(PlainTextContents.EMPTY);
   }

   static MutableComponent keybind(String name) {
      return MutableComponent.create(new KeybindContents(name));
   }

   static MutableComponent nbt(String nbtPathPattern, boolean interpreting, Optional<Component> seperator, DataSource dataSource) {
      return MutableComponent.create(new NbtContents(nbtPathPattern, interpreting, seperator, dataSource));
   }

   static MutableComponent score(SelectorPattern selectorPattern, String objective) {
      return MutableComponent.create(new ScoreContents(Either.left(selectorPattern), objective));
   }

   static MutableComponent score(String name, String objective) {
      return MutableComponent.create(new ScoreContents(Either.right(name), objective));
   }

   static MutableComponent selector(SelectorPattern selectorPattern, Optional<Component> separator) {
      return MutableComponent.create(new SelectorContents(selectorPattern, separator));
   }

   static MutableComponent object(ObjectInfo contents) {
      return MutableComponent.create(new ObjectContents(contents));
   }

   static Component translationArg(Date date) {
      return literal(date.toString());
   }

   static Component translationArg(Message message) {
      return (Component)(message instanceof Component component ? component : literal(message.getString()));
   }

   static Component translationArg(UUID uuid) {
      return literal(uuid.toString());
   }

   static Component translationArg(Identifier location) {
      return literal(location.toString());
   }

   static Component translationArg(ChunkPos chunkPos) {
      return literal(chunkPos.toString());
   }

   static Component translationArg(URI uri) {
      return literal(uri.toString());
   }
}
