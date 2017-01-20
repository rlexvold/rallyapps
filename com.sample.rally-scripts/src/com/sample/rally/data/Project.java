package com.sample.rally.data;

import java.util.ArrayList;

import com.google.gson.JsonObject;

public class Project extends RallyObject
{
	private Project					parent;
	private String					notes;
	private String					state;
	private String					theme;
	private ArrayList<Project>		children	= new ArrayList<Project>();
	private ArrayList<Iteration>	iterations	= new ArrayList<Iteration>();
	private ArrayList<Release>		releases	= new ArrayList<Release>();
	private ArrayList<String>		teamMembers	= new ArrayList<String>();
	private ArrayList<String>		editors		= new ArrayList<String>();
	private ArrayList<String>		viewers		= new ArrayList<String>();
	private ArrayList<String>		admins		= new ArrayList<String>();
	protected String				description	= "";

	public Project()
	{
		queryName = "project";
	}
	
	public String getTheme()
	{
		return theme;
	}

	public void setTheme(String theme)
	{
		this.theme = theme;
	}

	public ArrayList<String> getTeamMembers()
	{
		return teamMembers;
	}

	public void setTeamMembers(ArrayList<String> teamMembers)
	{
		this.teamMembers = teamMembers;
	}

	public ArrayList<String> getEditors()
	{
		return editors;
	}

	public void setEditors(ArrayList<String> editors)
	{
		this.editors = editors;
	}

	public ArrayList<String> getViewers()
	{
		return viewers;
	}

	public void setViewers(ArrayList<String> viewers)
	{
		this.viewers = viewers;
	}

	public String getDescription()
	{
		return description;
	}

	public void setDescription(String description)
	{
		this.description = description;
	}

	@Override
	protected void addFetch()
	{
		fetch.add("Description");
		fetch.add("State");
		fetch.add("Theme");
		fetch.add("Notes");
		fetch.add("Editors");
		fetch.add("Viewers");
		fetch.add("TeamMembers");
	}

	public ArrayList<Iteration> getIterations()
	{
		return iterations;
	}

	public void setIterations(ArrayList<Iteration> iterations)
	{
		this.iterations = iterations;
	}

	public ArrayList<Release> getReleases()
	{
		return releases;
	}

	public void setReleases(ArrayList<Release> releases)
	{
		this.releases = releases;
	}

	public ArrayList<Project> getChildren()
	{
		return children;
	}

	public void setChildren(ArrayList<Project> children)
	{
		this.children = children;
	}

	public Project getParent()
	{
		return parent;
	}

	public void setParent(Project parent)
	{
		this.parent = parent;
	}

	public String getNotes()
	{
		return notes;
	}

	public void setNotes(String notes)
	{
		this.notes = notes;
	}

	public String getState()
	{
		return state;
	}

	public void setState(String state)
	{
		this.state = state;
	}

	@Override
	public String toString()
	{
		return "Name: " + name + "\nOid: " + oid + "\nDescription: " + description;
	}

	@Override
	protected void read(JsonObject object) throws Exception
	{
		setDescription(getValue(object, "Description"));
		setState(getValue(object, "State"));
		setNotes(getValue(object, "Notes"));
		setTheme(getValue(object, "Theme"));
	}

	@Override
	protected void addJsonProperties(JsonObject object)
	{
		if (parent != null)
		{
			object.addProperty("Parent", getParent().get_ref());
		}
		object.addProperty("Description", getDescription());
		object.addProperty("State", getState());
		object.addProperty("Notes", getNotes());
		object.addProperty("Theme", getTheme());
	}

	public ArrayList<String> getAdmins()
	{
		return admins;
	}

	public void setAdmins(ArrayList<String> admins)
	{
		this.admins = admins;
	}
}
