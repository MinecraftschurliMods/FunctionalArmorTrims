package com.github.minecraftschurlimods.functionalarmortrims;

import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Map;
import java.util.UUID;

public record AttributeArmorTrimModifier(Attribute attribute, AttributeModifier modifier) implements ArmorTrimModifier {
    @Override
    public Codec<AttributeArmorTrimModifier> getCodec() {
        return FunctionalArmorTrims.ATTRIBUTE.get();
    }

    @Override
    public Multimap<Attribute, AttributeModifier> getAttributeModifiers() {
        return Multimaps.forMap(Map.of(attribute(), modifier()));
    }
}
