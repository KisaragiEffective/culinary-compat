# Block GameTests (NeoForge 1.21.1)

The source tests start at `2e77ed9563d8a39dc85203a8f3985a3b0d92488b`.
This port starts at `ab332edcb24c0091567c37f6b825d98ef354a7d6` and
retains its Minecraft 1.21.1 / NeoForge / Java 21 production configuration.
These tests cover Culinary Compat's non-recipe server-side block behavior.

## Running

```sh
bash gradlew -Dorg.gradle.java.home=/path/to/jdk21 runGameTestServer -PgameTests --console=plain
```

Expect **26 required block tests**, plus the 5 existing port tests, and
`All 31 required tests passed`, as well as a
successful Gradle exit. NeoForge may otherwise report Gradle success after a mod
initialization failure before any GameTests start. Logs are written to
`run-gametest/logs/latest.log`.

Tests live in `src/gameTest/java`. Only the dedicated `gameTestServer` run loads
that source set; the main mod jar contains neither its classes nor its generated
empty structure. `processGameTestResources` generates the deterministic 5x5x5
empty structure; ModDevGradle does not require the legacy Mixin refmap copy. The run directory is
ignored by Git.

The runtime requires Farmer's Delight, Cooking for Blockheads, Balm, AE2 and
AE2's GuideME dependency. Missing integrations cause failures, not silent skips.
Main and the test runtime use the NeoForge FD 1.3.4 dependency and its
`addItem(ItemStack): ItemStack` API. The test retains the reflective call from
the source suite. FD 1.2.11's older `addItem` signature is not covered by this
runtime suite. All test-runtime mods target Minecraft 1.21.1 / NeoForge.

## Behavior and branch mapping

These are explicit behavior checks, **not measured line/branch coverage
percentages**. The table identifies the main server-side alternatives exercised.

| Block / integration | GameTest methods | Main alternatives asserted |
| --- | --- | --- |
| Bakeware | `placementFacesPlayerInEveryDirection`, `shapeTracksDirectionAndKitchenSupport` | Placement opposite the player in all four directions; both geometry axes; lowered CFB counter versus unshifted cooking table and ordinary support |
| AE2 kitchen station | `directionalShapesAndSupportOffsets` | Four facing geometries; sturdy block/cooking table height offset; air, slab and counter retain base height |
| AE2 kitchen station | `placementPickupAndPersistentSettings` | Placed-item link copied to world BE and pickup item; all mode/view persistence combinations into a second world BE; signed priority; invalid enum fallback; absent item link clears the previous link |
| AE2 kitchen station | `connectionDisplayAndStorageDirection` | Connected/disconnected display and OFF suppression; AE-to-CFB versus CFB-to-AE/bidirectional storage mounting; configured priority; null/unchanged settings; unlinked tick |
| AE2 kitchen station | `invalidLinksAndServerInteraction` | Missing dimension/non-access-point links; server menu opening; near/far menu validity; matching/wrong BE ticker types |
| AE2 kitchen station | `accessPointLifecycleAndStorageRegistration` | Inactive endpoint; active endpoint without grid; active inventory; single registration across repeated ticks; refresh only changed settings; grid switch; power loss/reconnection; block-entity removal unregisters storage |
| AE2 kitchen station | `registeredBlockLootPreservesOnlyPresentLinks` | Actual registered block loot table and loot function preserve a present link; unlinked BE and absent BE produce an unlinked drop |
| AE2 kitchen station | `mountedKitchenStorageMovesRealCounterItems` | Real adjacent CFB counter and station-mounted storage; NBT-distinct listing/extraction; non-mutating simulation; partial extraction; insertion across slots and merging; partial/full capacity; non-item key rejection; all three direction modes |
| AE2 kitchen station | `kitchenProviderReservesConsumesAndReturnsNetworkItems` | Real station item-provider implementation with a controlled ME storage boundary; reservation without extraction, insufficient unreserved amount, reset, real extraction, partial return/remainder, prohibited direction, both enabled directions |
| AE2 kitchen station | `serverUseHandlesGenericPlayersAndMissingBlockEntities` | Generic-player server use; menu initially valid and invalid after actual block removal; missing-BE use does not open a menu |
| FD cutting board | `singleBaseSlotAndLastInFirstOut` | Empty insertion/removal; exactly one base item; occupied base rejects; EMPTY singleton; highest extra slot first; sparse and extras-only inventories |
| FD cutting board | `extrasSlotLimitAndPersistence` | Eight extra slots; one-item slot limit; item NBT persistence into a second world BE; missing legacy extras key |
| FD cutting board | `useAddsExtrasWithSurvivalAndCreativeRules` | Survival consumes one; creative keeps held stack; full extra slots reject use; disabled multi-cutting does not add extras |
| FD cutting board | `crouchingEmptyHandRetrievesAllIngredients`, `bulkPickupHandlesFullInventoryAndCreativePlayer` | Crouching main-hand bulk retrieval; offhand avoids bulk; overflow drops without loss; creative clearing neither grants nor duplicates items |
| FD cutting board | `replacementDropsExtrasButStateChangeDoesNot` | Same-block state changes retain extras; block replacement drops every sparse occupied slot exactly once |
| FD cutting board | `counterAndCornerSupportAndShape` | Counter survival and lowered shape; ordinary sturdy support; unsupported air |
| FD cooking pot / skillet | `cookingPotShapeTracksKitchenSupport`, `skilletShapeTracksKitchenSupport` | Each block, four directions and five supports: native shape retained versus CFB counter lowered; cooking table excluded |
| CFB sink | `finiteSinkBottleBucketBoundariesSimulationAndRefund` | 249/250 and 999/1000 mB availability boundaries; four bottle reservations and exhaustion; reset; exact 250/1000 mB consumption/refund; insufficient fluid does not drain |
| CFB sink | `infiniteSinkSuppliesWaterWithEmptyTank` | Empty underlying tank supplies water in infinite mode; API reports unlimited amount; returning to finite mode confirms stored water is unchanged |
| CFB milk jar | `milkBottleAndBucketQuantitiesSimulationAndRefund` | Bottle/bucket availability and count boundaries; four bottle reservations, exhaustion and reset; exact 250/1000 mB consumption/refund; insufficient milk; the former required-empty-bucket parameter no longer exists |
| Shared kitchen membership | `adjacentCookwareRecognizedInAllSixDirections` | Bakeware, cutting board, skillet and cooking pot recognized in all six neighboring directions |
| Shared kitchen membership | `airNonTargetsAndDistantCookwareAreExcluded`, `realKitchenItemProvidersAreExposed` | Empty origin/air excluded; visited origin recorded; unrelated neighbor and cookware separated by air excluded; real adjacent sink provider exposed |

