package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import com.mojang.brigadier.exceptions.Dynamic3CommandExceptionType;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import java.util.stream.Stream;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.IdentifierArgument;
import net.minecraft.commands.arguments.ResourceArgument;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeMap;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

public class AttributeCommand {
   private static final DynamicCommandExceptionType ERROR_NOT_LIVING_ENTITY = new DynamicCommandExceptionType(
      p_304174_ -> Component.translatableEscape("commands.attribute.failed.entity", p_304174_)
   );
   private static final Dynamic2CommandExceptionType ERROR_NO_SUCH_ATTRIBUTE = new Dynamic2CommandExceptionType(
      (p_304185_, p_304186_) -> Component.translatableEscape("commands.attribute.failed.no_attribute", p_304185_, p_304186_)
   );
   private static final Dynamic3CommandExceptionType ERROR_NO_SUCH_MODIFIER = new Dynamic3CommandExceptionType(
      (p_304182_, p_304183_, p_304184_) -> Component.translatableEscape("commands.attribute.failed.no_modifier", p_304183_, p_304182_, p_304184_)
   );
   private static final Dynamic3CommandExceptionType ERROR_MODIFIER_ALREADY_PRESENT = new Dynamic3CommandExceptionType(
      (p_304187_, p_304188_, p_304189_) -> Component.translatableEscape("commands.attribute.failed.modifier_already_present", p_304189_, p_304188_, p_304187_)
   );

