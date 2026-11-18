package dev.whizzle.glowingglass.client.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import dev.whizzle.glowingglass.client.Prediction;
import dev.whizzle.glowingglass.client.Sounds;
import dev.whizzle.glowingglass.model.Glass;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** The player's own placement: mark the position before the block appears, and play the glass place sound. */
@Mixin(BlockItem.class)
abstract class BlockItemMixin {
    @WrapOperation(method = "place", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/BlockItem;placeBlock(Lnet/minecraft/world/item/context/BlockPlaceContext;Lnet/minecraft/world/level/block/state/BlockState;)Z"))
    private boolean glowingglass$markBeforePlacing(BlockItem self, BlockPlaceContext ctx, BlockState state, Operation<Boolean> original) {
        Glass marked = Prediction.before(ctx);
        boolean placed = original.call(self, ctx, state);
        Prediction.after(ctx, marked, placed);
        return placed;
    }

    @WrapOperation(method = "place", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;getSoundType()Lnet/minecraft/world/level/block/SoundType;"))
    private SoundType glowingglass$placeVolume(BlockState state, Operation<SoundType> original, @Local BlockPos pos) {
        return Sounds.at(pos, state, original.call(state));
    }

    @WrapOperation(method = "place", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/BlockItem;getPlaceSound(Lnet/minecraft/world/level/block/state/BlockState;)Lnet/minecraft/sounds/SoundEvent;"))
    private SoundEvent glowingglass$placeSound(BlockItem self, BlockState state, Operation<SoundEvent> original, @Local BlockPos pos) {
        SoundType carrier = state.getSoundType();
        return Sounds.at(pos, state, carrier) != carrier ? SoundType.GLASS.getPlaceSound() : original.call(self, state);
    }
}
