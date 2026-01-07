package io.cucumber.eclipse.java.builder;

import org.eclipse.core.expressions.PropertyTester;
import org.eclipse.core.resources.ICommand;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.runtime.CoreException;

/**
 * Property tester to check if a project has the Cucumber builder configured.
 * 
 * @author cucumber-eclipse
 */
public class CucumberBuilderPropertyTester extends PropertyTester {

	private static final String HAS_CUCUMBER_BUILDER = "hasCucumberBuilder";

	@Override
	public boolean test(Object receiver, String property, Object[] args, Object expectedValue) {
		if (HAS_CUCUMBER_BUILDER.equals(property) && receiver instanceof IProject) {
			IProject project = (IProject) receiver;
			return hasBuilder(project);
		}
		return false;
	}

	/**
	 * Checks if the given project has the Cucumber builder.
	 * 
	 * @param project the project to check
	 * @return true if the project has the builder, false otherwise
	 */
	public static boolean hasBuilder(IProject project) {
		try {
			IProjectDescription description = project.getDescription();
			ICommand[] commands = description.getBuildSpec();
			for (int i = 0; i < commands.length; ++i) {
				if (commands[i].getBuilderName().equals(CucumberFeatureBuilder.BUILDER_ID)) {
					return true;
				}
			}
		} catch (CoreException e) {
			// Ignore
		}
		return false;
	}
}
