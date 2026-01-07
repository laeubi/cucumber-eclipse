package io.cucumber.eclipse.editor.validation;

import org.eclipse.core.runtime.ILog;
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
	 * Fully qualified class name of the Java backend validator.
	 */
	String JAVA_VALIDATOR_CLASS = "io.cucumber.eclipse.java.validation.CucumberGlueValidator";

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
			Class<?> validatorClass = Class.forName(JAVA_VALIDATOR_CLASS);
			java.lang.reflect.Method method = validatorClass.getMethod("validate", GherkinEditorDocument.class);
			return (Job) method.invoke(null, editorDocument);
		} catch (ClassNotFoundException e) {
			// Validator not available (Java backend not installed)
			ILog.get().info("Java validator not available: " + JAVA_VALIDATOR_CLASS);
			return null;
		} catch (NoSuchMethodException e) {
			// Method signature changed
			ILog.get().error("Validator method not found", e);
			return null;
		} catch (java.lang.reflect.InvocationTargetException e) {
			// Error during validation
			ILog.get().error("Error invoking validator", e.getCause());
			return null;
		} catch (IllegalAccessException e) {
			// Security or access issue
			ILog.get().error("Cannot access validator", e);
			return null;
		}
	}
}
