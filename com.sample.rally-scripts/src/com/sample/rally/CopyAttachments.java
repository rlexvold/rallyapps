package com.sample.rally;

import java.util.ArrayList;
import java.util.HashMap;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.rallydev.rest.request.CreateRequest;
import com.rallydev.rest.request.GetRequest;
import com.rallydev.rest.request.QueryRequest;
import com.rallydev.rest.response.CreateResponse;
import com.rallydev.rest.response.GetResponse;
import com.rallydev.rest.response.QueryResponse;
import com.rallydev.rest.util.Fetch;
import com.rallydev.rest.util.QueryFilter;
import com.sample.rally.data.Attachment;
import com.sample.rally.data.Project;

public class CopyAttachments extends RallyTask
{
	private String						sourceProject;
	private ArrayList<String>			successes			= new ArrayList<String>();
	protected String					sourceWorkspace		= "";
	protected String					sourceWorkspaceId	= "";
	protected String					destWorkspaceId		= "";
	protected String					destWorkspace		= "";
	private HashMap<String, Project>	projectReferences	= new HashMap<String, Project>();
	private Boolean						includeChildren		= false;

	@Override
	public void performTask() throws Exception
	{
		sourceWorkspaceId = getWorkspaceReference(sourceWorkspace);
		destWorkspaceId = getWorkspaceReference(destWorkspace);

		projectReferences = loadProjectTree(sourceWorkspaceId, sourceProject, includeChildren);

		log.info("Querying for attachments in workspace: " + sourceWorkspace);
		QueryRequest request = new Attachment().getQueryRequest();
		request.setWorkspace(sourceWorkspaceId);
		QueryResponse response = restApi.query(request);
		printErrorsAndWarnings(response);

		log.info("Found " + response.getTotalResultCount() + " attachments.");
		JsonArray results = response.getResults();
		for (int i = 0; i < response.getTotalResultCount(); i++)
		{
			try
			{
				log.info(i + "/" + response.getTotalResultCount() + " checking project for a match");
				Attachment attachment = new Attachment();
				attachment.setWorkspace(sourceWorkspaceId);
				JsonObject result = results.get(i).getAsJsonObject();
				attachment.readObject(result);
				String projectRef = getProjectRef(attachment.getArtifact());
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

					Boolean success = updateAttachmentArtifactReference(attachment);

					if (success)
						writeAttachment(attachment);
				}
			}
			catch (Exception e)
			{
				log.error("Error during results: ", e);
			}
		}
		for (String success : successes)
		{
			log.info(success);
		}
		log.info("Completed: " + successes.size() + " attachments successfully created");
	}

	private String getProjectRef(String artifact) throws Exception
	{
		GetRequest request = new GetRequest(artifact);
		request.setFetch(new Fetch("Project"));
		GetResponse response = restApi.get(request);
		return response.getObject().get("Project").getAsJsonObject().get("_ref").getAsString();
	}

	private Boolean updateAttachmentArtifactReference(Attachment attachment) throws Exception
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
		try
		{
			printErrorsAndWarnings(attachmentResponse);

			String myAttachmentRef = attachmentResponse.getObject().get("_ref").getAsString();
			successes.add("OldFormattedID: " + attachment.getOldId() + "  FormattedID: " + attachment.getNewId());
			log.info(successes.size() + " - Attachment  created: " + myAttachmentRef);
		}
		catch (Exception e)
		{
			log.error("Unable to create attachment: ", e);
		}
	}

	public static void main(String[] args)
	{
		CopyAttachments task = new CopyAttachments();
		task.doIt(args);
	}

	@Override
	public void addTaskCommandLineOptions()
	{
		Option sp = new Option("sp", "sourceProject", true, "Source project to copy attachments from");
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
