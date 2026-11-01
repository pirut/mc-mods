package net.minecraft.server.gui;

import java.util.Vector;
import javax.swing.JList;
import net.minecraft.server.MinecraftServer;

public class PlayerListComponent extends JList<String> {
   private final MinecraftServer server;
   private int tickCount;

   public PlayerListComponent(MinecraftServer p_139953_) {
      this.server = p_139953_;
      p_139953_.addTickable(this::tick);
   }

   public void tick() {
      if (this.tickCount++ % 20 == 0) {
         Vector<String> $$0 = new Vector<>();

         for (int $$1 = 0; $$1 < this.server.getPlayerList().getPlayers().size(); $$1++) {
            $$0.add(this.server.getPlayerList().getPlayers().get($$1).getGameProfile().getName());
         }

         this.setListData($$0);
      }
   }
}
