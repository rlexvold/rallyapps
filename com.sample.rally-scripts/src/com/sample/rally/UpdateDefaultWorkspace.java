package com.sample.rally;

import java.util.HashMap;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;

import com.sample.rally.controller.ProjectController;
import com.sample.rally.controller.WorkspaceController;
import com.sample.rally.data.Project;

public class UpdateDefaultWorkspace extends RallyTask
{
	private String		sourceProject;
	protected String	sourceWorkspace	= "";
	protected String	destWorkspace	= "";
	private Boolean		includeChildren	= false;

	@Override
	public void performTask() throws Exception
	{
		ProjectController project = new ProjectController();

		String sourceWorkspaceId = WorkspaceController.getWorkspaceReference(sourceWorkspace);
		String destWorkspaceId = WorkspaceController.getWorkspaceReference(destWorkspace);

		HashMap<String, Project> sourceProjectReferences = project.loadProjectTree(sourceWorkspaceId, sourceProject, includeChildren);
		HashMap<String, Project> destProjectReferences = project.loadProjectTree(destWorkspaceId, sourceProject, includeChildren);

		WorkspaceController.setDefaultWorkspace(sourceWorkspaceId, destWorkspaceId, sourceProjectReferences, destProjectReferences);
	}

	public static void main(String[] args)
	{
		UpdateDefaultWorkspace task = new UpdateDefaultWorkspace();
		task.doIt(args);
	}

	@Override
	public void addTaskCommandLineOptions()
	{
		Option sp = new Option("sp", "sourceProject", true, "Source Project to look for in the user profile");
		sp.setRequired(true);
		commandLineOptions.addOption(sp);

		Option sw = new Option("sw", "sourceWorkspace", true, "Source workspace name to copy from");
		sw.setRequired(true);
		commandLineOptions.addOption(sw);

		Option dw = new Option("dw", "destWorkspace", true, "Destination workspace name to copy to");
		dw.setRequired(true);
		commandLineOptions.addOption(dw);

		commandLineOptions.addOption("children", false, "Include the child projects in the task?");
	}

	@Override
	public void parseTaskCommandLineOptions(CommandLine commands)
	{
		if (commands.hasOption("dw"))
		{
			destWorkspace = commands.getOptionValue("dw");
		}
		if (commands.hasOption("sw"))
		{
			sourceWorkspace = commands.getOptionValue("sw");
		}
		if (commands.hasOption("sp"))
		{
			sourceProject = commands.getOptionValue("sp");
		}
		if (commands.hasOption("children"))
			includeChildren = true;
	}
}
