package io.cucumber.eclipse.java.validation;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

import org.eclipse.core.filebuffers.FileBuffers;
import org.eclipse.core.filebuffers.IDocumentSetupParticipant;
import org.eclipse.core.filebuffers.IFileBuffer;
import org.eclipse.core.filebuffers.IFileBufferListener;
import org.eclipse.core.filebuffers.ITextFileBuffer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.IJobChangeListener;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentListener;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;

import io.cucumber.eclipse.editor.document.GherkinEditorDocument;
import io.cucumber.eclipse.java.Activator;
import io.cucumber.eclipse.java.plugins.CucumberStepDefinition;
import io.cucumber.eclipse.java.plugins.MatchedStep;

/**
 * Performs a dry-run on the document to verify step definition matching
 * 
 * @author christoph
 *
 */
public class CucumberGlueValidator implements IDocumentSetupParticipant {

	private static ConcurrentMap<IDocument, FeatureGlueJob> jobMap = new ConcurrentHashMap<>();
	private static ConcurrentMap<IProject, ProjectGlueJob> projectJobMap = new ConcurrentHashMap<>();
	
	// State management for matched and parsed steps
	private static ConcurrentMap<IDocument, Collection<MatchedStep<?>>> matchedStepsMap = new ConcurrentHashMap<>();
	private static ConcurrentMap<IDocument, Collection<CucumberStepDefinition>> parsedStepsMap = new ConcurrentHashMap<>();
	
	// Global preference listeners
	private static boolean globalListenersInitialized = false;

	static {
		// TODO implement generic DocumentCache class
		FileBuffers.getTextFileBufferManager().addFileBufferListener(new IFileBufferListener() {

			@Override
			public void underlyingFileMoved(IFileBuffer buffer, IPath path) {

			}

			@Override
			public void underlyingFileDeleted(IFileBuffer buffer) {

			}

			@Override
			public void stateValidationChanged(IFileBuffer buffer, boolean isStateValidated) {

			}

			@Override
			public void stateChanging(IFileBuffer buffer) {

			}

			@Override
			public void stateChangeFailed(IFileBuffer buffer) {

			}

			@Override
			public void dirtyStateChanged(IFileBuffer buffer, boolean isDirty) {
			}

			@Override
			public void bufferDisposed(IFileBuffer buffer) {
				if (buffer instanceof ITextFileBuffer) {
					IDocument document = ((ITextFileBuffer) buffer).getDocument();
					FeatureGlueJob remove = jobMap.remove(document);
					if (remove != null) {
						remove.cancel();
					}
					// Clean up state when buffer is disposed
					clearState(document);
				}

			}

			@Override
			public void bufferCreated(IFileBuffer buffer) {

			}

			@Override
			public void bufferContentReplaced(IFileBuffer buffer) {

			}

			@Override
			public void bufferContentAboutToBeReplaced(IFileBuffer buffer) {

			}
		});
	}

	public static void revalidate(IDocument document) {
		validate(document, 0);
	}

	@Override
	public void setup(IDocument document) {
		// Initialize global preference listeners on first setup
		initializeGlobalPreferenceListeners();
		
		document.addDocumentListener(new IDocumentListener() {

			@Override
			public void documentChanged(DocumentEvent event) {
				// TODO configurable
				validate(document, 1000);
			}

			@Override
			public void documentAboutToBeChanged(DocumentEvent event) {

			}
		});
		validate(document, 0);
	}


	private static void validate(IDocument document, int delay) {
		jobMap.compute(document, (key, oldJob) -> {
			if (oldJob != null) {
				oldJob.cancel();
			}
			FeatureGlueJob verificationJob = new FeatureGlueJob(oldJob, () -> GherkinEditorDocument.get(document));
			verificationJob.setUser(false);
			verificationJob.setPriority(Job.DECORATE);
			if (delay > 0) {
				verificationJob.schedule(delay);
			} else {
				verificationJob.schedule();
			}
			return verificationJob;
		});
	}

	/**
	 * Allows to trigger a validation of the document and matching glue codes. This
	 * can be used by other plugins to enforce a validation of a document while
	 * cucumber-eclipse only triggers an update if the user opens an editor.
	 * 
	 * @param editorDocument the document to validate
	 * @return the job triggered for the computation, can be used to wait for the
	 *         computation to be finished or cancel it
	 */
	public static Job validate(GherkinEditorDocument editorDocument) {
		return jobMap.compute(editorDocument.getDocument(), (key, oldJob) -> {
			if (oldJob != null) {
				oldJob.cancel();
			}
			FeatureGlueJob verificationJob = new FeatureGlueJob(oldJob, () -> editorDocument);
			verificationJob.addJobChangeListener(new IJobChangeListener() {

				@Override
				public void sleeping(IJobChangeEvent event) {
				}

				@Override
				public void scheduled(IJobChangeEvent event) {
				}

				@Override
				public void running(IJobChangeEvent event) {
				}

				@Override
				public void done(IJobChangeEvent event) {
					jobMap.compute(editorDocument.getDocument(), (key, currentJob) -> {
						if (currentJob == verificationJob
								&& GherkinEditorDocument.get(editorDocument.getDocument()) == null) {
							// this was a temporary job and there is no editor for it so we need to remove
							// the job from the map as we won'T get notifications from the text-buffer!
							return null;
						}
						return currentJob;
					});
				}

				@Override
				public void awake(IJobChangeEvent event) {

				}

				@Override
				public void aboutToRun(IJobChangeEvent event) {

				}
			});
			verificationJob.setUser(false);
			verificationJob.setPriority(Job.DECORATE);
			verificationJob.schedule();
			return verificationJob;
		});
	}

