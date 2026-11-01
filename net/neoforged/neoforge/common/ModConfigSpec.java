package net.neoforged.neoforge.common;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.Config;
import com.electronwill.nightconfig.core.EnumGetMethod;
import com.electronwill.nightconfig.core.InMemoryFormat;
import com.electronwill.nightconfig.core.UnmodifiableCommentedConfig;
import com.electronwill.nightconfig.core.UnmodifiableConfig;
import com.electronwill.nightconfig.core.ConfigSpec.CorrectionAction;
import com.electronwill.nightconfig.core.ConfigSpec.CorrectionListener;
import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.Map.Entry;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.DoubleSupplier;
import java.util.function.Function;
import java.util.function.IntSupplier;
import java.util.function.LongSupplier;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import net.neoforged.fml.Logging;
import net.neoforged.fml.config.IConfigSpec;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.config.IConfigSpec.ILoadedConfig;
import net.neoforged.fml.config.ModConfig.Type;
import net.neoforged.fml.loading.FMLEnvironment;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jspecify.annotations.Nullable;

public class ModConfigSpec implements IConfigSpec {
   private final Map<List<String>, String> levelComments;
   private final Map<List<String>, String> levelTranslationKeys;
   private final UnmodifiableConfig spec;
   private final UnmodifiableConfig values;
   @Nullable
   private ILoadedConfig loadedConfig;
   private static final Logger LOGGER = LogManager.getLogger();
   private static final Joiner LINE_JOINER = Joiner.on("\n");
   private static final Joiner DOT_JOINER = Joiner.on(".");
   private static final Splitter DOT_SPLITTER = Splitter.on(".");

   private ModConfigSpec(
      UnmodifiableConfig spec, UnmodifiableConfig values, Map<List<String>, String> levelComments, Map<List<String>, String> levelTranslationKeys
   ) {
      this.spec = spec;
      this.values = values;
      this.levelComments = levelComments;
      this.levelTranslationKeys = levelTranslationKeys;
   }

   public boolean isEmpty() {
      return this.spec.isEmpty();
   }

   public String getLevelComment(List<String> path) {
      return this.levelComments.get(path);
   }

   public String getLevelTranslationKey(List<String> path) {
      return this.levelTranslationKeys.get(path);
   }

   public void acceptConfig(@Nullable ILoadedConfig config) {
      this.loadedConfig = config;
      if (config != null && !this.isCorrect(config.config())) {
         LOGGER.warn(Logging.CORE, "Configuration {} is not correct. Correcting", config);
         this.correct(
            config.config(),
            (action, path, incorrectValue, correctedValue) -> LOGGER.warn(
               Logging.CORE,
               "Incorrect key {} was corrected from {} to its default, {}. {}",
               DOT_JOINER.join(path),
               incorrectValue,
               correctedValue,
               incorrectValue == correctedValue ? "This seems to be an error." : ""
            ),
            (action, path, incorrectValue, correctedValue) -> LOGGER.debug(
               Logging.CORE, "The comment on key {} does not match the spec. This may create a backup.", DOT_JOINER.join(path)
            )
         );
         config.save();
      }

      this.afterReload();
   }

   public void validateSpec(ModConfig config) {
      this.forEachValue(
         this.getValues().valueMap().values(),
         configValue -> {
            if (!configValue.getSpec().restartType().isValid(config.getType())) {
               throw new IllegalArgumentException(
                  "Configuration value "
                     + String.join(".", configValue.getPath())
                     + " defined in config "
                     + config.getFileName()
                     + " has restart of type "
                     + configValue.getSpec().restartType()
                     + " which cannot be used for configs of type "
                     + config.getType()
               );
            }
         }
      );
   }

   public boolean isLoaded() {
      return this.loadedConfig != null;
   }

   public UnmodifiableConfig getSpec() {
      return this.spec;
   }

   public UnmodifiableConfig getValues() {
      return this.values;
   }

   private void forEachValue(Iterable<Object> configValues, Consumer<ModConfigSpec.ConfigValue<?>> consumer) {
      configValues.forEach(value -> {
         if (value instanceof ModConfigSpec.ConfigValue<?> configValue) {
            consumer.accept(configValue);
         } else if (value instanceof Config innerConfig) {
            this.forEachValue(innerConfig.valueMap().values(), consumer);
         }
      });
   }

   public void afterReload() {
      this.resetCaches(ModConfigSpec.RestartType.NONE);
   }

   @Internal
   public void resetCaches(ModConfigSpec.RestartType restartType) {
      this.forEachValue(this.getValues().valueMap().values(), configValue -> {
         if (configValue.getSpec().restartType == restartType) {
            configValue.clearCache();
         }
      });
   }

   public void save() {
      Preconditions.checkNotNull(this.loadedConfig, "Cannot save config value without assigned Config object present");
      this.loadedConfig.save();
   }

   public boolean isCorrect(UnmodifiableCommentedConfig config) {
      LinkedList<String> parentPath = new LinkedList<>();
      return this.correct(this.spec, config, parentPath, Collections.unmodifiableList(parentPath), (a, b, c, d) -> {}, null, true) == 0;
   }

   public void correct(CommentedConfig config) {
      this.correct(config, (action, path, incorrectValue, correctedValue) -> {}, null);
   }

   public int correct(CommentedConfig config, CorrectionListener listener) {
      return this.correct(config, listener, null);
   }

   public int correct(CommentedConfig config, CorrectionListener listener, @Nullable CorrectionListener commentListener) {
      LinkedList<String> parentPath = new LinkedList<>();
      return this.correct(this.spec, config, parentPath, Collections.unmodifiableList(parentPath), listener, commentListener, false);
   }

