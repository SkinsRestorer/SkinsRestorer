/*
 * SkinsRestorer
 *
 * Copyright (C) 2023 SkinsRestorer
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 */
package net.skinsrestorer.api;

import com.google.gson.Gson;
import net.skinsrestorer.api.model.MojangProfileResponse;
import net.skinsrestorer.api.model.MojangProfileTextureMeta;
import net.skinsrestorer.api.property.SkinProperty;
import net.skinsrestorer.api.property.SkinVariant;
import org.jetbrains.annotations.NotNull;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Utility class for retrieving information from profile properties related to skins.
 */
public class PropertyUtil {
    private static final Gson GSON = new Gson();

    /**
     * Returns a <a href="https://textures.minecraft.net/id">Texture Url</a> based on skin
     * This is useful for skull plugins like Dynmap or DiscordSRV
     * for example <a href="https://mc-heads.net/avatar/cb50beab76e56472637c304a54b330780e278decb017707bf7604e484e4d6c9f/100.png">https://mc-heads.net/avatar/%texture_id%/%size%.png</a>
     *
     * @param property Profile property
     * @return full textures.minecraft.net url
     */
    public static String getSkinTextureUrl(@NotNull SkinProperty property) {
        return getSkinProfileData(property).getTextures().getSKIN().getUrl();
    }

    public static SkinVariant getSkinVariant(@NotNull SkinProperty property) {
        MojangProfileTextureMeta meta = getSkinProfileData(property).getTextures().getSKIN().getMetadata();
        if (meta == null) {
            return SkinVariant.CLASSIC;
        }

        return meta.getModel().equalsIgnoreCase("slim") ? SkinVariant.SLIM : SkinVariant.CLASSIC;
    }

    /**
     * Only returns the id at the end of the url.
     * Example:
     * <a href="https://textures.minecraft.net/texture/cb50beab76e56472637c304a54b330780e278decb017707bf7604e484e4d6c9f">
     * https://textures.minecraft.net/texture/cb50beab76e56472637c304a54b330780e278decb017707bf7604e484e4d6c9f
     * </a>
     * Would return: cb50beab76e56472637c304a54b330780e278decb017707bf7604e484e4d6c9f
     *
     * @param property Profile property
     * @return textures.minecraft.net id
     * @see #getSkinTextureUrl(SkinProperty)
     */
    public static String getSkinTextureUrlStripped(@NotNull SkinProperty property) {
        return getSkinProfileData(property).getTextures().getSKIN().getStrippedUrl();
    }

    /**
     * Returns the decoded profile data from the profile property.
     * This is useful for getting the skin data from the property and other information like cape.
     * The user stored in this property may not be the same as the player who has the skin.
     * APIs like MineSkin use multiple shared accounts to generate these properties.
     * Or it could be the property of another player that the player set their skin to.
     *
     * @param property Profile property
     * @return Decoded profile data as java object
     */
    public static MojangProfileResponse getSkinProfileData(@NotNull SkinProperty property) {
        String decodedString = new String(Base64.getDecoder().decode(property.getValue()), StandardCharsets.UTF_8);

        return GSON.fromJson(decodedString, MojangProfileResponse.class);
    }
}
