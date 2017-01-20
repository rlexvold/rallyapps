package com.sample.rally;

import java.util.HashMap;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.rallydev.rest.request.CreateRequest;
import com.rallydev.rest.request.GetRequest;
import com.rallydev.rest.request.QueryRequest;
import com.rallydev.rest.request.UpdateRequest;
import com.rallydev.rest.response.CreateResponse;
import com.rallydev.rest.response.GetResponse;
import com.rallydev.rest.response.QueryResponse;
import com.rallydev.rest.response.UpdateResponse;
import com.rallydev.rest.util.Fetch;
import com.rallydev.rest.util.QueryFilter;
import com.sample.rally.data.Iteration;
import com.sample.rally.data.Project;
import com.sample.rally.data.ProjectPermission;
import com.sample.rally.data.Release;
import com.sample.rally.data.UserIterationCapacity;

public class CopyProjectStructure extends RallyTask
{
	private String		sourceProjectName	= "";
	protected String	sourceWorkspace		= "";
	protected String	sourceWorkspaceId	= "";
	protected String	destWorkspaceId		= "";
	protected String	destWorkspace		= "";
	protected Boolean	includeChildren		= false;

	private void getReleasesAndIterations(Project project) throws Exception
	{
		QueryRequest releaseRequest = new Release().getQueryRequest();
		releaseRequest.setScopedDown(false);
		releaseRequest.setScopedUp(false);
		releaseRequest.setProject(project.get_ref());
		QueryResponse releaseQueryResponse = restApi.query(releaseRequest);

		printErrorsAndWarnings(releaseQueryResponse);

		int numberOfReleasesInProject = releaseQueryResponse.getTotalResultCount();
		log.info("Number of releases in project: " + numberOfReleasesInProject);

		for (int i = 0; i < numberOfReleasesInProject; i++)
		{
			JsonObject releaseJsonObject = releaseQueryResponse.getResults().get(i).getAsJsonObject();
			Release release = new Release();
			release.setProject(project);
			release.readObject(releaseJsonObject);
			project.getReleases().add(release);
		}

		QueryRequest iterationRequest = new Iteration().getQueryRequest();
		iterationRequest.setScopedDown(false);
		iterationRequest.setScopedUp(false);
		iterationRequest.setProject(project.get_ref());

		QueryResponse iterationQueryResponse = restApi.query(iterationRequest);
		printErrorsAndWarnings(iterationQueryResponse);

		int numberOfIterations = iterationQueryResponse.getTotalResultCount();
		log.info("numberOfIterations " + numberOfIterations);
		for (int i = 0; i < numberOfIterations; i++)
		{
			JsonObject iterationJsonObject = iterationQueryResponse.getResults().get(i).getAsJsonObject();
			Iteration iteration = new Iteration();
			iteration.setProject(project);
			iteration.readObject(iterationJsonObject);
			QueryRequest capacityRequest = new UserIterationCapacity().getQueryRequest();
			capacityRequest.setQueryFilter(new QueryFilter("Iteration", "=", iteration.get_ref()));
			capacityRequest.setLimit(999999);
			QueryResponse capacityResponse = restApi.query(capacityRequest);
			printErrorsAndWarnings(capacityResponse);
			log.info("Iteration " + iteration.getName() + " found " + capacityResponse.getTotalResultCount() + " capacities");
			for (int j = 0; j < capacityResponse.getTotalResultCount(); j++)
			{
				UserIterationCapacity capacity = new UserIterationCapacity();
				capacity.readObject(capacityResponse.getResults().get(j).getAsJsonObject());
				if (capacity.getCapacity() != null)
					iteration.getUserIterationCapacities().add(capacity);
			}
			project.getIterations().add(iteration);
		}
	}

	@Override
	public void performTask() throws Exception
	{
		sourceWorkspaceId = getWorkspaceReference(sourceWorkspace);
		destWorkspaceId = getWorkspaceReference(destWorkspace);
		HashMap<String, Project> projectMap = loadProjectTree(sourceWorkspaceId, sourceProjectName, includeChildren, true, true);
		for (Project project : projectMap.values())
		{
			getReleasesAndIterations(project);
		}

		Project topProject = getProjectByName(projectMap, sourceProjectName);
		writeProject(topProject);
	}

