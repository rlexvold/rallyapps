package com.sample.rally.controller;

import java.util.ArrayList;
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
import com.sample.rally.data.Attachment;
import com.sample.rally.data.Project;

public class WorkItemController extends RallyController
{
	private static final String	storyType	= "hierarchicalrequirement";
	private static final String	defectType	= "defect";

	public void fixDefectPermissions(Project sourceProject, Project destProject, String sourceWorkspaceId, String destWorkspaceId, Boolean includeChildren) throws Exception
	{
		fixPermissions(defectType, sourceProject, destProject, sourceWorkspaceId, destWorkspaceId, includeChildren);
	}

	public void fixStoryPermissions(Project sourceProject, Project destProject, String sourceWorkspaceId, String destWorkspaceId, Boolean includeChildren) throws Exception
	{
		fixPermissions(storyType, sourceProject, destProject, sourceWorkspaceId, destWorkspaceId, includeChildren);
	}

	public void fixPermissions(String type, Project sourceProject, Project destProject, String sourceWorkspaceId, String destWorkspaceId, Boolean includeChildren) throws Exception
	{
		QueryRequest request = new QueryRequest(type);
		request.setFetch(new Fetch("FormattedID"));
		request.setLimit(999999);
		request.setWorkspace(sourceWorkspaceId);
		request.setProject(sourceProject.get_ref());
		request.setScopedDown(includeChildren);
		request.setQueryFilter(new QueryFilter("ScheduleState", "=", "Accepted"));
		QueryResponse response = restApi.query(request);
		printErrorsAndWarnings(response);

		ArrayList<String> acceptedStories = new ArrayList<String>();

		log.info("Found " + response.getTotalResultCount() + " accepted " + type);
		JsonArray results = response.getResults();
		for (int i = 0; i < results.size(); i++)
		{
			acceptedStories.add(results.get(i).getAsJsonObject().get("FormattedID").getAsString());
		}

		request = new QueryRequest(type);
		request.setFetch(new Fetch("FormattedID", "OldFormattedID", "ScheduleState"));
		request.setLimit(999999);
		request.setWorkspace(destWorkspaceId);
		request.setProject(destProject.get_ref());
		request.setScopedDown(includeChildren);
		if (!type.equals("defect"))
			request.setQueryFilter(new QueryFilter("DirectChildrenCount", "=", "0"));
		response = restApi.query(request);
		printErrorsAndWarnings(response);

		results = response.getResults();

		JsonObject updateStory = new JsonObject();
		updateStory.addProperty("ScheduleState", "Accepted");

		for (int i = 0; i < results.size(); i++)
		{
			JsonObject storyObject = results.get(i).getAsJsonObject();
			String oldFormattedId = storyObject.get("c_OldFormattedID").getAsString();
			String formattedId = storyObject.get("FormattedID").getAsString();
			if (acceptedStories.contains(oldFormattedId))
			{
				String storyRef = storyObject.get("_ref").getAsString();
				String scheduleState = storyObject.get("ScheduleState").getAsString();
				if (!scheduleState.equals("Accepted"))
				{
					log.info(formattedId + " ScheduleState changed from Accepted to " + scheduleState + ", changing back to accepted");
					UpdateRequest updateStoryRequest = new UpdateRequest(storyRef, updateStory);
					UpdateResponse updateStoryResponse = restApi.update(updateStoryRequest);
					printErrorsAndWarnings(updateStoryResponse);
				}
			}
		}
	}

