package io.cucumber.eclipse.java.validation;

import static io.cucumber.eclipse.editor.Tracing.PERFORMANCE_STEPS;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.IPreferenceChangeListener;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.PreferenceChangeEvent;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.osgi.service.debug.DebugTrace;

import io.cucumber.core.eventbus.IncrementingUuidGenerator;
import io.cucumber.core.feature.GluePath;
import io.cucumber.core.gherkin.FeatureParserException;
import io.cucumber.core.options.RuntimeOptionsBuilder;
import io.cucumber.eclipse.editor.Tracing;
import io.cucumber.eclipse.editor.document.GherkinEditorDocument;
import io.cucumber.eclipse.editor.marker.MarkerFactory;
import io.cucumber.eclipse.java.Activator;
import io.cucumber.eclipse.java.JDTUtil;
import io.cucumber.eclipse.java.plugins.CucumberMatchedStepsPlugin;
import io.cucumber.eclipse.java.plugins.CucumberMissingStepsPlugin;
import io.cucumber.eclipse.java.plugins.CucumberStepDefinition;
import io.cucumber.eclipse.java.plugins.CucumberStepParserPlugin;
import io.cucumber.eclipse.java.plugins.MatchedStep;
import io.cucumber.eclipse.java.preferences.CucumberJavaPreferences;
import io.cucumber.eclipse.java.runtime.CucumberRuntime;
import io.cucumber.plugin.Plugin;

/**
 * Abstract base class for Cucumber glue validation jobs. Provides common
 * functionality for validating feature files against glue code.
 * 
 * @author cucumber-eclipse
 */
abstract class AbstractGlueJob<T extends AbstractGlueJob<T>> extends Job {

	private T oldJob;
	protected Runnable listenerRegistration;

	protected AbstractGlueJob(String name, T oldJob) {
		super(name);
		this.oldJob = oldJob;
	}

	/**
	 * Returns the list of feature files to validate in this job.
	 * 
	 * @return list of feature files to process
	 */
	protected abstract List<IFile> getFeatureFiles();

	/**
	 * Returns the name to use for this job.
	 * 
	 * @return job name
	 */
	protected abstract String getJobName();

	@Override
	protected void canceling() {
		disposeListener();
	}

	protected void disposeListener() {
		synchronized (this) {
			if (listenerRegistration != null) {
				listenerRegistration.run();
				listenerRegistration = () -> {
					// dummy to prevent further registration...
				};
			}
		}
	}

	@Override
	protected IStatus run(IProgressMonitor monitor) {
		if (oldJob != null) {
			try {
				oldJob.join();
				oldJob = null;
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				return Status.CANCEL_STATUS;
			}
		}

		List<IFile> featureFiles = getFeatureFiles();
		if (featureFiles.isEmpty()) {
			return Status.OK_STATUS;
		}

		try {
			// Get the first feature to determine the project
			IFile firstFile = featureFiles.get(0);
			GherkinEditorDocument firstDoc = GherkinEditorDocument.get(firstFile);
			if (firstDoc == null) {
				return Status.OK_STATUS;
			}

			IResource resource = firstDoc.getResource();
			IJavaProject javaProject = JDTUtil.getJavaProject(resource);
			if (javaProject == null) {
				return Status.OK_STATUS;
			}

			long start = System.currentTimeMillis();
			DebugTrace debug = Tracing.get();
			debug.traceEntry(PERFORMANCE_STEPS, resource);

			CucumberJavaPreferences projectProperties = getProperties(firstDoc);

			try (CucumberRuntime rt = CucumberRuntime.create(javaProject)) {
				rt.setGenerator(new IncrementingUuidGenerator());
				RuntimeOptionsBuilder runtimeOptions = rt.getRuntimeOptions();
				runtimeOptions.setDryRun();

				// Add all features to the runtime
				int successfullyAddedFeatures = 0;
				for (IFile file : featureFiles) {
					if (monitor.isCanceled()) {
						return Status.CANCEL_STATUS;
					}

					try {
						GherkinEditorDocument doc = GherkinEditorDocument.get(file);
						if (doc != null) {
							// Clear any existing glue validation error markers for this file
							MarkerFactory.clearGlueValidationError(file, "glue_validation_error");
							rt.addFeature(doc);
							successfullyAddedFeatures++;
						}
					} catch (FeatureParserException e) {
						// This feature has syntax errors, skip it and return cancel
						if (featureFiles.size() == 1) {
							return Status.CANCEL_STATUS;
						}
						ILog.get().info("Skipping feature with syntax errors: " + file.getName());
					}
				}

				if (successfullyAddedFeatures == 0) {
					return Status.OK_STATUS;
				}

				addGlueOptions(runtimeOptions, projectProperties);
				CucumberMissingStepsPlugin missingStepsPlugin = new CucumberMissingStepsPlugin();
				CucumberStepParserPlugin stepParserPlugin = new CucumberStepParserPlugin();
				CucumberMatchedStepsPlugin matchedStepsPlugin = new CucumberMatchedStepsPlugin();
				rt.addPlugin(stepParserPlugin);
				rt.addPlugin(matchedStepsPlugin);
				rt.addPlugin(missingStepsPlugin);

				Collection<Plugin> validationPlugins = addValidationPlugins(firstDoc, rt, projectProperties);

				try {
					rt.run(monitor);

					// Process results
					Map<Integer, String> validationErrors = new HashMap<>();
					for (Plugin plugin : validationPlugins) {
						addErrors(plugin, validationErrors);
					}

					Map<Integer, Collection<String>> snippets = missingStepsPlugin.getSnippets();
					Collection<MatchedStep<?>> matchedSteps = matchedStepsPlugin.getMatchedSteps();
					Collection<CucumberStepDefinition> steps = stepParserPlugin.getStepList();

					// Update markers for all feature files
					for (IFile file : featureFiles) {
						MarkerFactory.validationErrorOnStepDefinition(file, validationErrors, false);
						MarkerFactory.missingSteps(file, snippets, Activator.PLUGIN_ID, false);
					}

					// Store matched steps for single-file jobs
					onValidationSuccess(matchedSteps, steps);

					debug.traceExit(PERFORMANCE_STEPS,
							matchedSteps.size() + " step(s) / " + steps.size() + " step(s) matched, "
									+ snippets.size() + " snippet(s) suggested for " + featureFiles.size()
									+ " feature(s) || total run time " + (System.currentTimeMillis() - start) + "ms)");
				} catch (Throwable e) {
					ILog.get().error("Validate Glue-Code failed", e);
					// Create an error marker on each feature file
					for (IFile file : featureFiles) {
						MarkerFactory.glueValidationError(file,
								getErrorMessage(e),
								"glue_validation_error");
					}
					return Status.OK_STATUS;
				}
			}

			// Register preference change listeners
			registerPreferenceListeners(projectProperties);

		} catch (Exception e) {
			ILog.get().error("Failed to validate features", e);
			return Status.OK_STATUS;
		}

		return monitor.isCanceled() ? Status.CANCEL_STATUS : Status.OK_STATUS;
	}

