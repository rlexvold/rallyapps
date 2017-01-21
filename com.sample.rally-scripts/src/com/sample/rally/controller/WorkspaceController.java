package com.sample.rally.controller;

import java.util.HashMap;

import com.google.gson.JsonObject;
import com.rallydev.rest.request.QueryRequest;
import com.rallydev.rest.request.UpdateRequest;
import com.rallydev.rest.response.QueryResponse;
import com.rallydev.rest.util.Fetch;
import com.sample.rally.data.Project;
import com.sample.rally.data.UserProfile;

public class WorkspaceController extends RallyController
{
	public static String getWorkspaceReference(String name) throws Exception
	{
		QueryRequest request = new QueryRequest("Subscription");
		request.setFetch(new Fetch("Workspaces"));
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

	public static void setDefaultWorkspace(String sourceWorkspaceId, String destWorkspaceId, HashMap<String, Project> sourceProjectReferences,
			HashMap<String, Project> destProjectReferences) throws Exception
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
}
