# Home Section Split Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Rename the "Untuk manusia" tier to "Siap Santap" and split Home's flat category/rail list into two visually distinct stacked sections — "Siap Santap" (human food) and "Pakan Ternak & Kompos" (animal feed + compost) — per the lecturer's feedback.

**Architecture:** Pure UI restructuring inside `HomeScreen.kt`. The existing 3-tile `CategoryRow` is replaced by two smaller tile groups, one per section; the existing per-rail `LazyListScope.listingRail` extension (used by Section 1, unchanged) is joined by a new plain `@Composable RailBlock` (used by Section 2, so its rails can sit inside one tinted container as a single `LazyColumn` item). No ViewModel, UiState, repository, or navigation changes.

**Tech Stack:** Kotlin, Jetpack Compose (unchanged from the rest of the codebase).

**Spec:** `docs/superpowers/specs/2026-09-30-home-section-split-design.md`

## Global Constraints

- No Figma frame exists for the two-section split (confirmed live against node `259:10165` — the Figma Home frame is one flat list). Section 2's `Neutral50` tint/20dp-corner container is an original UI decision reusing existing tokens, not a Figma trace.
- Everything else on Home (`HomeTopBar`, `PromoBanner`, `SgSearchField`, `ListingCard`, `LocationPickerSheet`) is already Figma-accurate and stays untouched.
- No test infrastructure changes — this codebase verifies Compose UI via `@Preview` + `compileDebugKotlin`, not automated UI tests (see sub-project 1's Global Constraints for why).
- `ListingTier`, `HomeUiState`, `HomeViewModel`, `ListingRepository` are untouched — the split is purely how existing data is arranged on screen, not a data-layer change.

## Review Focus

- A listing search query that empties one section's rails but not the other's must still show that section's category tiles (tiles are navigation, not data-dependent) — covered in Task 2's Preview check across all three `HomeUiState` variants (Success with data, Success empty via search, Loading/Error unaffected).
- Section 2's category tiles must still call `onCategoryClick` with the correct tier (`ANIMAL_FEED` / `COMPOST`) after the tile-construction refactor — covered by Task 2's `CategoryTile` extraction keeping the same `onClick(tier)` call shape as the removed `CategoryRow`.
- The rename must apply everywhere "Untuk manusia" appeared, not just `strings.xml` — `CategoryListScreen.kt`'s hardcoded filter-chip label is a second, easy-to-miss site — covered by Task 1's grep-verified sweep.
- Section 1's "Siap Santap" tile and Section 2's two tiles must remain three separate, independently-tappable click targets (not merged into one accidentally-shared `clickable` during the `CategoryRow` → `CategoryTile` extraction) — covered by Task 2's Preview interaction check.

---

## File Structure

```
app/src/main/res/values/strings.xml       (modify — rename + 4 new strings)
app/src/main/java/com/sisaguna/android/feature/category/CategoryListScreen.kt  (modify — one literal)
app/src/main/java/com/sisaguna/android/feature/home/HomeScreen.kt              (modify — section restructure)
```

---

### Task 1: Rename "Untuk manusia" → "Siap Santap", add section strings

**Files:**
- Modify: `app/src/main/res/values/strings.xml`
- Modify: `app/src/main/java/com/sisaguna/android/feature/category/CategoryListScreen.kt:207`

**Interfaces:**
- Consumes: nothing.
- Produces: `R.string.tier_human` now resolves to `"Siap Santap"`; new `R.string.home_section_human_title`, `R.string.home_section_human_subtitle`, `R.string.home_section_farm_title`, `R.string.home_section_farm_subtitle`. Consumed by Task 2.

- [ ] **Step 1: Update strings.xml**

In `app/src/main/res/values/strings.xml`, change:

```xml
    <string name="tier_human">Untuk manusia</string>
```
to:
```xml
    <string name="tier_human">Siap Santap</string>
```

Add these four new strings after `home_rail_compost_subtitle` (before `home_empty_title`):

```xml
    <string name="home_section_human_title">Siap Santap</string>
    <string name="home_section_human_subtitle">Makanan sisa yang masih layak dikonsumsi</string>
    <string name="home_section_farm_title">Pakan Ternak &amp; Kompos</string>
    <string name="home_section_farm_subtitle">Sisa makanan untuk hewan ternak &amp; bahan kompos</string>
```

- [ ] **Step 2: Update CategoryListScreen's filter chip literal**

In `CategoryListScreen.kt`, find the `LazyRow` of filter chips (around line 206-211):

```kotlin
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            item { CategoryFilterChip(label = "Untuk manusia", selected = tier == ListingTier.HUMAN, onClick = { onTierChange(ListingTier.HUMAN) }) }
```

Change the literal to match the rename:

```kotlin
            item { CategoryFilterChip(label = "Siap Santap", selected = tier == ListingTier.HUMAN, onClick = { onTierChange(ListingTier.HUMAN) }) }
```

(The other two chips, "Untuk Ternak" and "Untuk kompos", are unaffected — this sub-project's rename is scoped to the human-food tier only, per the spec.)

- [ ] **Step 3: Verify no other "Untuk manusia" sites remain**

Run: `grep -rn "Untuk manusia" app/src/main/`
Expected: no matches.

- [ ] **Step 4: Build**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/res/values/strings.xml app/src/main/java/com/sisaguna/android/feature/category/CategoryListScreen.kt
git commit -m "rename: tier_human to Siap Santap, add Home section-split strings"
```

---

### Task 2: Split Home into "Siap Santap" and "Pakan Ternak & Kompos" sections

**Files:**
- Modify: `app/src/main/java/com/sisaguna/android/feature/home/HomeScreen.kt`

**Interfaces:**
- Consumes: `R.string.home_section_human_title/subtitle`, `R.string.home_section_farm_title/subtitle`, `R.string.tier_human/tier_animal_feed/tier_compost` (Task 1).
- Produces: nothing consumed elsewhere — `HomeFeedList`'s public shape (`HomeScreen`/`HomeScreenContent` signatures) is unchanged, only its internal layout changes.

- [ ] **Step 1: Replace `CategoryRow` with a reusable `CategoryTile` and two section composables**

In `HomeScreen.kt`, remove the existing `CategoryRow` composable (around lines 338-378):

```kotlin
/** Figma node 33:5316: three tier tiles. Tapping opens Category List filtered by tier. */
@Composable
private fun CategoryRow(onTierClick: (ListingTier) -> Unit, modifier: Modifier = Modifier) {
    val tiles = listOf(
        Triple(ListingTier.HUMAN, R.string.tier_human, R.drawable.category_human to SgColor.Green100),
        Triple(ListingTier.ANIMAL_FEED, R.string.tier_animal_feed, R.drawable.category_animal to SgColor.Orange100),
        Triple(ListingTier.COMPOST, R.string.tier_compost, R.drawable.category_compost to SgColor.Sky100),
    )
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        tiles.forEach { (tier, labelRes, imageAndBg) ->
            val (image, bg) = imageAndBg
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(7.dp),
                modifier = Modifier
                    .background(SgColor.BaseWhite, RoundedCornerShape(20.dp))
                    .clickableNoRipple(onClick = { onTierClick(tier) })
                    .padding(16.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .background(bg, RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Image(
                        painter = painterResource(image),
                        contentDescription = stringResource(labelRes),
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.size(48.dp),
                    )
                }
                Text(
                    text = stringResource(labelRes),
                    style = SgTextStyle.TextSmRegular,
                    color = SgColor.LabelsPrimary,
                )
            }
        }
    }
}
```

Replace it with:

```kotlin
/** One tier tile (Figma node 259:10226-259:10237 pattern, reused per-section after the
 * section split). Tapping opens Category List filtered by tier. */
@Composable
private fun CategoryTile(
    tier: ListingTier,
    labelRes: Int,
    image: Int,
    bg: Color,
    onClick: (ListingTier) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(7.dp),
        modifier = modifier
            .background(SgColor.BaseWhite, RoundedCornerShape(20.dp))
            .clickableNoRipple(onClick = { onClick(tier) })
            .padding(16.dp),
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .background(bg, RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(image),
                contentDescription = stringResource(labelRes),
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(48.dp),
            )
        }
        Text(
            text = stringResource(labelRes),
            style = SgTextStyle.TextSmRegular,
            color = SgColor.LabelsPrimary,
        )
    }
}

