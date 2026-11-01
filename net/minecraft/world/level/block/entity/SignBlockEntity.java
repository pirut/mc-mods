package net.minecraft.world.level.block.entity;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.DynamicOps;
import java.util.List;
import java.util.UUID;
import java.util.function.UnaryOperator;
import javax.annotation.Nullable;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.Style;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.network.FilteredText;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SignBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;

public class SignBlockEntity extends BlockEntity {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final int MAX_TEXT_LINE_WIDTH = 90;
   private static final int TEXT_LINE_HEIGHT = 10;
   @Nullable
   private UUID playerWhoMayEdit;
   private SignText frontText = this.createDefaultSignText();
   private SignText backText = this.createDefaultSignText();
   private boolean isWaxed;

   public SignBlockEntity(BlockPos p_155700_, BlockState p_155701_) {
      this(BlockEntityType.SIGN, p_155700_, p_155701_);
   }

   public SignBlockEntity(BlockEntityType p_249609_, BlockPos p_248914_, BlockState p_249550_) {
      super(p_249609_, p_248914_, p_249550_);
   }

   protected SignText createDefaultSignText() {
      return new SignText();
   }

   public boolean isFacingFrontText(Player p_277382_) {
      if (this.getBlockState().getBlock() instanceof SignBlock $$1) {
         Vec3 $$2 = $$1.getSignHitboxCenterPosition(this.getBlockState());
         double $$3 = p_277382_.getX() - (this.getBlockPos().getX() + $$2.x);
         double $$4 = p_277382_.getZ() - (this.getBlockPos().getZ() + $$2.z);
         float $$5 = $$1.getYRotationDegrees(this.getBlockState());
         float $$6 = (float)(Mth.atan2($$4, $$3) * 180.0F / (float)Math.PI) - 90.0F;
         return Mth.degreesDifferenceAbs($$5, $$6) <= 90.0F;
      } else {
         return false;
      }
   }

   public SignText getText(boolean p_277918_) {
      return p_277918_ ? this.frontText : this.backText;
   }

   public SignText getFrontText() {
      return this.frontText;
   }

   public SignText getBackText() {
      return this.backText;
   }

   public int getTextLineHeight() {
      return 10;
   }

   public int getMaxTextLineWidth() {
      return 90;
   }

   @Override
   protected void saveAdditional(CompoundTag p_187515_, HolderLookup.Provider p_324471_) {
      super.saveAdditional(p_187515_, p_324471_);
      DynamicOps<Tag> $$2 = p_324471_.createSerializationContext(NbtOps.INSTANCE);
      SignText.DIRECT_CODEC.encodeStart($$2, this.frontText).resultOrPartial(LOGGER::error).ifPresent(p_277417_ -> p_187515_.put("front_text", p_277417_));
      SignText.DIRECT_CODEC.encodeStart($$2, this.backText).resultOrPartial(LOGGER::error).ifPresent(p_277389_ -> p_187515_.put("back_text", p_277389_));
      p_187515_.putBoolean("is_waxed", this.isWaxed);
   }

   @Override
   protected void loadAdditional(CompoundTag p_155716_, HolderLookup.Provider p_324351_) {
      super.loadAdditional(p_155716_, p_324351_);
      DynamicOps<Tag> $$2 = p_324351_.createSerializationContext(NbtOps.INSTANCE);
      if (p_155716_.contains("front_text")) {
         SignText.DIRECT_CODEC
            .parse($$2, p_155716_.getCompound("front_text"))
            .resultOrPartial(LOGGER::error)
            .ifPresent(p_278212_ -> this.frontText = this.loadLines(p_278212_));
      }

      if (p_155716_.contains("back_text")) {
         SignText.DIRECT_CODEC
            .parse($$2, p_155716_.getCompound("back_text"))
            .resultOrPartial(LOGGER::error)
            .ifPresent(p_278213_ -> this.backText = this.loadLines(p_278213_));
      }

      this.isWaxed = p_155716_.getBoolean("is_waxed");
   }

   private SignText loadLines(SignText p_278305_) {
      for (int $$1 = 0; $$1 < 4; $$1++) {
         Component $$2 = this.loadLine(p_278305_.getMessage($$1, false));
         Component $$3 = this.loadLine(p_278305_.getMessage($$1, true));
         p_278305_ = p_278305_.setMessage($$1, $$2, $$3);
      }

      return p_278305_;
   }

   private Component loadLine(Component p_278307_) {
      if (this.level instanceof ServerLevel $$1) {
         try {
            return ComponentUtils.updateForEntity(createCommandSourceStack(null, $$1, this.worldPosition), p_278307_, null, 0);
         } catch (CommandSyntaxException var4) {
         }
      }

      return p_278307_;
   }

   public void updateSignText(Player p_278048_, boolean p_278103_, List<FilteredText> p_277990_) {
      if (!this.isWaxed() && p_278048_.getUUID().equals(this.getPlayerWhoMayEdit()) && this.level != null) {
         this.updateText(p_277776_ -> this.setMessages(p_278048_, p_277990_, p_277776_), p_278103_);
         this.setAllowedPlayerEditor(null);
         this.level.sendBlockUpdated(this.getBlockPos(), this.getBlockState(), this.getBlockState(), 3);
      } else {
         LOGGER.warn("Player {} just tried to change non-editable sign", p_278048_.getName().getString());
      }
   }