   private int correct(
      UnmodifiableConfig spec,
      UnmodifiableCommentedConfig config,
      LinkedList<String> parentPath,
      List<String> parentPathUnmodifiable,
      CorrectionListener listener,
      @Nullable CorrectionListener commentListener,
      boolean dryRun
   ) {
      int count = 0;
      Map<String, Object> specMap = spec.valueMap();
      Map<String, Object> configMap = config.valueMap();

      for (Entry<String, Object> specEntry : specMap.entrySet()) {
         String key = specEntry.getKey();
         Object specValue = specEntry.getValue();
         Object configValue = configMap.get(key);
         CorrectionAction action = configValue == null ? CorrectionAction.ADD : CorrectionAction.REPLACE;
         parentPath.addLast(key);
         if (specValue instanceof Config) {
            if (configValue instanceof CommentedConfig) {
               count += this.correct((Config)specValue, (CommentedConfig)configValue, parentPath, parentPathUnmodifiable, listener, commentListener, dryRun);
               if (count > 0 && dryRun) {
                  return count;
               }
            } else {
               if (dryRun) {
                  return 1;
               }

               CommentedConfig newValue = ((CommentedConfig)config).createSubConfig();
               configMap.put(key, newValue);
               listener.onCorrect(action, parentPathUnmodifiable, configValue, newValue);
               count = ++count + this.correct((Config)specValue, newValue, parentPath, parentPathUnmodifiable, listener, commentListener, dryRun);
            }

            String newComment = this.levelComments.get(parentPath);
            String oldComment = config.getComment(key);
            if (!this.stringsMatchNormalizingNewLines(oldComment, newComment)) {
               if (commentListener != null) {
                  commentListener.onCorrect(action, parentPathUnmodifiable, oldComment, newComment);
               }

               if (dryRun) {
                  return 1;
               }

               ((CommentedConfig)config).setComment(key, newComment);
            }
         } else {
            ModConfigSpec.ValueSpec valueSpec = (ModConfigSpec.ValueSpec)specValue;
            if (!valueSpec.test(configValue)) {
               if (dryRun) {
                  return 1;
               }

               Object newValue = valueSpec.correct(configValue);
               configMap.put(key, newValue);
               listener.onCorrect(action, parentPathUnmodifiable, configValue, newValue);
               count++;
            }

            String oldComment = config.getComment(key);
            if (!this.stringsMatchNormalizingNewLines(oldComment, valueSpec.getComment())) {
               if (commentListener != null) {
                  commentListener.onCorrect(action, parentPathUnmodifiable, oldComment, valueSpec.getComment());
               }

               if (dryRun) {
                  return 1;
               }

               ((CommentedConfig)config).setComment(key, valueSpec.getComment());
            }
         }

         parentPath.removeLast();
      }

      Iterator<Entry<String, Object>> ittr = configMap.entrySet().iterator();

      while (ittr.hasNext()) {
         Entry<String, Object> entry = ittr.next();
         if (!specMap.containsKey(entry.getKey())) {
            if (dryRun) {
               return 1;
            }

            ittr.remove();
            parentPath.addLast(entry.getKey());
            listener.onCorrect(CorrectionAction.REMOVE, parentPathUnmodifiable, entry.getValue(), null);
            parentPath.removeLast();
            count++;
         }
      }

      return count;
   }

   private boolean stringsMatchNormalizingNewLines(@Nullable String string1, @Nullable String string2) {
      boolean blank1 = string1 == null || string1.isBlank();
      boolean blank2 = string2 == null || string2.isBlank();
      if (blank1 != blank2) {
         return false;
      } else {
         return blank1 && blank2 ? true : string1.replaceAll("\r\n", "\n").equals(string2.replaceAll("\r\n", "\n"));
      }
   }

   private static List<String> split(String path) {
      return Lists.newArrayList(DOT_SPLITTER.split(path));
   }

   public static class BooleanValue extends ModConfigSpec.ConfigValue<Boolean> implements BooleanSupplier {
      BooleanValue(ModConfigSpec.Builder parent, List<String> path, Supplier<Boolean> defaultSupplier) {
         super(parent, path, defaultSupplier);
      }

      @Override
      public boolean getAsBoolean() {
         return this.get();
      }

      public boolean isTrue() {
         return this.getAsBoolean();
      }

      public boolean isFalse() {
         return !this.getAsBoolean();
      }
   }

   public static class Builder {
      private final Config spec = Config.of(LinkedHashMap::new, InMemoryFormat.withUniversalSupport());
      private ModConfigSpec.BuilderContext context = new ModConfigSpec.BuilderContext();
      private final Map<List<String>, String> levelComments = new HashMap<>();
      private final Map<List<String>, String> levelTranslationKeys = new HashMap<>();
      private final List<String> currentPath = new ArrayList<>();
      private final List<ModConfigSpec.ConfigValue<?>> values = new ArrayList<>();

      public <T> ModConfigSpec.ConfigValue<T> define(String path, T defaultValue) {
         return this.define(ModConfigSpec.split(path), defaultValue);
      }

      public <T> ModConfigSpec.ConfigValue<T> define(List<String> path, T defaultValue) {
         return this.define(path, defaultValue, o -> o != null && defaultValue.getClass().isAssignableFrom(o.getClass()));
      }

      public <T> ModConfigSpec.ConfigValue<T> define(String path, T defaultValue, Predicate<Object> validator) {
         return this.define(ModConfigSpec.split(path), defaultValue, validator);
      }

      public <T> ModConfigSpec.ConfigValue<T> define(List<String> path, T defaultValue, Predicate<Object> validator) {
         Objects.requireNonNull(defaultValue, "Default value can not be null");
         return this.define(path, () -> defaultValue, validator);
      }

      public <T> ModConfigSpec.ConfigValue<T> define(String path, Supplier<T> defaultSupplier, Predicate<Object> validator) {
         return this.define(ModConfigSpec.split(path), defaultSupplier, validator);
      }

      public <T> ModConfigSpec.ConfigValue<T> define(List<String> path, Supplier<T> defaultSupplier, Predicate<Object> validator) {
         return this.define(path, defaultSupplier, validator, Object.class);
      }

      public <T> ModConfigSpec.ConfigValue<T> define(List<String> path, Supplier<T> defaultSupplier, Predicate<Object> validator, Class<?> clazz) {
         this.context.setClazz(clazz);
         return this.define(path, new ModConfigSpec.ValueSpec(defaultSupplier, validator, this.context, path), defaultSupplier);
      }

