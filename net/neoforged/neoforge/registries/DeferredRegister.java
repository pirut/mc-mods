package net.neoforged.neoforge.registries;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.Map.Entry;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import org.jspecify.annotations.Nullable;

public class DeferredRegister<T> {
   private final ResourceKey<? extends Registry<T>> registryKey;
   private final String namespace;
   private final Map<DeferredHolder<T, ? extends T>, Supplier<? extends T>> entries = new LinkedHashMap<>();
   private final Set<DeferredHolder<T, ? extends T>> entriesView = Collections.unmodifiableSet(this.entries.keySet());
   private final Map<Identifier, Identifier> aliases = new HashMap<>();
   @Nullable
   private Registry<T> customRegistry;
   @Nullable
   private DeferredRegister.RegistryHolder<T> registryHolder;
   private boolean seenRegisterEvent = false;
   private boolean seenNewRegistryEvent = false;
   private boolean registeredEventBus = false;

   public static <T> DeferredRegister<T> create(Registry<T> registry, String namespace) {
      return new DeferredRegister<>(registry.key(), namespace);
   }

   public static <T> DeferredRegister<T> create(ResourceKey<? extends Registry<T>> key, String namespace) {
      return new DeferredRegister<>(key, namespace);
   }

   public static <B> DeferredRegister<B> create(Identifier registryName, String modid) {
      return new DeferredRegister(ResourceKey.createRegistryKey(registryName), modid);
   }

   public static DeferredRegister.Items createItems(String modid) {
      return new DeferredRegister.Items(modid);
   }

   public static DeferredRegister.Blocks createBlocks(String modid) {
      return new DeferredRegister.Blocks(modid);
   }

   public static DeferredRegister.DataComponents createDataComponents(ResourceKey<Registry<DataComponentType<?>>> registryKey, String modid) {
      return new DeferredRegister.DataComponents(registryKey, modid);
   }

   public static DeferredRegister.Entities createEntities(String modid) {
      return new DeferredRegister.Entities(modid);
   }

   protected DeferredRegister(ResourceKey<? extends Registry<T>> registryKey, String namespace) {
      this.registryKey = Objects.requireNonNull(registryKey);
      this.namespace = Objects.requireNonNull(namespace);
   }

   public <I extends T> DeferredHolder<T, I> register(String name, Supplier<? extends I> sup) {
      return this.register(name, key -> sup.get());
   }

   public <I extends T> DeferredHolder<T, I> register(String name, Function<Identifier, ? extends I> func) {
      if (this.seenRegisterEvent) {
         throw new IllegalStateException("Cannot register new entries to DeferredRegister after RegisterEvent has been fired.");
      } else {
         Objects.requireNonNull(name);
         Objects.requireNonNull(func);
         Identifier key = Identifier.fromNamespaceAndPath(this.namespace, name);
         DeferredHolder<T, I> ret = this.createHolder(this.registryKey, key);
         if (this.entries.putIfAbsent(ret, () -> (T)func.apply(key)) != null) {
            throw new IllegalArgumentException("Duplicate registration " + name);
         } else {
            return ret;
         }
      }
   }

   protected <I extends T> DeferredHolder<T, I> createHolder(ResourceKey<? extends Registry<T>> registryKey, Identifier key) {
      return DeferredHolder.create(registryKey, key);
   }

   public Registry<T> makeRegistry(Consumer<RegistryBuilder<T>> consumer) {
      return this.makeRegistry(this.registryKey.identifier(), consumer);
   }

   public Supplier<Registry<T>> getRegistry() {
      if (this.registryHolder == null) {
         this.registryHolder = new DeferredRegister.RegistryHolder<>(this.registryKey);
      }

      return this.registryHolder;
   }

   public TagKey<T> createTagKey(String path) {
      Objects.requireNonNull(path);
      return this.createTagKey(Identifier.fromNamespaceAndPath(this.namespace, path));
   }

