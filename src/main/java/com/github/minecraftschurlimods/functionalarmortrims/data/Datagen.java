package com.github.minecraftschurlimods.functionalarmortrims.data;

import com.github.minecraftschurlimods.functionalarmortrims.ArmorTrimModifier;
import com.github.minecraftschurlimods.functionalarmortrims.FunctionalArmorTrims;
import com.mojang.serialization.JsonOps;
import net.minecraft.DetectedVersion;
import net.minecraft.Util;
import net.minecraft.WorldVersion;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.data.metadata.PackMetadataGenerator;
import net.minecraft.data.registries.UpdateOneTwentyRegistries;
import net.minecraft.data.worldgen.BootstapContext;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.metadata.pack.PackMetadataSection;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.armortrim.TrimMaterials;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.common.data.DatapackBuiltinEntriesProvider;
import net.minecraftforge.common.data.JsonCodecProvider;
import net.minecraftforge.data.event.GatherDataEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.forgespi.language.IModInfo;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

@Mod.EventBusSubscriber(modid = FunctionalArmorTrims.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class Datagen {
    @SubscribeEvent
    static void gatherData(GatherDataEvent evt) {
        DataGenerator generator = evt.getGenerator();
        boolean includeClient = evt.includeClient();
        boolean includeServer = evt.includeServer();
        DataGenerator.PackGenerator common = generator.getVanillaPack(includeClient || includeServer);
        DataGenerator.PackGenerator server = generator.getBuiltinDatapack(includeServer, "update_1_20");
        common.addProvider(wrapWith(Datagen::createMetaGenerator, evt.getModContainer().getModInfo()));
        server.addProvider(output -> new DatapackBuiltinEntriesProvider(output, UpdateOneTwentyRegistries.createLookup(evt.getLookupProvider()), new RegistrySetBuilder().add(ArmorTrimModifier.REGISTRY_KEY, Datagen::buildArmorTrimModifiers), Set.of(FunctionalArmorTrims.MODID)));
    }

    private static PackMetadataGenerator createMetaGenerator(PackOutput output, IModInfo modInfo) {
        WorldVersion version = DetectedVersion.BUILT_IN;
        Map<PackType, Integer> versions = new EnumMap<>(PackType.class);
        int maxVersion = 0;
        for (PackType packType : PackType.values()) {
            int v = version.getPackVersion(packType);
            versions.put(packType, v);
            maxVersion = Math.max(maxVersion, v);
        }
        PackMetadataSection metadataSection = new PackMetadataSection(Component.literal(modInfo.getDisplayName()), maxVersion, versions);
        return new PackMetadataGenerator(output).add(PackMetadataSection.TYPE, metadataSection);
    }

    private static <T extends DataProvider, S> DataProvider.Factory<T> wrapWith(BiFunction<PackOutput, S, T> factory, S s) {
        return (output) -> factory.apply(output, s);
    }

    private static void buildArmorTrimModifiers(BootstapContext<ArmorTrimModifier> context) {
        register(context, "redstone_haste", new ArmorTrimModifier(Optional.of(HolderSet.direct(context.lookup(Registries.TRIM_MATERIAL).getOrThrow(TrimMaterials.REDSTONE))), Optional.empty(), List.of(new ArmorTrimModifier.Modifier(Attributes.ATTACK_SPEED, new AttributeModifier("Mining Speed", 0.1F, AttributeModifier.Operation.MULTIPLY_TOTAL)))));
    }

    private static void register(BootstapContext<ArmorTrimModifier> context, String name, ArmorTrimModifier modifier) {
        context.register(ResourceKey.create(ArmorTrimModifier.REGISTRY_KEY, new ResourceLocation(FunctionalArmorTrims.MODID, name)), modifier);
    }
}
