package io.cucumber.eclipse.java.builder;

import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
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
 * It supports both full and incremental builds.
 * </p>
 * 
 * @author cucumber-eclipse
 */
public class CucumberFeatureBuilder extends IncrementalProjectBuilder {

	public static final String BUILDER_ID = "io.cucumber.eclipse.java.cucumberFeatureBuilder";

	@Override
	protected IProject[] build(int kind, Map<String, String> args, IProgressMonitor monitor) throws CoreException {
		if (kind == FULL_BUILD) {
			fullBuild(monitor);
		} else {
			IResourceDelta delta = getDelta(getProject());
			if (delta == null) {
				fullBuild(monitor);
			} else {
				incrementalBuild(delta, monitor);
			}
		}
		return null;
	}

	@Override
	protected void clean(IProgressMonitor monitor) throws CoreException {
		// Clean is handled by marker deletion which is automatic when resources are cleaned
	}

	/**
	 * Performs a full build of all feature files in the project.
	 */
	private void fullBuild(IProgressMonitor monitor) throws CoreException {
		getProject().accept(new FeatureFileVisitor(monitor));
	}

	/**
	 * Performs an incremental build based on resource delta.
	 */
	private void incrementalBuild(IResourceDelta delta, IProgressMonitor monitor) throws CoreException {
		delta.accept(new FeatureDeltaVisitor(monitor));
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
	 * Delta visitor for incremental builds.
	 */
	private class FeatureDeltaVisitor implements IResourceDeltaVisitor {
		private final IProgressMonitor monitor;

		public FeatureDeltaVisitor(IProgressMonitor monitor) {
			this.monitor = monitor;
		}

		@Override
		public boolean visit(IResourceDelta delta) throws CoreException {
			IResource resource = delta.getResource();
			switch (delta.getKind()) {
			case IResourceDelta.ADDED:
			case IResourceDelta.CHANGED:
				if (resource instanceof IFile) {
					IFile file = (IFile) resource;
					if ("feature".equals(file.getFileExtension())) {
						validateFeatureFile(file, monitor);
					}
				}
				break;
			case IResourceDelta.REMOVED:
				// Markers are automatically removed when resource is deleted
				break;
			}
			return true; // Continue visiting
		}
	}
}
