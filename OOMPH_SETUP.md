# Oomph Setup Guide for Cucumber Eclipse

This document explains the Oomph setup file and how to use it for setting up the Cucumber Eclipse development environment.

## What is Oomph?

[Eclipse Oomph](https://projects.eclipse.org/projects/tools.oomph) is a framework for automating Eclipse installations and workspace setups. It provides:

- Automated installation of Eclipse IDE with required features
- Automatic project checkout from version control
- Target platform configuration
- Workspace preferences setup
- Working set organization

## Quick Start

### Using the Setup File

1. **Download Eclipse Installer**
   - Get it from: https://www.eclipse.org/downloads/packages/installer

2. **Launch Installer in Advanced Mode**
   - Open the Eclipse Installer
   - Click the hamburger menu (☰) in the top-right
   - Select "Advanced Mode..."

3. **Add Cucumber Eclipse Setup**
   - In the "Product" page, select "Eclipse IDE for Eclipse Committers"
   - Click the "+" icon (green plus) at the top-right of the project list
   - Choose "Github Projects" as the catalog
   - Enter the setup file URL or browse to a local file:
     ```
     https://raw.githubusercontent.com/laeubi/cucumber-eclipse/main/CucumberEclipse.setup
     ```

4. **Configure and Install**
   - Check "Cucumber Eclipse" in the project list
   - Click "Next"
   - Configure installation and workspace locations
   - Set GitHub credentials (optional, for contributors)
   - Choose the target platform version (default: 2025-09)
   - Click "Next" and "Finish"

5. **Wait for Provisioning**
   - Oomph will download and install everything
   - This includes Eclipse IDE, plugins, and project sources
   - Initial setup takes 10-20 minutes depending on your connection

6. **Start Developing**
   - Eclipse launches automatically when complete
   - All projects are imported and ready to use

## What the Setup File Does

The `CucumberEclipse.setup` file automates the following tasks:

### 1. JRE Configuration
- Defines JavaSE-21 as the required JRE
- Sets up the JRE in Eclipse's installed JREs

### 2. Eclipse IDE Configuration
- Sets heap space to 2048MB for better performance
- Installs required Eclipse features:
  - PDE (Plugin Development Environment)
  - M2E (Maven integration)
  - Oomph Setup SDK

### 3. Repository Clone
- Clones the Cucumber Eclipse repository from GitHub
- Supports both HTTPS and SSH URLs
- Allows choosing fork vs. upstream repository

### 4. Target Platform Setup
- Configures the Eclipse target platform
- Supports multiple Eclipse releases:
  - 2025-09 (default)
  - 2024-12
  - 2024-09
  - 2024-06
- Includes M2E update sites
- Resolves Maven dependencies automatically

### 5. Project Import
- Automatically imports all plugin projects:
  - io.cucumber.eclipse.editor
  - io.cucumber.eclipse.java
  - io.cucumber.eclipse.java.plugins
  - io.cucumber.eclipse.python
  - io.cucumber.eclipse.feature
  - io.cucumber.eclipse.product
  - io.cucumber.eclipse.updatesite
  - io.cucumber.eclipse.targetdefinition
- Excludes test projects from target platform

### 6. Working Set Organization
- Creates a "Cucumber Eclipse" working set
- Adds all imported projects to the working set
- Helps organize the workspace

## Configuration Options

### Target Platform Selection

During setup, you can choose which Eclipse platform version to target:

- **2025-09**: Latest release (default, recommended)
- **2024-12**: Stable release
- **2024-09**: Previous stable release
- **2024-06**: Older stable release

The target platform determines which Eclipse APIs are available during development.

### GitHub Configuration

If you plan to contribute, configure your GitHub settings:

- **Username**: Your GitHub username
- **SSH/HTTPS**: Choose your preferred clone method
- **Repository**: Can use upstream or your fork

### Installation Locations

Choose where to install:

- **Installation Location**: Where Eclipse IDE is installed
- **Workspace Location**: Where project files are stored

## Troubleshooting

### Setup Takes Too Long

- First-time setup downloads large files (Eclipse IDE, plugins, dependencies)
- Expected time: 10-20 minutes
- Check your internet connection
- Try a different Eclipse mirror if slow

### "Cannot resolve target platform"

- The target platform is resolving dependencies
- This is normal and may take 5-10 minutes
- Wait for the progress indicator to complete
- If it fails, try refreshing the target platform

### Import Errors

- Ensure Java 21 is installed and configured
- Check that the target platform is set correctly
- Try cleaning and rebuilding: Project → Clean...

### GitHub Clone Fails

- Check your GitHub credentials
- Verify repository access permissions
- Try HTTPS instead of SSH or vice versa

## Updating Your Setup

### Getting Latest Changes

To update your local repository:

```bash
cd path/to/cucumber-eclipse
git pull origin main
```

Eclipse will automatically rebuild the workspace.

### Updating Target Platform

If the target platform changes:

1. Open `io.cucumber.eclipse.targetdefinition/cucumber.eclipse.targetdefinition.target`
2. Wait for resolution to complete
3. Click "Reload Target Platform" if needed
4. Click "Set as Active Target Platform"

## Manual Setup Alternative

If Oomph setup fails or you prefer manual setup, see the [Manual Setup](README.md#option-2-manual-setup) section in the README.

## Benefits of Using Oomph

- **Consistency**: Everyone gets the same setup
- **Automation**: No manual configuration needed
- **Speed**: Faster than manual setup after initial download
- **Maintenance**: Easy to update setup for new requirements
- **Onboarding**: New contributors can start quickly

## Customizing the Setup

If you want to modify the setup file:

1. Edit `CucumberEclipse.setup`
2. Test changes with Eclipse Installer
3. Submit changes via pull request
4. Setup file uses Eclipse Modeling Framework (EMF) format
5. Can edit with Eclipse Setup Editor (File → Open With → Setup Editor)

## Additional Resources

- [Oomph Documentation](https://wiki.eclipse.org/Eclipse_Oomph_Installer)
- [Oomph Authoring Guide](https://wiki.eclipse.org/Eclipse_Oomph_Authoring)
- [Eclipse Installer Download](https://www.eclipse.org/downloads/packages/installer)
- [Cucumber Eclipse Wiki](https://github.com/cucumber/cucumber-eclipse/wiki)

## Support

If you encounter issues with the Oomph setup:

1. Check this documentation
2. Review the troubleshooting section
3. Try manual setup as alternative
4. Report issues on GitHub: https://github.com/cucumber/cucumber-eclipse/issues