	private void writeProject(Project project) throws Exception
	{
		project.setWorkspace(destWorkspaceId);
		CreateRequest createRequest = new CreateRequest(project.getQueryName(), project.create());
		CreateResponse projectCreateResponse = restApi.create(createRequest);
		printErrorsAndWarnings(projectCreateResponse);
		JsonObject projectObject = projectCreateResponse.getObject();

		project.readObject(projectObject);

		for (Project child : project.getChildren())
		{
			child.setWorkspace(project.getWorkspace());
			writeProject(child);
		}

		int i = 0;
		for (String editor : project.getEditors())
		{
			log.info("Writing editor: " + ++i + " of " + project.getEditors().size());
			ProjectPermission permission = new ProjectPermission();
			permission.setProject(project.get_ref());
			permission.setRole("Editor");
			permission.setWorkspace(project.getWorkspace());
			permission.setUser(editor);
			createRequest = new CreateRequest(permission.getQueryName(), permission.create());
			CreateResponse createResponse = restApi.create(createRequest);
			printErrorsAndWarnings(createResponse);
		}

		for (String member : project.getTeamMembers())
		{
			GetRequest userRequest = new GetRequest(member);
			userRequest.setFetch(new Fetch("UserPermissions", "TeamMemberships"));
			GetResponse userQueryResponse = restApi.get(userRequest);
			printErrorsAndWarnings(userQueryResponse);

			JsonObject teamMembershipReference = userQueryResponse.getObject().getAsJsonObject().get("TeamMemberships").getAsJsonObject();
			QueryRequest permissionsRequest = new QueryRequest(teamMembershipReference);
			QueryResponse permissionResponse = restApi.query(permissionsRequest);

			JsonArray existTeamMemberships = permissionResponse.getResults().getAsJsonArray();

			existTeamMemberships.add(projectObject);

			// Setup update fields/values for Team Membership
			JsonObject updateUserTeamMembershipObj = new JsonObject();
			updateUserTeamMembershipObj.add("TeamMemberships", existTeamMemberships);
			UpdateRequest updateTeamMembershipsRequest = new UpdateRequest(userQueryResponse.getObject().get("_ref").getAsString(), updateUserTeamMembershipObj);
			UpdateResponse updateTeamMembershipResponse = restApi.update(updateTeamMembershipsRequest);
			printErrorsAndWarnings(updateTeamMembershipResponse);
		}

		i = 0;
		for (Release release : project.getReleases())
		{
			log.info("Writing release: " + ++i + " of " + project.getReleases().size());
			release.setWorkspace(project.getWorkspace());
			createRequest = new CreateRequest(release.getQueryName(), release.create());
			CreateResponse createResponse = restApi.create(createRequest);
			printErrorsAndWarnings(createResponse);
		}

		i = 0;
		for (Iteration iteration : project.getIterations())
		{
			log.info("Writing iteration: " + ++i + " of " + project.getIterations().size() + " " + iteration.getName());
			iteration.setWorkspace(project.getWorkspace());
			createRequest = new CreateRequest(iteration.getQueryName(), iteration.create());
			CreateResponse iterationCreateResponse = restApi.create(createRequest);
			printErrorsAndWarnings(iterationCreateResponse);
			int capacityCount = 0;
			int capacityError = 0;
			for (UserIterationCapacity capacity : iteration.getUserIterationCapacities())
			{
				if (capacity.getCapacity() != null)
				{
					log.info("Writing capacity: " + ++capacityCount);
					capacity.setProject(projectObject.get("_ref").getAsString());
					capacity.setIteration(iterationCreateResponse.getObject().get("_ref").getAsString());
					JsonObject newCapacity = capacity.create();
					createRequest = new CreateRequest(capacity.getQueryName(), newCapacity);
					try
					{
						CreateResponse createResponse = restApi.create(createRequest);
						printErrorsAndWarnings(createResponse);
					}
					catch (Exception e)
					{
						log.error("Error writing capacity", e);
						capacityError++;
					}
				}
			}
			log.info("Wrote " + capacityCount + " of " + iteration.getUserIterationCapacities().size() + " capacities. Error count=" + capacityError);
		}
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
