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
package net.skinsrestorer.shared.commands.library;

import io.leangen.geantyref.TypeToken;
import net.skinsrestorer.shared.subjects.SRCommandSender;
import net.skinsrestorer.shared.subjects.SRPlayer;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.key.CloudKey;
import org.incendo.cloud.processors.requirements.Requirement;
import org.incendo.cloud.processors.requirements.Requirements;

public class ConsoleOnlyRequirement implements Requirement<SRCommandSender, ConsoleOnlyRequirement> {
    public static final CloudKey<Requirements<SRCommandSender, ConsoleOnlyRequirement>> REQUIREMENT_KEY = CloudKey.of(
            "requirements",
            new TypeToken<>() {
            }
    );

    @Override
    public boolean evaluateRequirement(@NonNull CommandContext<SRCommandSender> commandContext) {
        return !(commandContext.sender() instanceof SRPlayer);
    }
}
