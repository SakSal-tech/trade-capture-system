# UBS Theme Customizations

## Overview

This document details the CSS and visual customizations I made to brand the Trade Capture System with UBS corporate identity.

---

## CSS Custom Properties (CSS Variables)

**File**: `frontend/src/index.css`

I defined CSS variables in the `:root` selector to maintain consistent UBS branding throughout the application:

```css
:root {
  --ubs-red: #e41e26;
  --ubs-bg: #f6f7f9;
  --ubs-text: #0f172a;
  --ubs-accent: var(--ubs-red);
}
```

### Variable Breakdown:

- **`--ubs-red`**: The signature UBS red colour (`#e41e26`) used for branding and accents
- **`--ubs-bg`**: Light grey background (`#f6f7f9`) for a clean, professional appearance
- **`--ubs-text`**: Dark slate text colour (`#0f172a`) for optimal readability
- **`--ubs-accent`**: References `--ubs-red` for accent elements

---

## Custom CSS Classes

**File**: `frontend/src/index.css`

I created reusable CSS classes that apply UBS theming to major components:

### `.ubs-root`

Applied to the main application container (`Layout.tsx`):

```css
.ubs-root {
  background-color: var(--ubs-bg);
  color: var(--ubs-text);
  font-family: Inter, ui-sans-serif, system-ui, -apple-system, "Segoe UI", Roboto,
    "Helvetica Neue", Arial;
  min-height: 100vh;
}
```

**Purpose**: Sets the overall background, text colour, and font family for the entire application.

**Where used**: `frontend/src/components/Layout.tsx` (line 10)

### `.ubs-navbar`

Applied to the navigation bar:

```css
.ubs-navbar {
  /* Use a neutral background so the logo remains visible. Removed the heavy red banner. */
  background-color: #ffffff;
  color: var(--ubs-text);
  border-bottom: 1px solid #e5e7eb;
}
```

**Purpose**: Creates a clean white navbar with a subtle grey border. I deliberately avoided a heavy red background to ensure the UBS logo remains clearly visible.

**Where used**: `frontend/src/components/Navbar.tsx` (line 60)

### `.ubs-sidebar`

Applied to the sidebar navigation:

```css
.ubs-sidebar {
  background-color: #ffffff;
  color: var(--ubs-text);
}
```

**Purpose**: Matches the sidebar styling to the navbar for visual consistency.

**Where used**: `frontend/src/components/Sidebar.tsx` (line 88)

---

## Logo Integration

### UBS Logo Asset

**File**: `frontend/src/assets/ubs-logo.png`

I added the official UBS logo as a PNG file to ensure accurate rendering across all browsers.

### Logo Implementation

**File**: `frontend/src/components/Navbar.tsx`

**Import** (line 4):

```tsx
import logo from "../assets/ubs-logo.png";
```

**Usage** (line 62):

```tsx
<img src={logo} alt="logo" className="ml-2 w-24 h-auto" />
```

The logo is displayed in the navbar at 24rem width (`w-24`) with automatic height scaling (`h-auto`) and left margin spacing (`ml-2`).

**Accompanying Text** (lines 63-65):

```tsx
<div className="font-bold font-sans text-lg text-black">Trading Platform</div>
```

The text "Trading Platform" appears next to the logo to identify the application.

---

## Dashboard Branding

**File**: `frontend/src/pages/TradeDashboard.tsx`

**Dashboard Title** (line 162):

```tsx
<h1 className="text-2xl font-semibold">UBS Trader Dashboard</h1>
```

I updated the dashboard heading to explicitly reference UBS, reinforcing the branding throughout the user experience.

---

## How Tailwind and Custom CSS Work Together

In this project, I used a hybrid approach:

1. **Custom CSS Variables**: Defined in `index.css` to store UBS brand colours that can be referenced throughout the application

2. **Custom CSS Classes**: Created `.ubs-root`, `.ubs-navbar`, and `.ubs-sidebar` for consistent theming of major components

3. **Tailwind Utility Classes**: Applied directly in TSX files for component-specific styling (e.g., `className="ml-2 w-24 h-auto"` on the logo)

This approach gives me:

- **Consistency**: CSS variables ensure UBS colours are identical everywhere
- **Maintainability**: Changing the UBS red value in one place updates the entire application
- **Flexibility**: Tailwind utilities handle layout and spacing, whilst custom classes handle branding

---

## Files Modified

| File                                    | Change                                                       |
| --------------------------------------- | ------------------------------------------------------------ |
| `frontend/src/index.css`                | Added CSS variables and custom `.ubs-*` classes              |
| `frontend/src/components/Layout.tsx`    | Applied `.ubs-root` class to main container                  |
| `frontend/src/components/Navbar.tsx`    | Imported and displayed UBS logo, applied `.ubs-navbar` class |
| `frontend/src/components/Sidebar.tsx`   | Applied `.ubs-sidebar` class                                 |
| `frontend/src/pages/TradeDashboard.tsx` | Changed heading to "UBS Trader Dashboard"                    |
| `frontend/src/assets/ubs-logo.png`      | Added UBS logo image asset                                   |

---

## Design Decisions

### Why white navbar instead of red?

I initially considered using the UBS red (`#e41e26`) as the navbar background, but this created visual issues:

- The logo became less visible against a coloured background
- The heavy red banner dominated the interface and reduced readability

The white navbar with subtle grey border provides:

- Clear logo visibility
- Professional, clean appearance
- Better visual hierarchy (logo stands out)

### Why CSS variables instead of hard-coded values?

CSS variables allow:

- Single source of truth for brand colours
- Easy theme updates (change one value, update entire app)
- Semantic naming (`--ubs-red` is clearer than `#e41e26`)
- Potential for future theme switching (light/dark modes)

### Why combine Tailwind with custom CSS?

- **Custom CSS**: Handles application-wide theming and brand identity
- **Tailwind**: Handles component-specific layout, spacing, and responsive design
- **Result**: Clean separation of concerns with maximum flexibility

---

## Summary

I customized the Trade Capture System with UBS branding by creating CSS variables for brand colours, defining reusable CSS classes for major components (`.ubs-root`, `.ubs-navbar`, `.ubs-sidebar`), and integrating the UBS logo in the navbar. The white navbar design ensures logo visibility whilst maintaining professional aesthetics. This hybrid approach combining custom CSS with Tailwind utilities provides consistent branding with flexible styling capabilities.