      public <T> ModConfigSpec.ConfigValue<T> define(List<String> path, ModConfigSpec.ValueSpec value, Supplier<T> defaultSupplier) {
         if (!this.currentPath.isEmpty()) {
            List<String> tmp = new ArrayList<>(this.currentPath.size() + path.size());
            tmp.addAll(this.currentPath);
            tmp.addAll(path);
            path = tmp;
         }

         this.spec.set(path, value);
         this.context = new ModConfigSpec.BuilderContext();
         return new ModConfigSpec.ConfigValue<>(this, path, defaultSupplier);
      }

      public <V extends Comparable<? super V>> ModConfigSpec.ConfigValue<V> defineInRange(String path, V defaultValue, V min, V max, Class<V> clazz) {
         return this.defineInRange(ModConfigSpec.split(path), defaultValue, min, max, clazz);
      }

      public <V extends Comparable<? super V>> ModConfigSpec.ConfigValue<V> defineInRange(List<String> path, V defaultValue, V min, V max, Class<V> clazz) {
         return this.defineInRange(path, () -> defaultValue, min, max, clazz);
      }

      public <V extends Comparable<? super V>> ModConfigSpec.ConfigValue<V> defineInRange(
         String path, Supplier<V> defaultSupplier, V min, V max, Class<V> clazz
      ) {
         return this.defineInRange(ModConfigSpec.split(path), defaultSupplier, min, max, clazz);
      }

      public <V extends Comparable<? super V>> ModConfigSpec.ConfigValue<V> defineInRange(
         List<String> path, Supplier<V> defaultSupplier, V min, V max, Class<V> clazz
      ) {
         ModConfigSpec.Range<V> range = new ModConfigSpec.Range<>(clazz, min, max);
         this.context.setRange(range);
         this.comment(" Default: " + defaultSupplier.get());
         this.comment(" Range: " + range);
         return this.define(path, defaultSupplier, range);
      }

      public <T> ModConfigSpec.ConfigValue<T> defineInList(String path, T defaultValue, Collection<? extends T> acceptableValues) {
         return this.defineInList(ModConfigSpec.split(path), defaultValue, acceptableValues);
      }

      public <T> ModConfigSpec.ConfigValue<T> defineInList(String path, Supplier<T> defaultSupplier, Collection<? extends T> acceptableValues) {
         return this.defineInList(ModConfigSpec.split(path), defaultSupplier, acceptableValues);
      }

      public <T> ModConfigSpec.ConfigValue<T> defineInList(List<String> path, T defaultValue, Collection<? extends T> acceptableValues) {
         return this.defineInList(path, () -> defaultValue, acceptableValues);
      }

      public <T> ModConfigSpec.ConfigValue<T> defineInList(List<String> path, Supplier<T> defaultSupplier, Collection<? extends T> acceptableValues) {
         return this.define(path, defaultSupplier, acceptableValues::contains);
      }

      @Deprecated
      public <T> ModConfigSpec.ConfigValue<List<? extends T>> defineList(String path, List<? extends T> defaultValue, Predicate<Object> elementValidator) {
         return this.defineList(ModConfigSpec.split(path), defaultValue, elementValidator);
      }

      public <T> ModConfigSpec.ConfigValue<List<? extends T>> defineList(
         String path, List<? extends T> defaultValue, Supplier<T> newElementSupplier, Predicate<Object> elementValidator
      ) {
         return this.defineList(ModConfigSpec.split(path), defaultValue, newElementSupplier, elementValidator);
      }

      @Deprecated
      public <T> ModConfigSpec.ConfigValue<List<? extends T>> defineList(
         String path, Supplier<List<? extends T>> defaultSupplier, Predicate<Object> elementValidator
      ) {
         return this.defineList(ModConfigSpec.split(path), defaultSupplier, elementValidator);
      }

      public <T> ModConfigSpec.ConfigValue<List<? extends T>> defineList(
         String path, Supplier<List<? extends T>> defaultSupplier, Supplier<T> newElementSupplier, Predicate<Object> elementValidator
      ) {
         return this.defineList(ModConfigSpec.split(path), defaultSupplier, newElementSupplier, elementValidator);
      }

      @Deprecated
      public <T> ModConfigSpec.ConfigValue<List<? extends T>> defineList(List<String> path, List<? extends T> defaultValue, Predicate<Object> elementValidator) {
         return this.defineList(path, () -> defaultValue, elementValidator);
      }

      public <T> ModConfigSpec.ConfigValue<List<? extends T>> defineList(
         List<String> path, List<? extends T> defaultValue, Supplier<T> newElementSupplier, Predicate<Object> elementValidator
      ) {
         return this.defineList(path, () -> defaultValue, newElementSupplier, elementValidator);
      }

      @Deprecated
      public <T> ModConfigSpec.ConfigValue<List<? extends T>> defineList(
         List<String> path, Supplier<List<? extends T>> defaultSupplier, Predicate<Object> elementValidator
      ) {
         return this.defineList(path, defaultSupplier, null, elementValidator);
      }

      public <T> ModConfigSpec.ConfigValue<List<? extends T>> defineList(
         List<String> path, Supplier<List<? extends T>> defaultSupplier, Supplier<T> newElementSupplier, Predicate<Object> elementValidator
      ) {
         return this.defineList(path, defaultSupplier, newElementSupplier, elementValidator, ModConfigSpec.ListValueSpec.NON_EMPTY);
      }

      @Deprecated
      public <T> ModConfigSpec.ConfigValue<List<? extends T>> defineListAllowEmpty(
         String path, List<? extends T> defaultValue, Predicate<Object> elementValidator
      ) {
         return this.defineListAllowEmpty(ModConfigSpec.split(path), defaultValue, elementValidator);
      }

      public <T> ModConfigSpec.ConfigValue<List<? extends T>> defineListAllowEmpty(
         String path, List<? extends T> defaultValue, Supplier<T> newElementSupplier, Predicate<Object> elementValidator
      ) {
         return this.defineListAllowEmpty(ModConfigSpec.split(path), defaultValue, newElementSupplier, elementValidator);
      }

