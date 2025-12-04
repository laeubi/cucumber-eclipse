package io.cucumber.eclipse.java.properties;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.ui.JavaElementLabelProvider;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.ui.dialogs.ElementListSelectionDialog;
import org.eclipse.ui.dialogs.PropertyPage;
import org.osgi.service.prefs.BackingStoreException;

import io.cucumber.eclipse.java.Activator;
import io.cucumber.eclipse.java.CucumberJavaUIMessages;

/**
 * Project property page for Cucumber configuration.
 * This page allows users to configure cucumber glue options and step definition scanning preferences.
 * 
 * Note: This class uses JavaPluginImages from internal JDT API to display package icons.
 * This is necessary as there is no public API for obtaining the standard package icon,
 * and this pattern is already used elsewhere in the codebase (e.g., GlueCodePackageTable).
 */
@SuppressWarnings("restriction")
public class CucumberProjectPropertyPage extends PropertyPage {

	private TableViewer gluePackagesViewer;
	private Button addButton;
	private Button removeButton;
	private Button scanOnProjectOpenButton;
	private Button scanOnFeatureOpenButton;
	private List<String> gluePackages;

	public CucumberProjectPropertyPage() {
		setTitle(CucumberJavaUIMessages.CucumberProjectPropertyPage_title);
		setDescription(CucumberJavaUIMessages.CucumberProjectPropertyPage_description);
	}

	@Override
	protected Control createContents(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		composite.setLayout(layout);
		composite.setLayoutData(new GridData(GridData.FILL_BOTH));

		createGluePackagesSection(composite);
		createScanningPreferencesSection(composite);

		loadPreferences();

		return composite;
	}

