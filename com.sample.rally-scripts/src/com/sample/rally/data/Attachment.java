package com.sample.rally.data;

import com.google.gson.JsonObject;

public class Attachment extends RallyObject
{
	private String	artifact;
	private String	content;
	private String	contentType;
	private String	description;
	private Long	size;
	private String	user;
	private String	oldId;
	private String	newId;

	public Attachment()
	{
		queryName = "attachment";
	}

	@Override
	protected void addFetch()
	{
		fetch.add("Description");
		fetch.add("Artifact");
		fetch.add("ContentType");
		fetch.add("Size");
		// fetch.add("User");
	}

	@Override
	protected void read(JsonObject object) throws Exception
	{
		setDescription(getValue(object, "Description"));
		setArtifact(getReference(object, "Artifact"));
		setContentType(getValue(object, "ContentType"));
		setSize(Long.parseLong(getValue(object, "Size")));
		// setUser(getReference(object, "User"));
	}

	@Override
	protected void addJsonProperties(JsonObject object)
	{
		object.addProperty("Description", getDescription());
		object.addProperty("Artifact", getArtifact());
		object.addProperty("ContentType", getContentType());
		object.addProperty("Size", getSize());
		// object.addProperty("User", getUser());
		object.remove("Workspace");
	}

	public String getArtifact()
	{
		return artifact;
	}

	public void setArtifact(String artifact)
	{
		this.artifact = artifact;
	}

	public String getContent()
	{
		return content;
	}

	public void setContent(String content)
	{
		this.content = content;
	}

	public String getContentType()
	{
		return contentType;
	}

	public void setContentType(String contentType)
	{
		this.contentType = contentType;
	}

	public String getDescription()
	{
		return description;
	}

	public void setDescription(String description)
	{
		this.description = description;
	}

	public Long getSize()
	{
		return size;
	}

	public void setSize(Long size)
	{
		this.size = size;
	}

	public String getUser()
	{
		return user;
	}

	public void setUser(String user)
	{
		this.user = user;
	}

	public String getOldId()
	{
		return oldId;
	}

	public void setOldId(String oldId)
	{
		this.oldId = oldId;
	}

	public String getNewId()
	{
		return newId;
	}

	public void setNewId(String newId)
	{
		this.newId = newId;
	}
}
