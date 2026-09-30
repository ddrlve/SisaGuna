# Home Section Split — "Siap Santap" vs "Pakan Ternak & Kompos"

Status: approved for planning
Sub-project: 2 of 4 (Foundation ✅ → Home → Saved & Profile → Activity & Upload)

## Context

Dosen pembimbing feedback (carried over from the original brainstorm this
sub-project traces back to): separate the section for human food from the
section for animal feed/compost on Home, and rename "Makanan Manusia" to
something better — resolved as **"Siap Santap"** (chosen by the user from a
brainstormed shortlist: "Siap Santap" / "Layak Konsumsi" / "Untuk Kamu" /
"Sisa Pangan").

**New finding this session:** live-verified against the actual Figma file
(`LUsLvGVUhvskfA6xrAiSrP`, node `259:10165` "Home") via the Figma MCP server
— not the stale `figma/figma_to_code.md` export. The current `HomeScreen.kt`,
`ListingCard.kt`, and `LocationPickerSheet.kt` already match this frame
closely: same colors, same rail titles/subtitles, same category tile
labels/colors, same card dimensions (200dp/25dp corners), same saved-address
copy. The Figma frame itself has **no section split** — it's one flat list
(Kategori → Terdekat → Hemat → Ternak → Kompos), same as the current
implementation. So "rebuild sesuai Figma" for Home is **already mostly
satisfied**; the section split is a deliberate deviation from Figma per the
dosen's feedback, not a trace of a new frame.

This narrows sub-project 2 to: the rename, the section split itself, and
confirming/keeping everything else (search, promo banner, listing cards,
location picker) exactly as it already is.

## Approach

Restructure `HomeFeedList` in `HomeScreen.kt` into two stacked blocks
instead of one flat list:

**Section "Siap Santap"** (no visual tint — default `Neutral100` page
background, matching Figma's un-split look for this half):
- One category tile (HUMAN) — quick nav into Category List
- "Terdekat dari kamu" rail (unchanged)
- "Buat kamu yang hemat" rail (unchanged)

**Section "Pakan Ternak & Kompos"** (wrapped in a `Neutral50`-tinted rounded
container for visual separation — reusing an existing token, not inventing
a new color):
- Two category tiles side by side (ANIMAL_FEED, COMPOST)
- "Untuk makanan ternakmu" rail (unchanged)
- "Kompos" rail (unchanged)

The existing unified 3-tile `CategoryRow` under a single "Kategori" heading
is removed and replaced by the two per-section tile groups above — this is
the actual mechanism of the split (tiles move with their section instead of
sitting in one shared row).

No changes to `HomeViewModel`, `HomeUiState`, `ListingRepository`,
`ListingCard`, `PromoBanner`, `HomeTopBar`, or `LocationPickerSheet` — all
already Figma-accurate and out of this sub-project's scope.

## Rename

`strings.xml`: `tier_human` = `"Untuk manusia"` → `"Siap Santap"`.

`CategoryListScreen.kt`'s filter chip (`CategoryHeader`, line ~207) hardcodes
`"Untuk manusia"` as a chip label instead of using the string resource —
update that literal too, for consistency, since it's the same UI concept
shown in a different screen.

## Section 2 Container Styling

`SgColor.Neutral50` (`#FAFAFA`) background, `RoundedCornerShape(20.dp)`,
`padding(16.dp)` — matches the corner-radius language already used for
category tiles (20dp) and cards (25dp) elsewhere on this screen. No new
design tokens.

## Testing

Same convention as the rest of this codebase (no Compose UI test
infrastructure): `@Preview` for the new section layout + `compileDebugKotlin`
+ manual visual check. `HomeViewModel`/`HomeUiState` are untouched, so no
new unit-test surface is introduced.

## Out of Scope

- Any other screen's Figma fidelity (Saved, Profile, Activity, Upload —
  sub-projects 3/4).
- Changing how tiers are fetched/grouped in `ListingRepository` — the UI
  groups tiles/rails visually by tier, but the data layer's shape
  (`HomeFeed.nearby/deals/animalFeed/compost`) doesn't change.
