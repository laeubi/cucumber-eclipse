package io.cucumber.eclipse.java.validation;

import static io.cucumber.eclipse.editor.Tracing.PERFORMANCE_STEPS;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
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
 * Job for validating all feature files in a project at once. This is more
 * efficient than validating files individually as it only sets up the Cucumber
 * runtime once and processes all features together.
 */
final class ProjectGlueJob extends Job {

	private ProjectGlueJob oldJob;
	private Runnable listenerRegistration;
	private final IProject project;
	private final List<IFile> featureFiles;

	ProjectGlueJob(ProjectGlueJob oldJob, IProject project, List<IFile> featureFiles) {
		super("Verify Cucumber Glue Code (" + project.getName() + ")");
		this.oldJob = oldJob;
		this.project = project;
		this.featureFiles = featureFiles;
	}

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

		if (featureFiles.isEmpty()) {
			return Status.OK_STATUS;
		}

		try {
			IJavaProject javaProject = JDTUtil.getJavaProject(project);
			if (javaProject == null) {
				return Status.OK_STATUS;
			}

			long start = System.currentTimeMillis();
			DebugTrace debug = Tracing.get();
			debug.traceEntry(PERFORMANCE_STEPS, project);

			// Get properties from the first feature file (they should all be in the same project)
			GherkinEditorDocument firstDoc = GherkinEditorDocument.get(featureFiles.get(0));
			if (firstDoc == null) {
				return Status.OK_STATUS;
			}
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
						// This feature has syntax errors, skip it
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

				// Add validation plugins for the first feature (for now)
				Collection<Plugin> validationPlugins = addValidationPlugins(firstDoc, rt, projectProperties);

				try {
					rt.run(monitor);

					// Process results for each feature file
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

					debug.traceExit(PERFORMANCE_STEPS,
							matchedSteps.size() + " step(s) / " + steps.size() + " step(s) matched, "
									+ snippets.size() + " snippet(s) suggested for " + featureFiles.size()
									+ " feature(s) || total run time " + (System.currentTimeMillis() - start) + "ms)");
				} catch (Throwable e) {
					ILog.get().error("Validate Glue-Code failed for project " + project.getName(), e);
					// Create an error marker on each feature file
					for (IFile file : featureFiles) {
						MarkerFactory.glueValidationError(file,
								"Glue validation failed: " + e.getMessage()
										+ "\nPlease check the error log for more details.",
								"glue_validation_error");
					}
					return Status.OK_STATUS;
				}
			}

			// Register preference change listeners
			registerPreferenceListeners(projectProperties);

		} catch (Exception e) {
			ILog.get().error("Failed to validate project " + project.getName(), e);
			return Status.OK_STATUS;
		}

		return Status.OK_STATUS;
	}

	private CucumberJavaPreferences getProperties(GherkinEditorDocument editorDocument) {
		IResource resource = editorDocument.getResource();
		if (resource != null) {
			return CucumberJavaPreferences.of(resource);
		}
		return CucumberJavaPreferences.of((IResource) null);
	}

	private void addGlueOptions(RuntimeOptionsBuilder runtimeOptions, CucumberJavaPreferences projectProperties) {
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

	private Collection<Plugin> addValidationPlugins(GherkinEditorDocument editorDocument, CucumberRuntime rt,
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
	private void addErrors(Plugin plugin, Map<Integer, String> errors) {
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

	private void registerPreferenceListeners(CucumberJavaPreferences projectProperties) {
		synchronized (this) {
			// Register listeners for preference changes
			List<Runnable> list = new ArrayList<>();
			
			IEclipsePreferences node = projectProperties.node();
			if (node != null) {
				IPreferenceChangeListener listener = new IPreferenceChangeListener() {
					@Override
					public void preferenceChange(PreferenceChangeEvent event) {
						// Revalidate when preferences change
						CucumberGlueValidator.validateProject(project, featureFiles, null);
					}
				};
				node.addPreferenceChangeListener(listener);
				list.add(() -> node.removePreferenceChangeListener(listener));
			}

			IPreferenceStore store = projectProperties.store();
			if (store != null) {
				IPropertyChangeListener propertyListener = new IPropertyChangeListener() {
					@Override
					public void propertyChange(PropertyChangeEvent event) {
						// Revalidate when preferences change
						CucumberGlueValidator.validateProject(project, featureFiles, null);
					}
				};
				store.addPropertyChangeListener(propertyListener);
				list.add(() -> store.removePropertyChangeListener(propertyListener));
			}

			listenerRegistration = () -> {
				list.forEach(Runnable::run);
			};
		}
	}
}
