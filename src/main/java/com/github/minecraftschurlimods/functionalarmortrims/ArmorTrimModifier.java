package com.github.minecraftschurlimods.functionalarmortrims;

import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.mojang.serialization.Codec;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.armortrim.ArmorTrim;

import java.util.Collections;
import java.util.List;
import java.util.function.Function;

public interface ArmorTrimModifier {
    Codec<ArmorTrimModifier> CODEC = ExtraCodecs.lazyInitializedCodec(() -> FunctionalArmorTrims.MODIFIER_TYPES_REGISTRY.get().getCodec()).dispatch(ArmorTrimModifier::getCodec, Function.identity());

    Codec<? extends ArmorTrimModifier> getCodec();

    default Multimap<Attribute, AttributeModifier> getAttributeModifiers() {
        return Multimaps.forMap(Collections.emptyMap());
    }

    default void onArmorEquip(LivingEntity entity, ItemStack stack, ArmorTrim armorTrim) {
    }

    default void onArmorUnequip(LivingEntity entity, ItemStack stack, ArmorTrim armorTrim) {
    }

    default List<Component> getTooltip(ItemStack stack, ArmorTrim armorTrim, EquipmentSlot slot, TooltipFlag flags) {
        return Collections.emptyList();
    }
}
