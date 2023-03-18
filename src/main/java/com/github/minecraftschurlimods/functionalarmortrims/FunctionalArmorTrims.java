package com.github.minecraftschurlimods.functionalarmortrims;

import net.minecraft.core.RegistryAccess;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.armortrim.ArmorTrim;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.ItemAttributeModifierEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DataPackRegistryEvent;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

@Mod(FunctionalArmorTrims.MODID)
public class FunctionalArmorTrims {
    public static final String MODID = "functionalarmortrims";

    public FunctionalArmorTrims() {
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::dataPackRegistries);
        MinecraftForge.EVENT_BUS.addListener(this::handleAttributes);
    }

    private void dataPackRegistries(DataPackRegistryEvent.NewRegistry event) {
        event.dataPackRegistry(ArmorTrimModifier.REGISTRY_KEY, ArmorTrimModifier.DIRECT_CODEC, ArmorTrimModifier.DIRECT_CODEC);
    }

    private void handleAttributes(ItemAttributeModifierEvent evt) {
        ItemStack stack = evt.getItemStack();
        EquipmentSlot slot = evt.getSlotType();
        EquipmentSlot slotForItem = LivingEntity.getEquipmentSlotForItem(stack);
        if (slotForItem != slot) return;
        ArmorTrim.getTrim(getRegistryAccess(), stack)
                .map(this::getModifiers)
                .ifPresent(modifiers -> modifiers.forEach(modifier -> evt.addModifier(modifier.attribute(), modifier.modifier())));
    }

    private Collection<ArmorTrimModifier.Modifier> getModifiers(ArmorTrim trim) {
        Set<ArmorTrimModifier.Modifier> modifiers = new HashSet<>();
        for (ArmorTrimModifier modifier : getRegistryAccess().registryOrThrow(ArmorTrimModifier.REGISTRY_KEY)) {
            if (!modifier.test(trim)) continue;
            modifiers.addAll(modifier.modifiers());
        }
        return modifiers;
    }

    private static RegistryAccess getRegistryAccess() {
        return DistExecutor.safeRunForDist(
                () -> RegistryAccessClientProxy::getRegistryAccessClient,
                () -> RegistryAccessServerProxy::getRegistryAccessServer
        );
    }
}
