package dev.whizzle.glowingglass.client.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.whizzle.glowingglass.client.Sounds;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Landing on glowing glass. The block is found the way vanilla finds it (0.2 below the feet). */
@Mixin(LivingEntity.class)
abstract class LivingEntityMixin {
    @WrapOperation(method = "playBlockFallSound", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;getSoundType()Lnet/minecraft/world/level/block/SoundType;"))
    private SoundType glowingglass$fallSound(BlockState state, Operation<SoundType> original) {
        LivingEntity self = (LivingEntity) (Object) this;
        BlockPos pos = new BlockPos(Mth.floor(self.getX()), Mth.floor(self.getY() - 0.2), Mth.floor(self.getZ()));
        return Sounds.at(pos, state, original.call(state));
    }
}
