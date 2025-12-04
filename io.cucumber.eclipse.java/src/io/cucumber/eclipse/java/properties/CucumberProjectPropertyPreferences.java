package io.cucumber.eclipse.java.properties;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ProjectScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;

/**
 * Manages Cucumber project-specific property preferences.
 */
public final class CucumberProjectPropertyPreferences {

	static final String NAMESPACE = "io.cucumber.eclipse.java.project";
	static final String KEY_GLUE_PACKAGES = "gluePackages";
	static final String KEY_SCAN_ON_PROJECT_OPEN = "scanOnProjectOpen";

	private CucumberProjectPropertyPreferences() {
		// Utility class
	}

	/**
	 * Gets the preferences node for the given project.
	 * 
	 * @param project the project
	 * @return the preferences node
	 */
	public static IEclipsePreferences getNode(IProject project) {
		ProjectScope scope = new ProjectScope(project);
		return scope.getNode(NAMESPACE);
	}

	/**
	 * Gets the glue packages configured for the project.
	 * 
	 * @param project the project
	 * @return list of glue package names
	 */
	public static List<String> getGluePackages(IProject project) {
		IEclipsePreferences node = getNode(project);
		String packagesStr = node.get(KEY_GLUE_PACKAGES, "");
		if (packagesStr.isEmpty()) {
			return List.of();
		}
		return Arrays.stream(packagesStr.split(","))
				.map(String::trim)
				.filter(s -> !s.isEmpty())
				.collect(Collectors.toUnmodifiableList());
	}

	/**
	 * Checks if step definitions should be scanned when the project opens.
	 * 
	 * @param project the project
	 * @return true if scanning on project open is enabled
	 */
	public static boolean isScanOnProjectOpen(IProject project) {
		IEclipsePreferences node = getNode(project);
		return node.getBoolean(KEY_SCAN_ON_PROJECT_OPEN, false);
	}
}