   public static void register(CommandDispatcher<CommandSourceStack> p_251026_, CommandBuildContext p_250936_) {
      p_251026_.register(
         (LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("attribute").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)))
            .then(
               Commands.argument("target", EntityArgument.entity())
                  .then(
                     ((RequiredArgumentBuilder)((RequiredArgumentBuilder)Commands.argument(
                                 "attribute", ResourceArgument.resource(p_250936_, Registries.ATTRIBUTE)
                              )
                              .then(
                                 ((LiteralArgumentBuilder)Commands.literal("get")
                                       .executes(
                                          p_248109_ -> getAttributeValue(
                                             (CommandSourceStack)p_248109_.getSource(),
                                             EntityArgument.getEntity(p_248109_, "target"),
                                             ResourceArgument.getAttribute(p_248109_, "attribute"),
                                             1.0
                                          )
                                       ))
                                    .then(
                                       Commands.argument("scale", DoubleArgumentType.doubleArg())
                                          .executes(
                                             p_248104_ -> getAttributeValue(
                                                (CommandSourceStack)p_248104_.getSource(),
                                                EntityArgument.getEntity(p_248104_, "target"),
                                                ResourceArgument.getAttribute(p_248104_, "attribute"),
                                                DoubleArgumentType.getDouble(p_248104_, "scale")
                                             )
                                          )
                                    )
                              ))
                           .then(
                              ((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("base")
                                       .then(
                                          Commands.literal("set")
                                             .then(
                                                Commands.argument("value", DoubleArgumentType.doubleArg())
                                                   .executes(
                                                      p_248102_ -> setAttributeBase(
                                                         (CommandSourceStack)p_248102_.getSource(),
                                                         EntityArgument.getEntity(p_248102_, "target"),
                                                         ResourceArgument.getAttribute(p_248102_, "attribute"),
                                                         DoubleArgumentType.getDouble(p_248102_, "value")
                                                      )
                                                   )
                                             )
                                       ))
                                    .then(
                                       ((LiteralArgumentBuilder)Commands.literal("get")
                                             .executes(
                                                p_248112_ -> getAttributeBase(
                                                   (CommandSourceStack)p_248112_.getSource(),
                                                   EntityArgument.getEntity(p_248112_, "target"),
                                                   ResourceArgument.getAttribute(p_248112_, "attribute"),
                                                   1.0
                                                )
                                             ))
                                          .then(
                                             Commands.argument("scale", DoubleArgumentType.doubleArg())
                                                .executes(
                                                   p_248106_ -> getAttributeBase(
                                                      (CommandSourceStack)p_248106_.getSource(),
                                                      EntityArgument.getEntity(p_248106_, "target"),
                                                      ResourceArgument.getAttribute(p_248106_, "attribute"),
                                                      DoubleArgumentType.getDouble(p_248106_, "scale")
                                                   )
                                                )
                                          )
                                    ))
                                 .then(
                                    Commands.literal("reset")
                                       .executes(
                                          p_382664_ -> resetAttributeBase(
                                             (CommandSourceStack)p_382664_.getSource(),
                                             EntityArgument.getEntity(p_382664_, "target"),
                                             ResourceArgument.getAttribute(p_382664_, "attribute")
                                          )
                                       )
                                 )
                           ))
                        .then(
                           ((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("modifier")
                                    .then(
                                       Commands.literal("add")
                                          .then(
                                             Commands.argument("id", IdentifierArgument.id())
                                                .then(
                                                   ((RequiredArgumentBuilder)((RequiredArgumentBuilder)Commands.argument(
                                                               "value", DoubleArgumentType.doubleArg()
                                                            )
                                                            .then(
                                                               Commands.literal("add_value")
                                                                  .executes(
                                                                     p_466197_ -> addModifier(
                                                                        (CommandSourceStack)p_466197_.getSource(),
                                                                        EntityArgument.getEntity(p_466197_, "target"),
                                                                        ResourceArgument.getAttribute(p_466197_, "attribute"),
                                                                        IdentifierArgument.getId(p_466197_, "id"),
                                                                        DoubleArgumentType.getDouble(p_466197_, "value"),
                                                                        AttributeModifier.Operation.ADD_VALUE
                                                                     )
                                                                  )
                                                            ))
                                                         .then(
                                                            Commands.literal("add_multiplied_base")
                                                               .executes(
                                                                  p_466191_ -> addModifier(
                                                                     (CommandSourceStack)p_466191_.getSource(),
                                                                     EntityArgument.getEntity(p_466191_, "target"),
                                                                     ResourceArgument.getAttribute(p_466191_, "attribute"),
                                                                     IdentifierArgument.getId(p_466191_, "id"),
                                                                     DoubleArgumentType.getDouble(p_466191_, "value"),
                                                                     AttributeModifier.Operation.ADD_MULTIPLIED_BASE
                                                                  )
                                                               )
                                                         ))
                                                      .then(
                                                         Commands.literal("add_multiplied_total")
                                                            .executes(
                                                               p_466185_ -> addModifier(
                                                                  (CommandSourceStack)p_466185_.getSource(),
                                                                  EntityArgument.getEntity(p_466185_, "target"),
                                                                  ResourceArgument.getAttribute(p_466185_, "attribute"),
                                                                  IdentifierArgument.getId(p_466185_, "id"),
                                                                  DoubleArgumentType.getDouble(p_466185_, "value"),
                                                                  AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
                                                               )
                                                            )
                                                      )
                                                )
                                          )
                                    ))
                                 .then(
                                    Commands.literal("remove")
                                       .then(
                                          Commands.argument("id", IdentifierArgument.id())
                                             .suggests(
                                                (p_382668_, p_382669_) -> SharedSuggestionProvider.suggestResource(
                                                   getAttributeModifiers(
                                                      EntityArgument.getEntity(p_382668_, "target"), ResourceArgument.getAttribute(p_382668_, "attribute")
                                                   ),
                                                   p_382669_
                                                )
                                             )
                                             .executes(
                                                p_466193_ -> removeModifier(
                                                   (CommandSourceStack)p_466193_.getSource(),
                                                   EntityArgument.getEntity(p_466193_, "target"),
                                                   ResourceArgument.getAttribute(p_466193_, "attribute"),
                                                   IdentifierArgument.getId(p_466193_, "id")
                                                )
                                             )
                                       )
                                 ))
                              .then(
                                 Commands.literal("value")
                                    .then(
                                       Commands.literal("get")
                                          .then(
                                             ((RequiredArgumentBuilder)Commands.argument("id", IdentifierArgument.id())
                                                   .suggests(
                                                      (p_382670_, p_382671_) -> SharedSuggestionProvider.suggestResource(
                                                         getAttributeModifiers(
                                                            EntityArgument.getEntity(p_382670_, "target"),
                                                            ResourceArgument.getAttribute(p_382670_, "attribute")
                                                         ),
                                                         p_382671_
                                                      )
                                                   )
                                                   .executes(
                                                      p_466190_ -> getAttributeModifier(
                                                         (CommandSourceStack)p_466190_.getSource(),
                                                         EntityArgument.getEntity(p_466190_, "target"),
                                                         ResourceArgument.getAttribute(p_466190_, "attribute"),
                                                         IdentifierArgument.getId(p_466190_, "id"),
                                                         1.0
                                                      )
                                                   ))
                                                .then(
                                                   Commands.argument("scale", DoubleArgumentType.doubleArg())
                                                      .executes(
                                                         p_466192_ -> getAttributeModifier(
                                                            (CommandSourceStack)p_466192_.getSource(),
                                                            EntityArgument.getEntity(p_466192_, "target"),
                                                            ResourceArgument.getAttribute(p_466192_, "attribute"),
                                                            IdentifierArgument.getId(p_466192_, "id"),
                                                            DoubleArgumentType.getDouble(p_466192_, "scale")
                                                         )
                                                      )
                                                )
                                          )
                                    )
                              )
                        )
                  )
            )
      );
   }

   private static AttributeInstance getAttributeInstance(Entity p_252177_, Holder<Attribute> p_249942_) throws CommandSyntaxException {
      AttributeInstance attributeinstance = getLivingEntity(p_252177_).getAttributes().getInstance(p_249942_);
      if (attributeinstance == null) {
         throw ERROR_NO_SUCH_ATTRIBUTE.create(p_252177_.getName(), getAttributeDescription(p_249942_));
      } else {
         return attributeinstance;
      }
   }

   private static LivingEntity getLivingEntity(Entity p_136440_) throws CommandSyntaxException {
      if (!(p_136440_ instanceof LivingEntity)) {
         throw ERROR_NOT_LIVING_ENTITY.create(p_136440_.getName());
      } else {
         return (LivingEntity)p_136440_;
      }
   }

   private static LivingEntity getEntityWithAttribute(Entity p_252105_, Holder<Attribute> p_248921_) throws CommandSyntaxException {
      LivingEntity livingentity = getLivingEntity(p_252105_);
      if (!livingentity.getAttributes().hasAttribute(p_248921_)) {
         throw ERROR_NO_SUCH_ATTRIBUTE.create(p_252105_.getName(), getAttributeDescription(p_248921_));
      } else {
         return livingentity;
      }
   }

   private static int getAttributeValue(CommandSourceStack p_251776_, Entity p_249647_, Holder<Attribute> p_250986_, double p_251395_) throws CommandSyntaxException {
      LivingEntity livingentity = getEntityWithAttribute(p_249647_, p_250986_);
      double d0 = livingentity.getAttributeValue(p_250986_);
      p_251776_.sendSuccess(
         () -> Component.translatable("commands.attribute.value.get.success", getAttributeDescription(p_250986_), p_249647_.getName(), d0), false
      );
      return (int)(d0 * p_251395_);
   }

   private static int getAttributeBase(CommandSourceStack p_248780_, Entity p_251083_, Holder<Attribute> p_250388_, double p_250194_) throws CommandSyntaxException {
      LivingEntity livingentity = getEntityWithAttribute(p_251083_, p_250388_);
      double d0 = livingentity.getAttributeBaseValue(p_250388_);
      p_248780_.sendSuccess(
         () -> Component.translatable("commands.attribute.base_value.get.success", getAttributeDescription(p_250388_), p_251083_.getName(), d0), false
      );
      return (int)(d0 * p_250194_);
   }

   private static int getAttributeModifier(CommandSourceStack p_136464_, Entity p_136465_, Holder<Attribute> p_250680_, Identifier p_468422_, double p_136468_) throws CommandSyntaxException {
      LivingEntity livingentity = getEntityWithAttribute(p_136465_, p_250680_);
      AttributeMap attributemap = livingentity.getAttributes();
      if (!attributemap.hasModifier(p_250680_, p_468422_)) {
         throw ERROR_NO_SUCH_MODIFIER.create(p_136465_.getName(), getAttributeDescription(p_250680_), p_468422_);
      } else {
         double d0 = attributemap.getModifierValue(p_250680_, p_468422_);
         p_136464_.sendSuccess(
            () -> Component.translatable(
               "commands.attribute.modifier.value.get.success",
               Component.translationArg(p_468422_),
               getAttributeDescription(p_250680_),
               p_136465_.getName(),
               d0
            ),
            false
         );
         return (int)(d0 * p_136468_);
      }
   }

   private static Stream<Identifier> getAttributeModifiers(Entity p_382972_, Holder<Attribute> p_383129_) throws CommandSyntaxException {
      AttributeInstance attributeinstance = getAttributeInstance(p_382972_, p_383129_);
      return attributeinstance.getModifiers().stream().map(AttributeModifier::id);
   }

   private static int setAttributeBase(CommandSourceStack p_248556_, Entity p_248620_, Holder<Attribute> p_249456_, double p_252212_) throws CommandSyntaxException {
      getAttributeInstance(p_248620_, p_249456_).setBaseValue(p_252212_);
      p_248556_.sendSuccess(
         () -> Component.translatable("commands.attribute.base_value.set.success", getAttributeDescription(p_249456_), p_248620_.getName(), p_252212_), false
      );
      return 1;
   }

   private static int resetAttributeBase(CommandSourceStack p_383106_, Entity p_382924_, Holder<Attribute> p_382828_) throws CommandSyntaxException {
      LivingEntity livingentity = getLivingEntity(p_382924_);
      if (!livingentity.getAttributes().resetBaseValue(p_382828_)) {
         throw ERROR_NO_SUCH_ATTRIBUTE.create(p_382924_.getName(), getAttributeDescription(p_382828_));
      } else {
         double d0 = livingentity.getAttributeBaseValue(p_382828_);
         p_383106_.sendSuccess(
            () -> Component.translatable("commands.attribute.base_value.reset.success", getAttributeDescription(p_382828_), p_382924_.getName(), d0), false
         );
         return 1;
      }
   }

   private static int addModifier(
      CommandSourceStack p_136470_,
      Entity p_136471_,
      Holder<Attribute> p_251636_,
      Identifier p_469156_,
      double p_136475_,
      AttributeModifier.Operation p_136476_
   ) throws CommandSyntaxException {
      AttributeInstance attributeinstance = getAttributeInstance(p_136471_, p_251636_);
      AttributeModifier attributemodifier = new AttributeModifier(p_469156_, p_136475_, p_136476_);
      if (attributeinstance.hasModifier(p_469156_)) {
         throw ERROR_MODIFIER_ALREADY_PRESENT.create(p_136471_.getName(), getAttributeDescription(p_251636_), p_469156_);
      } else {
         attributeinstance.addPermanentModifier(attributemodifier);
         p_136470_.sendSuccess(
            () -> Component.translatable(
               "commands.attribute.modifier.add.success", Component.translationArg(p_469156_), getAttributeDescription(p_251636_), p_136471_.getName()
            ),
            false
         );
         return 1;
      }
   }

   private static int removeModifier(CommandSourceStack p_136459_, Entity p_136460_, Holder<Attribute> p_250830_, Identifier p_468165_) throws CommandSyntaxException {
      AttributeInstance attributeinstance = getAttributeInstance(p_136460_, p_250830_);
      if (attributeinstance.removeModifier(p_468165_)) {
         p_136459_.sendSuccess(
            () -> Component.translatable(
               "commands.attribute.modifier.remove.success", Component.translationArg(p_468165_), getAttributeDescription(p_250830_), p_136460_.getName()
            ),
            false
         );
         return 1;
      } else {
         throw ERROR_NO_SUCH_MODIFIER.create(p_136460_.getName(), getAttributeDescription(p_250830_), p_468165_);
      }
   }

   private static Component getAttributeDescription(Holder<Attribute> p_250602_) {
      return Component.translatable(p_250602_.value().getDescriptionId());
   }
}
