package com.sample.rally.data;

import com.google.gson.JsonObject;

public class UserProfile extends RallyObject
{
	private String	defaultWorkspace;
	private String	defaultProject;

	public UserProfile()
	{
		queryName = "userprofile";
	}
	
	@Override
	protected void addFetch()
	{
		fetch.add("DefaultWorkspace");
		fetch.add("DefaultProject");
	}

	@Override
	protected void read(JsonObject object) throws Exception
	{
		JsonObject tmp = getJsonObject(object, "DefaultProject");
		if (tmp != null)
			setDefaultProject(tmp.get("_ref").getAsString());
		tmp = getJsonObject(object, "DefaultWorkspace");
		if (tmp != null)
			setDefaultWorkspace(tmp.get("_ref").getAsString());
	}

	@Override
	protected void addJsonProperties(JsonObject object)
	{
		object.addProperty("DefaultProject", getDefaultProject());
		object.addProperty("DefaultWorkspace", getDefaultWorkspace());
	}

	public String getDefaultWorkspace()
	{
		return defaultWorkspace;
	}

	public void setDefaultWorkspace(String defaultWorkspace)
	{
		this.defaultWorkspace = defaultWorkspace;
	}

	public String getDefaultProject()
	{
		return defaultProject;
	}

	public void setDefaultProject(String defaultProject)
	{
		this.defaultProject = defaultProject;
	}

}