   public TagKey<T> createTagKey(Identifier location) {
      Objects.requireNonNull(location);
      return TagKey.create(this.registryKey, location);
   }

   public void addAlias(Identifier from, Identifier to) {
      if (this.seenRegisterEvent) {
         throw new IllegalStateException("Cannot add aliases to DeferredRegister after RegisterEvent has been fired.");
      } else {
         this.aliases.put(from, to);
      }
   }

   public void register(IEventBus bus) {
      if (this.registeredEventBus) {
         throw new IllegalStateException("Cannot register DeferredRegister to more than one event bus.");
      } else {
         this.registeredEventBus = true;
         bus.addListener(this::addEntries);
         bus.addListener(this::addRegistry);
      }
   }

   public Collection<DeferredHolder<T, ? extends T>> getEntries() {
      return this.entriesView;
   }

   public ResourceKey<? extends Registry<T>> getRegistryKey() {
      return this.registryKey;
   }

   public Identifier getRegistryName() {
      return this.registryKey.identifier();
   }

   public String getNamespace() {
      return this.namespace;
   }

   private Registry<T> makeRegistry(Identifier registryName, Consumer<RegistryBuilder<T>> consumer) {
      if (registryName == null) {
         throw new IllegalStateException("Cannot create a registry without specifying a registry name");
      } else if (BuiltInRegistries.REGISTRY.containsKey(registryName) || this.customRegistry != null) {
         throw new IllegalStateException("Cannot create a registry that already exists - " + this.registryKey);
      } else if (this.seenNewRegistryEvent) {
         throw new IllegalStateException("Cannot create a registry after NewRegistryEvent was fired");
      } else {
         RegistryBuilder<T> registryBuilder = new RegistryBuilder<>(this.registryKey);
         consumer.accept(registryBuilder);
         this.customRegistry = registryBuilder.create();
         this.registryHolder = new DeferredRegister.RegistryHolder<>(this.registryKey);
         this.registryHolder.registry = this.customRegistry;
         return this.customRegistry;
      }
   }

   private void addEntries(RegisterEvent event) {
      if (event.getRegistryKey().equals(this.registryKey)) {
         this.seenRegisterEvent = true;
         Registry<T> registry = event.getRegistry(this.registryKey);
         this.aliases.forEach(registry::addAlias);

         for (Entry<DeferredHolder<T, ? extends T>, Supplier<? extends T>> e : this.entries.entrySet()) {
            event.register(this.registryKey, e.getKey().getId(), () -> (T)e.getValue().get());
            e.getKey().bind(false);
         }
      }
   }

   private void addRegistry(NewRegistryEvent event) {
      this.seenNewRegistryEvent = true;
      if (this.customRegistry != null) {
         event.register(this.customRegistry);
      }
   }

   public static class Blocks extends DeferredRegister<Block> {
      protected Blocks(String namespace) {
         super(Registries.BLOCK, namespace);
      }

      public <B extends Block> DeferredBlock<B> register(String name, Function<Identifier, ? extends B> func) {
         return (DeferredBlock<B>)super.<B>register(name, func);
      }

      public <B extends Block> DeferredBlock<B> register(String name, Supplier<? extends B> sup) {
         return this.register(name, key -> sup.get());
      }

      @Deprecated(
         since = "1.21.10",
         forRemoval = true
      )
      public <B extends Block> DeferredBlock<B> registerBlock(
         String name, Function<BlockBehaviour.Properties, ? extends B> func, BlockBehaviour.Properties props
      ) {
         return this.registerBlock(name, func, () -> props);
      }

      public <B extends Block> DeferredBlock<B> registerBlock(
         String name, Function<BlockBehaviour.Properties, ? extends B> func, Supplier<BlockBehaviour.Properties> properties
      ) {
         return this.register(name, key -> func.apply(properties.get().setId(ResourceKey.create(Registries.BLOCK, key))));
      }

