package io.cucumber.eclipse.java.validation;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.IPreferenceChangeListener;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.PreferenceChangeEvent;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;

import io.cucumber.eclipse.java.preferences.CucumberJavaPreferences;

/**
 * Validation job for all feature files in a project at once. This is more
 * efficient than validating files individually as it only sets up the Cucumber
 * runtime once and processes all features together.
 * 
 * @author cucumber-eclipse
 */
final class ProjectGlueJob extends AbstractGlueJob<ProjectGlueJob> {

	private final IProject project;
	private final List<IFile> featureFiles;

	ProjectGlueJob(ProjectGlueJob oldJob, IProject project, List<IFile> featureFiles) {
		super("Verify Cucumber Glue Code (" + project.getName() + ")", oldJob);
		this.project = project;
		this.featureFiles = featureFiles;
	}

	@Override
	protected List<IFile> getFeatureFiles() {
		return featureFiles;
	}

	@Override
	protected String getJobName() {
		return getName();
	}

	@Override
	protected String getErrorMessage(Throwable e) {
		return "Glue validation failed: " + e.getMessage()
				+ "\nPlease check the error log for more details.";
	}

	@Override
	protected void registerPreferenceListeners(CucumberJavaPreferences projectProperties) {
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
