package com.sample.rally;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;

import com.sample.rally.controller.ProjectController;
import com.sample.rally.controller.UserController;
import com.sample.rally.controller.WorkspaceController;

public class OpennessAndTransparencyMaker extends RallyTask
{
	private String		sourceProject	= "";
	protected String	sourceWorkspace	= "";
	protected Boolean	includeChildren	= false;

	@Override
	public void performTask() throws Exception
	{
		ProjectController project = new ProjectController();
		UserController user = new UserController();

		String workspaceRef = WorkspaceController.getWorkspaceReference(sourceWorkspace);
		user.makeAllUsersViewers(project.loadProjectTree(workspaceRef, sourceProject, includeChildren), workspaceRef);
	}

	public static void main(String[] args)
	{
		OpennessAndTransparencyMaker task = new OpennessAndTransparencyMaker();
		task.doIt(args);
	}

	@Override
	public void addTaskCommandLineOptions()
	{
		commandLineOptions.addOption("children", false, "Flag to include the child projects");
		Option sp = new Option("pr", "project", true, "Project Name");
		sp.setRequired(true);
		commandLineOptions.addOption(sp);

		Option sw = new Option("w", "workspace", true, "Workspace to apply permissions");
		sw.setRequired(true);
		commandLineOptions.addOption(sw);
	}

	@Override
	public void parseTaskCommandLineOptions(CommandLine commands)
	{
		if (commands.hasOption("children"))
		{
			includeChildren = true;
		}
		if (commands.hasOption("w"))
		{
			sourceWorkspace = commands.getOptionValue("w");
		}
		if (commands.hasOption("pr"))
		{
			sourceProject = commands.getOptionValue("pr");
		}
	}
}