      @Deprecated
      public <T> ModConfigSpec.ConfigValue<List<? extends T>> defineListAllowEmpty(
         String path, Supplier<List<? extends T>> defaultSupplier, Predicate<Object> elementValidator
      ) {
         return this.defineListAllowEmpty(ModConfigSpec.split(path), defaultSupplier, elementValidator);
      }

      public <T> ModConfigSpec.ConfigValue<List<? extends T>> defineListAllowEmpty(
         String path, Supplier<List<? extends T>> defaultSupplier, Supplier<T> newElementSupplier, Predicate<Object> elementValidator
      ) {
         return this.defineListAllowEmpty(ModConfigSpec.split(path), defaultSupplier, newElementSupplier, elementValidator);
      }

      @Deprecated
      public <T> ModConfigSpec.ConfigValue<List<? extends T>> defineListAllowEmpty(
         List<String> path, List<? extends T> defaultValue, Predicate<Object> elementValidator
      ) {
         return this.defineListAllowEmpty(path, () -> defaultValue, elementValidator);
      }

      public <T> ModConfigSpec.ConfigValue<List<? extends T>> defineListAllowEmpty(
         List<String> path, List<? extends T> defaultValue, Supplier<T> newElementSupplier, Predicate<Object> elementValidator
      ) {
         return this.defineListAllowEmpty(path, () -> defaultValue, newElementSupplier, elementValidator);
      }

      @Deprecated
      public <T> ModConfigSpec.ConfigValue<List<? extends T>> defineListAllowEmpty(
         List<String> path, Supplier<List<? extends T>> defaultSupplier, Predicate<Object> elementValidator
      ) {
         return this.defineListAllowEmpty(path, defaultSupplier, null, elementValidator);
      }

      public <T> ModConfigSpec.ConfigValue<List<? extends T>> defineListAllowEmpty(
         List<String> path, Supplier<List<? extends T>> defaultSupplier, Supplier<T> newElementSupplier, Predicate<Object> elementValidator
      ) {
         return this.defineList(path, defaultSupplier, newElementSupplier, elementValidator, null);
      }

      public <T> ModConfigSpec.ConfigValue<List<? extends T>> defineList(
         final List<String> path,
         Supplier<List<? extends T>> defaultSupplier,
         @Nullable Supplier<T> newElementSupplier,
         final Predicate<Object> elementValidator,
         @Nullable ModConfigSpec.Range<Integer> sizeRange
      ) {
         this.context.setClazz(List.class);
         return this.define(
            path,
            new ModConfigSpec.ListValueSpec(
               defaultSupplier,
               newElementSupplier,
               x -> x instanceof List && ((List)x).stream().allMatch(elementValidator),
               elementValidator,
               this.context,
               path,
               sizeRange
            ) {
               @Override
               public Object correct(Object value) {
                  if (value instanceof List && (this.getSizeRange() == null || this.getSizeRange().test(((List)value).size()))) {
                     List<?> list = Lists.newArrayList((List)value);
                     list.removeIf(elementValidator.negate());
                     if (list.isEmpty()) {
                        ModConfigSpec.LOGGER.debug(Logging.CORE, "List on key {} is deemed to need correction. It failed validation.", path.getLast());
                        return this.getDefault();
                     } else {
                        return list;
                     }
                  } else {
                     ModConfigSpec.LOGGER
                        .debug(Logging.CORE, "List on key {} is deemed to need correction, as it is null, not a list, or the wrong size.", path.getLast());
                     return this.getDefault();
                  }
               }
            },
            defaultSupplier
         );
      }

      public <V extends Enum<V>> ModConfigSpec.EnumValue<V> defineEnum(String path, V defaultValue) {
         return this.defineEnum(ModConfigSpec.split(path), defaultValue);
      }

      public <V extends Enum<V>> ModConfigSpec.EnumValue<V> defineEnum(String path, V defaultValue, EnumGetMethod converter) {
         return this.defineEnum(ModConfigSpec.split(path), defaultValue, converter);
      }

      public <V extends Enum<V>> ModConfigSpec.EnumValue<V> defineEnum(List<String> path, V defaultValue) {
         return this.defineEnum(path, defaultValue, defaultValue.getDeclaringClass().getEnumConstants());
      }

      public <V extends Enum<V>> ModConfigSpec.EnumValue<V> defineEnum(List<String> path, V defaultValue, EnumGetMethod converter) {
         return this.defineEnum(path, defaultValue, converter, defaultValue.getDeclaringClass().getEnumConstants());
      }

      public <V extends Enum<V>> ModConfigSpec.EnumValue<V> defineEnum(String path, V defaultValue, V... acceptableValues) {
         return this.defineEnum(ModConfigSpec.split(path), defaultValue, acceptableValues);
      }

      public <V extends Enum<V>> ModConfigSpec.EnumValue<V> defineEnum(String path, V defaultValue, EnumGetMethod converter, V... acceptableValues) {
         return this.defineEnum(ModConfigSpec.split(path), defaultValue, converter, acceptableValues);
      }

      public <V extends Enum<V>> ModConfigSpec.EnumValue<V> defineEnum(List<String> path, V defaultValue, V... acceptableValues) {
         return this.defineEnum(path, defaultValue, Arrays.asList(acceptableValues));
      }

      public <V extends Enum<V>> ModConfigSpec.EnumValue<V> defineEnum(List<String> path, V defaultValue, EnumGetMethod converter, V... acceptableValues) {
         return this.defineEnum(path, defaultValue, converter, Arrays.asList(acceptableValues));
      }

      public <V extends Enum<V>> ModConfigSpec.EnumValue<V> defineEnum(String path, V defaultValue, Collection<V> acceptableValues) {
         return this.defineEnum(ModConfigSpec.split(path), defaultValue, acceptableValues);
      }

      public <V extends Enum<V>> ModConfigSpec.EnumValue<V> defineEnum(String path, V defaultValue, EnumGetMethod converter, Collection<V> acceptableValues) {
         return this.defineEnum(ModConfigSpec.split(path), defaultValue, converter, acceptableValues);
      }

