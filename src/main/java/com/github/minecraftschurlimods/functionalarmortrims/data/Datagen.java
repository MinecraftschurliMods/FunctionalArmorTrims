package com.github.minecraftschurlimods.functionalarmortrims.data;

import com.github.minecraftschurlimods.functionalarmortrims.ArmorTrimFunction;
import com.github.minecraftschurlimods.functionalarmortrims.AttributeArmorTrimModifier;
import com.github.minecraftschurlimods.functionalarmortrims.FunctionalArmorTrims;
import com.github.minecraftschurlimods.functionalarmortrims.PiglinIgnoreArmorTrimModifier;
import net.minecraft.DetectedVersion;
import net.minecraft.WorldVersion;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.data.metadata.PackMetadataGenerator;
import net.minecraft.data.worldgen.BootstapContext;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.metadata.pack.PackMetadataSection;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.armortrim.TrimMaterial;
import net.minecraft.world.item.armortrim.TrimMaterials;
import net.minecraft.world.item.armortrim.TrimPattern;
import net.minecraft.world.item.armortrim.TrimPatterns;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.common.data.DatapackBuiltinEntriesProvider;
import net.minecraftforge.common.data.LanguageProvider;
import net.minecraftforge.data.event.GatherDataEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.forgespi.language.IModInfo;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.BiFunction;

@Mod.EventBusSubscriber(modid = FunctionalArmorTrims.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class Datagen {
    @SubscribeEvent
    static void gatherData(GatherDataEvent evt) {
        DataGenerator generator = evt.getGenerator();
        boolean includeClient = evt.includeClient();
        boolean includeServer = evt.includeServer();
        DataGenerator.PackGenerator common = generator.getVanillaPack(includeClient || includeServer);
        DataGenerator.PackGenerator server = generator.getVanillaPack(includeServer);
        DataGenerator.PackGenerator client = generator.getVanillaPack(includeClient);
        common.addProvider(wrapWith(Datagen::createMetaGenerator, evt.getModContainer().getModInfo()));
        server.addProvider(output -> new DatapackBuiltinEntriesProvider(output, evt.getLookupProvider(), new RegistrySetBuilder().add(ArmorTrimFunction.REGISTRY_KEY, Datagen::buildArmorTrimModifiers), Set.of(FunctionalArmorTrims.MODID)));
        client.addProvider(output -> new LanguageProvider(output, FunctionalArmorTrims.MODID, "en_us") {
            @Override
            protected void addTranslations() {
                add(PiglinIgnoreArmorTrimModifier.TOOLTIP, "Piglins ignore you when you wear this");
            }
        });
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

    private static void buildArmorTrimModifiers(BootstapContext<ArmorTrimFunction> context) {
        register(context, "redstone_haste", ArmorTrimFunction.builder()
                .trimMaterial(getTrimMaterial(context, TrimMaterials.REDSTONE))
                .modifier(new AttributeArmorTrimModifier(Attributes.ATTACK_SPEED, new AttributeModifier("Mining Speed", 0.1F, AttributeModifier.Operation.MULTIPLY_TOTAL)))
                .build()
        );
        register(context, "gold_piglin_ignore", ArmorTrimFunction.builder()
                .trimMaterial(getTrimMaterial(context, TrimMaterials.GOLD))
                .modifier(new PiglinIgnoreArmorTrimModifier())
                .build()
        );
        register(context, "snout_piglin_ignore", ArmorTrimFunction.builder()
                .trimPattern(getTrimPattern(context, TrimPatterns.SNOUT))
                .modifier(new PiglinIgnoreArmorTrimModifier())
                .build()
        );
    }

    @NotNull
    private static Holder.Reference<TrimMaterial> getTrimMaterial(BootstapContext<ArmorTrimFunction> context, ResourceKey<TrimMaterial> key) {
        return context.lookup(Registries.TRIM_MATERIAL).getOrThrow(key);
    }

    @NotNull
    private static Holder.Reference<TrimPattern> getTrimPattern(BootstapContext<ArmorTrimFunction> context, ResourceKey<TrimPattern> key) {
        return context.lookup(Registries.TRIM_PATTERN).getOrThrow(key);
    }

    private static void register(BootstapContext<ArmorTrimFunction> context, String name, ArmorTrimFunction modifier) {
        context.register(ResourceKey.create(ArmorTrimFunction.REGISTRY_KEY, new ResourceLocation(FunctionalArmorTrims.MODID, name)), modifier);
    }
}
