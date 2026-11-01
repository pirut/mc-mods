package net.minecraft.network.protocol.game;

import com.google.common.collect.Queues;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.tree.ArgumentCommandNode;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.mojang.brigadier.tree.RootCommandNode;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.ints.IntSets;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntMaps;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import java.util.List;
import java.util.Queue;
import java.util.function.BiPredicate;
import javax.annotation.Nullable;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.commands.synchronization.ArgumentTypeInfos;
import net.minecraft.commands.synchronization.SuggestionProviders;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;
import net.minecraft.resources.ResourceLocation;

public class ClientboundCommandsPacket implements Packet<ClientGamePacketListener> {
   public static final StreamCodec<FriendlyByteBuf, ClientboundCommandsPacket> STREAM_CODEC = Packet.codec(
      ClientboundCommandsPacket::write, ClientboundCommandsPacket::new
   );
   private static final byte MASK_TYPE = 3;
   private static final byte FLAG_EXECUTABLE = 4;
   private static final byte FLAG_REDIRECT = 8;
   private static final byte FLAG_CUSTOM_SUGGESTIONS = 16;
   private static final byte TYPE_ROOT = 0;
   private static final byte TYPE_LITERAL = 1;
   private static final byte TYPE_ARGUMENT = 2;
   private final int rootIndex;
   private final List<ClientboundCommandsPacket.Entry> entries;

   public ClientboundCommandsPacket(RootCommandNode<SharedSuggestionProvider> p_131861_) {
      Object2IntMap<CommandNode<SharedSuggestionProvider>> $$1 = enumerateNodes(p_131861_);
      this.entries = createEntries($$1);
      this.rootIndex = $$1.getInt(p_131861_);
   }

   private ClientboundCommandsPacket(FriendlyByteBuf p_178805_) {
      this.entries = p_178805_.readList(ClientboundCommandsPacket::readNode);
      this.rootIndex = p_178805_.readVarInt();
      validateEntries(this.entries);
   }

   private void write(FriendlyByteBuf p_131886_) {
      p_131886_.writeCollection(this.entries, (p_237642_, p_237643_) -> p_237643_.write(p_237642_));
      p_131886_.writeVarInt(this.rootIndex);
   }

   private static void validateEntries(List<ClientboundCommandsPacket.Entry> p_237631_, BiPredicate<ClientboundCommandsPacket.Entry, IntSet> p_237632_) {
      IntSet $$2 = new IntOpenHashSet(IntSets.fromTo(0, p_237631_.size()));

      while (!$$2.isEmpty()) {
         boolean $$3 = $$2.removeIf(p_237637_ -> p_237632_.test(p_237631_.get(p_237637_), $$2));
         if (!$$3) {
            throw new IllegalStateException("Server sent an impossible command tree");
         }
      }
   }

   private static void validateEntries(List<ClientboundCommandsPacket.Entry> p_237629_) {
      validateEntries(p_237629_, ClientboundCommandsPacket.Entry::canBuild);
      validateEntries(p_237629_, ClientboundCommandsPacket.Entry::canResolve);
   }

   private static Object2IntMap<CommandNode<SharedSuggestionProvider>> enumerateNodes(RootCommandNode<SharedSuggestionProvider> p_131863_) {
      Object2IntMap<CommandNode<SharedSuggestionProvider>> $$1 = new Object2IntOpenHashMap();
      Queue<CommandNode<SharedSuggestionProvider>> $$2 = Queues.newArrayDeque();
      $$2.add(p_131863_);

      CommandNode<SharedSuggestionProvider> $$3;
      while (($$3 = $$2.poll()) != null) {
         if (!$$1.containsKey($$3)) {
            int $$4 = $$1.size();
            $$1.put($$3, $$4);
            $$2.addAll($$3.getChildren());
            if ($$3.getRedirect() != null) {
               $$2.add($$3.getRedirect());
            }
         }
      }

      return $$1;
   }

   private static List<ClientboundCommandsPacket.Entry> createEntries(Object2IntMap<CommandNode<SharedSuggestionProvider>> p_237627_) {
      ObjectArrayList<ClientboundCommandsPacket.Entry> $$1 = new ObjectArrayList(p_237627_.size());
      $$1.size(p_237627_.size());
      ObjectIterator var2 = Object2IntMaps.fastIterable(p_237627_).iterator();

      while (var2.hasNext()) {
         it.unimi.dsi.fastutil.objects.Object2IntMap.Entry<CommandNode<SharedSuggestionProvider>> $$2 = (it.unimi.dsi.fastutil.objects.Object2IntMap.Entry<CommandNode<SharedSuggestionProvider>>)var2.next();
         $$1.set($$2.getIntValue(), createEntry((CommandNode<SharedSuggestionProvider>)$$2.getKey(), p_237627_));
      }

      return $$1;
   }

