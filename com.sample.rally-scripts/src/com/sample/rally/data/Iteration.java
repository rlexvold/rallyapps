package com.sample.rally.data;

import java.util.ArrayList;

import com.google.gson.JsonObject;

public class Iteration extends RallyObject
{
	private Project								project;

	private String								state;
	private String								notes;
	private String								startDate;
	private String								endDate;
	private String								plannedVelocity;
	protected String							description				= "";
	private String								theme;
	private ArrayList<UserIterationCapacity>	userIterationCapacities	= new ArrayList<UserIterationCapacity>();

	public Iteration()
	{
		queryName = "iteration";
	}
	
	public Project getProject()
	{
		return project;
	}

	public void setProject(Project project)
	{
		this.project = project;
	}

	public String getDescription()
	{
		return description;
	}

	public void setDescription(String description)
	{
		this.description = description;
	}

	public String getState()
	{
		return state;
	}

	public void setState(String state)
	{
		this.state = state;
	}

	public String getNotes()
	{
		return notes;
	}

	public void setNotes(String notes)
	{
		this.notes = notes;
	}

	public String getStartDate()
	{
		return startDate;
	}

	public void setStartDate(String startDate)
	{
		this.startDate = startDate;
	}

	public String getEndDate()
	{
		return endDate;
	}

	public void setEndDate(String endDate)
	{
		this.endDate = endDate;
	}

	public String getPlannedVelocity()
	{
		return plannedVelocity;
	}

	public void setPlannedVelocity(String plannedVelocity)
	{
		this.plannedVelocity = plannedVelocity;
	}

	@Override
	protected void addFetch()
	{
		fetch.add("Description");
		fetch.add("State");
		fetch.add("Notes");
		fetch.add("StartDate");
		fetch.add("EndDate");
		fetch.add("PlannedVelocity");
		fetch.add("Theme");
	}

	@Override
	protected void read(JsonObject object) throws Exception
	{
		setState(getValue(object, "State"));
		setNotes(getValue(object, "Notes"));
		setStartDate(getValue(object, "StartDate"));
		setDescription(getValue(object, "Description"));
		setEndDate(getValue(object, "EndDate"));
		setPlannedVelocity(getValue(object, "PlannedVelocity"));
		setTheme(getValue(object, "Theme"));
	}

	@Override
	protected void addJsonProperties(JsonObject object)
	{
		if (project != null)
			object.addProperty("Project", project.get_ref());
		object.addProperty("State", getState());
		object.addProperty("Notes", getNotes());
		if (getStartDate().length() > 1)
			object.addProperty("StartDate", getStartDate());

		if (getEndDate().length() > 1)
			object.addProperty("EndDate", getEndDate());

		if (getPlannedVelocity().length() > 1)
			object.addProperty("PlannedVelocity", getPlannedVelocity());

		object.addProperty("Description", getDescription());
		object.addProperty("Theme", getTheme());

	}

	public String getTheme()
	{
		return theme;
	}

	public void setTheme(String theme)
	{
		this.theme = theme;
	}

	public ArrayList<UserIterationCapacity> getUserIterationCapacities()
	{
		return userIterationCapacities;
	}

	public void setUserIterationCapacities(ArrayList<UserIterationCapacity> userIterationCapacities)
	{
		this.userIterationCapacities = userIterationCapacities;
	}
}