## Fixtures and limits

The station connection fixture uses world-placed access-point block entities
and recording AE2 grid/service proxies, not a powered AE2 network topology. The
ME-to-kitchen transfer fixture uses a controlled capacity-limited ME inventory
at the storage boundary; kitchen-to-ME transfers use a real world CFB counter.
The sink test registers a known bottle-sized water item through a test-only
CFB water-item tag, replacing CFB's removed item-registration API. Mutable
configuration is restored in `finally` blocks. `ProviderApi` maps the former
slot/bulk operations to real `KitchenItemProvider.findIngredient` tokens:
reservation counts are measured by repeated token discovery, consumption uses
`consume`, and refunds use the actual consumed token's `restore`.

One former assertion is inapplicable after the API change: the milk-jar check
that `useItemStack(..., requireBucket=true)` rejects a missing empty bucket.
`KitchenItemProvider`, `IngredientToken` and `CraftingContext` no longer expose
that parameter or a corresponding container-checking operation. This assertion
is omitted rather than emulated in the fixture. All other source assertions
are retained through the API adaptation, except for the removed block cases
below.

CFB 21.1.24 no longer registers `cookingforblockheads:corner`, and has no
replacement corner block. The corner iteration alone is omitted from the
bakeware shape, cutting-board support/shape, cooking-pot shape and skillet
shape tests. Their other support cases and expected shapes are unchanged.
The mock server-player fixture uses NeoForge's GameTest-specific
`NetworkRegistry.configureMockConnection` to negotiate its modded payloads.

Not covered: client rendering/tooltips and client-only branches; recipe
generation, matching, crafting, cutting, knife durability during processing,
stove recipe compatibility and their recipe-dependent provider searches;
every upstream-mod behavior; all malformed NBT or third-party storage/provider
implementations. Exception-recovery paths for a broken third-party grid service
are also not covered. StationBlock implements no player authorization check;
the suite checks its existing menu distance restriction rather than inventing
one. The tests do not claim exhaustive branch coverage.

The source 1.20.1 base contains unconditional Croptopia recipe/advancement
overrides. The 1.21.1 base already conditions those resources on mod presence;
this test port does not change production data.
