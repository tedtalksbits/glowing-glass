package dev.whizzle.glowingglass.client.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import dev.whizzle.glowingglass.client.Carriers;
import dev.whizzle.glowingglass.client.Sounds;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The mining (hit) sound while you break a block; and block updates from the server, which drop a mark whose block is
 * no longer its carrier (a placement the server refused).
 */
@Mixin(ClientLevel.class)
abstract class ClientLevelMixin {
    @WrapOperation(method = "playBreakingSound", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;getSoundType()Lnet/minecraft/world/level/block/SoundType;"))
    private SoundType glowingglass$hitSound(BlockState state, Operation<SoundType> original, @Local(argsOnly = true) BlockPos pos) {
        return Sounds.at(pos, state, original.call(state));
    }

    @Inject(method = "setServerVerifiedBlockState", at = @At("HEAD"))
    private void glowingglass$serverBlock(BlockPos pos, BlockState state, int flags, CallbackInfo ci) {
        Carriers.serverSet(pos, state);
    }
}
