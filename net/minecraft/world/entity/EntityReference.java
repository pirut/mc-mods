package net.minecraft.world.entity;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.players.OldUsersConverter;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.entity.UUIDLookup;
import net.minecraft.world.level.entity.UniquelyIdentifyable;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

public final class EntityReference<StoredEntityType extends UniquelyIdentifyable> {
   private static final Codec<? extends EntityReference<?>> CODEC = UUIDUtil.CODEC.xmap(EntityReference::new, EntityReference::getUUID);
   private static final StreamCodec<ByteBuf, ? extends EntityReference<?>> STREAM_CODEC = UUIDUtil.STREAM_CODEC
      .map(EntityReference::new, EntityReference::getUUID);
   private Either<UUID, StoredEntityType> entity;

   public static <Type extends UniquelyIdentifyable> Codec<EntityReference<Type>> codec() {
      return (Codec<EntityReference<Type>>)CODEC;
   }

   public static <Type extends UniquelyIdentifyable> StreamCodec<ByteBuf, EntityReference<Type>> streamCodec() {
      return (StreamCodec<ByteBuf, EntityReference<Type>>)STREAM_CODEC;
   }

   private EntityReference(StoredEntityType p_394384_) {
      this.entity = Either.right(p_394384_);
   }

   private EntityReference(UUID p_394483_) {
      this.entity = Either.left(p_394483_);
   }

   @Nullable
   public static <T extends UniquelyIdentifyable> EntityReference<T> of(@Nullable T p_440249_) {
      return p_440249_ != null ? new EntityReference<>(p_440249_) : null;
   }

   public static <T extends UniquelyIdentifyable> EntityReference<T> of(UUID p_440648_) {
      return new EntityReference<>(p_440648_);
   }

   public UUID getUUID() {
      return (UUID)this.entity.map(p_394562_ -> p_394562_, UniquelyIdentifyable::getUUID);
   }

   @Nullable
   public StoredEntityType getEntity(UUIDLookup<? extends UniquelyIdentifyable> p_394375_, Class<StoredEntityType> p_394097_) {
      Optional<StoredEntityType> optional = this.entity.right();
      if (optional.isPresent()) {
         StoredEntityType storedentitytype = optional.get();
         if (!storedentitytype.isRemoved()) {
            return storedentitytype;
         }

         this.entity = Either.left(storedentitytype.getUUID());
      }

      Optional<UUID> optional1 = this.entity.left();
      if (optional1.isPresent()) {
         StoredEntityType storedentitytype1 = this.resolve(p_394375_.lookup(optional1.get()), p_394097_);
         if (storedentitytype1 != null && !storedentitytype1.isRemoved()) {
            this.entity = Either.right(storedentitytype1);
            return storedentitytype1;
         }
      }

      return null;
   }

   @Nullable
   public StoredEntityType getEntity(Level p_439083_, Class<StoredEntityType> p_438991_) {
      return Player.class.isAssignableFrom(p_438991_)
         ? this.getEntity(p_439083_::getPlayerInAnyDimension, p_438991_)
         : this.getEntity(p_439083_::getEntityInAnyDimension, p_438991_);
   }

   @Nullable
   private StoredEntityType resolve(@Nullable UniquelyIdentifyable p_394069_, Class<StoredEntityType> p_394131_) {
      return p_394069_ != null && p_394131_.isAssignableFrom(p_394069_.getClass()) ? p_394131_.cast(p_394069_) : null;
   }

   public boolean matches(StoredEntityType p_394272_) {
      return this.getUUID().equals(p_394272_.getUUID());
   }

   public void store(ValueOutput p_422692_, String p_393486_) {
      p_422692_.store(p_393486_, UUIDUtil.CODEC, this.getUUID());
   }

   public static void store(@Nullable EntityReference<?> p_423520_, ValueOutput p_423510_, String p_423668_) {
      if (p_423520_ != null) {
         p_423520_.store(p_423510_, p_423668_);
      }
   }

   @Nullable
   public static <StoredEntityType extends UniquelyIdentifyable> StoredEntityType get(
      @Nullable EntityReference<StoredEntityType> p_393467_, Level p_440390_, Class<StoredEntityType> p_394473_
   ) {
      return p_393467_ != null ? p_393467_.getEntity(p_440390_, p_394473_) : null;
   }

   @Nullable
   public static Entity getEntity(@Nullable EntityReference<Entity> p_440206_, Level p_440128_) {
      return get(p_440206_, p_440128_, Entity.class);
   }

   @Nullable
   public static LivingEntity getLivingEntity(@Nullable EntityReference<LivingEntity> p_440105_, Level p_439674_) {
      return get(p_440105_, p_439674_, LivingEntity.class);
   }

   @Nullable
   public static Player getPlayer(@Nullable EntityReference<Player> p_440320_, Level p_439792_) {
      return get(p_440320_, p_439792_, Player.class);
   }

   @Nullable
   public static <StoredEntityType extends UniquelyIdentifyable> EntityReference<StoredEntityType> read(ValueInput p_421654_, String p_394012_) {
      return p_421654_.<EntityReference<StoredEntityType>>read(p_394012_, codec()).orElse(null);
   }

   @Nullable
   public static <StoredEntityType extends UniquelyIdentifyable> EntityReference<StoredEntityType> readWithOldOwnerConversion(
      ValueInput p_422383_, String p_394543_, Level p_393661_
   ) {
      Optional<UUID> optional = p_422383_.read(p_394543_, UUIDUtil.CODEC);
      return optional.isPresent()
         ? of(optional.get())
         : p_422383_.getString(p_394543_)
            .map(p_409192_ -> OldUsersConverter.convertMobOwnerIfNecessary(p_393661_.getServer(), p_409192_))
            .map(EntityReference::new)
            .orElse(null);
   }

   @Override
   public boolean equals(Object p_423529_) {
      return p_423529_ == this ? true : p_423529_ instanceof EntityReference<?> entityreference && this.getUUID().equals(entityreference.getUUID());
   }

   @Override
   public int hashCode() {
      return this.getUUID().hashCode();
   }
}
