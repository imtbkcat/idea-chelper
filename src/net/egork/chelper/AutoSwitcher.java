package net.egork.chelper;

import com.intellij.execution.RunManagerAdapter;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.impl.RunManagerImpl;
import com.intellij.execution.impl.RunnerAndConfigurationSettingsImpl;
import com.intellij.openapi.components.ProjectComponent;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.FileEditorManagerAdapter;
import com.intellij.openapi.fileEditor.FileEditorManagerEvent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import net.egork.chelper.configurations.TaskConfiguration;
import net.egork.chelper.configurations.TopCoderConfiguration;
import net.egork.chelper.task.Task;
import net.egork.chelper.task.TopCoderTask;
import net.egork.chelper.util.FileUtilities;
import net.egork.chelper.util.TaskUtilities;
import net.egork.chelper.util.Utilities;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

/**
 * @author Egor Kulikov (egor@egork.net)
 */
public class AutoSwitcher implements ProjectComponent {
	private final Project project;
	private boolean busy;

	public AutoSwitcher(Project project) {
		this.project = project;
	}

	public void initComponent() {
		// TODO: insert component initialization logic here
	}

	public void disposeComponent() {
		// TODO: insert component disposal logic here
	}

	@NotNull
	public String getComponentName() {
		return "AutoSwitcher";
	}

	public void projectOpened() {
		RunManagerImpl.getInstanceImpl(project).addRunManagerListener(new RunManagerAdapter() {
			@Override
			public void runConfigurationSelected() {
				RunnerAndConfigurationSettings selectedConfiguration =
					RunManagerImpl.getInstanceImpl(project).getSelectedConfiguration();
				if (selectedConfiguration == null)
					return;
				RunConfiguration configuration = selectedConfiguration.getConfiguration();
				if (busy ||
					!(configuration instanceof TopCoderConfiguration || configuration instanceof TaskConfiguration))
				{
					return;
				}
				busy = true;
				VirtualFile toOpen = null;
				if (configuration instanceof TopCoderConfiguration)
					toOpen = TaskUtilities.getFile(Utilities.getData(project).defaultDirectory, ((TopCoderConfiguration) configuration).getConfiguration().name, project);
				else if (configuration instanceof TaskConfiguration)
					toOpen = FileUtilities.getFileByFQN(((TaskConfiguration) configuration).getConfiguration().taskClass, configuration.getProject());
				if (toOpen != null) {
					final VirtualFile finalToOpen = toOpen;
					SwingUtilities.invokeLater(new Runnable() {
						public void run() {
							FileEditorManager.getInstance(project).openFile(finalToOpen, true);
						}
					});
				}
				busy = false;
			}
		});
		FileEditorManager.getInstance(project).addFileEditorManagerListener(new FileEditorManagerAdapter() {
			@Override
			public void fileOpened(FileEditorManager source, VirtualFile file) {
				selectTask(file);
			}

			private void selectTask(VirtualFile file) {
				if (busy || file == null)
					return;
				RunManagerImpl runManager = RunManagerImpl.getInstanceImpl(project);
				for (RunConfiguration configuration : runManager.getAllConfigurations()) {
					if (configuration instanceof TopCoderConfiguration) {
						TopCoderTask task = ((TopCoderConfiguration) configuration).getConfiguration();
						if (file.equals(TaskUtilities.getFile(Utilities.getData(project).defaultDirectory, task.name, project))) {
							busy = true;
							runManager.setActiveConfiguration(new RunnerAndConfigurationSettingsImpl(runManager,
								configuration, false));
							busy = false;
							return;
						}
					} else if (configuration instanceof TaskConfiguration) {
						Task task = ((TaskConfiguration) configuration).getConfiguration();
						if (file.equals(FileUtilities.getFileByFQN(task.taskClass, configuration.getProject()))) {
							busy = true;
							runManager.setActiveConfiguration(new RunnerAndConfigurationSettingsImpl(runManager,
								configuration, false));
							busy = false;
							return;
						}
					}
				}
			}

			@Override
			public void selectionChanged(FileEditorManagerEvent event) {
				selectTask(event.getNewFile());
			}
		});
	}

	public void projectClosed() {
		// called when project is being closed
	}
}
