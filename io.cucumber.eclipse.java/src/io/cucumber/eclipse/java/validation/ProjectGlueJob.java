package io.cucumber.eclipse.java.validation;

import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;

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
		// Project-level jobs should NOT register preference listeners
		// Preference changes are handled globally by CucumberGlueValidator for open documents
	}
}
