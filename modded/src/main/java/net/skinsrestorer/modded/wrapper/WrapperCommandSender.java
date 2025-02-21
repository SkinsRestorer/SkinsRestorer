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
package net.skinsrestorer.modded.wrapper;

import lombok.NonNull;
import lombok.experimental.SuperBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.skinsrestorer.modded.MinecraftKyoriSerializer;
import net.skinsrestorer.modded.SRModAdapter;
import net.skinsrestorer.modded.SRModPlatform;
import net.skinsrestorer.shared.subjects.AbstractSRCommandSender;
import net.skinsrestorer.shared.subjects.messages.ComponentString;
import net.skinsrestorer.shared.subjects.permissions.Permission;

@SuperBuilder
public class WrapperCommandSender extends AbstractSRCommandSender {
    protected final @NonNull SRModAdapter adapter;
    private final @NonNull CommandSourceStack sender;

    @Override
    public <S> S getAs(Class<S> senderClass) {
        return senderClass.cast(sender);
    }

    @Override
    public void sendMessage(ComponentString messageJson) {
        sender.sendSystemMessage(MinecraftKyoriSerializer.toNative(messageJson));
    }

    @Override
    public boolean hasPermission(Permission permission) {
        return permission.checkPermission(p -> SRModPlatform.INSTANCE.test(sender, permission));
    }
}
