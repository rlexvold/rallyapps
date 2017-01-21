package com.sample.rally;

import java.util.HashMap;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;

import com.sample.rally.controller.ProjectController;
import com.sample.rally.controller.WorkspaceController;
import com.sample.rally.data.Project;

public class CopyProjectStructure extends RallyTask
{
	private String		sourceProjectName	= "";
	protected String	sourceWorkspace		= "";
	protected String	sourceWorkspaceId	= "";
	protected String	destWorkspaceId		= "";
	protected String	destWorkspace		= "";
	protected Boolean	includeChildren		= false;

	@Override
	public void performTask() throws Exception
	{
		ProjectController projectCtl = new ProjectController();

		sourceWorkspaceId = WorkspaceController.getWorkspaceReference(sourceWorkspace);
		destWorkspaceId = WorkspaceController.getWorkspaceReference(destWorkspace);
		HashMap<String, Project> projectMap = projectCtl.loadProjectTree(sourceWorkspaceId, sourceProjectName, includeChildren, true, true);
		for (Project project : projectMap.values())
		{
			projectCtl.getReleasesAndIterations(project);
		}

		Project topProject = ProjectController.getProjectByName(projectMap, sourceProjectName);
		projectCtl.writeProject(topProject, destWorkspaceId);
	}

	public static void main(String[] args)
	{
		CopyProjectStructure task = new CopyProjectStructure();
		task.doIt(args);
	}

	@Override
	public void addTaskCommandLineOptions()
	{
		commandLineOptions.addOption("children", false, "Flag to include the child projects");
		Option sp = new Option("sp", "sourceProject", true, "Source Project Name");
		sp.setRequired(true);
		commandLineOptions.addOption(sp);

		Option sw = new Option("sw", "sourceWorkspace", true, "Source workspace name to copy from");
		sw.setRequired(true);
		commandLineOptions.addOption(sw);

		Option dw = new Option("dw", "destWorkspace", true, "Destination workspace name to copy to");
		dw.setRequired(true);
		commandLineOptions.addOption(dw);
	}

	@Override
	public void parseTaskCommandLineOptions(CommandLine commands)
	{
		if (commands.hasOption("children"))
		{
			includeChildren = true;
		}
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
			sourceProjectName = commands.getOptionValue("sp");
		}
	}
}
