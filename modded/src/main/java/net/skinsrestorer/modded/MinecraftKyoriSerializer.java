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
package net.skinsrestorer.modded;

import io.netty.buffer.ByteBufOutputStream;
import io.netty.buffer.Unpooled;
import lombok.SneakyThrows;
import net.lenni0451.mcstructs.nbt.NbtTag;
import net.lenni0451.mcstructs.nbt.io.NbtIO;
import net.lenni0451.mcstructs.text.TextComponent;
import net.lenni0451.mcstructs.text.serializer.TextComponentCodec;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.skinsrestorer.shared.subjects.messages.ComponentString;

public class MinecraftKyoriSerializer {
    @SneakyThrows
    public static Component toNative(ComponentString componentString) {
        TextComponent textComponent = TextComponentCodec.V1_21_4.deserializeJson(componentString.jsonString());
        NbtTag compoundTag = TextComponentCodec.V1_21_4.serializeNbtTree(textComponent);
        RegistryFriendlyByteBuf registryFriendlyByteBuf = new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY);
        NbtIO.V1_12.writeUnnamed(new ByteBufOutputStream(registryFriendlyByteBuf), compoundTag);

        return ComponentSerialization.TRUSTED_STREAM_CODEC.decode(registryFriendlyByteBuf);
    }
}
