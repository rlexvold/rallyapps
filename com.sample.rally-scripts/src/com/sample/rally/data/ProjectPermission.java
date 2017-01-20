package com.sample.rally.data;

import com.google.gson.JsonObject;

public class ProjectPermission extends RallyObject
{
	private String	role;
	private String	customObjectId;
	private String	project;
	private String	user;

	public ProjectPermission()
	{
		queryName = "projectpermission";
	}

	public String getUser()
	{
		return user;
	}

	public void setUser(String user)
	{
		this.user = user;
	}

	public String getProject()
	{
		return project;
	}

	public void setProject(String project)
	{
		this.project = project;
	}

	public String getCustomObjectId()
	{
		return customObjectId;
	}

	public void setCustomObjectId(String customObjectId)
	{
		this.customObjectId = customObjectId;
	}

	public String getRole()
	{
		return role;
	}

	public void setRole(String role)
	{
		this.role = role;
	}

	@Override
	protected void addFetch()
	{
		fetch.add("Role");
		fetch.add("CustomObjectID");
		fetch.add("User");
		fetch.add("Project");
	}

	@Override
	protected void read(JsonObject object) throws Exception
	{
		setRole(getValue(object, "Role"));
		setCustomObjectId(getValue(object, "CustomObjectID"));
		setProject(getReference(object, "Project"));
		setUser(getReference(object, "User"));
	}

	@Override
	protected void addJsonProperties(JsonObject object)
	{
		object.addProperty("Role", getRole());
		object.addProperty("Project", getProject());
		object.addProperty("User", getUser());
	}
}
