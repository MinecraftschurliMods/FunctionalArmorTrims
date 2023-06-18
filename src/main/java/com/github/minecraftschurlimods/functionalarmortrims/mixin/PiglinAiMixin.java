package com.github.minecraftschurlimods.functionalarmortrims.mixin;

import com.github.minecraftschurlimods.functionalarmortrims.FunctionalArmorTrims;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.piglin.PiglinAi;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PiglinAi.class)
public class PiglinAiMixin {
    @Inject(method = "isWearingGold", at = @At("RETURN"), cancellable = true)
    private static void isWearingGold(LivingEntity entity, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValueZ() && FunctionalArmorTrims.doPiglinsIgnore(entity)) {
            cir.setReturnValue(true);
        }
    }
}
