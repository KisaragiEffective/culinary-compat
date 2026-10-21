<p align="center">
  <img src="src/main/resources/icon.png" width="200" alt="Culinary Compat icon"/>
</p>

# Culinary Compat!
[![NeoForge](https://img.shields.io/badge/NeoForge-1.21.1-orange)](https://neoforged.net/)

The ultimate culinary compatibility mod! Bridging the culinary gaps between mods like Farmers Delight (+ addons) and Cooking for Blockheads, the Pam's mods, Croptopia, AE2, & many others with additions, bug fixes, & more! A must-have mod for culinary connoisseurs!

Minecraft 1.21.1, NeoForge 21.1.264, MIT.

  [![CurseForge Version](https://img.shields.io/curseforge/v/1516050?label=CurseForge&color=orange&logo=curseforge&style=for-the-badge)](https://www.curseforge.com/minecraft/mc-mods/culinary-compat)
  [![Modrinth Version](https://img.shields.io/modrinth/v/culinary-compat?label=Modrinth&color=00AF5C&logo=modrinth&style=for-the-badge)](https://modrinth.com/mod/culinary-compat)

## Development

Use JDK 21 and run `./gradlew build`. Development runs are available through `./gradlew runClient` and `./gradlew runServer`.

The build uses ModDevGradle, Java 21, and NeoForge 21.1.264. All integration dependencies target Minecraft 1.21.1 / NeoForge. Run `./gradlew runGameTestServer -PgameTests` for the standalone tests, or add `-PcompatTest` to exercise the main food and kitchen integrations. Food rebalancing uses default data components and is loaded from the startup config `culinarycompat-common.toml`; restart after changing it.

Porting references: [Minecraft and NeoForge migration primers](https://docs.neoforged.net/primer/docs/), especially the [1.20.5 data component changes](https://docs.neoforged.net/primer/docs/1.20.5/) and [1.21 recipe and resource changes](https://docs.neoforged.net/primer/docs/1.21/).

## Changelog

See the [CHANGELOG](CHANGELOG.md) for all changes.

--------------------------------------

## Full Featureset

### - Kitchen Multiblock

The Farmer's Delight (Pot, Skillet, Cutting Board) blocks are now registered as valid CFB "Kitchen Multiblock" blocks. All their respective recipes are visible through the CFB "Cooking Table"!

Pam's HarvestCraft 2 recipes (cutting board, skillet, pot, saucepan, oven) are bridged too, each gated on the correct Farmer's Delight tool being in the kitchen. If Farmer's Delight Version doesn't exist, Pam's tool serves as replacement.

Croptopia is the same: frying pan recipes route to the Skillet, cooking pot recipes to the Cooking Pot, knife recipes to the Cutting Board (with a Farmer's Delight knife), and food press / mortar & pestle recipes just need the tool stored anywhere in the kitchen. Multi-tool recipes require all of their tools.

### - Croptopia Integration

Full Croptopia support with Farmer's Delight taking priority! Croptopia duplicates (tomato, onion, rice, cabbage, bacon, milk bottle, dough, and 9 duplicate meals) are hidden and their recipes disabled or rewritten to the Farmer's Delight item. Croptopia meals like jams, borscht, cheese, and ice creams can be cooked in the Farmer's Delight cooking pot (adapted from ACCBDD's Farmers Croptopia). The kitchen Sink provides water bottles and the Milk Jar / Cow in a Jar provides milk bottles, each draining only a quarter bucket. Croptopia's gather advancements are also fixed to count the Farmer's Delight equivalents.

### - Custom Bakeware Block

New Bakeware block, custom block to Culinary Compat which allows all Pam's bakeware recipes plus Croptopia's baked goods (pizzas, pies, pastries, roasted dishes)! Instead of unintuitive shapeless crafting, the CFB recipes mimic an oven! Click once to confirm, click again to bake, 4-second timer with cooldown overlay, sounds, and a cancel-refund. Fully configurable, and available with either Pam's or Croptopia installed.

### - AE2 Wireless Bridge

The ME Kitchen Station wirelessly links any Applied Energistics 2 ME network to your CFB kitchen multiblock. Bidirectional by default: your AE2 network can feed kitchen recipes AND your kitchen cabinets, fridges, and counters show up on your ME terminal. Right-click the block for a settings panel to switch direction modes (ME → Kitchen, Kitchen → ME, or Bidirectional), a cosmetic screen toggle, and a wrench tab opening AE2's native priority page. Thanks Sebastrn!

### - Head Chef's Journal

A Patchouli guidebook crafted with a vanilla book + cake. Categories and entries auto-show/hide based on which optional mods you have installed, so it stays relevant to whatever subset of integrations you're running. Currently covers the AE2 bridge in depth, more sections will be added as the mod grows as well!

### - Food Rebalancing

Optional restore of Pam's 1.12.2 food values, optional strip of edibility from ingredients (butter, spices, etc.), optional nerf of vanilla Minecraft foods. These Features have been forked from Pixel1011's Mod PamsHC2 Food Nerf!

### - Recipe Viewers

JEI and EMI plugins with seven kitchen-bridge categories (cutting board, skillet, oven, pot, bakeware, food press, mortar & pestle) and workstation hints.

### - Configuration

Client and server config files are included, covering things like the bakeware timer, cutting board multi-input stacking (`multiCutting`), and missing-dependency login notices (`notifyMissingDeps`). More options will be added as well. Feel free to give some suggestions!

## Currently Supported (optional) Addons & Integrations 
<details> <summary>Farmer's Delight Addons</summary>
  
    - Storage Delight
    - Ender's Delight
    - End's Delight
    - My Nether's Delight (the 1.21.1 successor to Nether's Delight)
    - Twilight's Flavor and Delight
</details>

<details> <summary>Croptopia</summary>
    - Full compat (frying pan, cooking pot, knife, food press, mortar & pestle, bakeware), Farmer's Delight duplicate cleanup, cooking pot meals, and advancement fixes
</details>

<details> <summary>Applied Energistics 2</summary>
    - ME Kitchen Station: wirelessly connect your CFB kitchen to your AE2 Network (Refined storage coming soon!)
</details>

<details> <summary>Patchouli</summary>
    - Adds an in game guidebook, titled "Head Chef's Journal"
</details>

### Integrations:
- Supports Farmer's Delight 1.3.4 for NeoForge 1.21.1
- Fixes crashes from multiple addons that reference outdated (pre 1.3.1) Farmers Delight methods 

--------------------------------------
## Installation

Install NeoForge 21.1.264 for Minecraft 1.21.1, then drop `culinarycompat-<version>.jar` into `mods/`. All integrations are soft dependencies, runs cleanly with any subset. INTENDED TO BE USED WITH ALL OF THE FOLLOWING:

[Pam's HC2 Food Core](https://www.curseforge.com/minecraft/mc-mods/pams-harvestcraft-2-food-core), [Food Extended](https://www.curseforge.com/minecraft/mc-mods/pams-harvestcraft-2-food-extended), [Crops](https://www.curseforge.com/minecraft/mc-mods/pams-harvestcraft-2-crops), [Trees](https://www.curseforge.com/minecraft/mc-mods/pams-harvestcraft-2-trees), [Farmer's Delight](https://www.curseforge.com/minecraft/mc-mods/farmers-delight), [Cooking for Blockheads](https://www.curseforge.com/minecraft/mc-mods/cooking-for-blockheads).

Optional: [Croptopia](https://www.curseforge.com/minecraft/mc-mods/croptopia) (full kitchen bridge), [Applied Energistics 2](https://www.curseforge.com/minecraft/mc-mods/applied-energistics-2) (ME Kitchen Station), [Patchouli](https://www.curseforge.com/minecraft/mc-mods/patchouli) (Head Chef's Journal).

--------------------------------------

## Credits

This mod wouldn't exist without: Pam (Pam's HarvestCraft 2), vectorwing (Farmer's Delight), BlayTheNinth (Cooking for Blockheads), Pixel 1101 (PXH2 Nerf), ItsSebastrn (AppliedCooking), and ACCBDD (Farmers Croptopia).

--------------------------------------

## Issues

[GitHub issue tracker](https://github.com/smokahs/Culinary-Compat/issues). Include MC+NeoForge version, Culinary Compat version, installed mod subset, crash log (if applicable), and `logs/latest.log`.
