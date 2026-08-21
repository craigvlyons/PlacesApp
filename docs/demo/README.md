# Places UI Demo

Open [`mobile-directions.html`](./mobile-directions.html) to review the selected Android-first visual direction. This is the approved UI guidance for implementation. The earlier broad concept in [`index.html`](./index.html) is archived and must not be used for implementation decisions.

The mobile comparison uses the agreed product structure:

- Bottom navigation for Saved, Find, and Nearby.
- Neutral cards with a narrow personal-color accent.
- Name, place type, city/area, and truncated notes on Saved cards.
- Type, City, unlabeled Color swatches, and an independent Favorite-heart filter inside the consolidated filter sheet.
- Dedicated Find and Nearby screens so Saved stays focused on organizing the collection.
- A deliberately plain Edit Place child screen that stays close to the current app: no property cards, a neutral form, color-accented header and bottom navigation, an independent Favorite heart, map confirmation, rating, notes, and one clear Save action.
- One selected Option B-derived direction with soft spacing and compact controls.

The current demo uses one consistent screen hierarchy: app bar, primary input/location context, minimal text actions, result count, and content. The home title is **Places** and **Saved** remains the navigation label. The five saved colors are deliberately unlabeled personal grouping choices. Favorite is an independent heart that can coexist with any color. White main app bars use restrained teal branding and a pale-teal active-navigation indicator; the Edit screen uses the selected place color only in its header and bottom navigation. Saved puts Filter and Sort together above the count. Find removes the extra headline and category-chip row, putting place type inside Search filters; every result has a map-preview icon beside Save so its pin can be confirmed first. Nearby puts List/Map in the app bar and keeps distance/type/color/Favorite controls inside its filter sheet.