	/**
	 * Called when validation succeeds. Subclasses can override to store results.
	 * 
	 * @param matchedSteps the matched steps
	 * @param parsedSteps  the parsed step definitions
	 */
	protected void onValidationSuccess(Collection<MatchedStep<?>> matchedSteps,
			Collection<CucumberStepDefinition> parsedSteps) {
		// Default: do nothing
	}

	/**
	 * Gets the error message for validation failures.
	 * 
	 * @param e the exception
	 * @return error message to display
	 */
	protected String getErrorMessage(Throwable e) {
		return "Failed to validate step definitions. Check that your project is properly configured and dependencies are available. See error log for details.";
	}

	/**
	 * Registers preference change listeners. Subclasses should override to
	 * reschedule appropriately.
	 * 
	 * @param projectProperties the project preferences
	 */
	protected abstract void registerPreferenceListeners(CucumberJavaPreferences projectProperties);

	protected CucumberJavaPreferences getProperties(GherkinEditorDocument editorDocument) {
		IResource resource = editorDocument.getResource();
		if (resource != null) {
			return CucumberJavaPreferences.of(resource);
		}
		return CucumberJavaPreferences.of((IResource) null);
	}

	protected void addGlueOptions(RuntimeOptionsBuilder runtimeOptions, CucumberJavaPreferences projectProperties) {
		projectProperties.glueFilter().forEach(gluePath -> {
			gluePath = gluePath.trim();
			if (gluePath.endsWith("*")) {
				gluePath = gluePath.substring(0, gluePath.length() - 1);
			}
			if (gluePath.endsWith("/")) {
				gluePath = gluePath.substring(0, gluePath.length() - 1);
			}
			if (gluePath.endsWith(".")) {
				gluePath = gluePath.substring(0, gluePath.length() - 1);
			}
			try {
				runtimeOptions.addGlue(GluePath.parse(gluePath));
			} catch (RuntimeException e) {
				ILog.get().error("Failed to add glue path: " + gluePath, e);
			}
		});
	}

	protected Collection<Plugin> addValidationPlugins(GherkinEditorDocument editorDocument, CucumberRuntime rt,
			CucumberJavaPreferences projectProperties) {
		List<Plugin> validationPlugins = new ArrayList<>();
		for (String className : projectProperties.plugins()) {
			Plugin plugin = rt.addPluginFromClasspath(className);
			if (plugin != null) {
				validationPlugins.add(plugin);
			}
		}
		return validationPlugins;
	}

	@SuppressWarnings("unchecked")
	protected void addErrors(Plugin plugin, Map<Integer, String> errors) {
		try {
			Method method = plugin.getClass().getMethod("getValidationErrors");
			Object invoke = method.invoke(plugin);
			if (invoke instanceof Map) {
				@SuppressWarnings("rawtypes")
				Map map = (Map) invoke;
				errors.putAll(map);
			}
		} catch (Exception e) {
			// Plugin doesn't support error reporting
		}
	}
}
