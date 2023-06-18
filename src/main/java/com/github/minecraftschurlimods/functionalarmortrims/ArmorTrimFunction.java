package com.github.minecraftschurlimods.functionalarmortrims;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.HolderSetCodec;
import net.minecraft.resources.RegistryFileCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.armortrim.ArmorTrim;
import net.minecraft.world.item.armortrim.TrimMaterial;
import net.minecraft.world.item.armortrim.TrimPattern;

import java.util.*;
import java.util.function.BiPredicate;

public record ArmorTrimFunction(Optional<HolderSet<TrimMaterial>> trimMaterial, Optional<HolderSet<TrimPattern>> trimPattern, Optional<List<EquipmentSlot>> slot, List<ArmorTrimModifier> modifiers) implements BiPredicate<ArmorTrim, EquipmentSlot> {
    public static final ResourceKey<Registry<ArmorTrimFunction>> REGISTRY_KEY = ResourceKey.createRegistryKey(new ResourceLocation(FunctionalArmorTrims.MODID, "armor_trim_modifier"));
    public static final Codec<ArmorTrimFunction> DIRECT_CODEC = RecordCodecBuilder.create(inst -> inst.group(
            HolderSetCodec.create(Registries.TRIM_MATERIAL, TrimMaterial.CODEC, false).optionalFieldOf("trim_material").forGetter(ArmorTrimFunction::trimMaterial),
            HolderSetCodec.create(Registries.TRIM_PATTERN, TrimPattern.CODEC, false).optionalFieldOf("trim_pattern").forGetter(ArmorTrimFunction::trimPattern),
            Codec.STRING.xmap(EquipmentSlot::byName, EquipmentSlot::getName).listOf().optionalFieldOf("slot").forGetter(ArmorTrimFunction::slot),
            ArmorTrimModifier.CODEC.listOf().fieldOf("modifiers").forGetter(ArmorTrimFunction::modifiers)
    ).apply(inst, ArmorTrimFunction::new));
    public static final Codec<Holder<ArmorTrimFunction>> CODEC = RegistryFileCodec.create(REGISTRY_KEY, DIRECT_CODEC);

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public boolean test(ArmorTrim armorTrim, EquipmentSlot slot) {
        boolean materialIsValid = trimMaterial().map(holders -> holders.contains(armorTrim.material())).orElse(true);
        boolean patternIsValid = trimPattern().map(holders -> holders.contains(armorTrim.pattern())).orElse(true);
        boolean slotIsValid = slot().map(equipmentSlot -> equipmentSlot.contains(slot)).orElse(true);
        return materialIsValid && patternIsValid && slotIsValid;
    }

    public Multimap<Attribute, AttributeModifier> getAttributeModifiers() {
        return modifiers
                .stream()
                .flatMap(modifier -> modifier.getAttributeModifiers().entries().stream())
                .collect(Multimaps.toMultimap(Map.Entry::getKey, Map.Entry::getValue, HashMultimap::create));
    }

    public void onArmorUnequip(LivingEntity entity, ItemStack stack, ArmorTrim armorTrim) {
        modifiers.forEach(modifier -> modifier.onArmorUnequip(entity, stack, armorTrim));
    }

    public void onArmorEquip(LivingEntity entity, ItemStack stack, ArmorTrim armorTrim) {
        modifiers.forEach(modifier -> modifier.onArmorEquip(entity, stack, armorTrim));
    }

    public List<Component> getTooltip(ItemStack stack, ArmorTrim armorTrim, EquipmentSlot slot, TooltipFlag flags) {
        List<Component> tooltip = new ArrayList<>();
        for (ArmorTrimModifier modifier : modifiers()) {
            tooltip.addAll(modifier.getTooltip(stack, armorTrim, slot, flags));
        }
        return tooltip;
    }

    public static final class Builder {
        private final List<ArmorTrimModifier> modifiers = new ArrayList<>();
        private Optional<HolderSet<TrimMaterial>> trimMaterial = Optional.empty();
        private Optional<HolderSet<TrimPattern>> trimPattern = Optional.empty();
        private Optional<List<EquipmentSlot>> slot = Optional.empty();

        @SafeVarargs
        public final Builder trimMaterial(Holder<TrimMaterial>... trimMaterials) {
            this.trimMaterial = Optional.of(HolderSet.direct(Arrays.asList(trimMaterials)));
            return this;
        }

        @SafeVarargs
        public final Builder trimPattern(Holder<TrimPattern>... trimPatterns) {
            this.trimPattern = Optional.of(HolderSet.direct(Arrays.asList(trimPatterns)));
            return this;
        }

        public Builder slot(EquipmentSlot... slot) {
            this.slot = Optional.of(Arrays.asList(slot));
            return this;
        }

        public Builder modifier(ArmorTrimModifier... modifiers) {
            this.modifiers.addAll(Arrays.asList(modifiers));
            return this;
        }

        public ArmorTrimFunction build() {
            if (modifiers.isEmpty()) throw new IllegalStateException("No modifiers specified");
            return new ArmorTrimFunction(trimMaterial, trimPattern, slot, modifiers);
        }
    }
}
