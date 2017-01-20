package com.sample.rally;

import java.util.ArrayList;
import java.util.HashMap;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;

import com.rallydev.rest.request.CreateRequest;
import com.rallydev.rest.request.DeleteRequest;
import com.rallydev.rest.request.QueryRequest;
import com.rallydev.rest.response.CreateResponse;
import com.rallydev.rest.response.DeleteResponse;
import com.rallydev.rest.response.QueryResponse;
import com.rallydev.rest.util.QueryFilter;
import com.sample.rally.data.Project;
import com.sample.rally.data.ProjectPermission;

public class MakeProjectReadOnly extends RallyTask
{
	private String		sourceProjectName		= "";
	protected String	sourceWorkspace			= "";
	protected String	sourceWorkspaceId		= "";
	protected Boolean	includeChildren			= false;
	protected Boolean	includeProjectAdmins	= false;

	private void switchToViewer(ProjectPermission permission)
	{
		try
		{
			DeleteRequest deleteRequest = new DeleteRequest(permission.get_ref());
			DeleteResponse deleteResponse = restApi.delete(deleteRequest);
			printErrorsAndWarnings(deleteResponse);
			permission.setRole("Viewer");
			CreateRequest create = new CreateRequest(permission.getQueryName(), permission.create());
			CreateResponse createResponse = restApi.create(create);
			printErrorsAndWarnings(createResponse);
		}
		catch (Exception e)
		{
			log.error("Error updating user: " + permission.getUser(), e);
		}
	}

	@Override
	public void performTask() throws Exception
	{
		sourceWorkspaceId = getWorkspaceReference(sourceWorkspace);
		HashMap<String, Project> projectMap = loadProjectTree(sourceWorkspaceId, sourceProjectName, includeChildren, false, true);

		ArrayList<String> users = new ArrayList<String>();
		for (Project project : projectMap.values())
		{
			for (String user : project.getEditors())
			{
				if (!users.contains(user))
					users.add(user);
			}
		}
		for (String user : users)
		{
			QueryRequest permissionRequest = new ProjectPermission().getQueryRequest();
			permissionRequest.setQueryFilter(new QueryFilter("Role", "=", "Editor"));
			permissionRequest.setQueryFilter(new QueryFilter("User", "=", user));
			permissionRequest.setWorkspace(sourceWorkspaceId);
			log.info("Querying project permissions for user: " + user);
			QueryResponse response = restApi.query(permissionRequest);
			printErrorsAndWarnings(response);

			int count = 0;
			log.info("Found " + response.getTotalResultCount() + " permissions matching role criteria");
			for (int i = 0; i < response.getTotalResultCount(); i++)
			{
				ProjectPermission permission = new ProjectPermission();
				permission.readObject(response.getResults().get(i).getAsJsonObject());
				if (projectMap.containsKey(permission.getProject()))
				{
					log.info(++count + ": Found an editor, switching to viewer");
					switchToViewer(permission);
				}
			}
		}

		if (includeProjectAdmins)
		{
			QueryRequest permissionRequest = new ProjectPermission().getQueryRequest();
			permissionRequest.setQueryFilter(new QueryFilter("Role", "=", "Admin"));
			permissionRequest.setWorkspace(sourceWorkspaceId);
			log.info("Querying project permissions for project admins, this can take a long time...");
			QueryResponse response = restApi.query(permissionRequest);
			printErrorsAndWarnings(response);

			Integer count = 0;
			log.info("Found " + response.getTotalResultCount() + " permissions matching role criteria");
			for (int i = 0; i < response.getTotalResultCount(); i++)
			{
				ProjectPermission permission = new ProjectPermission();
				permission.readObject(response.getResults().get(i).getAsJsonObject());
				if (projectMap.containsKey(permission.getProject()))
				{
					log.info(++count + ": Found a project admin, switching to viewer");
					switchToViewer(permission);
				}
			}
		}
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
