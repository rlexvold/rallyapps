package com.sample.rally;

import java.util.HashMap;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;

import com.sample.rally.controller.ProjectController;
import com.sample.rally.controller.UserController;
import com.sample.rally.controller.WorkspaceController;
import com.sample.rally.data.Project;

public class MakeProjectReadOnly extends RallyTask
{
	private String		sourceProjectName		= "";
	protected String	sourceWorkspace			= "";
	protected Boolean	includeChildren			= false;
	protected Boolean	includeProjectAdmins	= false;

	@Override
	public void performTask() throws Exception
	{
		ProjectController project = new ProjectController();
		UserController user = new UserController();
		String sourceWorkspaceId = WorkspaceController.getWorkspaceReference(sourceWorkspace);

		HashMap<String, Project> projectMap = project.loadProjectTree(sourceWorkspaceId, sourceProjectName, includeChildren, false, true);

		user.switchUserToViewer(projectMap, sourceWorkspaceId);
		if (includeProjectAdmins)
			user.switchProjectAdminToViewer(projectMap, sourceWorkspaceId);
	}

	public static void main(String[] args)
	{
		MakeProjectReadOnly task = new MakeProjectReadOnly();
		task.doIt(args);
	}

	@Override
	public void addTaskCommandLineOptions()
	{
		commandLineOptions.addOption("children", false, "Flag to include the child projects");
		commandLineOptions.addOption("admin", false, "Flag to include project admins");
		Option sp = new Option("sp", "sourceProject", true, "Source Project Name");
		sp.setRequired(true);
		commandLineOptions.addOption(sp);

		Option sw = new Option("sw", "sourceWorkspace", true, "Source workspace name to copy from");
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
		if (commands.hasOption("admin"))
		{
			includeProjectAdmins = true;
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
