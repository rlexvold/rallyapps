package com.sample.rally;

import java.util.ArrayList;
import java.util.HashMap;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;

import com.rallydev.rest.request.CreateRequest;
import com.rallydev.rest.request.QueryRequest;
import com.rallydev.rest.response.CreateResponse;
import com.rallydev.rest.response.QueryResponse;
import com.rallydev.rest.util.QueryFilter;
import com.sample.rally.data.Project;
import com.sample.rally.data.ProjectPermission;
import com.sample.rally.data.User;

public class OpennessAndTransparencyMaker extends RallyTask
{
	private String								sourceProject		= "";
	protected String							sourceWorkspace		= "";
	protected String							sourceWorkspaceId	= "";
	protected Boolean							includeChildren		= false;
	private HashMap<String, Project>			projectReferences	= new HashMap<String, Project>();
	private HashMap<String, ArrayList<Project>>	userMap				= new HashMap<String, ArrayList<Project>>();

	private void writeEditorPermissions(String userRef, ArrayList<Project> projects) throws Exception
	{
		int count = 0;
		for (Project project : projects)
		{
			log.info(++count + " - " + project.getName());
			ProjectPermission newPermission = new ProjectPermission();
			newPermission.setProject(project.get_ref());
			newPermission.setRole("Viewer");
			newPermission.setWorkspace(sourceWorkspaceId);
			newPermission.setUser(userRef);
			CreateRequest createRequest = new CreateRequest(newPermission.getQueryName(), newPermission.create());
			CreateResponse createResponse = restApi.create(createRequest);
			printErrorsAndWarnings(createResponse);
		}

	}

	@Override
	public void performTask() throws Exception
	{
		sourceWorkspaceId = getWorkspaceReference(sourceWorkspace);
		projectReferences = loadProjectTree(sourceWorkspaceId, sourceProject, includeChildren);
		lookupUsers();
		int count = 0;
		for (String userRef : userMap.keySet())
		{
			log.info(++count + "/" + userMap.keySet().size() + " - Writing " + userMap.get(userRef).size() + " viewer permissions: " + userRef);
			writeEditorPermissions(userRef, userMap.get(userRef));
		}
	}

	private void lookupUsers() throws Exception
	{
		ArrayList<String> enabledUsers = getEnabledUsers();

		int count = 0;
		for (String user : enabledUsers)
		{
			log.info("User " + ++count + " of " + enabledUsers.size());
			QueryRequest permissionsRequest = new ProjectPermission().getQueryRequest();
			permissionsRequest.setQueryFilter(new QueryFilter("User", "=", user));
			QueryResponse permissionResponse = restApi.query(permissionsRequest);
			printErrorsAndWarnings(permissionResponse);

			ArrayList<Project> projectsToAdd = new ArrayList<Project>();
			for (Project tmp : projectReferences.values())
			{
				projectsToAdd.add(tmp);
			}

			for (int i = 0; i < permissionResponse.getTotalResultCount(); i++)
			{
				String projectRef = permissionResponse.getResults().get(i).getAsJsonObject().get("Project").getAsJsonObject().get("_ref").getAsString();
				if (projectReferences.containsKey(projectRef))
					projectsToAdd.remove(projectReferences.get(projectRef));
			}
			if (projectsToAdd.size() > 0)
			{
				userMap.put(user, projectsToAdd);
				log.info(projectsToAdd.size() + " permissions to add for " + user);
			}
		}
	}

	private ArrayList<String> getEnabledUsers() throws Exception
	{
		ArrayList<String> users = new ArrayList<String>();

		QueryRequest userRequest = new User().getQueryRequest();
		userRequest.setQueryFilter(new QueryFilter("Disabled", "=", "false"));
		QueryResponse userResponse = restApi.query(userRequest);
		log.info("Users Found: " + userResponse.getTotalResultCount());

		for (int i = 0; i < userResponse.getTotalResultCount(); i++)
		{
			users.add(userResponse.getResults().get(i).getAsJsonObject().get("_ref").getAsString());
		}
		return users;
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
