---
name: Executive Dark Insight
colors:
  surface: '#051424'
  surface-dim: '#051424'
  surface-bright: '#2c3a4c'
  surface-container-lowest: '#010f1f'
  surface-container-low: '#0d1c2d'
  surface-container: '#122131'
  surface-container-high: '#1c2b3c'
  surface-container-highest: '#273647'
  on-surface: '#d4e4fa'
  on-surface-variant: '#c2c6d6'
  inverse-surface: '#d4e4fa'
  inverse-on-surface: '#233143'
  outline: '#8c909f'
  outline-variant: '#424754'
  surface-tint: '#adc6ff'
  primary: '#adc6ff'
  on-primary: '#002e6a'
  primary-container: '#4d8eff'
  on-primary-container: '#00285d'
  inverse-primary: '#005ac2'
  secondary: '#4edea3'
  on-secondary: '#003824'
  secondary-container: '#00a572'
  on-secondary-container: '#00311f'
  tertiary: '#ffb95f'
  on-tertiary: '#472a00'
  tertiary-container: '#ca8100'
  on-tertiary-container: '#3e2400'
  error: '#ffb4ab'
  on-error: '#690005'
  error-container: '#93000a'
  on-error-container: '#ffdad6'
  primary-fixed: '#d8e2ff'
  primary-fixed-dim: '#adc6ff'
  on-primary-fixed: '#001a42'
  on-primary-fixed-variant: '#004395'
  secondary-fixed: '#6ffbbe'
  secondary-fixed-dim: '#4edea3'
  on-secondary-fixed: '#002113'
  on-secondary-fixed-variant: '#005236'
  tertiary-fixed: '#ffddb8'
  tertiary-fixed-dim: '#ffb95f'
  on-tertiary-fixed: '#2a1700'
  on-tertiary-fixed-variant: '#653e00'
  background: '#051424'
  on-background: '#d4e4fa'
  surface-variant: '#273647'
typography:
  display-lg:
    fontFamily: Inter
    fontSize: 32px
    fontWeight: '700'
    lineHeight: 40px
    letterSpacing: -0.02em
  headline-md:
    fontFamily: Inter
    fontSize: 24px
    fontWeight: '600'
    lineHeight: 32px
    letterSpacing: -0.01em
  title-sm:
    fontFamily: Inter
    fontSize: 18px
    fontWeight: '600'
    lineHeight: 24px
  body-md:
    fontFamily: Inter
    fontSize: 14px
    fontWeight: '400'
    lineHeight: 20px
  body-sm:
    fontFamily: Inter
    fontSize: 13px
    fontWeight: '400'
    lineHeight: 18px
  label-caps:
    fontFamily: Inter
    fontSize: 11px
    fontWeight: '600'
    lineHeight: 16px
    letterSpacing: 0.05em
  data-mono:
    fontFamily: JetBrains Mono
    fontSize: 13px
    fontWeight: '400'
    lineHeight: 18px
rounded:
  sm: 0.25rem
  DEFAULT: 0.5rem
  md: 0.75rem
  lg: 1rem
  xl: 1.5rem
  full: 9999px
spacing:
  unit: 4px
  container-padding: 24px
  gutter: 16px
  row-height-dense: 32px
  row-height-standard: 48px
---

## Brand & Style

The design system is engineered for high-density business management, prioritizing clarity, authority, and reduced eye strain during long-form professional use. The aesthetic is a fusion of **Corporate Modern** and **Minimalism**, characterized by deep charcoal surfaces, precise typography, and a "data-first" philosophy.

The emotional response should be one of competence and reliability. By utilizing a "Deep Navy" foundation rather than true black, we maintain a sense of depth and premium quality that feels like a high-end physical workstation. Visual noise is aggressively reduced to allow data visualizations and status indicators to command attention.

## Colors

This design system utilizes a layered dark palette to establish hierarchy and focus.
- **Primary (#3B82F6):** Used for primary actions, active states, and brand recognition.
- **Surface Layering:** The core background is a deep navy-charcoal (#0F172A). Cards and containers use a slightly lighter charcoal (#1E293B) to create a sense of elevation.
- **Semantic Colors:** Success (Emerald), Warning (Amber), and Danger (Crimson) are used sparingly for status indicators and alerts, ensuring high contrast against the dark background.
- **Borders:** Instead of heavy shadows, use a 1px solid border at 10% white opacity to define element boundaries.

## Typography

The system relies on **Inter** for its neutral, highly legible characteristic in complex UI environments. 
- **Hierarchy:** Use bold weights for page headers and medium weights for sub-sections.
- **Data Display:** For financial figures or ID strings within tables, use **JetBrains Mono** to ensure character alignment and readability.
- **Labels:** Use `label-caps` for table headers and section overviews to differentiate functional labels from user-generated content.
- **Contrast:** Ensure body text uses a high-contrast off-white (Slate 200/300) to maintain readability without the harshness of pure #FFFFFF.

## Layout & Spacing

This design system adopts a **Fixed-Fluid Hybrid** model optimized for desktop:
- **Sidebar:** Fixed width (240px) using the `#1E293B` surface color.
- **Main Content:** Fluid width with a maximum scale-out for ultra-wide monitors, maintaining a 24px inner margin.
- **Grid:** A 12-column grid system with 16px gutters for dashboard widgets.
- **Density:** Provide a "Dense" toggle for data tables, reducing row height from 48px to 32px for power users managing large datasets.

## Elevation & Depth

Depth is conveyed through **Tonal Layers** rather than heavy shadows:
- **Level 0 (Base):** `#0F172A` - The application canvas.
- **Level 1 (Surface):** `#1E293B` - Navigation bars, sidebars, and card backgrounds.
- **Level 2 (Overlay):** `#2D3748` - Modals and dropdown menus, which should include a very soft, large-radius shadow (0px 10px 25px rgba(0,0,0,0.4)) and a subtle white border-top to catch "light."
- **Interaction:** Buttons and interactive cards should transition to a slightly lighter background hex or increase border opacity on hover.

## Shapes

The shape language is professional and balanced. 
- **Components:** Standard UI elements like buttons, inputs, and cards use an 8px (`0.5rem`) radius.
- **Large Surfaces:** Dashboard widgets and main content containers use a 12px (`0.75rem`) radius for a more contemporary feel.
- **Selection Indicators:** Use pill-shaped (full radius) tags for status chips (e.g., "Active", "Pending") to distinguish them from functional buttons.

## Components

### Buttons
- **Primary:** Solid `#3B82F6` with white text. No gradient. 
- **Secondary:** Ghost style with a 1px border (`rgba(255,255,255,0.2)`) and hover state filling the background.
- **Icon Buttons:** Square with 8px radius, used for table actions.

### Data Tables
- **Header:** Background of `#1E293B`, `label-caps` typography, 1px bottom border.
- **Rows:** Alternating zebra striping is discouraged; instead, use a 1px subtle border between rows. On hover, the entire row should highlight with a subtle blue tint (`rgba(59, 130, 246, 0.08)`).

### Input Fields
- **Default State:** Background `#0F172A`, 1px border `rgba(255,255,255,0.1)`.
- **Focus State:** Border color changes to Primary Blue with a 2px outer "glow" (ring) at 20% opacity.

### Cards & Widgets
- Background: `#1E293B`.
- Padding: 20px internally.
- Header: Separated by a thin 1px line from the body content.

### Chips/Badges
- Small, uppercase text. Use low-saturation background tints (e.g., Success is Emerald at 15% opacity with 100% opacity text).