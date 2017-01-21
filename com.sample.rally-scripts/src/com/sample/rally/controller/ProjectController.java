package com.sample.rally.controller;

import java.util.HashMap;

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
import com.rallydev.rest.util.Ref;
import com.sample.rally.data.Iteration;
import com.sample.rally.data.Project;
import com.sample.rally.data.ProjectPermission;
import com.sample.rally.data.Release;
import com.sample.rally.data.User;
import com.sample.rally.data.UserIterationCapacity;

public class ProjectController extends RallyController
{
	public static String getProjectRef(String artifact) throws Exception
	{
		GetRequest request = new GetRequest(artifact);
		request.setFetch(new Fetch("Project"));
		GetResponse response = restApi.get(request);
		return response.getObject().get("Project").getAsJsonObject().get("_ref").getAsString();
	}

	public Project getProjectReference(String workspace, String projectName) throws Exception
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

	public static Project getProjectByName(HashMap<String, Project> projectMap, String projectName)
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
		HashMap<String, Project> projectReferences = new HashMap<String, Project>();

		QueryRequest projectRequest = new Project().getQueryRequest();
		projectRequest.setQueryFilter(new QueryFilter("Name", "=", projectName));
		projectRequest.setWorkspace("/workspace/" + workspaceId);

		log.info("Looking for project: " + projectName + " in workspace: " + workspaceId);
		QueryResponse projectsResponse = restApi.query(projectRequest);
		log.info("Items Found: " + projectsResponse.getTotalResultCount());

		JsonObject object = projectsResponse.getResults().get(0).getAsJsonObject();

		String ref = Ref.getRelativeRef(object.get("_ref").getAsString());

		printErrorsAndWarnings(projectsResponse);

		Project project = getProject(projectReferences, workspaceId, null, ref, includeChildren, includeTeamMembers, includeEditors);
		log.info("Read project: " + project.getName());
		return projectReferences;
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

		projectsResponse.getResults().iterator().forEachRemaining(object -> {
			try
			{
				String ref = Ref.getRelativeRef(object.getAsJsonObject().get("_ref").getAsString());
				Project project = getProject(projectMap, parent.getWorkspace(), parent, ref, true, includeTeamMembers, includeEditors);
				parent.getChildren().add(project);
				log.info("Read project: " + project);
			}
			catch (Exception e)
			{
				log.error(e.getMessage());
			}
		});
	}

	public void getReleasesAndIterations(Project project) throws Exception
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

	public void writeProject(Project project, String destWorkspaceId) throws Exception
	{
		project.setWorkspace(destWorkspaceId);
		CreateRequest createProjectRequest = new CreateRequest(project.getQueryName(), project.create());
		CreateResponse projectCreateResponse = restApi.create(createProjectRequest);
		printErrorsAndWarnings(projectCreateResponse);
		JsonObject projectObject = projectCreateResponse.getObject();

		project.readObject(projectObject);

		for (Project child : project.getChildren())
		{
			child.setWorkspace(project.getWorkspace());
			writeProject(child, destWorkspaceId);
		}

		project.getEditors().forEach(editor -> {
			try
			{
				ProjectPermission permission = new ProjectPermission();
				permission.setProject(project.get_ref());
				permission.setRole("Editor");
				permission.setWorkspace(project.getWorkspace());
				permission.setUser(editor);
				CreateRequest createRequest = new CreateRequest(permission.getQueryName(), permission.create());
				CreateResponse createResponse = restApi.create(createRequest);
				printErrorsAndWarnings(createResponse);
			}
			catch (Exception e)
			{
				log.error(e.getMessage());
			}
		});

		project.getTeamMembers().forEach(member -> {
			try
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
			catch (Exception e)
			{
				log.error(e.getMessage());
			}

		});

		project.getReleases().forEach(release -> {
			try
			{
				release.setWorkspace(project.getWorkspace());
				CreateRequest createRequest = new CreateRequest(release.getQueryName(), release.create());
				CreateResponse createResponse = restApi.create(createRequest);
				printErrorsAndWarnings(createResponse);
			}
			catch (Exception e)
			{
				log.error(e.getMessage());
			}

		});

		project.getIterations().forEach(iteration -> {
			try
			{
				iteration.setWorkspace(project.getWorkspace());
				CreateRequest createRequest = new CreateRequest(iteration.getQueryName(), iteration.create());
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
			catch (Exception e)
			{
				log.error(e.getMessage());
			}
		});
	}

}
