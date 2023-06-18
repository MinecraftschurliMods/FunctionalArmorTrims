package com.github.minecraftschurlimods.functionalarmortrims;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.armortrim.ArmorTrim;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.ItemAttributeModifierEvent;
import net.minecraftforge.event.entity.living.LivingEquipmentChangeEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.*;

import java.util.*;
import java.util.function.Supplier;

@Mod(FunctionalArmorTrims.MODID)
public class FunctionalArmorTrims {
    public static final String MODID = "functionalarmortrims";
    private static final UUID NIL_UUID = UUID.fromString("00000000-0000-0000-0000-000000000000");
    static final DeferredRegister<Codec<? extends ArmorTrimModifier>> MODIFIER_TYPES = DeferredRegister.create(new ResourceLocation(MODID, "modifier_types"), MODID);
    static final Supplier<IForgeRegistry<Codec<? extends ArmorTrimModifier>>> MODIFIER_TYPES_REGISTRY = MODIFIER_TYPES.makeRegistry(RegistryBuilder::new);

    public static final RegistryObject<Codec<AttributeArmorTrimModifier>> ATTRIBUTE = MODIFIER_TYPES.register("attribute", () -> RecordCodecBuilder.create(inst -> inst.group(
            ExtraCodecs.lazyInitializedCodec(ForgeRegistries.ATTRIBUTES::getCodec).fieldOf("attribute").forGetter(AttributeArmorTrimModifier::attribute),
            Codec.STRING.xmap(UUID::fromString, UUID::toString).optionalFieldOf("uuid", NIL_UUID).forGetter(modifier -> modifier.modifier().getId()),
            Codec.STRING.fieldOf("name").forGetter(modifier -> modifier.modifier().getName()),
            Codec.DOUBLE.fieldOf("amount").forGetter(modifier -> modifier.modifier().getAmount()),
            ExtraCodecs.stringResolverCodec(op -> op.name().toLowerCase(), s -> {
                try {
                    return AttributeModifier.Operation.valueOf(s.toUpperCase());
                } catch (IllegalArgumentException e) {
                    return null;
                }
            }).fieldOf("operation").forGetter(modifier -> modifier.modifier().getOperation())
    ).apply(inst, (attribute, uuid, name, amount, operation) -> {
        if (NIL_UUID.equals(uuid)) {
            uuid = Mth.createInsecureUUID();
        }
        return new AttributeArmorTrimModifier(attribute, new AttributeModifier(uuid, name, amount, operation));
    })));

    public static final RegistryObject<Codec<PiglinIgnoreArmorTrimModifier>> PIGLIN_IGNORE = MODIFIER_TYPES.register("piglin_ignore", () -> Codec.unit(new PiglinIgnoreArmorTrimModifier()));

    public FunctionalArmorTrims() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        modEventBus.addListener(this::dataPackRegistries);
        MODIFIER_TYPES.register(modEventBus);
        MinecraftForge.EVENT_BUS.addListener(this::handleAttributes);
        MinecraftForge.EVENT_BUS.addListener(this::equipmentChanged);
        MinecraftForge.EVENT_BUS.addListener(this::modifyTooltip);
    }

    public static boolean doPiglinsIgnore(LivingEntity entity) {
        RegistryAccess registryAccess = entity.level().registryAccess();
        for (ArmorTrimFunction function : registryAccess.registryOrThrow(ArmorTrimFunction.REGISTRY_KEY)) {
            for (EquipmentSlot equipmentSlot : EquipmentSlot.values()) {
                if (!equipmentSlot.isArmor()) continue;
                ItemStack item = entity.getItemBySlot(equipmentSlot);
                if (item.isEmpty()) continue;
                Optional<ArmorTrim> trim = ArmorTrim.getTrim(registryAccess, item);
                if (trim.isEmpty()) continue;
                if (!function.test(trim.get(), equipmentSlot)) continue;
                for (ArmorTrimModifier modifier : function.modifiers()) {
                    if (modifier.getClass() == PiglinIgnoreArmorTrimModifier.class) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private void dataPackRegistries(DataPackRegistryEvent.NewRegistry event) {
        event.dataPackRegistry(ArmorTrimFunction.REGISTRY_KEY, ArmorTrimFunction.DIRECT_CODEC, ArmorTrimFunction.DIRECT_CODEC);
    }

    private void modifyTooltip(ItemTooltipEvent evt) {
        ItemStack stack = evt.getItemStack();
        EquipmentSlot slot = LivingEntity.getEquipmentSlotForItem(stack);
        RegistryAccess registryAccess = getRegistryAccess();
        Optional<ArmorTrim> optionalArmorTrim = ArmorTrim.getTrim(registryAccess, stack);
        if (optionalArmorTrim.isEmpty()) return;
        ArmorTrim armorTrim = optionalArmorTrim.get();
        for (ArmorTrimFunction function : registryAccess.registryOrThrow(ArmorTrimFunction.REGISTRY_KEY)) {
            if (!function.test(armorTrim, slot)) continue;
            evt.getToolTip().addAll(function.getTooltip(stack, armorTrim, slot, evt.getFlags()));
        }
    }

    private void handleAttributes(ItemAttributeModifierEvent evt) {
        ItemStack stack = evt.getItemStack();
        EquipmentSlot slot = evt.getSlotType();
        EquipmentSlot slotForItem = LivingEntity.getEquipmentSlotForItem(stack);
        if (slotForItem != slot || !slot.isArmor()) return;
        RegistryAccess registryAccess = getRegistryAccess();
        Optional<ArmorTrim> optionalArmorTrim = ArmorTrim.getTrim(registryAccess, stack);
        if (optionalArmorTrim.isEmpty()) return;
        ArmorTrim armorTrim = optionalArmorTrim.get();
        for (ArmorTrimFunction function : registryAccess.registryOrThrow(ArmorTrimFunction.REGISTRY_KEY)) {
            if (!function.test(armorTrim, slot)) continue;
            for (Map.Entry<Attribute, AttributeModifier> attributeModifier : function.getAttributeModifiers().entries()) {
                evt.addModifier(attributeModifier.getKey(), attributeModifier.getValue());
            }
        }
    }

    private void equipmentChanged(LivingEquipmentChangeEvent evt) {
        RegistryAccess registryAccess = evt.getEntity().level().registryAccess();
        ItemStack from = evt.getFrom();
        Optional<ArmorTrim> fromTrim = ArmorTrim.getTrim(registryAccess, from);
        ItemStack to = evt.getTo();
        Optional<ArmorTrim> toTrim = ArmorTrim.getTrim(registryAccess, to);
        EquipmentSlot slot = evt.getSlot();
        if (!slot.isArmor()) return;
        for (ArmorTrimFunction function : registryAccess.registryOrThrow(ArmorTrimFunction.REGISTRY_KEY)) {
            if (fromTrim.map(trim -> function.test(trim, slot)).orElse(false)) {
                function.onArmorUnequip(evt.getEntity(), from, fromTrim.get());
            }
            if (toTrim.map(trim -> function.test(trim, slot)).orElse(false)) {
                function.onArmorEquip(evt.getEntity(), to, toTrim.get());
            }
        }
    }

    private static RegistryAccess getRegistryAccess() {
        return DistExecutor.safeRunForDist(
                () -> RegistryAccessClientProxy::getRegistryAccessClient,
                () -> RegistryAccessServerProxy::getRegistryAccessServer
        );
    }
}
