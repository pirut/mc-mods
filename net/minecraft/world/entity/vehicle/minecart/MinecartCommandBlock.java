package net.minecraft.world.entity.vehicle.minecart;

import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.permissions.LevelBasedPermissionSet;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BaseCommandBlock;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class MinecartCommandBlock extends AbstractMinecart {
   static final EntityDataAccessor<String> DATA_ID_COMMAND_NAME = SynchedEntityData.defineId(MinecartCommandBlock.class, EntityDataSerializers.STRING);
   static final EntityDataAccessor<Component> DATA_ID_LAST_OUTPUT = SynchedEntityData.defineId(MinecartCommandBlock.class, EntityDataSerializers.COMPONENT);
   private final BaseCommandBlock commandBlock = new MinecartCommandBlock.MinecartCommandBase();
   private static final int ACTIVATION_DELAY = 4;
   private int lastActivated;

   public MinecartCommandBlock(EntityType<? extends MinecartCommandBlock> p_481800_, Level p_478012_) {
      super(p_481800_, p_478012_);
   }

   @Override
   protected Item getDropItem() {
      return Items.MINECART;
   }

   @Override
   public ItemStack getPickResult() {
      return new ItemStack(Items.COMMAND_BLOCK_MINECART);
   }

   @Override
   protected void defineSynchedData(SynchedEntityData.Builder p_479758_) {
      super.defineSynchedData(p_479758_);
      p_479758_.define(DATA_ID_COMMAND_NAME, "");
      p_479758_.define(DATA_ID_LAST_OUTPUT, CommonComponents.EMPTY);
   }

   @Override
   protected void readAdditionalSaveData(ValueInput p_479325_) {
      super.readAdditionalSaveData(p_479325_);
      this.commandBlock.load(p_479325_);
      this.getEntityData().set(DATA_ID_COMMAND_NAME, this.getCommandBlock().getCommand());
      this.getEntityData().set(DATA_ID_LAST_OUTPUT, this.getCommandBlock().getLastOutput());
   }

   @Override
   protected void addAdditionalSaveData(ValueOutput p_481386_) {
      super.addAdditionalSaveData(p_481386_);
      this.commandBlock.save(p_481386_);
   }

   @Override
   public BlockState getDefaultDisplayBlockState() {
      return Blocks.COMMAND_BLOCK.defaultBlockState();
   }

   public BaseCommandBlock getCommandBlock() {
      return this.commandBlock;
   }

   @Override
   public void activateMinecart(ServerLevel p_481634_, int p_479341_, int p_479485_, int p_480569_, boolean p_479374_) {
      if (p_479374_ && this.tickCount - this.lastActivated >= 4) {
         this.getCommandBlock().performCommand(p_481634_);
         this.lastActivated = this.tickCount;
      }
   }

   @Override
   public InteractionResult interact(Player p_479614_, InteractionHand p_481796_) {
      InteractionResult ret = super.interact(p_479614_, p_481796_);
      if (ret.consumesAction()) {
         return ret;
      } else if (!p_479614_.canUseGameMasterBlocks()) {
         return InteractionResult.PASS;
      } else {
         if (p_479614_.level().isClientSide()) {
            p_479614_.openMinecartCommandBlock(this);
         }

         return InteractionResult.SUCCESS;
      }
   }

   @Override
   public void onSyncedDataUpdated(EntityDataAccessor<?> p_479902_) {
      super.onSyncedDataUpdated(p_479902_);
      if (DATA_ID_LAST_OUTPUT.equals(p_479902_)) {
         try {
            this.commandBlock.setLastOutput(this.getEntityData().get(DATA_ID_LAST_OUTPUT));
         } catch (Throwable var3) {
         }
      } else if (DATA_ID_COMMAND_NAME.equals(p_479902_)) {
         this.commandBlock.setCommand(this.getEntityData().get(DATA_ID_COMMAND_NAME));
      }
   }

   class MinecartCommandBase extends BaseCommandBlock {
      @Override
      public void onUpdated(ServerLevel p_480994_) {
         MinecartCommandBlock.this.getEntityData().set(MinecartCommandBlock.DATA_ID_COMMAND_NAME, this.getCommand());
         MinecartCommandBlock.this.getEntityData().set(MinecartCommandBlock.DATA_ID_LAST_OUTPUT, this.getLastOutput());
      }

      @Override
      public CommandSourceStack createCommandSourceStack(ServerLevel p_480955_, CommandSource p_480149_) {
         return new CommandSourceStack(
            p_480149_,
            MinecartCommandBlock.this.position(),
            MinecartCommandBlock.this.getRotationVector(),
            p_480955_,
            LevelBasedPermissionSet.GAMEMASTER,
            this.getName().getString(),
            MinecartCommandBlock.this.getDisplayName(),
            p_480955_.getServer(),
            MinecartCommandBlock.this
         );
      }

      @Override
      public boolean isValid() {
         return !MinecartCommandBlock.this.isRemoved();
      }
   }
}
