package dev.whizzle.glowingglass.client.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import dev.whizzle.glowingglass.client.Sounds;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Hoofsteps on glowing glass (horses have their own step sound code). */
@Mixin(AbstractHorse.class)
abstract class AbstractHorseMixin {
    @WrapOperation(method = "playStepSound", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;getSoundType()Lnet/minecraft/world/level/block/SoundType;"))
    private SoundType glowingglass$stepSound(BlockState state, Operation<SoundType> original, @Local(argsOnly = true) BlockPos pos) {
        return Sounds.at(pos, state, original.call(state));
    }
}
