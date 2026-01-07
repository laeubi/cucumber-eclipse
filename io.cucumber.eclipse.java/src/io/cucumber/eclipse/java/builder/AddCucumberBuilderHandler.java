package io.cucumber.eclipse.java.builder;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.ICommand;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.handlers.HandlerUtil;

/**
 * Handler for adding the Cucumber builder to a project.
 * 
 * @author cucumber-eclipse
 */
public class AddCucumberBuilderHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		ISelection selection = HandlerUtil.getCurrentSelection(event);
		if (selection instanceof IStructuredSelection) {
			Object element = ((IStructuredSelection) selection).getFirstElement();
			IProject project = null;

			if (element instanceof IProject) {
				project = (IProject) element;
			} else if (element instanceof IAdaptable) {
				project = ((IAdaptable) element).getAdapter(IProject.class);
			}

			if (project != null) {
				try {
					addBuilder(project);
				} catch (CoreException e) {
					throw new ExecutionException("Failed to add Cucumber builder", e);
				}
			}
		}
		return null;
	}

	/**
	 * Adds the Cucumber builder to the given project.
	 * 
	 * @param project the project to add the builder to
	 * @throws CoreException if the builder could not be added
	 */
	public static void addBuilder(IProject project) throws CoreException {
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
}
