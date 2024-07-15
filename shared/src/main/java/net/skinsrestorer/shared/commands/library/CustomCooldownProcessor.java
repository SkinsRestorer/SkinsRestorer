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


import edu.umd.cs.findbugs.annotations.NonNull;
import io.leangen.geantyref.TypeToken;
import lombok.RequiredArgsConstructor;
import org.incendo.cloud.execution.postprocessor.CommandPostprocessingContext;
import org.incendo.cloud.execution.postprocessor.CommandPostprocessor;
import org.incendo.cloud.key.CloudKey;
import org.incendo.cloud.processors.cooldown.CooldownGroup;
import org.incendo.cloud.processors.cooldown.CooldownInstance;
import org.incendo.cloud.processors.cooldown.CooldownManager;
import org.incendo.cloud.processors.cooldown.profile.CooldownProfile;
import org.incendo.cloud.services.type.ConsumerService;

import java.time.Duration;
import java.time.Instant;

@RequiredArgsConstructor
public class CustomCooldownProcessor<C> implements CommandPostprocessor<C> {
    public static final CloudKey<CooldownGroup> META_COOLDOWN_GROUP = CloudKey.of("cloud:cooldown_duration", new TypeToken<>() {
    });
    private final CooldownManager<C> cooldownManager;

    @Override
    public void accept(final @NonNull CommandPostprocessingContext<C> context) {
        final CooldownGroup cooldownGroup = context.command().commandMeta().getOrDefault(META_COOLDOWN_GROUP, null);
        if (cooldownGroup == null) {
            return;
        }

        if (this.cooldownManager.configuration().bypassCooldown().test(context.commandContext())) {
            return;
        }

        final CooldownProfile profile = this.cooldownManager.repository().getProfile(
                context.commandContext().sender(),
                this.cooldownManager.configuration().profileFactory()
        );

        final CooldownInstance cooldownInstance = profile.getCooldown(cooldownGroup);
        if (cooldownInstance != null) {
            final Instant endTime = cooldownInstance.creationTime().plus(cooldownInstance.duration());
            final Duration remainingTime = Duration.between(Instant.now(this.cooldownManager.configuration().clock()), endTime);
            this.cooldownManager.configuration().activeCooldownListeners().forEach(listener -> listener.cooldownActive(
                    context.commandContext().sender(),
                    context.command(),
                    cooldownInstance,
                    remainingTime
            ));
            ConsumerService.interrupt();
        }
    }
}
