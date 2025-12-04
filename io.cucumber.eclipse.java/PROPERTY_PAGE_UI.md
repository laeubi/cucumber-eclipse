# Cucumber Project Property Page - UI Layout

## Overview

This document describes the visual layout and user interaction flow for the new Cucumber Project Property Page.

## Access Path

```
Right-click Java Project → Properties → Cucumber
```

## Page Layout

```
┌─────────────────────────────────────────────────────────────────┐
│ Cucumber                                                   [🥒] │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│ Configure Cucumber settings for this project.                  │
│                                                                 │
│ ┌─ Cucumber Glue Packages ──────────────────────────────────┐  │
│ │                                                            │  │
│ │ Specify the packages where Cucumber should look for step  │  │
│ │ definitions and hooks. These packages will be used when   │  │
│ │ running and validating features.                           │  │
│ │                                                            │  │
│ │ ┌──────────────────────────────────┐  ┌──────────────┐    │  │
│ │ │ 📦 com.example.stepdefs         │  │  [ Add... ]  │    │  │
│ │ │ 📦 com.example.hooks            │  │              │    │  │
│ │ │ 📦 com.example.common           │  │  [ Remove ]  │    │  │
│ │ │                                  │  │              │    │  │
│ │ │                                  │  │              │    │  │
│ │ │                                  │  │              │    │  │
│ │ └──────────────────────────────────┘  └──────────────┘    │  │
│ └────────────────────────────────────────────────────────────┘  │
│                                                                 │
│ ┌─ Step Definition Scanning ────────────────────────────────┐  │
│ │                                                            │  │
│ │ Select when step definitions should be scanned:           │  │
│ │                                                            │  │
│ │ ⚪ Scan when project opens (to be implemented)            │  │
│ │ 🔘 Scan when feature file opens (current default)         │  │
│ │                                                            │  │
│ └────────────────────────────────────────────────────────────┘  │
│                                                                 │
│                                      [  OK  ] [ Cancel ] [ Apply ]
└─────────────────────────────────────────────────────────────────┘
```

## Property Tree View

When you open Project Properties, the tree on the left will show:

```
Project Properties
├── Resource
├── Builders
├── ...
├── 🥒 Cucumber                    ← NEW ROOT-LEVEL PAGE
│   └── Java Backend               ← EXISTING PAGE (now nested)
├── Java Build Path
├── ...
```

## User Interactions

### Adding Packages

1. User clicks **"Add..."** button
2. Package Selection Dialog opens:
   ```
   ┌─ Select Package ──────────────────────────────┐
   │                                               │
   │ Select packages to add to the Cucumber       │
   │ glue path:                                    │
   │                                               │
   │ ┌───────────────────────────────────────────┐ │
   │ │ 📦 com.example.stepdefs                  │ │
   │ │ 📦 com.example.hooks                     │ │
   │ │ 📦 com.example.common                    │ │
   │ │ 📦 com.example.utils                     │ │
   │ │ 📦 com.myapp.features                    │ │
   │ └───────────────────────────────────────────┘ │
   │                                               │
   │            [  OK  ]     [ Cancel ]            │
   └───────────────────────────────────────────────┘
   ```
3. User selects one or more packages (multi-select enabled)
4. Clicks **OK**
5. Selected packages are added to the table (duplicates are ignored)

### Removing Packages

1. User selects a package in the table
2. Clicks **"Remove"** button
3. Package is removed from the list

Note: Remove button is only enabled when a package is selected.

### Scanning Preferences

Two radio buttons control when step definitions are scanned:

- **Scan when project opens**: (Currently disabled with tooltip: "This option will be implemented in a future version")
  - When enabled, step definitions will be scanned automatically when the project opens
  - Improves performance during feature file editing
  - Requires background job implementation (future enhancement)

- **Scan when feature file opens**: (Default - currently selected)
  - Current behavior
  - Step definitions are scanned when a feature file is opened
  - No change to existing functionality

## Data Persistence

Settings are saved in the project's `.settings` directory:

```
project-root/
├── .settings/
│   └── io.cucumber.eclipse.java.project.prefs
├── src/
├── ...
```

Example preferences file content:
```properties
eclipse.preferences.version=1
gluePackages=com.example.stepdefs,com.example.hooks,com.example.common
scanOnProjectOpen=false
```

## Visual Features

- **Cucumber icon (🥒)**: Displayed next to the page title and in the tree
- **Package icon (📦)**: Standard Eclipse package icon for each entry
- **Group boxes**: Clearly separate different configuration sections
- **Descriptive text**: Explains the purpose of each setting
- **Tooltips**: Additional information on hover (especially for disabled options)
- **Consistent styling**: Matches Eclipse UI conventions

## Validation

- No duplicate packages in the list
- Packages must be from the current project
- Only source packages are shown (excludes binary and default packages)
- Empty package list is valid (will use defaults)

## Integration Points

This property page provides configuration that can be used by:

1. **Launch Configurations**: Use configured glue packages when running features
2. **Step Definition Validation**: Limit search scope to configured packages
3. **Code Completion**: Suggest steps from configured packages only
4. **Navigation**: Ctrl+Click to jump to step definitions in configured packages
5. **Project Scanning**: Future feature to scan on project open

## Accessibility

- All controls are keyboard accessible
- Proper tab order for navigation
- Screen reader compatible labels
- Standard Eclipse shortcut keys work (Alt+O for OK, Alt+C for Cancel, etc.)

## Comparison with Old Implementation

### Before (JavaBackendPropertyPage nested under editor's Cucumber page)

```
Properties
└── Cucumber (from editor plugin)
    └── Java Backend
        - Validation Plugins field
        - Enable project specific settings checkbox
        - Glue Code Package Table (with asterisks)
        - Show Hook button
```

### After (New structure)

```
Properties  
└── Cucumber (NEW - from Java plugin)
    ├── Glue Packages table (clean display)
    ├── Scanning preferences
    └── Java Backend (old page, now nested)
        - (existing content unchanged)
```

## Key Improvements

1. ✅ Root-level "Cucumber" page (better visibility)
2. ✅ User-focused interface (clearer labels and descriptions)
3. ✅ Clean package display (no asterisks)
4. ✅ No dependency on internal JDT filter tables
5. ✅ Future-ready (scanning preference placeholder)
6. ✅ Proper project scoping
7. ✅ Consistent with Eclipse UI patterns
