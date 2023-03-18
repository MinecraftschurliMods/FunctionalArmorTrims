package com.github.minecraftschurlimods.functionalarmortrims;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.HolderSetCodec;
import net.minecraft.resources.RegistryFileCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.armortrim.ArmorTrim;
import net.minecraft.world.item.armortrim.TrimMaterial;
import net.minecraft.world.item.armortrim.TrimPattern;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;

public record ArmorTrimModifier(Optional<HolderSet<TrimMaterial>> trimMaterial, Optional<HolderSet<TrimPattern>> trimPattern, List<Modifier> modifiers) implements Predicate<ArmorTrim> {
    public static final ResourceKey<Registry<ArmorTrimModifier>> REGISTRY_KEY = ResourceKey.createRegistryKey(new ResourceLocation(FunctionalArmorTrims.MODID, "armor_trim_modifier"));
    public static final Codec<ArmorTrimModifier> DIRECT_CODEC = RecordCodecBuilder.create(inst -> inst.group(
            HolderSetCodec.create(Registries.TRIM_MATERIAL, TrimMaterial.CODEC, false).optionalFieldOf("trim_material").forGetter(ArmorTrimModifier::trimMaterial),
            HolderSetCodec.create(Registries.TRIM_PATTERN, TrimPattern.CODEC, false).optionalFieldOf("trim_pattern").forGetter(ArmorTrimModifier::trimPattern),
            Modifier.CODEC.listOf().fieldOf("modifiers").forGetter(ArmorTrimModifier::modifiers)
    ).apply(inst, ArmorTrimModifier::new));
    public static final Codec<Holder<ArmorTrimModifier>> CODEC = RegistryFileCodec.create(REGISTRY_KEY, DIRECT_CODEC);

    @Override
    public boolean test(ArmorTrim armorTrim) {
        boolean materialIsValid = trimMaterial().map(holders -> holders.contains(armorTrim.material())).orElse(true);
        boolean patternIsValid = trimPattern().map(holders -> holders.contains(armorTrim.pattern())).orElse(true);
        return materialIsValid && patternIsValid;
    }

    public record Modifier(Attribute attribute, AttributeModifier modifier) {
        private static final UUID NIL_UUID = UUID.fromString("00000000-0000-0000-0000-000000000000");
        public static final Codec<Modifier> CODEC = RecordCodecBuilder.create(inst -> inst.group(
                ExtraCodecs.lazyInitializedCodec(ForgeRegistries.ATTRIBUTES::getCodec).fieldOf("attribute").forGetter(Modifier::attribute),
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
            return new Modifier(attribute, new AttributeModifier(uuid, name, amount, operation));
        }));
    }
}
