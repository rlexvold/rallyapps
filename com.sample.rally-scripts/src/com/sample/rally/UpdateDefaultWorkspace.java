package com.sample.rally;

import java.util.HashMap;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.rallydev.rest.request.QueryRequest;
import com.rallydev.rest.request.UpdateRequest;
import com.rallydev.rest.response.QueryResponse;
import com.sample.rally.data.Project;
import com.sample.rally.data.UserProfile;

public class UpdateDefaultWorkspace extends RallyTask
{
	private String		sourceProject;
	protected String	sourceWorkspace		= "";
	protected String	sourceWorkspaceId	= "";
	protected String	destWorkspaceId		= "";
	protected String	destWorkspace		= "";
	private Boolean		includeChildren		= false;

	@Override
	public void performTask() throws Exception
	{
		sourceWorkspaceId = getWorkspaceReference(sourceWorkspace);
		destWorkspaceId = getWorkspaceReference(destWorkspace);

		HashMap<String, Project> sourceProjectReferences = loadProjectTree(sourceWorkspaceId, sourceProject, includeChildren);
		HashMap<String, Project> destProjectReferences = loadProjectTree(destWorkspaceId, sourceProject, includeChildren);

		QueryRequest userProfileRequest = new UserProfile().getQueryRequest();
		userProfileRequest.setWorkspace(sourceWorkspaceId);

		QueryResponse projectsResponse = restApi.query(userProfileRequest);
		log.info("Items Found: " + projectsResponse.getTotalResultCount());

		printErrorsAndWarnings(projectsResponse);

		int nullCount = 0;
		int count = 0;
		int numProcessed = 0;
		for (JsonElement result : projectsResponse.getResults())
		{
			UserProfile profile = new UserProfile();
			profile.readObject(result.getAsJsonObject());
			if (profile.getDefaultWorkspace() == null)
			{
				nullCount++;
			}
			else if (profile.getDefaultWorkspace().equals(sourceWorkspaceId))
			{
				Project source = sourceProjectReferences.get(profile.getDefaultProject());
				if (source != null)
				{
					log.info(profile.getDefaultWorkspace() + " - " + profile.getDefaultProject());
					Project dest = getProjectByName(destProjectReferences, source.getName());
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
						count++;
						log.info("Updated user " + count);
					}

				}
			}
			log.info(numProcessed++);
		}
		log.info("Found " + count + " profiles with a source workspace set to " + sourceWorkspace + "  null=" + nullCount);
	}

	public static void main(String[] args)
	{
		UpdateDefaultWorkspace task = new UpdateDefaultWorkspace();
		task.doIt(args);
	}

	@Override
	public void addTaskCommandLineOptions()
	{
		Option sp = new Option("sp", "sourceProject", true, "Source Project to look for in the user profile");
		sp.setRequired(true);
		commandLineOptions.addOption(sp);

		Option sw = new Option("sw", "sourceWorkspace", true, "Source workspace name to copy from");
		sw.setRequired(true);
		commandLineOptions.addOption(sw);

		Option dw = new Option("dw", "destWorkspace", true, "Destination workspace name to copy to");
		dw.setRequired(true);
		commandLineOptions.addOption(dw);

		commandLineOptions.addOption("children", false, "Include the child projects in the task?");
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
			sourceProject = commands.getOptionValue("sp");
		}
		if (commands.hasOption("children"))
			includeChildren = true;
	}
}
