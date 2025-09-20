<img src="https://cdn.modrinth.com/data/8C4QfJDU/070d802c6f909b3c1324b3cad46d6b4d9ab5131f_96.webp" width="128">

# ChunkClaimPlugin2

Protect your land by claiming chunks. Now upgraded for Minecraft 1.21.8 with per-player languages and a built-in GUI for managing claims.

• Server: Spigot/Paper 1.21.8
• Java: 21+

## ✨ Highlights (v0.4)
- Minecraft 1.21.8 compatibility (api-version 1.21)
- Internationalization (i18n) with per-player locale
	- Languages included: en_US, es_ES, fr_FR
	- Command to change language: /chunklang
- GUI: /chunksettings
	- Home: Claims, Visualize, Delete, Language
	- Claims: list your claims and open details (teleport)
	- Visualize: visualize just the selected claim
	- Delete: delete with a confirmation screen (Confirm/Cancel)
	- Language: pick from available locales
- Safer teleports and better UX (click sounds, drag/click protections)
- WorldGuard-aware claiming (compileOnly hook)

## � Commands
- /claimchunk – Claim the chunk you’re standing in
- /unclaimchunk – Unclaim the current chunk
- /checkchunk – See who owns the current chunk
- /infochunk – List your claimed chunks
- /visualizechunk [world x z] – Visualize your claimed chunks; or just one when specified
- /chunklang [get|list|set <locale>] – View/list/set your language (per-player)
- /chunksettings – Open the GUI

## 🧭 GUI Overview
Open with /chunksettings

- Home: access all features
- Claims: click a claim → Claim Details (teleport back to it)
- Visualize: click a claim to visualize only that chunk
- Delete: choose a claim → Confirm Delete menu (Confirm/Cancel)
- Language: pick a locale; your choice is saved per player

All titles and item names are localized from the lang files; the GUI logic is language-agnostic.

## ⚙️ Requirements
- Java 21+
- Spigot or Paper 1.21.8
- Optional (soft): WorldGuard, WorldEdit (for region checks)

## 🏗️ Build and Install
1) Using Gradle wrapper (recommended)
	 - Build: use your IDE Gradle task or run the wrapper
	 - Result: build/libs/ChunkClaimPlugin-0.4.jar
2) Install: drop the jar into your server’s plugins folder and restart

## � Configuration
- config.yml
	- locale: default server locale (e.g., en_US)
	- messages: override any message keys if desired
- lang/messages_*.yml
	- en_US, es_ES, fr_FR included

## 📝 Changelog
v0.4
- Upgrade to 1.21.8 (api-version 1.21)
- Added per-player i18n with /chunklang
- New /chunksettings GUI with Claims/Visualize/Delete/Language
- Single-claim visualization from GUI and command
- Delete confirmation menu
- Safer teleports and UX improvements

v0.3
- WorldGuard integration and general improvements

## 🛣️ Roadmap
- Pagination for large claim lists
- Trusts/permissions per claim
- Admin tools
- More locales

Issues and feature requests welcome!
