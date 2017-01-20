package com.sample.rally.data;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.rallydev.rest.request.QueryRequest;
import com.rallydev.rest.util.Fetch;

public abstract class RallyObject
{
	protected String	_ref;

	protected String	workspace;
	protected String	name;
	protected String	owner;
	protected String	oid;
	protected Fetch		fetch;
	protected String	queryName;

	protected abstract void addFetch();

	public QueryRequest getQueryRequest()
	{
		QueryRequest request = new QueryRequest(queryName);
		request.setLimit(999999);
		request.setFetch(getFetch());
		return request;
	}

	public Fetch getFetch()
	{
		if (fetch == null)
		{
			fetch = new Fetch("Name", "ObjectID", "Description");
			addFetch();
		}
		return fetch;
	}

	public String get_ref()
	{
		return _ref;
	}

	public void set_ref(String _ref)
	{
		this._ref = _ref;
	}

	public String getWorkspace()
	{
		return workspace;
	}

	public void setWorkspace(String workspace)
	{
		this.workspace = workspace;
	}

	public String getName()
	{
		return name;
	}

	public void setName(String name)
	{
		this.name = name;
	}

	public String getOwner()
	{
		return owner;
	}

	public void setOwner(String owner)
	{
		this.owner = owner;
	}

	public String getOid()
	{
		return oid;
	}

	public void setOid(String oid)
	{
		this.oid = oid;
	}

	protected String getValue(JsonObject object, String id)
	{
		JsonElement element = object.get(id);
		if (element == null || element instanceof JsonNull)
			return "";
		return element.getAsString();
	}

	protected Double getDoubleValue(JsonObject object, String id)
	{
		JsonElement element = object.get(id);
		if (element == null || element instanceof JsonNull)
			return null;
		return element.getAsDouble();
	}

	protected JsonObject getJsonObject(JsonObject object, String id)
	{
		JsonElement element = object.get(id);
		if (element == null || element instanceof JsonNull)
			return null;
		return element.getAsJsonObject();
	}

	protected String getReference(JsonObject object, String id)
	{
		JsonElement element = object.get(id);
		if (element == null || element instanceof JsonNull)
			return null;
		return element.getAsJsonObject().get("_ref").getAsString();
	}

	public void readObject(JsonObject object) throws Exception
	{
		set_ref(getValue(object, "_ref"));
		setName(getValue(object, "Name"));
		setOid(getValue(object, "ObjectID"));
		read(object);
	}

	protected abstract void read(JsonObject object) throws Exception;

	public JsonObject create()
	{
		JsonObject object = new JsonObject();
		object.addProperty("Name", getName());
		object.addProperty("Workspace", "/workspace/" + getWorkspace());
		addJsonProperties(object);
		return object;
	}

	protected abstract void addJsonProperties(JsonObject object);

	public String getQueryName()
	{
		return queryName;
	}

	public void setQueryName(String queryName)
	{
		this.queryName = queryName;
	}
}