      public <V extends Enum<V>> ModConfigSpec.EnumValue<V> defineEnum(List<String> path, V defaultValue, Collection<V> acceptableValues) {
         return this.defineEnum(path, defaultValue, EnumGetMethod.NAME_IGNORECASE, acceptableValues);
      }

      public <V extends Enum<V>> ModConfigSpec.EnumValue<V> defineEnum(
         List<String> path, V defaultValue, EnumGetMethod converter, Collection<V> acceptableValues
      ) {
         return this.defineEnum(path, defaultValue, converter, obj -> {
            if (obj instanceof Enum) {
               return acceptableValues.contains(obj);
            } else if (obj == null) {
               return false;
            } else {
               try {
                  return acceptableValues.contains(converter.get(obj, defaultValue.getDeclaringClass()));
               } catch (ClassCastException | IllegalArgumentException var5) {
                  return false;
               }
            }
         });
      }

      public <V extends Enum<V>> ModConfigSpec.EnumValue<V> defineEnum(String path, V defaultValue, Predicate<Object> validator) {
         return this.defineEnum(ModConfigSpec.split(path), defaultValue, validator);
      }

      public <V extends Enum<V>> ModConfigSpec.EnumValue<V> defineEnum(String path, V defaultValue, EnumGetMethod converter, Predicate<Object> validator) {
         return this.defineEnum(ModConfigSpec.split(path), defaultValue, converter, validator);
      }

      public <V extends Enum<V>> ModConfigSpec.EnumValue<V> defineEnum(List<String> path, V defaultValue, Predicate<Object> validator) {
         return this.defineEnum(path, () -> defaultValue, validator, defaultValue.getDeclaringClass());
      }

      public <V extends Enum<V>> ModConfigSpec.EnumValue<V> defineEnum(List<String> path, V defaultValue, EnumGetMethod converter, Predicate<Object> validator) {
         return this.defineEnum(path, () -> defaultValue, converter, validator, defaultValue.getDeclaringClass());
      }

      public <V extends Enum<V>> ModConfigSpec.EnumValue<V> defineEnum(String path, Supplier<V> defaultSupplier, Predicate<Object> validator, Class<V> clazz) {
         return this.defineEnum(ModConfigSpec.split(path), defaultSupplier, validator, clazz);
      }

      public <V extends Enum<V>> ModConfigSpec.EnumValue<V> defineEnum(
         String path, Supplier<V> defaultSupplier, EnumGetMethod converter, Predicate<Object> validator, Class<V> clazz
      ) {
         return this.defineEnum(ModConfigSpec.split(path), defaultSupplier, converter, validator, clazz);
      }

      public <V extends Enum<V>> ModConfigSpec.EnumValue<V> defineEnum(
         List<String> path, Supplier<V> defaultSupplier, Predicate<Object> validator, Class<V> clazz
      ) {
         return this.defineEnum(path, defaultSupplier, EnumGetMethod.NAME_IGNORECASE, validator, clazz);
      }

      public <V extends Enum<V>> ModConfigSpec.EnumValue<V> defineEnum(
         List<String> path, Supplier<V> defaultSupplier, EnumGetMethod converter, Predicate<Object> validator, Class<V> clazz
      ) {
         this.context.setClazz(clazz);
         V[] allowedValues = (V[])clazz.getEnumConstants();
         this.comment("Allowed Values: " + Arrays.stream(allowedValues).filter(validator).map(Enum::name).collect(Collectors.joining(", ")));
         return new ModConfigSpec.EnumValue<>(
            this,
            this.define(path, new ModConfigSpec.ValueSpec(defaultSupplier, validator, this.context, path), defaultSupplier).getPath(),
            defaultSupplier,
            converter,
            clazz
         );
      }

      public ModConfigSpec.BooleanValue define(String path, boolean defaultValue) {
         return this.define(ModConfigSpec.split(path), defaultValue);
      }

      public ModConfigSpec.BooleanValue define(List<String> path, boolean defaultValue) {
         return this.define(path, () -> defaultValue);
      }

      public ModConfigSpec.BooleanValue define(String path, Supplier<Boolean> defaultSupplier) {
         return this.define(ModConfigSpec.split(path), defaultSupplier);
      }

      public ModConfigSpec.BooleanValue define(List<String> path, Supplier<Boolean> defaultSupplier) {
         return new ModConfigSpec.BooleanValue(
            this,
            this.define(
                  path,
                  defaultSupplier,
                  o -> !(o instanceof String) ? o instanceof Boolean : ((String)o).equalsIgnoreCase("true") || ((String)o).equalsIgnoreCase("false"),
                  Boolean.class
               )
               .getPath(),
            defaultSupplier
         );
      }

      public ModConfigSpec.DoubleValue defineInRange(String path, double defaultValue, double min, double max) {
         return this.defineInRange(ModConfigSpec.split(path), defaultValue, min, max);
      }

      public ModConfigSpec.DoubleValue defineInRange(List<String> path, double defaultValue, double min, double max) {
         return this.defineInRange(path, () -> defaultValue, min, max);
      }

      public ModConfigSpec.DoubleValue defineInRange(String path, Supplier<Double> defaultSupplier, double min, double max) {
         return this.defineInRange(ModConfigSpec.split(path), defaultSupplier, min, max);
      }

      public ModConfigSpec.DoubleValue defineInRange(List<String> path, Supplier<Double> defaultSupplier, double min, double max) {
         return new ModConfigSpec.DoubleValue(this, this.defineInRange(path, defaultSupplier, min, max, Double.class).getPath(), defaultSupplier);
      }

      public ModConfigSpec.IntValue defineInRange(String path, int defaultValue, int min, int max) {
         return this.defineInRange(ModConfigSpec.split(path), defaultValue, min, max);
      }

      public ModConfigSpec.IntValue defineInRange(List<String> path, int defaultValue, int min, int max) {
         return this.defineInRange(path, () -> defaultValue, min, max);
      }

      public ModConfigSpec.IntValue defineInRange(String path, Supplier<Integer> defaultSupplier, int min, int max) {
         return this.defineInRange(ModConfigSpec.split(path), defaultSupplier, min, max);
      }

