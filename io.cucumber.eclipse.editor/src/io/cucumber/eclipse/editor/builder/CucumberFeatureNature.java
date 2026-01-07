package io.cucumber.eclipse.editor.builder;

import org.eclipse.core.resources.ICommand;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IProjectNature;
import org.eclipse.core.runtime.CoreException;

/**
 * Project nature for Cucumber feature file validation.
 * <p>
 * This nature is used to mark projects that have the Cucumber builder enabled.
 * It manages the association between the project and the builder.
 * </p>
 * 
 * @author cucumber-eclipse
 */
public class CucumberFeatureNature implements IProjectNature {

	public static final String NATURE_ID = "io.cucumber.eclipse.editor.cucumberFeatureNature";

	private IProject project;

	@Override
	public void configure() throws CoreException {
		// Add the builder to the project
		IProjectDescription desc = project.getDescription();
		ICommand[] commands = desc.getBuildSpec();

		// Check if builder is already present
		for (int i = 0; i < commands.length; ++i) {
			if (commands[i].getBuilderName().equals(CucumberFeatureBuilder.BUILDER_ID)) {
				return; // Already configured
			}
		}

		// Add builder to project
		ICommand[] newCommands = new ICommand[commands.length + 1];
		System.arraycopy(commands, 0, newCommands, 0, commands.length);
		ICommand command = desc.newCommand();
		command.setBuilderName(CucumberFeatureBuilder.BUILDER_ID);
		newCommands[newCommands.length - 1] = command;
		desc.setBuildSpec(newCommands);
		project.setDescription(desc, null);
	}

	@Override
	public void deconfigure() throws CoreException {
		// Remove the builder from the project
		IProjectDescription description = getProject().getDescription();
		ICommand[] commands = description.getBuildSpec();
		for (int i = 0; i < commands.length; ++i) {
			if (commands[i].getBuilderName().equals(CucumberFeatureBuilder.BUILDER_ID)) {
				ICommand[] newCommands = new ICommand[commands.length - 1];
				System.arraycopy(commands, 0, newCommands, 0, i);
				System.arraycopy(commands, i + 1, newCommands, i, commands.length - i - 1);
				description.setBuildSpec(newCommands);
				project.setDescription(description, null);
				return;
			}
		}
	}

	@Override
	public IProject getProject() {
		return project;
	}

	@Override
	public void setProject(IProject project) {
		this.project = project;
	}

	/**
	 * Adds the Cucumber nature to the given project.
	 * 
	 * @param project the project to add the nature to
	 * @throws CoreException if the nature could not be added
	 */
	public static void addNature(IProject project) throws CoreException {
		IProjectDescription description = project.getDescription();
		String[] natures = description.getNatureIds();

		// Check if nature is already present
		for (int i = 0; i < natures.length; ++i) {
			if (NATURE_ID.equals(natures[i])) {
				return; // Already has nature
			}
		}

		// Add the nature
		String[] newNatures = new String[natures.length + 1];
		System.arraycopy(natures, 0, newNatures, 0, natures.length);
		newNatures[natures.length] = NATURE_ID;
		description.setNatureIds(newNatures);
		project.setDescription(description, null);
	}

	/**
	 * Removes the Cucumber nature from the given project.
	 * 
	 * @param project the project to remove the nature from
	 * @throws CoreException if the nature could not be removed
	 */
	public static void removeNature(IProject project) throws CoreException {
		IProjectDescription description = project.getDescription();
		String[] natures = description.getNatureIds();

		for (int i = 0; i < natures.length; ++i) {
			if (NATURE_ID.equals(natures[i])) {
				// Remove the nature
				String[] newNatures = new String[natures.length - 1];
				System.arraycopy(natures, 0, newNatures, 0, i);
				System.arraycopy(natures, i + 1, newNatures, i, natures.length - i - 1);
				description.setNatureIds(newNatures);
				project.setDescription(description, null);
				return;
			}
		}
	}

	/**
	 * Checks if the given project has the Cucumber nature.
	 * 
	 * @param project the project to check
	 * @return true if the project has the nature, false otherwise
	 * @throws CoreException if the nature information could not be retrieved
	 */
	public static boolean hasNature(IProject project) throws CoreException {
		return project.hasNature(NATURE_ID);
	}
}
