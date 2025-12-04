# Cucumber Project Property Page Implementation

## Overview

This document describes the implementation of the new Cucumber project property page that provides a user-friendly interface for configuring Cucumber settings at the project level.

## Components

### 1. CucumberProjectPropertyPage

**Location:** `io.cucumber.eclipse.java.properties.CucumberProjectPropertyPage`

This is the main UI class that creates the property page interface. It extends Eclipse's `PropertyPage` class.

**Key Features:**
- **Root-level placement:** Registered as a root property page (not nested under another category)
- **Title:** "Cucumber"
- **Java project filtering:** Only appears for projects with the Java nature

**UI Components:**

#### Glue Packages Section
- A `TableViewer` displaying the list of configured glue packages
- Each package is displayed with the standard Eclipse package icon
- Description text explaining the purpose of glue packages
- **Add** button: Opens a package selection dialog
- **Remove** button: Removes selected package(s) from the list

#### Step Definition Scanning Section
- Two radio buttons for selecting when step definitions should be scanned:
  - "Scan when project opens" (to be implemented later - currently disabled)
  - "Scan when feature file opens" (current default behavior)

### 2. CucumberProjectPropertyPreferences

**Location:** `io.cucumber.eclipse.java.properties.CucumberProjectPropertyPreferences`

Utility class for managing project-level preferences.

**Storage:**
- Namespace: `io.cucumber.eclipse.java.project`
- Stored using Eclipse's `IEclipsePreferences` at project scope

**Properties:**
- `gluePackages`: Comma-separated list of package names
- `scanOnProjectOpen`: Boolean flag for scanning preference

**API Methods:**
- `getNode(IProject)`: Returns the preferences node for a project
- `getGluePackages(IProject)`: Retrieves the list of configured glue packages
- `isScanOnProjectOpen(IProject)`: Checks the scanning preference

### 3. UI Messages

**Location:** 
- `io.cucumber.eclipse.java.CucumberJavaUIMessages.java`
- `io.cucumber.eclipse.java.CucumberJavaUIMessages.properties`

All user-visible strings are externalized for internationalization support.

### 4. Plugin Registration

**Location:** `io.cucumber.eclipse.java/plugin.xml`

The property page is registered at lines 64-83:
- Root-level page: `io.cucumber.eclipse.java.properties.cucumberProjectPropertyPage`
- Child page: `JavaBackendPropertyPage` (now categorized under the new root page)
- Only enabled for Java projects (via `enabledWhen` condition)

## Design Decisions

### Why Not Use GlueCodePackageTable?

The existing `GlueCodePackageTable` class was not reused because:
1. It depends on internal JDT API (`org.eclipse.jdt.internal.*`)
2. It displays packages with asterisks (*) appended
3. The requirement explicitly stated to avoid using it

### Custom Table Implementation

Instead, a custom table implementation was created using:
- `TableViewer` with `ArrayContentProvider`
- Custom label provider extending `JavaElementLabelProvider`
- Direct use of `JavaPluginImages.IMG_OBJS_PACKAGE` for package icons
- Clean package name display (without asterisks)

### Package Selection Dialog

Uses Eclipse's `ElementListSelectionDialog` with:
- Filtered list showing only source packages (not binary/default packages)
- `JavaElementLabelProvider` for consistent labeling
- Multi-selection support
- Standard Eclipse UI patterns

### Property Storage

Preferences are stored at the project level using:
- `ProjectScope` for project-specific settings
- Comma-separated format for package lists (simple and readable)
- Eclipse's standard preferences API for persistence

## Future Enhancements

1. **Scan on Project Open:** Currently disabled with a tooltip explaining it will be implemented later. This feature would trigger step definition scanning when a project is opened in the workspace.

2. **Integration with Existing Glue Code Detection:** The configured packages could be used by:
   - Launch configurations
   - Step definition validation
   - Code completion
   - Navigation features

3. **Package Validation:** Add validation to prevent:
   - Duplicate package entries
   - Invalid package names
   - Non-existent packages

4. **Search Scope Integration:** Use the configured glue packages to narrow the search scope for step definitions.

## Usage

1. Right-click on a Java project
2. Select "Properties"
3. Navigate to "Cucumber" in the property pages tree
4. Add packages using the "Add..." button
5. Select scanning preference
6. Click "OK" or "Apply" to save

The settings are stored in the project's `.settings` directory and are project-specific.

## Testing Notes

Since the property page is a UI component within the Eclipse RCP framework, testing requires:
- Running within an Eclipse IDE instance
- Having a Java project with Cucumber features
- Manual verification of UI behavior

The implementation follows Eclipse UI best practices and patterns used elsewhere in the codebase.