      public ModConfigSpec.IntValue defineInRange(List<String> path, Supplier<Integer> defaultSupplier, int min, int max) {
         return new ModConfigSpec.IntValue(this, this.defineInRange(path, defaultSupplier, min, max, Integer.class).getPath(), defaultSupplier);
      }

      public ModConfigSpec.LongValue defineInRange(String path, long defaultValue, long min, long max) {
         return this.defineInRange(ModConfigSpec.split(path), defaultValue, min, max);
      }

      public ModConfigSpec.LongValue defineInRange(List<String> path, long defaultValue, long min, long max) {
         return this.defineInRange(path, () -> defaultValue, min, max);
      }

      public ModConfigSpec.LongValue defineInRange(String path, Supplier<Long> defaultSupplier, long min, long max) {
         return this.defineInRange(ModConfigSpec.split(path), defaultSupplier, min, max);
      }

      public ModConfigSpec.LongValue defineInRange(List<String> path, Supplier<Long> defaultSupplier, long min, long max) {
         return new ModConfigSpec.LongValue(this, this.defineInRange(path, defaultSupplier, min, max, Long.class).getPath(), defaultSupplier);
      }

      public ModConfigSpec.Builder comment(String comment) {
         this.context.addComment(comment);
         return this;
      }

      public ModConfigSpec.Builder comment(String... comment) {
         for (int i = 0; i < comment.length; i++) {
            Preconditions.checkNotNull(comment[i], "Comment string at " + i + " is null.");
         }

         for (String s : comment) {
            this.context.addComment(s);
         }

         return this;
      }

      public ModConfigSpec.Builder translation(String translationKey) {
         this.context.setTranslationKey(translationKey);
         return this;
      }

      public ModConfigSpec.Builder worldRestart() {
         this.context.worldRestart();
         return this;
      }

      public ModConfigSpec.Builder gameRestart() {
         this.context.gameRestart();
         return this;
      }

      public ModConfigSpec.Builder push(String path) {
         return this.push(ModConfigSpec.split(path));
      }

      public ModConfigSpec.Builder push(List<String> path) {
         this.currentPath.addAll(path);
         if (this.context.hasComment()) {
            this.levelComments.put(new ArrayList<>(this.currentPath), this.context.buildComment(path));
            this.context.clearComment();
         }

         if (this.context.getTranslationKey() != null) {
            this.levelTranslationKeys.put(new ArrayList<>(this.currentPath), this.context.getTranslationKey());
            this.context.setTranslationKey(null);
         }

         this.context.ensureEmpty();
         return this;
      }

      public ModConfigSpec.Builder pop() {
         return this.pop(1);
      }

      public ModConfigSpec.Builder pop(int count) {
         if (count > this.currentPath.size()) {
            throw new IllegalArgumentException("Attempted to pop " + count + " elements when we only had: " + this.currentPath);
         } else {
            for (int x = 0; x < count; x++) {
               this.currentPath.remove(this.currentPath.size() - 1);
            }

            return this;
         }
      }

      public <T> Pair<T, ModConfigSpec> configure(Function<ModConfigSpec.Builder, T> consumer) {
         T o = consumer.apply(this);
         return Pair.of(o, this.build());
      }

      public ModConfigSpec build() {
         this.context.ensureEmpty();
         Config valueCfg = Config.of(Config.getDefaultMapCreator(true, true), InMemoryFormat.withSupport(ModConfigSpec.ConfigValue.class::isAssignableFrom));
         this.values.forEach(v -> valueCfg.set(v.getPath(), v));
         ModConfigSpec ret = new ModConfigSpec(
            this.spec.unmodifiable(),
            valueCfg.unmodifiable(),
            Collections.unmodifiableMap(this.levelComments),
            Collections.unmodifiableMap(this.levelTranslationKeys)
         );
         this.values.forEach(v -> v.spec = ret);
         return ret;
      }
   }

   private static class BuilderContext {
      private final List<String> comment = new LinkedList<>();
      @Nullable
      private String langKey;
      @Nullable
      private ModConfigSpec.Range<?> range;
      private ModConfigSpec.RestartType restartType = ModConfigSpec.RestartType.NONE;
      @Nullable
      private Class<?> clazz;

      public void addComment(String value) {
         Preconditions.checkNotNull(value, "Passed in null value for comment");
         this.comment.add(value);
      }

      public void clearComment() {
         this.comment.clear();
      }

      public boolean hasComment() {
         return this.comment.size() > 0;
      }

      public String buildComment() {
         return this.buildComment(List.of("unknown", "unknown"));
      }

      public String buildComment(List<String> path) {
         if (this.comment.stream().allMatch(String::isBlank)) {
            if (FMLEnvironment.isProduction()) {
               ModConfigSpec.LOGGER
                  .warn(
                     Logging.CORE,
                     "Detected a comment that is all whitespace for config option {}, which causes obscure bugs in NeoForge's config system and will cause a crash in the future. Please report this to the mod author.",
                     ModConfigSpec.DOT_JOINER.join(path)
                  );
               return "A developer of this mod has defined this config option with a blank comment, which causes obscure bugs in NeoForge's config system and will cause a crash in the future. Please report this to the mod author.";
            } else {
               throw new IllegalStateException(
                  "Can not build comment for config option "
                     + ModConfigSpec.DOT_JOINER.join(path)
                     + " as it comprises entirely of blank lines/whitespace. This is not allowed as it causes a \"constantly correcting config\" bug with NightConfig in NeoForge's config system."
               );
            }
         } else {
            return ModConfigSpec.LINE_JOINER.join(this.comment);
         }
      }

      public void setTranslationKey(@Nullable String value) {
         this.langKey = value;
      }

      @Nullable
      public String getTranslationKey() {
         return this.langKey;
      }

      public <V extends Comparable<? super V>> void setRange(ModConfigSpec.Range<V> value) {
         this.range = value;
         this.setClazz(value.getClazz());
      }

      @Nullable
      public <V extends Comparable<? super V>> ModConfigSpec.Range<V> getRange() {
         return (ModConfigSpec.Range<V>)this.range;
      }

