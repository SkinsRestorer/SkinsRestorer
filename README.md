# SkinsRestorerX

<p align="center">
<a href="https://github.com/SkinsRestorer/SkinsRestorerX/releases/latest/download/SkinsRestorer.jar"><img src="https://img.shields.io/github/downloads/SkinsRestorer/SkinsRestorerX/latest/total.svg" alt="Downloads"></a>
<a href="https://www.spigotmc.org/resources/2124/"><img src="https://img.shields.io/spiget/downloads/2124?label=Spigot%20downloads" alt="Spigot downloads"></a>
<a href="https://www.spigotmc.org/resources/2124/"><img src="https://img.shields.io/spiget/rating/2124?label=Spigot%20rating" alt="Spigot rating"></a>
</p>
<p align="center">
<a href="https://ci.codemc.io/job/SkinsRestorer/job/SkinsRestorerX-DEV/"><img src="https://ci.codemc.io/job/SkinsRestorer/job/SkinsRestorerX-DEV/badge/icon" alt="Build Status"></a>
<a href="https://github.com/SkinsRestorer/SkinsRestorerX/actions/workflows/deploy.yml"><img src="https://github.com/SkinsRestorer/SkinsRestorerX/actions/workflows/deploy.yml/badge.svg?branch=stable" alt="Java CI"></a>
<a href="https://github.com/SkinsRestorer/SkinsRestorerX/releases/latest"><img src="https://img.shields.io/github/release/SkinsRestorer/SkinsRestorerX.svg" alt="Current Release"></a>
<a href="https://github.com/SkinsRestorer/SkinsRestorerX/graphs/contributors"><img src="https://img.shields.io/github/contributors/SkinsRestorer/SkinsRestorerX.svg" alt="Contributors"></a>
<a href="https://github.com/SkinsRestorer/SkinsRestorerX/commits/master"><img src="https://img.shields.io/github/commits-since/SkinsRestorer/SkinsRestorerX/latest.svg" alt="Commits since last release"></a>
<a href="https://github.com/SkinsRestorer/SkinsRestorerX/blob/dev/LICENSE"><img src="https://img.shields.io/github/license/SkinsRestorer/SkinsRestorerX.svg" alt="License"></a>
</p>
<p align="center"><a href="https://discord.gg/sAhVsyU"><img src="https://discordapp.com/api/guilds/186794372468178944/embed.png" alt="Discord embed"></a></p>
<p align="center"><a href="https://forthebadge.com"><img src="https://forthebadge.com/images/badges/contains-cat-gifs.svg" alt="Contains Cat GIFs"></a></p>

<p align="center">
<a href="https://github.com/SkinsRestorer/SkinsRestorerX/releases/latest/download/SkinsRestorer.jar"><img src="https://img.shields.io/badge/DOWNLOAD-LATEST-success?style=for-the-badge" alt="download badge"></a>
<a href="https://ci.codemc.io/job/SkinsRestorer/job/SkinsRestorerX-DEV/lastSuccessfulBuild/artifact/build/libs/SkinsRestorer.jar"><img src="https://img.shields.io/badge/DOWNLOAD-DEV__BUILD-important?style=for-the-badge" alt="download2 badge"></a>
</p>

This is the development repository for [SkinsRestorer](https://www.spigotmc.org/resources/skinsrestorer.2124/) (Minecraft plugin).

Restoring offline mode skins & changing skins for Bukkit/Spigot, BungeeCord/Waterfall, Sponge, catserver and Velocity
servers.

## :telescope: Compatibility

- Java 8 till 17 ([Adoptium](https://adoptium.net/)
  | [Oracle Java](https://www.oracle.com/de/java/technologies/javase-downloads.html))
- Minecraft 1.8.0 - 1.18.2

## :link: Links

- [Spigot Page](https://www.spigotmc.org/resources/skinsrestorer.2124/)
- [VelocityPowered Page](https://forums.velocitypowered.com/t/skinsrestorer-ability-to-restore-change-skins-on-servers/142)
- [Sponge ore Page](https://ore.spongepowered.org/SRTeam/SkinsRestorer)
- [PaperMC](https://papermc.io/forums/t/1-8-1-14-4-skinsrestorer/1996)
- [Wiki](https://github.com/SkinsRestorer/SkinsRestorerX/wiki/)
- [Jenkins](https://ci.codemc.io/job/SkinsRestorer/job/SkinsRestorerX/)
- [Discord](https://discord.me/skinsrestorer)
- [Website](https://skinsrestorer.net/)

## :scroll: License

SkinsRestorer is licensed under GNU General Public License v3.0. Please
see [`LICENSE.txt`](https://github.com/SkinsRestorer/SkinsRestorerX/blob/master/LICENSE) for more info.

## :family: Authors

See [Contributors](https://github.com/SkinsRestorer/SkinsRestorerX/graphs/contributors) for a list of people that have
supported this project by contributing.

## :building_construction: SkinsRestorer API

:rotating_light: Please note that this API is still WIP. Expect breaking changes! :rotating_light:

##### Maven repository

```xml
<repository>
    <id>codemc-releases</id>
    <url>https://repo.codemc.org/repository/maven-releases/</url>
</repository>
```

##### SkinsRestorer API

```xml
<!-- SkinsRestorer API -->
<dependency>
    <groupId>net.skinsrestorer</groupId>
    <artifactId>skinsrestorer-api</artifactId>
    <version>14.1.14</version>
</dependency>
```

##### Example Bukkit plugin

https://github.com/SkinsRestorer/SkinsRestorerAPIExample

### How to install? / installation / setup

Installing SkinsRestorer can be tricky when using a proxy server. Make sure to read on how to
install [here](https://github.com/SkinsRestorer/SkinsRestorerX/wiki/Installing-SkinsRestorer#Basic-Installation)