   private static ClientboundCommandsPacket.Entry readNode(FriendlyByteBuf p_131888_) {
      byte $$1 = p_131888_.readByte();
      int[] $$2 = p_131888_.readVarIntArray();
      int $$3 = ($$1 & 8) != 0 ? p_131888_.readVarInt() : 0;
      ClientboundCommandsPacket.NodeStub $$4 = read(p_131888_, $$1);
      return new ClientboundCommandsPacket.Entry($$4, $$1, $$3, $$2);
   }

   @Nullable
   private static ClientboundCommandsPacket.NodeStub read(FriendlyByteBuf p_237639_, byte p_237640_) {
      int $$2 = p_237640_ & 3;
      if ($$2 == 2) {
         String $$3 = p_237639_.readUtf();
         int $$4 = p_237639_.readVarInt();
         ArgumentTypeInfo<?, ?> $$5 = BuiltInRegistries.COMMAND_ARGUMENT_TYPE.byId($$4);
         if ($$5 == null) {
            return null;
         } else {
            ArgumentTypeInfo.Template<?> $$6 = $$5.deserializeFromNetwork(p_237639_);
            ResourceLocation $$7 = (p_237640_ & 16) != 0 ? p_237639_.readResourceLocation() : null;
            return new ClientboundCommandsPacket.ArgumentNodeStub($$3, $$6, $$7);
         }
      } else if ($$2 == 1) {
         String $$8 = p_237639_.readUtf();
         return new ClientboundCommandsPacket.LiteralNodeStub($$8);
      } else {
         return null;
      }
   }

   private static ClientboundCommandsPacket.Entry createEntry(
      CommandNode<SharedSuggestionProvider> p_237622_, Object2IntMap<CommandNode<SharedSuggestionProvider>> p_237623_
   ) {
      int $$2 = 0;
      int $$3;
      if (p_237622_.getRedirect() != null) {
         $$2 |= 8;
         $$3 = p_237623_.getInt(p_237622_.getRedirect());
      } else {
         $$3 = 0;
      }

      if (p_237622_.getCommand() != null) {
         $$2 |= 4;
      }

      ClientboundCommandsPacket.NodeStub $$5;
      if (p_237622_ instanceof RootCommandNode) {
         $$2 |= 0;
         $$5 = null;
      } else if (p_237622_ instanceof ArgumentCommandNode<SharedSuggestionProvider, ?> $$6) {
         $$5 = new ClientboundCommandsPacket.ArgumentNodeStub($$6);
         $$2 |= 2;
         if ($$6.getCustomSuggestions() != null) {
            $$2 |= 16;
         }
      } else {
         if (!(p_237622_ instanceof LiteralCommandNode $$8)) {
            throw new UnsupportedOperationException("Unknown node type " + p_237622_);
         }

         $$5 = new ClientboundCommandsPacket.LiteralNodeStub($$8.getLiteral());
         $$2 |= 1;
      }

      int[] $$11 = p_237622_.getChildren().stream().mapToInt(p_237623_::getInt).toArray();
      return new ClientboundCommandsPacket.Entry($$5, $$2, $$3, $$11);
   }

   @Override
   public PacketType<ClientboundCommandsPacket> type() {
      return GamePacketTypes.CLIENTBOUND_COMMANDS;
   }

   public void handle(ClientGamePacketListener p_131878_) {
      p_131878_.handleCommands(this);
   }

   public RootCommandNode<SharedSuggestionProvider> getRoot(CommandBuildContext p_237625_) {
      return (RootCommandNode<SharedSuggestionProvider>)new ClientboundCommandsPacket.NodeResolver(p_237625_, this.entries).resolve(this.rootIndex);
   }

   static class ArgumentNodeStub implements ClientboundCommandsPacket.NodeStub {
      private final String id;
      private final ArgumentTypeInfo.Template<?> argumentType;
      @Nullable
      private final ResourceLocation suggestionId;

      @Nullable
      private static ResourceLocation getSuggestionId(@Nullable SuggestionProvider<SharedSuggestionProvider> p_237654_) {
         return p_237654_ != null ? SuggestionProviders.getName(p_237654_) : null;
      }

      ArgumentNodeStub(String p_237650_, ArgumentTypeInfo.Template<?> p_237651_, @Nullable ResourceLocation p_237652_) {
         this.id = p_237650_;
         this.argumentType = p_237651_;
         this.suggestionId = p_237652_;
      }

      public ArgumentNodeStub(ArgumentCommandNode<SharedSuggestionProvider, ?> p_237648_) {
         this(p_237648_.getName(), ArgumentTypeInfos.unpack(p_237648_.getType()), getSuggestionId(p_237648_.getCustomSuggestions()));
      }

      @Override
      public ArgumentBuilder<SharedSuggestionProvider, ?> build(CommandBuildContext p_237656_) {
         ArgumentType<?> $$1 = this.argumentType.instantiate(p_237656_);
         RequiredArgumentBuilder<SharedSuggestionProvider, ?> $$2 = RequiredArgumentBuilder.argument(this.id, $$1);
         if (this.suggestionId != null) {
            $$2.suggests(SuggestionProviders.getProvider(this.suggestionId));
         }

         return $$2;
      }