/** Section 1 — human food. No tint (matches the page background), single tile. */
@Composable
private fun SiapSantapHeaderAndTile(onTierClick: (ListingTier) -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(horizontal = 23.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = stringResource(R.string.home_section_human_title),
                style = SgTextStyle.TextLgSemibold,
                color = SgColor.Neutral800,
            )
            Text(
                text = stringResource(R.string.home_section_human_subtitle),
                fontSize = 12.sp,
                color = SgColor.Neutral400,
            )
        }
        CategoryTile(
            tier = ListingTier.HUMAN,
            labelRes = R.string.tier_human,
            image = R.drawable.category_human,
            bg = SgColor.Green100,
            onClick = onTierClick,
        )
    }
}

/** Section 2 — animal feed + compost. Tinted Neutral50 container (no Figma source for this
 * grouping — see spec) holding its own two tiles and both its rails, so the tint stays
 * continuous behind all of it as a single LazyColumn item. */
@Composable
private fun PakanTernakSection(
    animalFeedTitle: String,
    animalFeedSubtitle: String,
    animalFeed: List<HomeListingUi>,
    compostTitle: String,
    compostSubtitle: String,
    compost: List<HomeListingUi>,
    now: Instant,
    onListingClick: (Listing) -> Unit,
    onTierClick: (ListingTier) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .padding(horizontal = 23.dp)
            .background(SgColor.Neutral50, RoundedCornerShape(20.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = stringResource(R.string.home_section_farm_title),
                style = SgTextStyle.TextLgSemibold,
                color = SgColor.Neutral800,
            )
            Text(
                text = stringResource(R.string.home_section_farm_subtitle),
                fontSize = 12.sp,
                color = SgColor.Neutral400,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            CategoryTile(ListingTier.ANIMAL_FEED, R.string.tier_animal_feed, R.drawable.category_animal, SgColor.Orange100, onTierClick)
            CategoryTile(ListingTier.COMPOST, R.string.tier_compost, R.drawable.category_compost, SgColor.Sky100, onTierClick)
        }
        RailBlock(animalFeedTitle, animalFeedSubtitle, { SeeAllLink(onClick = { onTierClick(ListingTier.ANIMAL_FEED) }) }, animalFeed, now, onListingClick)
        RailBlock(compostTitle, compostSubtitle, { SeeAllLink(onClick = { onTierClick(ListingTier.COMPOST) }) }, compost, now, onListingClick)
    }
}