      public void worldRestart() {
         this.restartType = ModConfigSpec.RestartType.WORLD;
      }

      public void gameRestart() {
         this.restartType = ModConfigSpec.RestartType.GAME;
      }

      public ModConfigSpec.RestartType restartType() {
         return this.restartType;
      }

      public void setClazz(Class<?> clazz) {
         this.clazz = clazz;
      }

      @Nullable
      public Class<?> getClazz() {
         return this.clazz;
      }

      public void ensureEmpty() {
         this.validate(this.hasComment(), "Non-empty comment when empty expected");
         this.validate(this.langKey, "Non-null translation key when null expected");
         this.validate(this.range, "Non-null range when null expected");
         this.validate(this.restartType != ModConfigSpec.RestartType.NONE, "Dangling restart value set to " + this.restartType);
      }

      private void validate(@Nullable Object value, String message) {
         if (value != null) {
            throw new IllegalStateException(message);
         }
      }

      private void validate(boolean value, String message) {
         if (value) {
            throw new IllegalStateException(message);
         }
      }
   }

   public static class ConfigValue<T> implements Supplier<T> {
      private final ModConfigSpec.Builder parent;
      private final List<String> path;
      private final Supplier<T> defaultSupplier;
      @Nullable
      private T cachedValue = (T)null;
      @Nullable
      private ModConfigSpec spec;

      ConfigValue(ModConfigSpec.Builder parent, List<String> path, Supplier<T> defaultSupplier) {
         this.parent = parent;
         this.path = path;
         this.defaultSupplier = defaultSupplier;
         this.parent.values.add(this);
      }

      public List<String> getPath() {
         return Lists.newArrayList(this.path);
      }

      @Override
      public T get() {
         if (this.cachedValue == null) {
            this.cachedValue = this.getRaw();
         }

         return this.cachedValue;
      }

      public T getRaw() {
         Preconditions.checkNotNull(this.spec, "Cannot get config value before spec is built");
         ILoadedConfig loadedConfig = this.spec.loadedConfig;
         Preconditions.checkState(loadedConfig != null, "Cannot get config value before config is loaded.");
         return this.getRaw(loadedConfig.config(), this.path, this.defaultSupplier);
      }

      public T getRaw(Config config, List<String> path, Supplier<T> defaultSupplier) {
         return (T)config.getOrElse(path, defaultSupplier);
      }

      public T getDefault() {
         return this.defaultSupplier.get();
      }

      public ModConfigSpec.Builder next() {
         return this.parent;
      }

      public void save() {
         Preconditions.checkNotNull(this.spec, "Cannot save config value before spec is built");
         Preconditions.checkNotNull(this.spec.loadedConfig, "Cannot save config value without assigned Config object present");
         this.spec.save();
      }

      public void set(T value) {
         Preconditions.checkNotNull(this.spec, "Cannot set config value before spec is built");
         ILoadedConfig loadedConfig = this.spec.loadedConfig;
         Preconditions.checkNotNull(loadedConfig, "Cannot set config value without assigned Config object present");
         loadedConfig.config().set(this.path, value);
         if (this.getSpec().restartType == ModConfigSpec.RestartType.NONE) {
            this.cachedValue = value;
         }
      }

      public ModConfigSpec.ValueSpec getSpec() {
         return (ModConfigSpec.ValueSpec)this.parent.spec.get(this.path);
      }

      public void clearCache() {
         this.cachedValue = null;
      }
   }

   public static class DoubleValue extends ModConfigSpec.ConfigValue<Double> implements DoubleSupplier {
      DoubleValue(ModConfigSpec.Builder parent, List<String> path, Supplier<Double> defaultSupplier) {
         super(parent, path, defaultSupplier);
      }

      public Double getRaw(Config config, List<String> path, Supplier<Double> defaultSupplier) {
         Number n = (Number)config.get(path);
         return n == null ? defaultSupplier.get() : n.doubleValue();
      }

      @Override
      public double getAsDouble() {
         return this.get();
      }
   }

   public static class EnumValue<T extends Enum<T>> extends ModConfigSpec.ConfigValue<T> {
      private final EnumGetMethod converter;
      private final Class<T> clazz;

      EnumValue(ModConfigSpec.Builder parent, List<String> path, Supplier<T> defaultSupplier, EnumGetMethod converter, Class<T> clazz) {
         super(parent, path, defaultSupplier);
         this.converter = converter;
         this.clazz = clazz;
      }

      public T getRaw(Config config, List<String> path, Supplier<T> defaultSupplier) {
         return (T)config.getEnumOrElse(path, this.clazz, this.converter, defaultSupplier);
      }
   }

   public static class IntValue extends ModConfigSpec.ConfigValue<Integer> implements IntSupplier {
      IntValue(ModConfigSpec.Builder parent, List<String> path, Supplier<Integer> defaultSupplier) {
         super(parent, path, defaultSupplier);
      }

      public Integer getRaw(Config config, List<String> path, Supplier<Integer> defaultSupplier) {
         return config.getIntOrElse(path, () -> defaultSupplier.get());
      }

      @Override
      public int getAsInt() {
         return this.get();
      }
   }

   public static class ListValueSpec extends ModConfigSpec.ValueSpec {
      private static final ModConfigSpec.Range<Integer> MAX_ELEMENTS = ModConfigSpec.Range.of(0, Integer.MAX_VALUE);
      private static final ModConfigSpec.Range<Integer> NON_EMPTY = ModConfigSpec.Range.of(1, Integer.MAX_VALUE);
      @Nullable
      private final Supplier<?> newElementSupplier;
      @Nullable
      private final ModConfigSpec.Range<Integer> sizeRange;
      private final Predicate<Object> elementValidator;

      private ListValueSpec(
         Supplier<?> supplier,
         @Nullable Supplier<?> newElementSupplier,
         Predicate<Object> listValidator,
         Predicate<Object> elementValidator,
         ModConfigSpec.BuilderContext context,
         List<String> path,
         @Nullable ModConfigSpec.Range<Integer> sizeRange
      ) {
         super(supplier, listValidator, context, path);
         Objects.requireNonNull(elementValidator, "ElementValidator can not be null");
         this.newElementSupplier = newElementSupplier;
         this.elementValidator = elementValidator;
         this.sizeRange = Objects.requireNonNullElse(sizeRange, MAX_ELEMENTS);
      }

