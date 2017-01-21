package com.sample.rally;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;

import com.sample.rally.controller.ProjectController;
import com.sample.rally.controller.WorkItemController;
import com.sample.rally.controller.WorkspaceController;
import com.sample.rally.data.Project;

public class FixStoryPermissions extends RallyTask
{
	private String		sourceProjectName;
	protected String	sourceWorkspace	= "";
	protected String	destWorkspace	= "";
	protected Boolean	includeChildren	= false;

	@Override
	public void performTask() throws Exception
	{
		ProjectController project = new ProjectController();
		WorkItemController workItem = new WorkItemController();

		String sourceWorkspaceId = WorkspaceController.getWorkspaceReference(sourceWorkspace);
		String destWorkspaceId = WorkspaceController.getWorkspaceReference(destWorkspace);

		Project sourceProject = project.getProjectReference(sourceWorkspaceId, sourceProjectName);
		Project destProject = project.getProjectReference(destWorkspaceId, sourceProjectName);

		workItem.fixStoryPermissions(sourceProject, destProject, sourceWorkspaceId, destWorkspaceId, includeChildren);
		workItem.fixDefectPermissions(sourceProject, destProject, sourceWorkspaceId, destWorkspaceId, includeChildren);
	}

	public static void main(String[] args)
	{
		FixStoryPermissions task = new FixStoryPermissions();
		task.doIt(args);
	}

	@Override
	public void addTaskCommandLineOptions()
	{
		Option sp = new Option("sp", "sourceProject", true, "Source Project to check for Accepted work items");
		sp.setRequired(true);
		commandLineOptions.addOption(sp);

		Option sw = new Option("sw", "sourceWorkspace", true, "Source workspace name to copy from");
		sw.setRequired(true);
		commandLineOptions.addOption(sw);

		Option dw = new Option("dw", "destWorkspace", true, "Destination workspace name to copy to");
		dw.setRequired(true);
		commandLineOptions.addOption(dw);

		commandLineOptions.addOption("children", false, "Include child projects in the analysis");
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
			sourceProjectName = commands.getOptionValue("sp");
		}

		if (commands.hasOption("children"))
			includeChildren = true;
	}
}
