package com.sample.rally;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Date;
import java.util.HashMap;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.google.gson.JsonObject;
import com.rallydev.rest.RallyRestApi;
import com.rallydev.rest.request.GetRequest;
import com.rallydev.rest.request.QueryRequest;
import com.rallydev.rest.response.GetResponse;
import com.rallydev.rest.response.QueryResponse;
import com.rallydev.rest.response.Response;
import com.rallydev.rest.util.Fetch;
import com.rallydev.rest.util.QueryFilter;
import com.rallydev.rest.util.Ref;
import com.sample.rally.data.Project;
import com.sample.rally.data.User;

public abstract class RallyTask
{
	protected Logger			log					= LogManager.getLogger(this.getClass());
	protected RallyRestApi		restApi;
	protected String			rallyUrl			= "";
	protected String			username			= "";
	protected String			password			= "";
	protected Date				start				= new Date();
	protected static Options	commandLineOptions	= new Options();

	protected void connectToRally() throws URISyntaxException
	{
		restApi = new RallyOnPremApi(new URI(rallyUrl), username, password);
		restApi.setWsapiVersion("v2.0");
		restApi.setApplicationName(this.getClass().getSimpleName());
	}

	protected Project getProjectReference(String workspace, String projectName) throws Exception
	{
		QueryRequest request = new Project().getQueryRequest();
		request.setWorkspace(workspace);
		request.setQueryFilter(new QueryFilter("Name", "=", projectName));
		QueryResponse response = restApi.query(request);
		printErrorsAndWarnings(response);

		Project project = new Project();
		project.readObject(response.getResults().get(0).getAsJsonObject());
		return project;
	}

	protected void parseCommandLine(String[] args, String className) throws Exception
	{
		CommandLineParser parser = new BasicParser();
		CommandLine commandLine = null;

		try
		{
			commandLine = parser.parse(commandLineOptions, args);
		}
		catch (Exception e)
		{
			System.out.println(e.getMessage());
			HelpFormatter help = new HelpFormatter();
			help.printHelp(className, commandLineOptions);
			System.exit(0);

		}

		if (commandLine.hasOption("h"))
		{
			HelpFormatter help = new HelpFormatter();
			help.printHelp(className, commandLineOptions);
			System.exit(0);
		}
		if (commandLine.hasOption("url"))
		{
			rallyUrl = commandLine.getOptionValue("url");
		}
		if (commandLine.hasOption("u"))
		{
			username = commandLine.getOptionValue("u");
		}
		if (commandLine.hasOption("p"))
		{
			password = commandLine.getOptionValue("p");
		}
		parseTaskCommandLineOptions(commandLine);
	}

	protected String getWorkspaceReference(String name) throws Exception
	{
		QueryRequest request = new QueryRequest("Subscription");
		request.setFetch(new Fetch("Workspaces"));
		log.info("Looking for workspace: " + name);
		QueryResponse response = restApi.query(request);

		request = new QueryRequest(response.getResults().get(0).getAsJsonObject().getAsJsonObject("Workspaces"));
		request.setFetch(new Fetch("Name"));
		response = restApi.query(request);

		for (int i = 0; i < response.getResults().size(); i++)
		{
			JsonObject workspaceObject = response.getResults().get(i).getAsJsonObject();
			if (workspaceObject.get("Name").getAsString().equals(name))
				return workspaceObject.get("_ref").getAsString();
		}
		return null;

	}

	private void getProjectEditors(JsonObject jsonProject, Project project) throws Exception
	{
		QueryRequest request = new QueryRequest(jsonProject.getAsJsonObject("Editors"));
		request.setLimit(1000000);
		request.setFetch(new User().getFetch());
		QueryResponse response = restApi.query(request);

		printErrorsAndWarnings(response);

		int number = response.getTotalResultCount();
		log.info("Number of editors: " + number);
		for (int i = 0; i < number; i++)
		{
			log.info("Reading editor: " + i + " of " + number);
			JsonObject object = response.getResults().get(i).getAsJsonObject();
			String user = object.get("_ref").getAsString();
			project.getEditors().add(user);
		}
	}

	private void getProjectTeamMembers(JsonObject jsonProject, Project project) throws Exception
	{

		QueryRequest request = new QueryRequest(jsonProject.getAsJsonObject("TeamMembers"));
		request.setLimit(1000000);
		request.setFetch(new User().getFetch());
		QueryResponse response = restApi.query(request);

		printErrorsAndWarnings(response);

		int number = response.getTotalResultCount();
		log.info("Number of team members: " + number);
		for (int i = 0; i < number; i++)
		{
			log.info("Reading team members: " + i + " of " + number);
			JsonObject object = response.getResults().get(i).getAsJsonObject();
			String user = object.get("_ref").getAsString();
			project.getTeamMembers().add(user);
		}
	}

	public Project getProjectByName(HashMap<String, Project> projectMap, String projectName)
	{
		for (Project project : projectMap.values())
		{
			if (project.getName().equals(projectName))
				return project;
		}
		return null;
	}

