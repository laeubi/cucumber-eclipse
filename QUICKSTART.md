# Quick Start Guide for Contributors

This is a quick reference for getting started with Cucumber Eclipse development.

## TL;DR - Fast Track Setup

### Using Oomph (15 minutes)
1. Download [Eclipse Installer](https://www.eclipse.org/downloads/packages/installer)
2. Switch to Advanced Mode (☰ menu)
3. Add setup URL: `https://raw.githubusercontent.com/cucumber/cucumber-eclipse/main/CucumberEclipse.setup`
4. Follow wizard → Done!

### Manual Setup (30 minutes)
```bash
git clone https://github.com/cucumber/cucumber-eclipse.git
# Import in Eclipse → File → Import → Existing Projects
# Set target platform: io.cucumber.eclipse.targetdefinition/cucumber.eclipse.targetdefinition.target
# Project → Clean
```

## What You Need

| Requirement | Version | Download |
|-------------|---------|----------|
| Eclipse IDE | 2024-06+ | [eclipse.org/downloads](https://www.eclipse.org/downloads/packages/) |
| Java JDK | 21+ | [adoptium.net](https://adoptium.net/) |
| Maven | 3.6+ | [maven.apache.org](https://maven.apache.org/) |

## Common Tasks

### Build the Plugin
```bash
mvn clean install
```

### Run in Development Mode
1. Right-click any plugin project
2. `Run As` → `Eclipse Application`

### Run Tests
```bash
mvn clean verify
```

### Install Built Plugin
1. Build: `mvn clean install`
2. Eclipse: `Help` → `Install New Software` → `Add`
3. Point to: `file:/path/to/repo/io.cucumber.eclipse.updatesite/target/repository`

### Update Target Platform
1. Open: `io.cucumber.eclipse.targetdefinition/cucumber.eclipse.targetdefinition.target`
2. Wait for resolution
3. Click "Set as Active Target Platform"

## Project Structure

```
cucumber-eclipse/
├── io.cucumber.eclipse.editor          # Core editor (syntax, content assist)
├── io.cucumber.eclipse.java            # Java/JVM backend
├── io.cucumber.eclipse.java.plugins    # Java extensions
├── io.cucumber.eclipse.python          # Python/Behave backend
├── io.cucumber.eclipse.feature         # Feature definition
├── io.cucumber.eclipse.product         # Product config
├── io.cucumber.eclipse.updatesite      # P2 update site
├── io.cucumber.eclipse.targetdefinition # Target platform
└── examples/                           # Example projects
```

## Key Files

| File | Purpose |
|------|---------|
| `pom.xml` | Maven build configuration |
| `*.target` | Target platform definition |
| `MANIFEST.MF` | OSGi bundle metadata |
| `plugin.xml` | Eclipse extension points |
| `build.properties` | PDE build configuration |

## Troubleshooting

### Build Fails
- Clean workspace: `mvn clean`
- Update target platform
- Check Java 21 is configured

### Can't Resolve Target Platform
- Wait 5-10 minutes (normal)
- Check internet connection
- Try refresh

### Import Errors
- Verify Java 21 installed
- Set target platform
- `Project` → `Clean`

## Need Help?

- 📖 Full docs: [README.md](README.md)
- 🔧 Oomph guide: [OOMPH_SETUP.md](OOMPH_SETUP.md)
- 🤝 Contributing: [CONTRIBUTING.md](CONTRIBUTING.md)
- 🐛 Issues: [github.com/cucumber/cucumber-eclipse/issues](https://github.com/cucumber/cucumber-eclipse/issues)
- 💬 Wiki: [github.com/cucumber/cucumber-eclipse/wiki](https://github.com/cucumber/cucumber-eclipse/wiki)

## Making Changes

1. Fork repository
2. Create feature branch
3. Make changes + add tests
4. Run `mvn clean verify`
5. Submit pull request
6. Address review feedback

## Code Style

- Follow Eclipse Java conventions
- Meaningful names, no abbreviations
- Add comments for complex logic
- Keep changes minimal and focused

---

**Ready to contribute?** Start with the [Oomph setup](OOMPH_SETUP.md) or [manual setup](README.md#option-2-manual-setup)!