/** Non-lazy counterpart of [listingRail] — used inside [PakanTernakSection] so both its rails
 * render as part of one LazyColumn item (keeping the tinted background continuous), rather
 * than as separate lazy items the way Section 1's rails still do. Renders nothing when empty,
 * same as [listingRail]. */
@Composable
private fun RailBlock(
    title: String,
    subtitle: String,
    trailing: @Composable () -> Unit,
    listings: List<HomeListingUi>,
    now: Instant,
    onListingClick: (Listing) -> Unit,
) {
    if (listings.isEmpty()) return

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = SgTextStyle.TextLgSemibold, color = SgColor.Neutral800)
                Text(text = subtitle, fontSize = 12.sp, color = SgColor.Neutral400)
            }
            trailing()
        }
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 8.dp),
        ) {
            items(listings, key = { it.listing.id }) { entry ->
                ListingCard(
                    listing = entry.listing,
                    merchant = entry.merchant,
                    now = now,
                    onClick = { onListingClick(entry.listing) },
                )
            }
        }
    }
}
```

- [ ] **Step 2: Restructure `HomeFeedList`'s body**

Replace this block (around lines 187-208):

```kotlin
        item {
            Column(
                modifier = Modifier.padding(horizontal = 23.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    text = stringResource(R.string.home_category_title),
                    style = SgTextStyle.TextLgSemibold,
                    color = SgColor.Neutral800,
                )
                CategoryRow(onTierClick = onCategoryClick)
            }
        }

        if (state.isEmpty) {
            item { EmptySearchState(modifier = Modifier.padding(24.dp)) }
        } else {
            listingRail(nearbyTitle, nearbySubtitle, { RadiusTag() }, state.nearby, state.now, onListingClick)
            listingRail(dealsTitle, dealsSubtitle, { SeeAllLink(onClick = { onCategoryClick(ListingTier.HUMAN) }) }, state.deals, state.now, onListingClick)
            listingRail(animalFeedTitle, animalFeedSubtitle, { SeeAllLink(onClick = { onCategoryClick(ListingTier.ANIMAL_FEED) }) }, state.animalFeed, state.now, onListingClick)
            listingRail(compostTitle, compostSubtitle, { SeeAllLink(onClick = { onCategoryClick(ListingTier.COMPOST) }) }, state.compost, state.now, onListingClick)
        }
