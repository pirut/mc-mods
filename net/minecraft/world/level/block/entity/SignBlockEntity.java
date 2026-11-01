package net.minecraft.world.level.block.entity;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.logging.LogUtils;
import java.util.List;
import java.util.UUID;
import java.util.function.UnaryOperator;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.Style;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.network.FilteredText;
import net.minecraft.server.permissions.LevelBasedPermissionSet;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SignBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public class SignBlockEntity extends BlockEntity {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final int MAX_TEXT_LINE_WIDTH = 90;
   private static final int TEXT_LINE_HEIGHT = 10;
   private static final boolean DEFAULT_IS_WAXED = false;
   @Nullable
   private UUID playerWhoMayEdit;
   private SignText frontText;
   private SignText backText;
   private boolean isWaxed = false;

   public SignBlockEntity(BlockPos pos, BlockState blockState) {
      this(BlockEntityType.SIGN, pos, blockState);
   }

   public SignBlockEntity(BlockEntityType blockEntityType, BlockPos blockPos, BlockState blockState) {
      super(blockEntityType, blockPos, blockState);
      this.frontText = this.createDefaultSignText();
      this.backText = this.createDefaultSignText();
   }

   protected SignText createDefaultSignText() {
      return new SignText();
   }

   public boolean isFacingFrontText(Player player) {
      if (this.getBlockState().getBlock() instanceof SignBlock signBlock) {
         Vec3 vec3 = signBlock.getSignHitboxCenterPosition(this.getBlockState());
         double d = player.getX() - (this.getBlockPos().getX() + vec3.x);
         double e = player.getZ() - (this.getBlockPos().getZ() + vec3.z);
         float f = signBlock.getYRotationDegrees(this.getBlockState());
         float g = (float)(Mth.atan2(e, d) * 180.0F / (float)Math.PI) - 90.0F;
         return Mth.degreesDifferenceAbs(f, g) <= 90.0F;
      } else {
         return false;
      }
   }

   public SignText getText(boolean isFrontText) {
      return isFrontText ? this.frontText : this.backText;
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
   protected void saveAdditional(ValueOutput output) {
      super.saveAdditional(output);
      output.store("front_text", SignText.DIRECT_CODEC, this.frontText);
      output.store("back_text", SignText.DIRECT_CODEC, this.backText);
      output.putBoolean("is_waxed", this.isWaxed);
   }

   @Override
   protected void loadAdditional(ValueInput input) {
      super.loadAdditional(input);
      this.frontText = input.<SignText>read("front_text", SignText.DIRECT_CODEC).map(this::loadLines).orElseGet(SignText::new);
      this.backText = input.<SignText>read("back_text", SignText.DIRECT_CODEC).map(this::loadLines).orElseGet(SignText::new);
      this.isWaxed = input.getBooleanOr("is_waxed", false);
   }

   private SignText loadLines(SignText text) {
      for (int i = 0; i < 4; i++) {
         Component component = this.loadLine(text.getMessage(i, false));
         Component component2 = this.loadLine(text.getMessage(i, true));
         text = text.setMessage(i, component, component2);
      }

      return text;
   }

   private Component loadLine(Component lineText) {
      if (this.level instanceof ServerLevel serverLevel) {
         try {
            return ComponentUtils.updateForEntity(createCommandSourceStack(null, serverLevel, this.worldPosition), lineText, null, 0);
         } catch (CommandSyntaxException var4) {
         }
      }

      return lineText;
   }

   public void updateSignText(Player player, boolean isFrontText, List<FilteredText> filteredText) {
      if (!this.isWaxed() && player.getUUID().equals(this.getPlayerWhoMayEdit()) && this.level != null) {
         this.updateText(signText -> this.setMessages(player, filteredText, signText), isFrontText);
         this.setAllowedPlayerEditor(null);
         this.level.sendBlockUpdated(this.getBlockPos(), this.getBlockState(), this.getBlockState(), 3);
      } else {
         LOGGER.warn("Player {} just tried to change non-editable sign", player.getPlainTextName());
      }
   }

   public boolean updateText(UnaryOperator<SignText> updater, boolean isFrontText) {
      SignText signText = this.getText(isFrontText);
      return this.setText(updater.apply(signText), isFrontText);
   }

   private SignText setMessages(Player player, List<FilteredText> filteredText, SignText text) {
      for (int i = 0; i < filteredText.size(); i++) {
         FilteredText filteredText2 = filteredText.get(i);
         Style style = text.getMessage(i, player.isTextFilteringEnabled()).getStyle();
         if (player.isTextFilteringEnabled()) {
            text = text.setMessage(i, Component.literal(filteredText2.filteredOrEmpty()).setStyle(style));
         } else {
            text = text.setMessage(
               i, Component.literal(filteredText2.raw()).setStyle(style), Component.literal(filteredText2.filteredOrEmpty()).setStyle(style)
            );
         }
      }

      return text;
   }

   public boolean setText(SignText text, boolean isFrontText) {
      return isFrontText ? this.setFrontText(text) : this.setBackText(text);
   }

   private boolean setBackText(SignText text) {
      if (text != this.backText) {
         this.backText = text;
         this.markUpdated();
         return true;
      } else {
         return false;
      }
   }

   private boolean setFrontText(SignText text) {
      if (text != this.frontText) {
         this.frontText = text;
         this.markUpdated();
         return true;
      } else {
         return false;
      }
   }

   public boolean canExecuteClickCommands(boolean isFrontText, Player player) {
      return this.isWaxed() && this.getText(isFrontText).hasAnyClickCommands(player);
   }

   public boolean executeClickCommandsIfPresent(ServerLevel level, Player player, BlockPos pos, boolean isFrontText) {
      boolean bl = false;

      for (Component component : this.getText(isFrontText).getMessages(player.isTextFilteringEnabled())) {
         Style style = component.getStyle();
         switch (style.getClickEvent()) {
            case ClickEvent.RunCommand runCommand:
               level.getServer().getCommands().performPrefixedCommand(createCommandSourceStack(player, level, pos), runCommand.command());
               bl = true;
               break;
            case ClickEvent.ShowDialog showDialog:
               player.openDialog(showDialog.dialog());
               bl = true;
               break;
            case ClickEvent.Custom custom:
               level.getServer().handleCustomClickAction(custom.id(), custom.payload());
               bl = true;
               break;
            case null:
            default:
         }
      }

      return bl;
   }

   private static CommandSourceStack createCommandSourceStack(@Nullable Player player, ServerLevel level, BlockPos pos) {
      String string = player == null ? "Sign" : player.getPlainTextName();
      Component component = (Component)(player == null ? Component.literal("Sign") : player.getDisplayName());
      return new CommandSourceStack(
         CommandSource.NULL, Vec3.atCenterOf(pos), Vec2.ZERO, level, LevelBasedPermissionSet.GAMEMASTER, string, component, level.getServer(), player
      );
   }

   public ClientboundBlockEntityDataPacket getUpdatePacket() {
      return ClientboundBlockEntityDataPacket.create(this);
   }

   @Override
   public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
      return this.saveCustomOnly(registries);
   }

   public void setAllowedPlayerEditor(@Nullable UUID playWhoMayEdit) {
      this.playerWhoMayEdit = playWhoMayEdit;
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

   public boolean setWaxed(boolean isWaxed) {
      if (this.isWaxed != isWaxed) {
         this.isWaxed = isWaxed;
         this.markUpdated();
         return true;
      } else {
         return false;
      }
   }

   public boolean playerIsTooFarAwayToEdit(UUID uuid) {
      Player player = this.level.getPlayerByUUID(uuid);
      return player == null || !player.isWithinBlockInteractionRange(this.getBlockPos(), 4.0);
   }

   public static void tick(Level level, BlockPos pos, BlockState state, SignBlockEntity sign) {
      UUID uUID = sign.getPlayerWhoMayEdit();
      if (uUID != null) {
         sign.clearInvalidPlayerWhoMayEdit(sign, level, uUID);
      }
   }

   private void clearInvalidPlayerWhoMayEdit(SignBlockEntity sign, Level level, UUID uuid) {
      if (sign.playerIsTooFarAwayToEdit(uuid)) {
         sign.setAllowedPlayerEditor(null);
      }
   }

   public SoundEvent getSignInteractionFailedSoundEvent() {
      return SoundEvents.WAXED_SIGN_INTERACT_FAIL;
   }
}
