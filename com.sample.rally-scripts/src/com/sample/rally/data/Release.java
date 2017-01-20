package com.sample.rally.data;

import com.google.gson.JsonObject;

public class Release extends RallyObject
{
	private Project		project;
	private String		state;
	private String		notes;
	private String		theme;
	private String		releaseDate;
	private String		releaseStartDate;
	private String		plannedVelocity;
	protected String	description	= "";

	public Release()
	{
		queryName = "release";
	}
	
	public String getDescription()
	{
		return description;
	}

	public Project getProject()
	{
		return project;
	}

	public void setProject(Project project)
	{
		this.project = project;
	}

	public void setDescription(String description)
	{
		this.description = description;
	}

	public String getReleaseDate()
	{
		return releaseDate;
	}

	public void setReleaseDate(String releaseDate)
	{
		this.releaseDate = releaseDate;
	}

	public String getReleaseStartDate()
	{
		return releaseStartDate;
	}

	public void setReleaseStartDate(String releaseStartDate)
	{
		this.releaseStartDate = releaseStartDate;
	}

	public String getPlannedVelocity()
	{
		return plannedVelocity;
	}

	public void setPlannedVelocity(String plannedVelocity)
	{
		this.plannedVelocity = plannedVelocity;
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

	@Override
	protected void addFetch()
	{
		fetch.add("Description");
		fetch.add("State");
		fetch.add("Notes");
		fetch.add("Theme");
		fetch.add("ReleaseDate");
		fetch.add("ReleaseStartDate");
		fetch.add("PlannedVelocity");
	}

	@Override
	protected void read(JsonObject object) throws Exception
	{
		setState(getValue(object, "State"));
		setNotes(getValue(object, "Notes"));
		setReleaseDate(getValue(object, "ReleaseDate"));
		setTheme(getValue(object, "Theme"));
		setReleaseStartDate(getValue(object, "ReleaseStartDate"));
		setDescription(getValue(object, "Description"));
		setPlannedVelocity(getValue(object, "PlannedVelocity"));
	}

	@Override
	protected void addJsonProperties(JsonObject object)
	{
		if (project != null)
			object.addProperty("Project", project.get_ref());
		object.addProperty("State", getState());
		object.addProperty("Notes", getNotes());
		object.addProperty("Theme", getTheme());
		if (getReleaseDate().length() > 1)
			object.addProperty("ReleaseDate", getReleaseDate());
		if (getReleaseStartDate().length() > 1)
			object.addProperty("ReleaseStartDate", getReleaseStartDate());
		object.addProperty("Description", getDescription());

		if (getPlannedVelocity().length() > 1)
			object.addProperty("PlannedVelocity", getPlannedVelocity());
	}

	public String getTheme()
	{
		return theme;
	}

	public void setTheme(String theme)
	{
		this.theme = theme;
	}
}