```

with:

```kotlin
        item { SiapSantapHeaderAndTile(onTierClick = onCategoryClick) }

        if (state.isEmpty) {
            item {
                PakanTernakSection(
                    animalFeedTitle = animalFeedTitle,
                    animalFeedSubtitle = animalFeedSubtitle,
                    animalFeed = emptyList(),
                    compostTitle = compostTitle,
                    compostSubtitle = compostSubtitle,
                    compost = emptyList(),
                    now = state.now,
                    onListingClick = onListingClick,
                    onTierClick = onCategoryClick,
                )
            }
            item { EmptySearchState(modifier = Modifier.padding(24.dp)) }
        } else {
            listingRail(nearbyTitle, nearbySubtitle, { RadiusTag() }, state.nearby, state.now, onListingClick)
            listingRail(dealsTitle, dealsSubtitle, { SeeAllLink(onClick = { onCategoryClick(ListingTier.HUMAN) }) }, state.deals, state.now, onListingClick)
            item {
                PakanTernakSection(
                    animalFeedTitle = animalFeedTitle,
                    animalFeedSubtitle = animalFeedSubtitle,
                    animalFeed = state.animalFeed,
                    compostTitle = compostTitle,
                    compostSubtitle = compostSubtitle,
                    compost = state.compost,
                    now = state.now,
                    onListingClick = onListingClick,
                    onTierClick = onCategoryClick,
                )
            }
        }
```

(`home_category_title`'s string resource is no longer referenced from this file — leave it in `strings.xml` unused rather than deleting it; removing unused resources is out of scope for this task.)

- [ ] **Step 3: Build**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Manual verification (Review Focus items)**

Open Android Studio's split/interactive preview and check all three existing `@Preview`s still render correctly with no code changes needed to them (`HomeScreenPreview`, `HomeScreenLoadingPreview`, `HomeScreenErrorPreview` all call `HomeScreenContent` directly, unaffected by this internal restructure):
1. `HomeScreenPreview` (mock data, all four lists populated): confirm two visually distinct sections — "Siap Santap" with one tile + 2 rails, then a `Neutral50`-tinted rounded block "Pakan Ternak & Kompos" with 2 tiles + 2 rails inside it.
2. Temporarily add a fourth preview call with `mockUiState()`'s `animalFeed`/`compost` set to `emptyList()` (or reuse an existing empty-search test path) to confirm the Pakan Ternak tiles still render with no rails underneath when there's no data for that section — then remove the temporary preview call, it was only for this manual check.
3. Tap (in Interactive Preview) the "Siap Santap" tile, then each of the two Pakan Ternak tiles — confirm each calls `onCategoryClick` with its own distinct tier (HUMAN / ANIMAL_FEED / COMPOST), not all three collapsing to the same callback.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/sisaguna/android/feature/home/HomeScreen.kt
git commit -m "feat: split Home into Siap Santap and Pakan Ternak & Kompos sections"
```

---

## Self-Review Notes

- **Spec coverage:** rename (Task 1), section split with tint container (Task 2), everything else left untouched (no task touches `HomeViewModel`/`HomeUiState`/`PromoBanner`/`HomeTopBar`/`LocationPickerSheet`/`ListingCard`). Matches the spec fully.
- **Placeholder scan:** none — both tasks have complete code and exact commands.
- **Type consistency:** `CategoryTile(tier, labelRes, image, bg, onClick, modifier)` (Task 2 Step 1) is called identically from both `SiapSantapHeaderAndTile` and `PakanTernakSection` in the same step. `RailBlock`'s signature matches `listingRail`'s parameter order/types exactly (both take `title, subtitle, trailing, listings, now, onListingClick`), so no confusion between the two call sites.
- **Review Focus:** all four items map to Task 2 Step 4's manual checks (Preview-based, per this codebase's established convention — no Compose UI test harness exists, consistent with sub-project 1).