   public boolean updateText(UnaryOperator<SignText> p_277877_, boolean p_277426_) {
      SignText $$2 = this.getText(p_277426_);
      return this.setText(p_277877_.apply($$2), p_277426_);
   }

   private SignText setMessages(Player p_277396_, List<FilteredText> p_277744_, SignText p_277359_) {
      for (int $$3 = 0; $$3 < p_277744_.size(); $$3++) {
         FilteredText $$4 = p_277744_.get($$3);
         Style $$5 = p_277359_.getMessage($$3, p_277396_.isTextFilteringEnabled()).getStyle();
         if (p_277396_.isTextFilteringEnabled()) {
            p_277359_ = p_277359_.setMessage($$3, Component.literal($$4.filteredOrEmpty()).setStyle($$5));
         } else {
            p_277359_ = p_277359_.setMessage($$3, Component.literal($$4.raw()).setStyle($$5), Component.literal($$4.filteredOrEmpty()).setStyle($$5));
         }
      }

      return p_277359_;
   }

   public boolean setText(SignText p_277733_, boolean p_277720_) {
      return p_277720_ ? this.setFrontText(p_277733_) : this.setBackText(p_277733_);
   }

   private boolean setBackText(SignText p_277777_) {
      if (p_277777_ != this.backText) {
         this.backText = p_277777_;
         this.markUpdated();
         return true;
      } else {
         return false;
      }
   }

   private boolean setFrontText(SignText p_278038_) {
      if (p_278038_ != this.frontText) {
         this.frontText = p_278038_;
         this.markUpdated();
         return true;
      } else {
         return false;
      }
   }

   public boolean canExecuteClickCommands(boolean p_278276_, Player p_278240_) {
      return this.isWaxed() && this.getText(p_278276_).hasAnyClickCommands(p_278240_);
   }

   public boolean executeClickCommandsIfPresent(Player p_279304_, Level p_279201_, BlockPos p_278282_, boolean p_278254_) {
      boolean $$4 = false;

      for (Component $$5 : this.getText(p_278254_).getMessages(p_279304_.isTextFilteringEnabled())) {
         Style $$6 = $$5.getStyle();
         ClickEvent $$7 = $$6.getClickEvent();
         if ($$7 != null && $$7.getAction() == ClickEvent.Action.RUN_COMMAND) {
            p_279304_.getServer().getCommands().performPrefixedCommand(createCommandSourceStack(p_279304_, p_279201_, p_278282_), $$7.getValue());
            $$4 = true;
         }
      }

      return $$4;
   }

   private static CommandSourceStack createCommandSourceStack(@Nullable Player p_279428_, Level p_279359_, BlockPos p_279430_) {
      String $$3 = p_279428_ == null ? "Sign" : p_279428_.getName().getString();
      Component $$4 = (Component)(p_279428_ == null ? Component.literal("Sign") : p_279428_.getDisplayName());
      return new CommandSourceStack(
         CommandSource.NULL, Vec3.atCenterOf(p_279430_), Vec2.ZERO, (ServerLevel)p_279359_, 2, $$3, $$4, p_279359_.getServer(), p_279428_
      );
   }

   public ClientboundBlockEntityDataPacket getUpdatePacket() {
      return ClientboundBlockEntityDataPacket.create(this);
   }

   @Override
   public CompoundTag getUpdateTag(HolderLookup.Provider p_324439_) {
      return this.saveCustomOnly(p_324439_);
   }

   @Override
   public boolean onlyOpCanSetNbt() {
      return true;
   }

   public void setAllowedPlayerEditor(@Nullable UUID p_155714_) {
      this.playerWhoMayEdit = p_155714_;
   }

   @Nullable
   public UUID getPlayerWhoMayEdit() {
      return this.playerWhoMayEdit;
   }

   private void markUpdated() {
      this.setChanged();
      this.level.sendBlockUpdated(this.getBlockPos(), this.getBlockState(), this.getBlockState(), 3);
   }

   public boolean isWaxed() {
      return this.isWaxed;
   }

   public boolean setWaxed(boolean p_277344_) {
      if (this.isWaxed != p_277344_) {
         this.isWaxed = p_277344_;
         this.markUpdated();
         return true;
      } else {
         return false;
      }
   }

   public boolean playerIsTooFarAwayToEdit(UUID p_277978_) {
      Player $$1 = this.level.getPlayerByUUID(p_277978_);
      return $$1 == null || !$$1.canInteractWithBlock(this.getBlockPos(), 4.0);
   }

   public static void tick(Level p_277662_, BlockPos p_278050_, BlockState p_277927_, SignBlockEntity p_277928_) {
      UUID $$4 = p_277928_.getPlayerWhoMayEdit();
      if ($$4 != null) {
         p_277928_.clearInvalidPlayerWhoMayEdit(p_277928_, p_277662_, $$4);
      }
   }

   private void clearInvalidPlayerWhoMayEdit(SignBlockEntity p_277656_, Level p_277853_, UUID p_277849_) {
      if (p_277656_.playerIsTooFarAwayToEdit(p_277849_)) {
         p_277656_.setAllowedPlayerEditor(null);
      }
   }

   public SoundEvent getSignInteractionFailedSoundEvent() {
      return SoundEvents.WAXED_SIGN_INTERACT_FAIL;
   }
}
