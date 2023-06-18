package com.github.minecraftschurlimods.functionalarmortrims;

import com.mojang.serialization.Codec;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.armortrim.ArmorTrim;

import java.util.List;

public record PiglinIgnoreArmorTrimModifier() implements ArmorTrimModifier {

    public static final String TOOLTIP = "functionalarmortrims.tooltip.piglin_ignore";

    @Override
    public Codec<PiglinIgnoreArmorTrimModifier> getCodec() {
        return FunctionalArmorTrims.PIGLIN_IGNORE.get();
    }

    @Override
    public List<Component> getTooltip(ItemStack stack, ArmorTrim armorTrim, EquipmentSlot slot, TooltipFlag flags) {
        return List.of(Component.translatable(TOOLTIP));
    }
}