	public HashMap<String, Project> loadProjectTree(String workspaceId, String projectName, Boolean includeChildren, Boolean includeTeamMembers, Boolean includeEditors)
			throws Exception
	{
		HashMap<String, Project> projectMap = new HashMap<String, Project>();

		QueryRequest projectRequest = new Project().getQueryRequest();
		projectRequest.setQueryFilter(new QueryFilter("Name", "=", projectName));
		projectRequest.setWorkspace("/workspace/" + workspaceId);

		log.info("Looking for project: " + projectName + " in workspace: " + workspaceId);
		QueryResponse projectsResponse = restApi.query(projectRequest);
		log.info("Items Found: " + projectsResponse.getTotalResultCount());

		JsonObject object = projectsResponse.getResults().get(0).getAsJsonObject();

		String ref = Ref.getRelativeRef(object.get("_ref").getAsString());

		printErrorsAndWarnings(projectsResponse);

		Project project = getProject(projectMap, workspaceId, null, ref, includeChildren, includeTeamMembers, includeEditors);
		log.info("Read project: " + project.getName());
		return projectMap;

	}

	public HashMap<String, Project> loadProjectTree(String workspaceId, String projectName, Boolean includeChildren) throws Exception
	{
		return loadProjectTree(workspaceId, projectName, includeChildren, false, false);
	}

	private Project getProject(HashMap<String, Project> projectMap, String workspaceId, Project parent, String ref, Boolean includeChildren, Boolean includeTeamMembers,
			Boolean includeEditors) throws Exception
	{
		Project project = new Project();
		project.setWorkspace(workspaceId);
		if (parent != null)
			project.setParent(parent);

		GetRequest projectRequest = new GetRequest(ref);
		projectRequest.setFetch(project.getFetch());

		GetResponse projectResponse = restApi.get(projectRequest);
		printErrorsAndWarnings(projectResponse);

		JsonObject returnObject = projectResponse.getObject();

		project.readObject(returnObject);
		if (includeEditors)
			getProjectEditors(returnObject, project);
		if (includeTeamMembers)
			getProjectTeamMembers(returnObject, project);

		projectMap.put(project.get_ref(), project);

		if (includeChildren)
		{
			getChildren(projectMap, project, includeTeamMembers, includeEditors);
		}
		return project;
	}

	private void getChildren(HashMap<String, Project> projectMap, Project parent, Boolean includeTeamMembers, Boolean includeEditors) throws Exception
	{
		QueryRequest projectRequest = new Project().getQueryRequest();
		projectRequest.setQueryFilter(new QueryFilter("Parent.ObjectID", "=", parent.getOid()));
		projectRequest.setWorkspace("/workspace/" + parent.getWorkspace());

		log.info("Looking for children of project: " + parent.getName() + " in workspace: " + parent.getWorkspace());
		QueryResponse projectsResponse = restApi.query(projectRequest);
		printErrorsAndWarnings(projectsResponse);
		int numProjects = projectsResponse.getTotalResultCount();
		log.info("Items Found: " + numProjects);

		for (int i = 0; i < numProjects; i++)
		{
			JsonObject object = projectsResponse.getResults().get(i).getAsJsonObject();

			String ref = Ref.getRelativeRef(object.get("_ref").getAsString());
			Project project = getProject(projectMap, parent.getWorkspace(), parent, ref, true, includeTeamMembers, includeEditors);
			parent.getChildren().add(project);
			log.info("Read project: " + project);
		}
	}

	public void addCommandLineOptions()
	{
		commandLineOptions.addOption("h", "help", false, "Print this message");

		Option url = new Option("url", "rallyUrl", true, "Rally URL instance");
		url.setRequired(true);
		commandLineOptions.addOption(url);

		Option user = new Option("u", "user", true, "username");
		user.setRequired(true);
		commandLineOptions.addOption(user);

		Option pass = new Option("p", "pass", true, "password");
		pass.setRequired(true);
		commandLineOptions.addOption(pass);

		addTaskCommandLineOptions();
	}

	public abstract void addTaskCommandLineOptions();

	public abstract void performTask() throws Exception;

	public abstract void parseTaskCommandLineOptions(CommandLine commands);

	protected void doIt(String[] args)
	{
		try
		{
			addCommandLineOptions();
			parseCommandLine(args, getClass().getSimpleName());
			connectToRally();
			performTask();
			Date end = new Date();
			Long elapsed = (end.getTime() - start.getTime()) / 1000;
			log.info("Completed task, Total time: " + elapsed + " seconds");
		}
		catch (Throwable e)
		{
			log.error(e);
		}
		finally
		{
			try
			{
				if (restApi != null)
					restApi.close();
			}
			catch (Throwable e)
			{
				log.error(e);
			}
		}

	}

	protected void printErrorsAndWarnings(Response response) throws Exception
	{
		// if (response.getWarnings().length > 0)
		// {
		// log.warn("Warnings:");
		// for (int i = 0; i < response.getWarnings().length; i++)
		// {
		// log.warn(response.getWarnings()[i]);
		// }
		// }
		if (!response.wasSuccessful())
		{
			String message = "";
			if (response.getErrors().length > 0)
			{
				message += "Errors found:";
				for (int i = 0; i < response.getErrors().length; i++)
				{
					message += response.getErrors()[i] + "\n";
				}
			}
			throw new Exception(message);
		}
	}

}
