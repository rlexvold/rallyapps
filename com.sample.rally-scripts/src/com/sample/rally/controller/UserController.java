package com.sample.rally.controller;

import java.util.ArrayList;
import java.util.HashMap;

import com.google.gson.JsonObject;
import com.rallydev.rest.request.CreateRequest;
import com.rallydev.rest.request.DeleteRequest;
import com.rallydev.rest.request.QueryRequest;
import com.rallydev.rest.request.UpdateRequest;
import com.rallydev.rest.response.CreateResponse;
import com.rallydev.rest.response.DeleteResponse;
import com.rallydev.rest.response.QueryResponse;
import com.rallydev.rest.util.QueryFilter;
import com.sample.rally.data.Project;
import com.sample.rally.data.ProjectPermission;
import com.sample.rally.data.User;
import com.sample.rally.data.UserProfile;

public class UserController extends RallyController
{
	private HashMap<String, ArrayList<Project>> userMap = new HashMap<String, ArrayList<Project>>();

	public void switchUserToViewer(HashMap<String, Project> projectMap, String sourceWorkspaceId) throws Exception
	{
		ArrayList<String> users = new ArrayList<String>();
		projectMap.values().forEach(project -> project.getEditors().forEach(user -> {
			if (!users.contains(user))
				users.add(user);
		}));

		users.forEach(user -> {
			try
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
			catch (Exception e)
			{
				log.error(e.getMessage());
			}
		});
	}

	public void switchProjectAdminToViewer(HashMap<String, Project> projectMap, String sourceWorkspaceId) throws Exception
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

	public void switchToViewer(ProjectPermission permission)
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

	public void copyCostCenter(HashMap<String, Project> sourceProjectReferences, HashMap<String, Project> destProjectReferences, String sourceWorkspaceId, String destWorkspaceId)
			throws Exception
	{
		QueryRequest userProfileRequest = new UserProfile().getQueryRequest();
		userProfileRequest.setWorkspace(sourceWorkspaceId);

		QueryResponse projectsResponse = restApi.query(userProfileRequest);
		log.info("Items Found: " + projectsResponse.getTotalResultCount());

		printErrorsAndWarnings(projectsResponse);

		projectsResponse.getResults().forEach(result -> {
			try
			{
				UserProfile profile = new UserProfile();
				profile.readObject(result.getAsJsonObject());
				if (profile.getDefaultWorkspace() != null && profile.getDefaultWorkspace().equals(sourceWorkspaceId))
				{
					Project source = sourceProjectReferences.get(profile.getDefaultProject());
					if (source != null)
					{
						log.info(profile.getDefaultWorkspace() + " - " + profile.getDefaultProject());
						Project dest = ProjectController.getProjectByName(destProjectReferences, source.getName());
						if (dest != null)
						{
							JsonObject updateProfile = new JsonObject();
							updateProfile.addProperty("DefaultWorkspace", destWorkspaceId);
							updateProfile.addProperty("DefaultProject", dest.get_ref());
							UpdateRequest updateRequest = new UpdateRequest(profile.get_ref(), updateProfile);
							try
							{
								restApi.update(updateRequest);
							}
							catch (Exception e)
							{
								log.error("Error updating: " + e);
							}
						}

					}
				}
			}
			catch (Exception e)
			{
				log.error(e.getMessage());
			}
		});
	}

	public void makeAllUsersViewers(HashMap<String, Project> projects, String workspaceId) throws Exception
	{
		lookupUsers(projects);
		userMap.keySet().forEach(userRef -> {
			log.info("/" + userMap.keySet().size() + " - Writing " + userMap.get(userRef).size() + " viewer permissions: " + userRef);
			try
			{
				writeEditorPermissions(userRef, userMap.get(userRef), workspaceId);
			}
			catch (Exception e)
			{
				log.error(e.getMessage());
			}
		});
	}

	private void writeEditorPermissions(String userRef, ArrayList<Project> projects, String sourceWorkspaceId) throws Exception
	{
		projects.forEach(project -> {
			try
			{
				ProjectPermission newPermission = new ProjectPermission();
				newPermission.setProject(project.get_ref());
				newPermission.setRole("Viewer");
				newPermission.setWorkspace(sourceWorkspaceId);
				newPermission.setUser(userRef);
				CreateRequest createRequest = new CreateRequest(newPermission.getQueryName(), newPermission.create());
				CreateResponse createResponse = restApi.create(createRequest);
				printErrorsAndWarnings(createResponse);
			}
			catch (Exception e)
			{
				log.error(e.getMessage());
			}

		});

	}

	private void lookupUsers(HashMap<String, Project> projects) throws Exception
	{
		ArrayList<String> enabledUsers = getEnabledUsers();

		enabledUsers.forEach(user -> {
			try
			{
				QueryRequest permissionsRequest = new ProjectPermission().getQueryRequest();
				permissionsRequest.setQueryFilter(new QueryFilter("User", "=", user));
				QueryResponse permissionResponse = restApi.query(permissionsRequest);
				printErrorsAndWarnings(permissionResponse);

				ArrayList<Project> projectsToAdd = new ArrayList<Project>();
				projects.values().forEach(project -> projectsToAdd.add(project));

				permissionResponse.getResults().forEach(project -> {
					String projectRef = project.getAsJsonObject().get("Project").getAsJsonObject().get("_ref").getAsString();
					if (projects.containsKey(projectRef))
						projectsToAdd.remove(projects.get(projectRef));
				});

				if (projectsToAdd.size() > 0)
				{
					userMap.put(user, projectsToAdd);
					log.info(projectsToAdd.size() + " permissions to add for " + user);
				}
			}
			catch (Exception e)
			{
				log.error(e.getMessage());
			}
		});
	}

	public ArrayList<String> getEnabledUsers() throws Exception
	{
		ArrayList<String> users = new ArrayList<String>();

		QueryRequest userRequest = new User().getQueryRequest();
		userRequest.setQueryFilter(new QueryFilter("Disabled", "=", "false"));
		QueryResponse userResponse = restApi.query(userRequest);
		log.info("Users Found: " + userResponse.getTotalResultCount());

		userResponse.getResults().forEach(user -> users.add(user.getAsJsonObject().get("_ref").getAsString()));
		return users;
	}

}