	/**
	 * Validates all feature files in a project at once. This is more efficient
	 * than validating files individually as it only sets up the Cucumber runtime
	 * once and processes all features together. Any running validation jobs for
	 * individual files in this project will be canceled.
	 * 
	 * @param project      the project containing the feature files
	 * @param featureFiles the list of feature files to validate
	 * @param monitor      progress monitor for cancellation
	 * @return the job triggered for the validation
	 */
	public static Job validateProject(IProject project, List<IFile> featureFiles, IProgressMonitor monitor) {
		return projectJobMap.compute(project, (key, oldJob) -> {
			if (oldJob != null) {
				oldJob.cancel();
			}
			
			// Cancel any individual file validation jobs for files in this project
			for (IFile file : featureFiles) {
				GherkinEditorDocument doc = GherkinEditorDocument.get(file);
				if (doc != null) {
					FeatureGlueJob fileJob = jobMap.get(doc.getDocument());
					if (fileJob != null) {
						fileJob.cancel();
					}
				}
			}
			
			ProjectGlueJob verificationJob = new ProjectGlueJob(oldJob, project, featureFiles);
			verificationJob.addJobChangeListener(new IJobChangeListener() {
				@Override
				public void done(IJobChangeEvent event) {
					projectJobMap.remove(project, verificationJob);
				}

				@Override
				public void sleeping(IJobChangeEvent event) {}
				@Override
				public void scheduled(IJobChangeEvent event) {}
				@Override
				public void running(IJobChangeEvent event) {}
				@Override
				public void awake(IJobChangeEvent event) {}
				@Override
				public void aboutToRun(IJobChangeEvent event) {}
			});
			verificationJob.setUser(false);
			verificationJob.setPriority(Job.BUILD);
			verificationJob.schedule();
			return verificationJob;
		});
	}

	/**
	 * Allows to sync with the current glue code computation
	 * 
	 * @param document the document to sync on
	 * @param monitor  the progress monitor that can be used to cancel the join
	 *                 operation, or null if cancellation is not required. No
	 *                 progress is reported on this monitor.
	 * @throws OperationCanceledException on cancellation
	 * @throws InterruptedException       if the thread was interrupted while
	 *                                    waiting
	 */
	private static FeatureGlueJob sync(IDocument document, IProgressMonitor monitor)
			throws OperationCanceledException, InterruptedException {
		FeatureGlueJob glueJob = jobMap.get(document);
		if (glueJob != null) {
			glueJob.join(TimeUnit.SECONDS.toMillis(30), monitor);
		}
		return glueJob;
	}

	/**
	 * Updates the state for a document after successful validation.
	 * 
	 * @param document the document
	 * @param matchedSteps the matched steps
	 * @param parsedSteps the parsed step definitions
	 */
	static void updateState(IDocument document, Collection<MatchedStep<?>> matchedSteps,
			Collection<CucumberStepDefinition> parsedSteps) {
		if (document != null) {
			matchedStepsMap.put(document, Collections.unmodifiableCollection(matchedSteps));
			parsedStepsMap.put(document, Collections.unmodifiableCollection(parsedSteps));
		}
	}

	/**
	 * Clears the state for a document.
	 * 
	 * @param document the document
	 */
	static void clearState(IDocument document) {
		if (document != null) {
			matchedStepsMap.remove(document);
			parsedStepsMap.remove(document);
		}
	}

	public static Collection<MatchedStep<?>> getMatchedSteps(IDocument document, IProgressMonitor monitor)
			throws OperationCanceledException, InterruptedException {
		if (document != null) {
			// Wait for any running job to complete
			sync(document, monitor);
			// Return the cached state
			Collection<MatchedStep<?>> steps = matchedStepsMap.get(document);
			if (steps != null) {
				return steps;
			}
		}
		return Collections.emptyList();
	}

	public static Collection<CucumberStepDefinition> getAvaiableSteps(IDocument document, IProgressMonitor monitor)
			throws OperationCanceledException, InterruptedException {
		if (document != null) {
			// Wait for any running job to complete
			sync(document, monitor);
			// Return the cached state
			Collection<CucumberStepDefinition> steps = parsedStepsMap.get(document);
			if (steps != null) {
				return steps;
			}
		}
		return Collections.emptyList();
	}
	
	/**
	 * Initializes global preference listeners that will revalidate all open documents
	 * when preferences change.
	 */
	private static synchronized void initializeGlobalPreferenceListeners() {
		if (globalListenersInitialized) {
			return;
		}
		globalListenersInitialized = true;
		
		// Global listener for workspace preferences
		IPreferenceStore store = Activator.getDefault().getPreferenceStore();
		store.addPropertyChangeListener(new IPropertyChangeListener() {
			@Override
			public void propertyChange(PropertyChangeEvent event) {
				revalidateAllOpenDocuments();
			}
		});
	}
	
	/**
	 * Revalidates all currently open documents.
	 */
	private static void revalidateAllOpenDocuments() {
		// Revalidate all documents that have jobs
		for (IDocument document : jobMap.keySet()) {
			validate(document, 0);
		}
	}

}
