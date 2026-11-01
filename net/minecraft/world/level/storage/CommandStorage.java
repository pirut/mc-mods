package net.minecraft.world.level.storage;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import org.jspecify.annotations.Nullable;

public class CommandStorage {
   private static final String ID_PREFIX = "command_storage_";
   private final Map<String, CommandStorage.Container> namespaces = new HashMap<>();
   private final DimensionDataStorage storage;

   public CommandStorage(DimensionDataStorage p_78035_) {
      this.storage = p_78035_;
   }

   public CompoundTag get(Identifier p_468248_) {
      CommandStorage.Container commandstorage$container = this.getContainer(p_468248_.getNamespace());
      return commandstorage$container != null ? commandstorage$container.get(p_468248_.getPath()) : new CompoundTag();
   }

   @Nullable
   private CommandStorage.Container getContainer(String p_401283_) {
      CommandStorage.Container commandstorage$container = this.namespaces.get(p_401283_);
      if (commandstorage$container != null) {
         return commandstorage$container;
      } else {
         CommandStorage.Container commandstorage$container1 = this.storage.get(CommandStorage.Container.type(p_401283_));
         if (commandstorage$container1 != null) {
            this.namespaces.put(p_401283_, commandstorage$container1);
         }

         return commandstorage$container1;
      }
   }

   private CommandStorage.Container getOrCreateContainer(String p_401189_) {
      CommandStorage.Container commandstorage$container = this.namespaces.get(p_401189_);
      if (commandstorage$container != null) {
         return commandstorage$container;
      } else {
         CommandStorage.Container commandstorage$container1 = this.storage.computeIfAbsent(CommandStorage.Container.type(p_401189_));
         this.namespaces.put(p_401189_, commandstorage$container1);
         return commandstorage$container1;
      }
   }

   public void set(Identifier p_466927_, CompoundTag p_78048_) {
      this.getOrCreateContainer(p_466927_.getNamespace()).put(p_466927_.getPath(), p_78048_);
   }

   public Stream<Identifier> keys() {
      return this.namespaces.entrySet().stream().flatMap(p_164841_ -> p_164841_.getValue().getKeys(p_164841_.getKey()));
   }

   static String createId(String p_78038_) {
      return "command_storage_" + p_78038_;
   }

   static class Container extends SavedData {
      public static final Codec<CommandStorage.Container> CODEC = RecordCodecBuilder.create(
         p_400967_ -> p_400967_.group(
               Codec.unboundedMap(ExtraCodecs.RESOURCE_PATH_CODEC, CompoundTag.CODEC).fieldOf("contents").forGetter(p_400968_ -> p_400968_.storage)
            )
            .apply(p_400967_, CommandStorage.Container::new)
      );
      private final Map<String, CompoundTag> storage;

      private Container(Map<String, CompoundTag> p_401367_) {
         this.storage = new HashMap<>(p_401367_);
      }

      private Container() {
         this(new HashMap<>());
      }

      public static SavedDataType<CommandStorage.Container> type(String p_401227_) {
         return new SavedDataType<>(CommandStorage.createId(p_401227_), CommandStorage.Container::new, CODEC, DataFixTypes.SAVED_DATA_COMMAND_STORAGE);
      }

      public CompoundTag get(String p_78059_) {
         CompoundTag compoundtag = this.storage.get(p_78059_);
         return compoundtag != null ? compoundtag : new CompoundTag();
      }

      public void put(String p_78064_, CompoundTag p_78065_) {
         if (p_78065_.isEmpty()) {
            this.storage.remove(p_78064_);
         } else {
            this.storage.put(p_78064_, p_78065_);
         }

         this.setDirty();
      }

      public Stream<Identifier> getKeys(String p_78073_) {
         return this.storage.keySet().stream().map(p_466773_ -> Identifier.fromNamespaceAndPath(p_78073_, p_466773_));
      }
   }
}
