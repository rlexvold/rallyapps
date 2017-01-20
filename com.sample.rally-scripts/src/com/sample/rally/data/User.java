package com.sample.rally.data;

import java.util.ArrayList;

import com.google.gson.JsonObject;

public class User extends RallyObject
{
	private String						UserName;
	private ArrayList<ProjectPermission>	permissions	= new ArrayList<ProjectPermission>();

	public User()
	{
		queryName = "user";
	}
	
	@Override
	protected void addFetch()
	{
		fetch.add("UserName");
	}

	public ArrayList<ProjectPermission> getPermissions()
	{
		return permissions;
	}

	public void setPermissions(ArrayList<ProjectPermission> permissions)
	{
		this.permissions = permissions;
	}

	public String getUserName()
	{
		return UserName;
	}

	public void setUserName(String userName)
	{
		UserName = userName;
	}

	@Override
	protected void read(JsonObject object) throws Exception
	{
		setUserName(getValue(object, "UserName"));
	}

	@Override
	protected void addJsonProperties(JsonObject object)
	{
	}
}