      public <B extends Block> DeferredBlock<B> registerBlock(
         String name, Function<BlockBehaviour.Properties, ? extends B> func, UnaryOperator<BlockBehaviour.Properties> properties
      ) {
         return this.registerBlock(name, func, () -> properties.apply(BlockBehaviour.Properties.of()));
      }

      public <B extends Block> DeferredBlock<B> registerBlock(String name, Function<BlockBehaviour.Properties, ? extends B> func) {
         return this.registerBlock(name, func, UnaryOperator.identity());
      }

      @Deprecated(
         since = "1.21.10",
         forRemoval = true
      )
      public DeferredBlock<Block> registerSimpleBlock(String name, BlockBehaviour.Properties props) {
         return this.registerBlock(name, Block::new, () -> props);
      }

      public DeferredBlock<Block> registerSimpleBlock(String name, Supplier<BlockBehaviour.Properties> properties) {
         return this.registerBlock(name, Block::new, properties);
      }

      public DeferredBlock<Block> registerSimpleBlock(String name, UnaryOperator<BlockBehaviour.Properties> properties) {
         return this.registerBlock(name, Block::new, properties);
      }

      public DeferredBlock<Block> registerSimpleBlock(String name) {
         return this.registerSimpleBlock(name, UnaryOperator.identity());
      }

      protected <I extends Block> DeferredBlock<I> createHolder(ResourceKey<? extends Registry<Block>> registryKey, Identifier key) {
         return DeferredBlock.createBlock(ResourceKey.create(registryKey, key));
      }
   }

   public static class DataComponents extends DeferredRegister<DataComponentType<?>> {
      protected DataComponents(ResourceKey<Registry<DataComponentType<?>>> registryKey, String namespace) {
         super(registryKey, namespace);
      }

      public <D> DeferredHolder<DataComponentType<?>, DataComponentType<D>> registerComponentType(
         String name, UnaryOperator<DataComponentType.Builder<D>> builder
      ) {
         return this.register(name, () -> builder.apply(DataComponentType.builder()).build());
      }
   }

   public static class Entities extends DeferredRegister<EntityType<?>> {
      protected Entities(String namespace) {
         super(Registries.ENTITY_TYPE, namespace);
      }

      public <E extends Entity> DeferredHolder<EntityType<?>, EntityType<E>> registerEntityType(
         String name, EntityType.EntityFactory<E> factory, MobCategory category
      ) {
         return this.registerEntityType(name, factory, category, UnaryOperator.identity());
      }

      public <E extends Entity> DeferredHolder<EntityType<?>, EntityType<E>> registerEntityType(
         String name, EntityType.EntityFactory<E> factory, MobCategory category, UnaryOperator<EntityType.Builder<E>> builder
      ) {
         return this.register(name, key -> builder.apply(EntityType.Builder.of(factory, category)).build(ResourceKey.create(Registries.ENTITY_TYPE, key)));
      }
   }

   public static class Items extends DeferredRegister<Item> {
      protected Items(String namespace) {
         super(Registries.ITEM, namespace);
      }

      public <I extends Item> DeferredItem<I> register(String name, Function<Identifier, ? extends I> func) {
         return (DeferredItem<I>)super.<I>register(name, func);
      }

      public <I extends Item> DeferredItem<I> register(String name, Supplier<? extends I> sup) {
         return this.register(name, key -> sup.get());
      }

      @Deprecated(
         since = "1.21.10",
         forRemoval = true
      )
      public DeferredItem<BlockItem> registerSimpleBlockItem(String name, Supplier<? extends Block> block, Item.Properties properties) {
         return this.registerSimpleBlockItem(name, block, () -> properties);
      }

      public DeferredItem<BlockItem> registerSimpleBlockItem(String name, Supplier<? extends Block> block, Supplier<Item.Properties> properties) {
         return this.registerItem(name, props -> new BlockItem(block.get(), props), () -> properties.get().useBlockDescriptionPrefix());
      }