      @Override
      public void write(FriendlyByteBuf p_237658_) {
         p_237658_.writeUtf(this.id);
         serializeCap(p_237658_, this.argumentType);
         if (this.suggestionId != null) {
            p_237658_.writeResourceLocation(this.suggestionId);
         }
      }

      private static <A extends ArgumentType<?>> void serializeCap(FriendlyByteBuf p_237660_, ArgumentTypeInfo.Template<A> p_237661_) {
         serializeCap(p_237660_, p_237661_.type(), p_237661_);
      }

      private static <A extends ArgumentType<?>, T extends ArgumentTypeInfo.Template<A>> void serializeCap(
         FriendlyByteBuf p_237663_, ArgumentTypeInfo<A, T> p_237664_, ArgumentTypeInfo.Template<A> p_237665_
      ) {
         p_237663_.writeVarInt(BuiltInRegistries.COMMAND_ARGUMENT_TYPE.getId(p_237664_));
         p_237664_.serializeToNetwork((T)p_237665_, p_237663_);
      }
   }

   static class Entry {
      @Nullable
      final ClientboundCommandsPacket.NodeStub stub;
      final int flags;
      final int redirect;
      final int[] children;

      Entry(@Nullable ClientboundCommandsPacket.NodeStub p_237668_, int p_237669_, int p_237670_, int[] p_237671_) {
         this.stub = p_237668_;
         this.flags = p_237669_;
         this.redirect = p_237670_;
         this.children = p_237671_;
      }

      public void write(FriendlyByteBuf p_237675_) {
         p_237675_.writeByte(this.flags);
         p_237675_.writeVarIntArray(this.children);
         if ((this.flags & 8) != 0) {
            p_237675_.writeVarInt(this.redirect);
         }

         if (this.stub != null) {
            this.stub.write(p_237675_);
         }
      }

      public boolean canBuild(IntSet p_237673_) {
         return (this.flags & 8) != 0 ? !p_237673_.contains(this.redirect) : true;
      }

      public boolean canResolve(IntSet p_237677_) {
         for (int $$1 : this.children) {
            if (p_237677_.contains($$1)) {
               return false;
            }
         }

         return true;
      }
   }

   static class LiteralNodeStub implements ClientboundCommandsPacket.NodeStub {
      private final String id;

      LiteralNodeStub(String p_237680_) {
         this.id = p_237680_;
      }

      @Override
      public ArgumentBuilder<SharedSuggestionProvider, ?> build(CommandBuildContext p_237682_) {
         return LiteralArgumentBuilder.literal(this.id);
      }

      @Override
      public void write(FriendlyByteBuf p_237684_) {
         p_237684_.writeUtf(this.id);
      }
   }

   static class NodeResolver {
      private final CommandBuildContext context;
      private final List<ClientboundCommandsPacket.Entry> entries;
      private final List<CommandNode<SharedSuggestionProvider>> nodes;

      NodeResolver(CommandBuildContext p_237689_, List<ClientboundCommandsPacket.Entry> p_237690_) {
         this.context = p_237689_;
         this.entries = p_237690_;
         ObjectArrayList<CommandNode<SharedSuggestionProvider>> $$2 = new ObjectArrayList();
         $$2.size(p_237690_.size());
         this.nodes = $$2;
      }

      public CommandNode<SharedSuggestionProvider> resolve(int p_237692_) {
         CommandNode<SharedSuggestionProvider> $$1 = this.nodes.get(p_237692_);
         if ($$1 != null) {
            return $$1;
         } else {
            ClientboundCommandsPacket.Entry $$2 = this.entries.get(p_237692_);
            CommandNode<SharedSuggestionProvider> $$3;
            if ($$2.stub == null) {
               $$3 = new RootCommandNode();
            } else {
               ArgumentBuilder<SharedSuggestionProvider, ?> $$4 = $$2.stub.build(this.context);
               if (($$2.flags & 8) != 0) {
                  $$4.redirect(this.resolve($$2.redirect));
               }

               if (($$2.flags & 4) != 0) {
                  $$4.executes(p_237694_ -> 0);
               }

               $$3 = $$4.build();
            }

            this.nodes.set(p_237692_, $$3);

            for (int $$6 : $$2.children) {
               CommandNode<SharedSuggestionProvider> $$7 = this.resolve($$6);
               if (!($$7 instanceof RootCommandNode)) {
                  $$3.addChild($$7);
               }
            }

            return $$3;
         }
      }
   }

   interface NodeStub {
      ArgumentBuilder<SharedSuggestionProvider, ?> build(CommandBuildContext var1);

      void write(FriendlyByteBuf var1);
   }
}
