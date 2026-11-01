package net.minecraft.network.chat.contents;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.datafixers.DataFixUtils;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.NbtPathArgument;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentContents;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.Entity;
import org.slf4j.Logger;

public class NbtContents implements ComponentContents {
   private static final Logger LOGGER = LogUtils.getLogger();
   public static final MapCodec<NbtContents> CODEC = RecordCodecBuilder.mapCodec(
      p_337510_ -> p_337510_.group(
            Codec.STRING.fieldOf("nbt").forGetter(NbtContents::getNbtPath),
            Codec.BOOL.lenientOptionalFieldOf("interpret", false).forGetter(NbtContents::isInterpreting),
            ComponentSerialization.CODEC.lenientOptionalFieldOf("separator").forGetter(NbtContents::getSeparator),
            DataSource.CODEC.forGetter(NbtContents::getDataSource)
         )
         .apply(p_337510_, NbtContents::new)
   );
   public static final ComponentContents.Type<NbtContents> TYPE = new ComponentContents.Type<>(CODEC, "nbt");
   private final boolean interpreting;
   private final Optional<Component> separator;
   private final String nbtPathPattern;
   private final DataSource dataSource;
   @Nullable
   protected final NbtPathArgument.NbtPath compiledNbtPath;

   public NbtContents(String p_237395_, boolean p_237396_, Optional<Component> p_237397_, DataSource p_237398_) {
      this(p_237395_, compileNbtPath(p_237395_), p_237396_, p_237397_, p_237398_);
   }

   private NbtContents(String p_237389_, @Nullable NbtPathArgument.NbtPath p_237390_, boolean p_237391_, Optional<Component> p_237392_, DataSource p_237393_) {
      this.nbtPathPattern = p_237389_;
      this.compiledNbtPath = p_237390_;
      this.interpreting = p_237391_;
      this.separator = p_237392_;
      this.dataSource = p_237393_;
   }

   @Nullable
   private static NbtPathArgument.NbtPath compileNbtPath(String p_237410_) {
      try {
         return new NbtPathArgument().parse(new StringReader(p_237410_));
      } catch (CommandSyntaxException var2) {
         return null;
      }
   }

   public String getNbtPath() {
      return this.nbtPathPattern;
   }

   public boolean isInterpreting() {
      return this.interpreting;
   }

   public Optional<Component> getSeparator() {
      return this.separator;
   }

   public DataSource getDataSource() {
      return this.dataSource;
   }

   @Override
   public boolean equals(Object p_237430_) {
      return this == p_237430_
         ? true
         : p_237430_ instanceof NbtContents $$1
            && this.dataSource.equals($$1.dataSource)
            && this.separator.equals($$1.separator)
            && this.interpreting == $$1.interpreting
            && this.nbtPathPattern.equals($$1.nbtPathPattern);
   }

   @Override
   public int hashCode() {
      int $$0 = this.interpreting ? 1 : 0;
      $$0 = 31 * $$0 + this.separator.hashCode();
      $$0 = 31 * $$0 + this.nbtPathPattern.hashCode();
      return 31 * $$0 + this.dataSource.hashCode();
   }

   @Override
   public String toString() {
      return "nbt{" + this.dataSource + ", interpreting=" + this.interpreting + ", separator=" + this.separator + "}";
   }

   @Override
   public MutableComponent resolve(@Nullable CommandSourceStack p_237401_, @Nullable Entity p_237402_, int p_237403_) throws CommandSyntaxException {
      if (p_237401_ != null && this.compiledNbtPath != null) {
         Stream<String> $$3 = this.dataSource.getData(p_237401_).flatMap(p_237417_ -> {
            try {
               return this.compiledNbtPath.get(p_237417_).stream();
            } catch (CommandSyntaxException var3x) {
               return Stream.empty();
            }
         }).map(Tag::getAsString);
         if (this.interpreting) {
            Component $$4 = (Component)DataFixUtils.orElse(
               ComponentUtils.updateForEntity(p_237401_, this.separator, p_237402_, p_237403_), ComponentUtils.DEFAULT_NO_STYLE_SEPARATOR
            );
            return $$3.flatMap(p_329869_ -> {
               try {
                  MutableComponent $$4x = Component.Serializer.fromJson(p_329869_, p_237401_.registryAccess());
                  return Stream.of(ComponentUtils.updateForEntity(p_237401_, $$4x, p_237402_, p_237403_));
               } catch (Exception var5x) {
                  LOGGER.warn("Failed to parse component: {}", p_329869_, var5x);
                  return Stream.of();
               }
            }).reduce((p_237420_, p_237421_) -> p_237420_.append($$4).append(p_237421_)).orElseGet(Component::empty);
         } else {
            return ComponentUtils.updateForEntity(p_237401_, this.separator, p_237402_, p_237403_)
               .map(
                  p_237415_ -> $$3.map(Component::literal)
                     .reduce((p_237424_, p_237425_) -> p_237424_.append(p_237415_).append(p_237425_))
                     .orElseGet(Component::empty)
               )
               .orElseGet(() -> Component.literal($$3.collect(Collectors.joining(", "))));
         }
      } else {
         return Component.empty();
      }
   }

   @Override
   public ComponentContents.Type<?> type() {
      return TYPE;
   }
}
