package io.cucumber.eclipse.java.builder;

import java.util.Map;

import org.eclipse.core.resources.ICommand;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;

import io.cucumber.eclipse.editor.document.GherkinEditorDocument;
import io.cucumber.eclipse.java.validation.CucumberGlueValidator;

/**
 * Builder for validating Cucumber feature files in a project.
 * <p>
 * This builder processes all .feature files in the project and triggers
 * validation to update markers for unmatched steps and other glue code issues.
 * Since glue code (Java files, class files) can change and affect validation,
 * this builder always performs a full build rather than incremental builds.
 * </p>
 * 
 * @author cucumber-eclipse
 */
public class CucumberFeatureBuilder extends IncrementalProjectBuilder {

	public static final String BUILDER_ID = "io.cucumber.eclipse.java.cucumberFeatureBuilder";

	@Override
	protected IProject[] build(int kind, Map<String, String> args, IProgressMonitor monitor) throws CoreException {
		// Always do a full build since glue code changes (Java/class files) 
		// can affect validation and it's non-trivial to detect if glue is affected
		getProject().accept(new FeatureFileVisitor(monitor));
		return null;
	}

	@Override
	protected void clean(IProgressMonitor monitor) throws CoreException {
		// Clean is handled by marker deletion which is automatic when resources are cleaned
	}

	/**
	 * Validates a single feature file.
	 */
	private void validateFeatureFile(IFile file, IProgressMonitor monitor) {
		if (monitor.isCanceled()) {
			throw new OperationCanceledException();
		}

		try {
			GherkinEditorDocument document = GherkinEditorDocument.get(file);
			if (document != null) {
				// Trigger validation using CucumberGlueValidator
				CucumberGlueValidator.validate(document);
			}
		} catch (OperationCanceledException e) {
			throw e; // Re-throw cancellation
		} catch (Exception e) {
			// Log but don't fail the build
			ILog.get().error("Failed to validate feature file: " + file.getFullPath(), e);
		}
	}

	/**
	 * Resource visitor for full builds.
	 */
	private class FeatureFileVisitor implements IResourceVisitor {
		private final IProgressMonitor monitor;

		public FeatureFileVisitor(IProgressMonitor monitor) {
			this.monitor = monitor;
		}

		@Override
		public boolean visit(IResource resource) throws CoreException {
			if (resource instanceof IFile) {
				IFile file = (IFile) resource;
				if ("feature".equals(file.getFileExtension())) {
					validateFeatureFile(file, monitor);
				}
			}
			return true; // Continue visiting
		}
	}

	/**
	 * Adds the Cucumber builder to the given project.
	 * 
	 * @param project the project to add the builder to
	 * @throws CoreException if the builder could not be added
	 */
	public static void addBuilder(IProject project) throws CoreException {
		if (!project.isOpen()) {
			return;
		}
		
		IProjectDescription desc = project.getDescription();
		ICommand[] commands = desc.getBuildSpec();

		// Check if builder is already present
		for (int i = 0; i < commands.length; ++i) {
			if (commands[i].getBuilderName().equals(BUILDER_ID)) {
				return; // Already configured
			}
		}

		// Add builder to project
		ICommand[] newCommands = new ICommand[commands.length + 1];
		System.arraycopy(commands, 0, newCommands, 0, commands.length);
		ICommand command = desc.newCommand();
		command.setBuilderName(BUILDER_ID);
		newCommands[newCommands.length - 1] = command;
		desc.setBuildSpec(newCommands);
		project.setDescription(desc, null);
	}

	/**
	 * Removes the Cucumber builder from the given project.
	 * 
	 * @param project the project to remove the builder from
	 * @throws CoreException if the builder could not be removed
	 */
	public static void removeBuilder(IProject project) throws CoreException {
		if (!project.isOpen()) {
			return;
		}
		
		IProjectDescription description = project.getDescription();
		ICommand[] commands = description.getBuildSpec();
		for (int i = 0; i < commands.length; ++i) {
			if (commands[i].getBuilderName().equals(BUILDER_ID)) {
				ICommand[] newCommands = new ICommand[commands.length - 1];
				System.arraycopy(commands, 0, newCommands, 0, i);
				System.arraycopy(commands, i + 1, newCommands, i, commands.length - i - 1);
				description.setBuildSpec(newCommands);
				project.setDescription(description, null);
				return;
			}
		}
	}

	/**
	 * Checks if the given project has the Cucumber builder.
	 * 
	 * @param project the project to check
	 * @return true if the project has the builder, false otherwise
	 */
	public static boolean hasBuilder(IProject project) {
		if (!project.isOpen()) {
			return false;
		}
		try {
			IProjectDescription description = project.getDescription();
			ICommand[] commands = description.getBuildSpec();
			for (int i = 0; i < commands.length; ++i) {
				if (commands[i].getBuilderName().equals(BUILDER_ID)) {
					return true;
				}
			}
		} catch (CoreException e) {
			// Ignore
		}
		return false;
	}
}
