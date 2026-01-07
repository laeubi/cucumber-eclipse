package io.cucumber.eclipse.editor.validation;

import org.eclipse.core.runtime.jobs.Job;

import io.cucumber.eclipse.editor.document.GherkinEditorDocument;

/**
 * Interface for backend-specific glue code validators.
 * <p>
 * Implementations of this interface are responsible for validating feature files
 * against their respective backend step definitions (e.g., Java, Python, etc.).
 * This interface allows the editor to trigger validation without depending on
 * specific backend implementations.
 * </p>
 * 
 * @author cucumber-eclipse
 */
public interface ICucumberGlueValidator {

	/**
	 * Validates the given Gherkin editor document and returns a Job that performs
	 * the validation asynchronously.
	 * <p>
	 * The validation job should:
	 * <ul>
	 * <li>Match steps in the document against available step definitions</li>
	 * <li>Create markers for unmatched steps or validation errors</li>
	 * <li>Update existing markers as needed</li>
	 * </ul>
	 * </p>
	 * 
	 * @param editorDocument the document to validate
	 * @return a Job that performs the validation, or null if validation is not
	 *         supported for this document
	 */
	Job validateDocument(GherkinEditorDocument editorDocument);

	/**
	 * Static utility method to trigger validation using available validators.
	 * This method will find and invoke the appropriate validator for the document.
	 * 
	 * @param editorDocument the document to validate
	 * @return the validation job, or null if no validator is available
	 */
	static Job validate(GherkinEditorDocument editorDocument) {
		// For now, we'll use reflection to call the Java validator if available
		// In the future, this could use OSGi service discovery
		try {
			Class<?> validatorClass = Class.forName("io.cucumber.eclipse.java.validation.CucumberGlueValidator");
			java.lang.reflect.Method method = validatorClass.getMethod("validate", GherkinEditorDocument.class);
			return (Job) method.invoke(null, editorDocument);
		} catch (Exception e) {
			// Validator not available or error occurred
			return null;
		}
	}
}