	public void doSomething(HashMap<String, Project> projectReferences, String sourceWorkspaceId, String destWorkspaceId) throws Exception
	{
		QueryRequest request = new Attachment().getQueryRequest();
		request.setWorkspace(sourceWorkspaceId);
		QueryResponse response = restApi.query(request);
		printErrorsAndWarnings(response);

		log.info("Found " + response.getTotalResultCount() + " attachments.");
		JsonArray results = response.getResults();
		results.forEach(result -> {
			try
			{
				Attachment attachment = new Attachment();
				attachment.setWorkspace(sourceWorkspaceId);
				attachment.readObject(result.getAsJsonObject());
				String projectRef = ProjectController.getProjectRef(attachment.getArtifact());
				if (projectReferences.containsKey(projectRef))
				{
					GetRequest attachmentRequest = new GetRequest(attachment.get_ref());
					attachmentRequest.setFetch(new Fetch("ObjectID", "Name", "Content"));
					GetResponse attachmentResponse = restApi.get(attachmentRequest);

					printErrorsAndWarnings(attachmentResponse);

					GetRequest contentRequest = new GetRequest("/attachmentcontent/" + attachmentResponse.getObject().get("Content").getAsJsonObject().get("ObjectID").toString());
					contentRequest.setFetch(new Fetch("Content"));
					GetResponse contentResponse = restApi.get(contentRequest);

					printErrorsAndWarnings(contentResponse);

					attachment.setContent(contentResponse.getObject().get("Content").getAsString());

					Boolean success = updateAttachmentArtifactReference(attachment, destWorkspaceId);

					if (success)
						writeAttachment(attachment);
				}
			}
			catch (Exception e)
			{
				log.error("Error during results: ", e);
			}
		});
	}

	private Boolean updateAttachmentArtifactReference(Attachment attachment, String destWorkspaceId) throws Exception
	{
		GetRequest contentRequest = new GetRequest(attachment.getArtifact());
		contentRequest.setFetch(new Fetch("FormattedID"));
		GetResponse contentResponse = restApi.get(contentRequest);
		printErrorsAndWarnings(contentResponse);

		attachment.setWorkspace(destWorkspaceId);

		attachment.setOldId(contentResponse.getObject().get("FormattedID").getAsString());
		String type = "portfolioitem";
		if (attachment.getOldId().startsWith("T"))
			type = "task";
		else if (attachment.getOldId().startsWith("US"))
			type = "hierarchicalrequirement";
		else if (attachment.getOldId().startsWith("DE"))
			type = "defect";

		log.info("Looking for work item in new workspace of type " + type + " with OldFormattedID=" + attachment.getOldId());
		QueryRequest request = new QueryRequest(type);
		request.setFetch(new Fetch("FormattedID", "OldFormattedID"));
		request.setWorkspace(attachment.getWorkspace());
		request.setQueryFilter(new QueryFilter("OldFormattedID", "=", attachment.getOldId()));
		QueryResponse response = restApi.query(request);
		printErrorsAndWarnings(response);

		if (response.getTotalResultCount() > 0)
		{
			String ref = response.getResults().get(0).getAsJsonObject().get("_ref").getAsString();
			attachment.setNewId(response.getResults().get(0).getAsJsonObject().get("FormattedID").getAsString());
			log.info("OldFormattedID: " + attachment.getOldId() + "  FormattedID: " + attachment.getNewId());
			attachment.setArtifact(ref);
			return true;
		}
		else
		{
			log.info("No results found for OldFormattedID=" + attachment.getOldId() + " in workspace: " + attachment.getWorkspace());
			return false;
		}
	}

	private void writeAttachment(Attachment attachment) throws Exception
	{
		// First create AttachmentContent from image string
		JsonObject myAttachmentContent = new JsonObject();
		myAttachmentContent.addProperty("Content", attachment.getContent());
		myAttachmentContent.addProperty("Workspace", attachment.getWorkspace());
		CreateRequest attachmentContentCreateRequest = new CreateRequest("AttachmentContent", myAttachmentContent);
		CreateResponse attachmentContentResponse = restApi.create(attachmentContentCreateRequest);
		String myAttachmentContentRef = attachmentContentResponse.getObject().get("_ref").getAsString();
		log.info("Attachment Content created: " + myAttachmentContentRef);

		// Now create the Attachment itself
		JsonObject myAttachment = attachment.create();

		myAttachment.addProperty("Content", myAttachmentContentRef);
		CreateRequest attachmentCreateRequest = new CreateRequest(attachment.getQueryName(), myAttachment);
		CreateResponse attachmentResponse = restApi.create(attachmentCreateRequest);
		printErrorsAndWarnings(attachmentResponse);
	}
}
