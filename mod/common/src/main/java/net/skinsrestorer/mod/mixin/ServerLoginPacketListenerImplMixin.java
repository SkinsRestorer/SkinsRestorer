/*
 * SkinsRestorer
 * Copyright (C) 2024  SkinsRestorer Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package net.skinsrestorer.mod.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.authlib.GameProfile;
import net.minecraft.DefaultUncaughtExceptionHandler;
import net.minecraft.server.network.ServerLoginPacketListenerImpl;
import net.skinsrestorer.mod.listener.PlayerJoinListener;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.concurrent.atomic.AtomicInteger;

@Mixin(ServerLoginPacketListenerImpl.class)
public class ServerLoginPacketListenerImplMixin {
    @Final
    @Shadow
    static Logger LOGGER;
    @Final
    @Shadow
    private static AtomicInteger UNIQUE_THREAD_ID;
    @Shadow
    private volatile ServerLoginPacketListenerImpl.State state;

    @WrapMethod(method = "finishLoginAndWaitForClient")
    private void skinsrestorer$onLoginFinish(GameProfile gameProfile, Operation<Void> original) {
        state = ServerLoginPacketListenerImpl.State.PROTOCOL_SWITCHING;
        Thread thread = new Thread("SkinsRestorer Login Handler #" + UNIQUE_THREAD_ID.incrementAndGet()) {
            public void run() {
                PlayerJoinListener.INSTANCE.join(gameProfile);
                original.call(gameProfile);
            }
        };
        thread.setUncaughtExceptionHandler(new DefaultUncaughtExceptionHandler(LOGGER));
        thread.start();
    }
}