      public DeferredItem<BlockItem> registerSimpleBlockItem(String name, Supplier<? extends Block> block, UnaryOperator<Item.Properties> properties) {
         return this.registerSimpleBlockItem(name, block, () -> properties.apply(new Item.Properties()));
      }

      public DeferredItem<BlockItem> registerSimpleBlockItem(String name, Supplier<? extends Block> block) {
         return this.registerSimpleBlockItem(name, block, UnaryOperator.identity());
      }

      @Deprecated(
         since = "1.21.10",
         forRemoval = true
      )
      public DeferredItem<BlockItem> registerSimpleBlockItem(Holder<Block> block, Item.Properties properties) {
         return this.registerSimpleBlockItem(block, () -> properties);
      }

      public DeferredItem<BlockItem> registerSimpleBlockItem(Holder<Block> block, Supplier<Item.Properties> properties) {
         return this.registerSimpleBlockItem(block.unwrapKey().orElseThrow().identifier().getPath(), block::value, properties);
      }

      public DeferredItem<BlockItem> registerSimpleBlockItem(Holder<Block> block, UnaryOperator<Item.Properties> properties) {
         return this.registerSimpleBlockItem(block, () -> properties.apply(new Item.Properties()));
      }

      public DeferredItem<BlockItem> registerSimpleBlockItem(Holder<Block> block) {
         return this.registerSimpleBlockItem(block, UnaryOperator.identity());
      }

      @Deprecated(
         since = "1.21.10",
         forRemoval = true
      )
      public <I extends Item> DeferredItem<I> registerItem(String name, Function<Item.Properties, ? extends I> func, Item.Properties props) {
         return this.registerItem(name, func, () -> props);
      }

      public <I extends Item> DeferredItem<I> registerItem(String name, Function<Item.Properties, ? extends I> func, Supplier<Item.Properties> properties) {
         return this.register(name, key -> func.apply(properties.get().setId(ResourceKey.create(Registries.ITEM, key))));
      }

      public <I extends Item> DeferredItem<I> registerItem(String name, Function<Item.Properties, ? extends I> func, UnaryOperator<Item.Properties> properties) {
         return this.registerItem(name, func, () -> properties.apply(new Item.Properties()));
      }

      public <I extends Item> DeferredItem<I> registerItem(String name, Function<Item.Properties, ? extends I> func) {
         return this.registerItem(name, func, UnaryOperator.identity());
      }

      @Deprecated(
         since = "1.21.10",
         forRemoval = true
      )
      public DeferredItem<Item> registerSimpleItem(String name, Item.Properties props) {
         return this.registerItem(name, Item::new, props);
      }

      public DeferredItem<Item> registerSimpleItem(String name, Supplier<Item.Properties> properties) {
         return this.registerItem(name, Item::new, properties);
      }

      public DeferredItem<Item> registerSimpleItem(String name, UnaryOperator<Item.Properties> properties) {
         return this.registerItem(name, Item::new, properties);
      }

      public DeferredItem<Item> registerSimpleItem(String name) {
         return this.registerItem(name, Item::new, UnaryOperator.identity());
      }

      protected <I extends Item> DeferredItem<I> createHolder(ResourceKey<? extends Registry<Item>> registryKey, Identifier key) {
         return DeferredItem.createItem(ResourceKey.create(registryKey, key));
      }
   }

   private static class RegistryHolder<V> implements Supplier<Registry<V>> {
      private final ResourceKey<? extends Registry<V>> registryKey;
      private Registry<V> registry = null;

      private RegistryHolder(ResourceKey<? extends Registry<V>> registryKey) {
         this.registryKey = registryKey;
      }

      @Nullable
      public Registry<V> get() {
         if (this.registry == null) {
            this.registry = (Registry<V>)BuiltInRegistries.REGISTRY.getValueOrThrow(this.registryKey);
         }

         return this.registry;
      }
   }
}
