package dev.whizzle.glowingglass.client.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.whizzle.glowingglass.client.Sounds;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.core.Holder;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Sounds the server plays (another player placing or walking on glowing glass): glass, not the carrier. */
@Mixin(ClientPacketListener.class)
abstract class ClientPacketListenerMixin {
    @WrapOperation(method = "handleSoundEvent", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/ClientLevel;playSeededSound(Lnet/minecraft/world/entity/Entity;DDDLnet/minecraft/core/Holder;Lnet/minecraft/sounds/SoundSource;FFJ)V"))
    private void glowingglass$serverSound(ClientLevel level, Entity except, double x, double y, double z, Holder<SoundEvent> sound,
                                          SoundSource source, float volume, float pitch, long seed, Operation<Void> original) {
        original.call(level, except, x, y, z, Sounds.fromServer(x, y, z, sound), source, volume, pitch, seed);
    }
}
