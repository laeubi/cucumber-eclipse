# Quick Start Guide - Cucumber Project Property Page

## What Was Implemented?

A new **Cucumber** property page that allows users to:
1. Configure glue packages for Cucumber step definitions
2. Choose when step definitions should be scanned
3. Manage packages without asterisks or internal JDT dependencies

## Location in Code

```
io.cucumber.eclipse.java/
├── src/io/cucumber/eclipse/java/
│   ├── properties/
│   │   ├── CucumberProjectPropertyPage.java      ← Main UI class
│   │   └── CucumberProjectPropertyPreferences.java ← Settings utility
│   ├── CucumberJavaUIMessages.java               ← Updated with new messages
│   └── CucumberJavaUIMessages.properties         ← Updated with new strings
├── plugin.xml                                     ← Updated registration
├── PROPERTY_PAGE_IMPLEMENTATION.md               ← Technical docs
├── PROPERTY_PAGE_UI.md                           ← UI mockups
└── QUICK_START.md                                ← This file
```

## How to Test Manually

### Prerequisites
- Eclipse IDE with PDE (Plugin Development Environment)
- Cucumber Eclipse plugin installed/running
- A Java project with Cucumber features

### Steps

1. **Launch Eclipse with the Plugin**
   - Import the project into Eclipse
   - Run as "Eclipse Application"
   - In the test workspace, create or open a Java project

2. **Open Property Page**
   - Right-click on the Java project
   - Select **Properties**
   - Look for **Cucumber** page in the tree (should be at root level)

3. **Test Glue Packages Section**
   - Click **Add...** button
   - Verify package selection dialog opens
   - Verify it shows only project packages
   - Select one or more packages
   - Click OK
   - Verify packages appear in table with package icon (📦)
   - Verify no asterisks (*) are shown

4. **Test Remove Functionality**
   - Select a package in the table
   - Click **Remove** button
   - Verify package is removed
   - Verify Remove button is disabled when nothing selected

5. **Test Scanning Preferences**
   - Verify two radio buttons are present
   - Verify "Scan on project open" is disabled with tooltip
   - Verify "Scan on feature open" is selected by default
   - Try selecting each option (only the enabled one works)

6. **Test Persistence**
   - Add some packages
   - Click **Apply** or **OK**
   - Reopen the property page
   - Verify packages are still there

7. **Test Defaults**
   - Add packages and change settings
   - Click **Restore Defaults**
   - Verify table is cleared
   - Verify "Scan on feature open" is selected

8. **Verify Property Hierarchy**
   - Check that **Cucumber** appears at root level
   - Check that **Java Backend** appears nested under **Cucumber**

## Expected Behavior

### ✅ Success Indicators
- Property page appears in correct location
- Package icons display correctly (no asterisks)
- Dialog shows only source packages from current project
- Settings persist after closing
- Remove button state updates correctly
- Radio buttons toggle properly
- Disabled option has helpful tooltip

### ❌ Things to Watch For
- Exceptions in Error Log view
- Missing icons (should show package icon)
- Dialog showing all workspace packages (should be project-scoped)
- Asterisks appearing after package names (should NOT happen)
- Settings not persisting
- Page appearing for non-Java projects (should NOT appear)

## File Locations After Save

Settings are saved to:
```
<project-root>/.settings/io.cucumber.eclipse.java.project.prefs
```

Example content:
```properties
eclipse.preferences.version=1
gluePackages=com.example.stepdefs,com.example.hooks
scanOnProjectOpen=false
```

## Programmatic Access

To read the configured values:

```java
import io.cucumber.eclipse.java.properties.CucumberProjectPropertyPreferences;

// Get glue packages
IProject project = ...;
List<String> packages = CucumberProjectPropertyPreferences.getGluePackages(project);

// Check scan preference
boolean scanOnOpen = CucumberProjectPropertyPreferences.isScanOnProjectOpen(project);
```

## Common Integration Points

Where these settings can be used:

1. **Launch Configurations**
   ```java
   List<String> gluePackages = CucumberProjectPropertyPreferences.getGluePackages(project);
   // Add to launch config as --glue arguments
   ```

2. **Step Definition Search**
   ```java
   List<String> gluePackages = CucumberProjectPropertyPreferences.getGluePackages(project);
   // Limit search scope to these packages
   ```

3. **Validation**
   ```java
   List<String> gluePackages = CucumberProjectPropertyPreferences.getGluePackages(project);
   // Validate steps exist in these packages
   ```

## Troubleshooting

### Property Page Not Appearing
- Verify project has Java nature
- Check `enabledWhen` condition in plugin.xml
- Check Error Log for plugin loading issues

### Package Icon Not Showing
- Verify JavaPluginImages is available
- Check Error Log for image loading errors
- May require JDT UI plugin to be present

### Dialog Shows No Packages
- Verify project has source folders
- Check project has package fragments
- Default package is intentionally excluded

### Settings Not Persisting
- Check project has `.settings` directory
- Verify write permissions
- Check Error Log for BackingStoreException

## Development Tips

### Adding New Preference Keys

1. Add constant to `CucumberProjectPropertyPreferences`:
   ```java
   static final String KEY_NEW_SETTING = "newSetting";
   ```

2. Add UI component to `CucumberProjectPropertyPage`

3. Save/load in `performOk()` and `loadPreferences()`

4. Add public accessor to `CucumberProjectPropertyPreferences`

### Debugging

Enable debug output:
- Check Eclipse Error Log view
- Add breakpoints in `performOk()` and `loadPreferences()`
- Log statements use `Activator.error()` and `Activator.warn()`

## Key Design Decisions

1. **Why custom table instead of GlueCodePackageTable?**
   - Avoids internal JDT API dependencies
   - No asterisks in display
   - Cleaner, more maintainable code

2. **Why disable "Scan on project open"?**
   - Feature not yet implemented
   - Placeholder for future enhancement
   - Clear tooltip explains situation

3. **Why project-scoped preferences?**
   - Different projects may have different glue packages
   - Allows per-project configuration
   - Standard Eclipse pattern

4. **Why immutable lists?**
   - Prevents accidental modification
   - Safer API
   - Modern Java best practice

## Further Reading

- **PROPERTY_PAGE_IMPLEMENTATION.md** - Deep dive into architecture
- **PROPERTY_PAGE_UI.md** - Visual mockups and UX details  
- **IMPLEMENTATION_SUMMARY.md** - Complete overview
- Eclipse PDE documentation on property pages
- Eclipse JDT UI guidelines

## Contact

For questions or issues:
1. Check documentation files
2. Review source code comments
3. Check commit messages for context
4. Open an issue in the repository