	private void createGluePackagesSection(Composite parent) {
		Group group = new Group(parent, SWT.NONE);
		group.setText(CucumberJavaUIMessages.CucumberProjectPropertyPage_gluePackages_title);
		group.setLayout(new GridLayout(2, false));
		group.setLayoutData(new GridData(GridData.FILL_BOTH));

		// Description label
		Label descLabel = new Label(group, SWT.WRAP);
		descLabel.setText(CucumberJavaUIMessages.CucumberProjectPropertyPage_gluePackages_description);
		GridData descData = new GridData(GridData.FILL_HORIZONTAL);
		descData.horizontalSpan = 2;
		descData.widthHint = 400;
		descLabel.setLayoutData(descData);

		// Table viewer
		Table table = new Table(group, SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL | SWT.SINGLE | SWT.FULL_SELECTION);
		GridData tableData = new GridData(GridData.FILL_BOTH);
		tableData.heightHint = 150;
		table.setLayoutData(tableData);

		gluePackagesViewer = new TableViewer(table);
		gluePackagesViewer.setContentProvider(ArrayContentProvider.getInstance());
		gluePackagesViewer.setLabelProvider(new JavaElementLabelProvider(JavaElementLabelProvider.SHOW_DEFAULT) {
			@Override
			public String getText(Object element) {
				if (element instanceof String) {
					return (String) element;
				}
				return super.getText(element);
			}

			@Override
			public org.eclipse.swt.graphics.Image getImage(Object element) {
				if (element instanceof String) {
					return JavaPluginImages.get(JavaPluginImages.IMG_OBJS_PACKAGE);
				}
				return super.getImage(element);
			}
		});
		gluePackagesViewer.addSelectionChangedListener(event -> updateButtons());

		// Button composite
		Composite buttonComposite = new Composite(group, SWT.NONE);
		buttonComposite.setLayout(new GridLayout(1, false));
		buttonComposite.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_BEGINNING));

		addButton = new Button(buttonComposite, SWT.PUSH);
		addButton.setText(CucumberJavaUIMessages.CucumberProjectPropertyPage_addButton);
		addButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		addButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				addPackage();
			}
		});

		removeButton = new Button(buttonComposite, SWT.PUSH);
		removeButton.setText(CucumberJavaUIMessages.CucumberProjectPropertyPage_removeButton);
		removeButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		removeButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				removePackage();
			}
		});
	}

	private void createScanningPreferencesSection(Composite parent) {
		Group group = new Group(parent, SWT.NONE);
		group.setText(CucumberJavaUIMessages.CucumberProjectPropertyPage_scanning_title);
		group.setLayout(new GridLayout());
		group.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		Label descLabel = new Label(group, SWT.WRAP);
		descLabel.setText(CucumberJavaUIMessages.CucumberProjectPropertyPage_scanning_description);
		GridData descData = new GridData(GridData.FILL_HORIZONTAL);
		descData.widthHint = 400;
		descLabel.setLayoutData(descData);

		scanOnProjectOpenButton = new Button(group, SWT.RADIO);
		scanOnProjectOpenButton.setText(CucumberJavaUIMessages.CucumberProjectPropertyPage_scanOnProjectOpen);
		scanOnProjectOpenButton.setEnabled(false); // Will be implemented later
		scanOnProjectOpenButton.setToolTipText(CucumberJavaUIMessages.CucumberProjectPropertyPage_scanOnProjectOpen_tooltip);

		scanOnFeatureOpenButton = new Button(group, SWT.RADIO);
		scanOnFeatureOpenButton.setText(CucumberJavaUIMessages.CucumberProjectPropertyPage_scanOnFeatureOpen);
		scanOnFeatureOpenButton.setSelection(true); // Default behavior
	}

	private void addPackage() {
		IProject project = getProject();
		if (project == null) {
			return;
		}

		IJavaProject javaProject = JavaCore.create(project);
		if (!javaProject.exists()) {
			return;
		}

		try {
			List<IPackageFragment> packages = new ArrayList<>();
			for (IJavaElement element : javaProject.getPackageFragments()) {
				if (element instanceof IPackageFragment) {
					IPackageFragment pkg = (IPackageFragment) element;
					if (!pkg.isDefaultPackage() && pkg.getKind() == IPackageFragment.K_SOURCE) {
						packages.add(pkg);
					}
				}
			}

			ElementListSelectionDialog dialog = new ElementListSelectionDialog(getShell(),
					new JavaElementLabelProvider(JavaElementLabelProvider.SHOW_DEFAULT));
			dialog.setTitle(CucumberJavaUIMessages.CucumberProjectPropertyPage_selectPackage_title);
			dialog.setMessage(CucumberJavaUIMessages.CucumberProjectPropertyPage_selectPackage_message);
			dialog.setElements(packages.toArray());
			dialog.setMultipleSelection(true);

			if (dialog.open() == Window.OK) {
				Object[] result = dialog.getResult();
				if (result != null) {
					for (Object obj : result) {
						if (obj instanceof IPackageFragment) {
							String packageName = ((IPackageFragment) obj).getElementName();
							if (!gluePackages.contains(packageName)) {
								gluePackages.add(packageName);
							}
						}
					}
					gluePackagesViewer.setInput(gluePackages);
					updateButtons();
				}
			}
		} catch (JavaModelException e) {
			Activator.error("Failed to load packages for project: " + project.getName(), e);
		}
	}

	private void removePackage() {
		IStructuredSelection selection = (IStructuredSelection) gluePackagesViewer.getSelection();
		if (!selection.isEmpty()) {
			gluePackages.remove(selection.getFirstElement());
			gluePackagesViewer.setInput(gluePackages);
			updateButtons();
		}
	}

	private void updateButtons() {
		removeButton.setEnabled(!gluePackagesViewer.getSelection().isEmpty());
	}

	private void loadPreferences() {
		gluePackages = new ArrayList<>();
		IEclipsePreferences node = getPreferencesNode();
		if (node != null) {
			String packagesStr = node.get(CucumberProjectPropertyPreferences.KEY_GLUE_PACKAGES, "");
			if (!packagesStr.isEmpty()) {
				String[] packages = packagesStr.split(",");
				for (String pkg : packages) {
					String trimmed = pkg.trim();
					if (!trimmed.isEmpty()) {
						gluePackages.add(trimmed);
					}
				}
			}

			boolean scanOnProjectOpen = node.getBoolean(
					CucumberProjectPropertyPreferences.KEY_SCAN_ON_PROJECT_OPEN, false);
			scanOnProjectOpenButton.setSelection(scanOnProjectOpen);
			scanOnFeatureOpenButton.setSelection(!scanOnProjectOpen);
		}

		gluePackagesViewer.setInput(gluePackages);
		updateButtons();
	}

	@Override
	public boolean performOk() {
		IEclipsePreferences node = getPreferencesNode();
		if (node != null) {
			// Save glue packages
			node.put(CucumberProjectPropertyPreferences.KEY_GLUE_PACKAGES, String.join(",", gluePackages));

			// Save scanning preference
			node.putBoolean(CucumberProjectPropertyPreferences.KEY_SCAN_ON_PROJECT_OPEN,
					scanOnProjectOpenButton.getSelection());

			try {
				node.flush();
			} catch (BackingStoreException e) {
				Activator.error("Failed to save Cucumber project preferences for: " + getProject().getName(), e);
			}
		}
		return super.performOk();
	}

	@Override
	protected void performDefaults() {
		gluePackages.clear();
		gluePackagesViewer.setInput(gluePackages);
		scanOnProjectOpenButton.setSelection(false);
		scanOnFeatureOpenButton.setSelection(true);
		updateButtons();
		super.performDefaults();
	}

	private IProject getProject() {
		IResource resource = getElement().getAdapter(IResource.class);
		return resource != null ? resource.getProject() : null;
	}

	private IEclipsePreferences getPreferencesNode() {
		IProject project = getProject();
		return project != null ? CucumberProjectPropertyPreferences.getNode(project) : null;
	}
}