      @Nullable
      public Supplier<?> getNewElementSupplier() {
         return this.newElementSupplier;
      }

      public boolean testElement(Object value) {
         return this.elementValidator.test(value);
      }

      public ModConfigSpec.Range<Integer> getSizeRange() {
         return this.sizeRange;
      }
   }

   public static class LongValue extends ModConfigSpec.ConfigValue<Long> implements LongSupplier {
      LongValue(ModConfigSpec.Builder parent, List<String> path, Supplier<Long> defaultSupplier) {
         super(parent, path, defaultSupplier);
      }

      public Long getRaw(Config config, List<String> path, Supplier<Long> defaultSupplier) {
         return config.getLongOrElse(path, () -> defaultSupplier.get());
      }

      @Override
      public long getAsLong() {
         return this.get();
      }
   }

   public static class Range<V extends Comparable<? super V>> implements Predicate<Object> {
      private final Class<? extends V> clazz;
      private final V min;
      private final V max;

      private Range(Class<V> clazz, V min, V max) {
         this.clazz = clazz;
         this.min = min;
         this.max = max;
         if (min.compareTo(max) > 0) {
            throw new IllegalArgumentException("Range min must be less then max.");
         }
      }

      public static ModConfigSpec.Range<Integer> of(int min, int max) {
         return new ModConfigSpec.Range<>(Integer.class, min, max);
      }

      public Class<? extends V> getClazz() {
         return this.clazz;
      }

      public V getMin() {
         return this.min;
      }

      public V getMax() {
         return this.max;
      }

      private boolean isNumber(@Nullable Object other) {
         return Number.class.isAssignableFrom(this.clazz) && other instanceof Number;
      }

      @Override
      public boolean test(Object t) {
         if (this.isNumber(t)) {
            Number n = (Number)t;
            boolean result = ((Number)this.min).doubleValue() <= n.doubleValue() && n.doubleValue() <= ((Number)this.max).doubleValue();
            if (!result) {
               ModConfigSpec.LOGGER
                  .debug(
                     Logging.CORE,
                     "Range value {} is not within its bounds {}-{}",
                     n.doubleValue(),
                     ((Number)this.min).doubleValue(),
                     ((Number)this.max).doubleValue()
                  );
            }

            return result;
         } else if (!this.clazz.isInstance(t)) {
            return false;
         } else {
            V c = (V)this.clazz.cast(t);
            boolean result = c.compareTo(this.min) >= 0 && c.compareTo(this.max) <= 0;
            if (!result) {
               ModConfigSpec.LOGGER.debug(Logging.CORE, "Range value {} is not within its bounds {}-{}", c, this.min, this.max);
            }

            return result;
         }
      }

      public Object correct(@Nullable Object value, Object def) {
         if (this.isNumber(value)) {
            Number n = (Number)value;
            return n.doubleValue() < ((Number)this.min).doubleValue() ? this.min : (n.doubleValue() > ((Number)this.max).doubleValue() ? this.max : value);
         } else if (!this.clazz.isInstance(value)) {
            return def;
         } else {
            V c = (V)this.clazz.cast(value);
            return c.compareTo(this.min) < 0 ? this.min : (c.compareTo(this.max) > 0 ? this.max : value);
         }
      }

      @Override
      public String toString() {
         if (this.clazz == Integer.class) {
            if (this.max.equals(Integer.MAX_VALUE)) {
               return "> " + this.min;
            }

            if (this.min.equals(Integer.MIN_VALUE)) {
               return "< " + this.max;
            }
         }

         return this.min + " ~ " + this.max;
      }
   }

   public static enum RestartType {
      NONE(),
      WORLD(),
      GAME(Type.SERVER);

      private final Set<Type> invalidTypes = EnumSet.noneOf(Type.class);

      private RestartType(Type... invalidTypes) {
         this.invalidTypes.addAll(Arrays.asList(invalidTypes));
      }

      private boolean isValid(Type type) {
         return !this.invalidTypes.contains(type);
      }

      public ModConfigSpec.RestartType with(ModConfigSpec.RestartType other) {
         return other == NONE ? this : (other != GAME && this != GAME ? WORLD : GAME);
      }
   }

   public static class ValueSpec {
      @Nullable
      private final String comment;
      @Nullable
      private final String langKey;
      @Nullable
      private final ModConfigSpec.Range<?> range;
      @Nullable
      private final Class<?> clazz;
      private final Supplier<?> supplier;
      private final Predicate<Object> validator;
      private final ModConfigSpec.RestartType restartType;

      private ValueSpec(Supplier<?> supplier, Predicate<Object> validator, ModConfigSpec.BuilderContext context, List<String> path) {
         Objects.requireNonNull(supplier, "Default supplier can not be null");
         Objects.requireNonNull(validator, "Validator can not be null");
         this.comment = context.hasComment() ? context.buildComment(path) : null;
         this.langKey = context.getTranslationKey();
         this.range = context.getRange();
         this.restartType = context.restartType();
         this.clazz = context.getClazz();
         this.supplier = supplier;
         this.validator = validator;
      }

      @Nullable
      public String getComment() {
         return this.comment;
      }

      @Nullable
      public String getTranslationKey() {
         return this.langKey;
      }

      @Nullable
      public <V extends Comparable<? super V>> ModConfigSpec.Range<V> getRange() {
         return (ModConfigSpec.Range<V>)this.range;
      }

      public ModConfigSpec.RestartType restartType() {
         return this.restartType;
      }

      @Nullable
      public Class<?> getClazz() {
         return this.clazz;
      }

      public boolean test(@Nullable Object value) {
         return this.validator.test(value);
      }

      public Object correct(@Nullable Object value) {
         return this.range == null ? this.getDefault() : this.range.correct(value, this.getDefault());
      }

      public Object getDefault() {
         return this.supplier.get();
      }
   }
}
