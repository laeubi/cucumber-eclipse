package io.cucumber.eclipse.java.validation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;

import io.cucumber.eclipse.editor.document.GherkinEditorDocument;
import io.cucumber.eclipse.java.plugins.CucumberStepDefinition;
import io.cucumber.eclipse.java.plugins.MatchedStep;
import io.cucumber.eclipse.java.preferences.CucumberJavaPreferences;
import io.cucumber.eclipse.java.runtime.CucumberRuntime;
import io.cucumber.plugin.Plugin;

/**
 * Validation job for a single feature file. Includes support for inline
 * validation plugins specified in the feature file itself.
 * 
 * @author cucumber-eclipse
 */
final class FeatureGlueJob extends AbstractGlueJob<FeatureGlueJob> {

	private Supplier<GherkinEditorDocument> documentSupplier;

	FeatureGlueJob(FeatureGlueJob oldJob, Supplier<GherkinEditorDocument> documentSupplier) {
		super(createJobName(documentSupplier), oldJob);
		this.documentSupplier = documentSupplier;
	}

	private static String createJobName(Supplier<GherkinEditorDocument> documentSupplier) {
		try {
			GherkinEditorDocument doc = documentSupplier.get();
			if (doc != null && doc.getResource() != null) {
				return "Verify Cucumber Glue Code (" + doc.getResource().getName() + ")";
			}
		} catch (Exception e) {
			// Ignore, use default name
		}
		return "Verify Cucumber Glue Code";
	}

	@Override
	protected List<IFile> getFeatureFiles() {
		GherkinEditorDocument doc = documentSupplier.get();
		if (doc != null) {
			IResource resource = doc.getResource();
			if (resource instanceof IFile) {
				return Collections.singletonList((IFile) resource);
			}
		}
		return Collections.emptyList();
	}

	@Override
	protected String getJobName() {
		return getName();
	}

	@Override
	protected void onValidationSuccess(Collection<MatchedStep<?>> matchedSteps,
			Collection<CucumberStepDefinition> parsedSteps) {
		// Update state in CucumberGlueValidator
		GherkinEditorDocument doc = documentSupplier.get();
		if (doc != null) {
			CucumberGlueValidator.updateState(doc.getDocument(), matchedSteps, parsedSteps);
		}
	}

	@Override
	protected Collection<Plugin> addValidationPlugins(GherkinEditorDocument editorDocument, CucumberRuntime rt,
			CucumberJavaPreferences projectProperties) {
		// For single feature files, also check for inline validation-plugin directives
		List<Plugin> validationPlugins = new ArrayList<>();
		IDocument doc = editorDocument.getDocument();
		int lines = doc.getNumberOfLines();
		Set<String> plugins = new LinkedHashSet<>();
		for (int i = 0; i < lines; i++) {
			try {
				IRegion firstLine = doc.getLineInformation(i);
				String line = doc.get(firstLine.getOffset(), firstLine.getLength()).trim();
				if (line.startsWith("#")) {
					String[] split = line.split("validation-plugin:", 2);
					if (split.length == 2) {
						plugins.add(split[1].trim());
					}
				}
			} catch (BadLocationException e) {
			}
		}
		projectProperties.plugins().forEach(plugins::add);
		for (String plugin : plugins) {
			Plugin classpathPlugin = rt.addPluginFromClasspath(plugin);
			if (classpathPlugin != null) {
				validationPlugins.add(classpathPlugin);
			}
		}
		return validationPlugins;
	}

	@Override
	protected void registerPreferenceListeners(CucumberJavaPreferences projectProperties) {
		// Preference listening is now handled globally by CucumberGlueValidator
		// No need for per-job listeners
	}
}
