package com.sample.rally.data;

import com.google.gson.JsonObject;

public class UserIterationCapacity extends RallyObject
{
	private Double	capacity;
	private String	iteration;
	private String	project;
	private String	user;

	public UserIterationCapacity()
	{
		queryName = "useriterationcapacity";
	}
	
	@Override
	protected void addFetch()
	{
		fetch.add("Capacity");
		fetch.add("Iteration");
		fetch.add("Project");
		fetch.add("User");
	}

	@Override
	protected void read(JsonObject object) throws Exception
	{
		setCapacity(getDoubleValue(object, "Capacity"));
		setIteration(getReference(object, "Iteration"));
		setProject(getReference(object, "Project"));
		setUser(getReference(object, "User"));
	}

	@Override
	protected void addJsonProperties(JsonObject object)
	{
		object.remove("Workspace");
		object.remove("Name");
		object.addProperty("Capacity", getCapacity());
		object.addProperty("Iteration", getIteration());
		object.addProperty("Project", getProject());
		object.addProperty("User", getUser());
	}

	public Double getCapacity()
	{
		return capacity;
	}

	public void setCapacity(Double capacity)
	{
		this.capacity = capacity;
	}

	public String getIteration()
	{
		return iteration;
	}

	public void setIteration(String iteration)
	{
		this.iteration = iteration;
	}

	public String getProject()
	{
		return project;
	}

	public void setProject(String project)
	{
		this.project = project;
	}

	public String getUser()
	{
		return user;
	}

	public void setUser(String user)
	{
		this.user = user;
	}

}
