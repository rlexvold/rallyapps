package com.sample.rally.controller;

import java.net.URI;
import java.net.URISyntaxException;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.rallydev.rest.RallyRestApi;
import com.rallydev.rest.response.Response;
import com.sample.rally.RallyOnPremApi;

public class RallyController
{
	protected static RallyRestApi	restApi;
	protected static Logger			log	= LogManager.getLogger(RallyController.class);

	public static void connectToRally(String rallyUrl, String username, String password, String name) throws URISyntaxException
	{
		restApi = new RallyOnPremApi(new URI(rallyUrl), username, password);
		restApi.setWsapiVersion("v2.0");
		restApi.setApplicationName(name);
	}

	public static void close()
	{
		try
		{
			if (restApi != null)
				restApi.close();
		}
		catch (Throwable e)
		{
			log.error(e);
		}
	}

	protected static void printErrorsAndWarnings(Response response)
	{
		if (!response.wasSuccessful())
		{
			String message = "";
			if (response.getErrors().length > 0)
			{
				message += "Errors found:";
				for (int i = 0; i < response.getErrors().length; i++)
				{
					message += response.getErrors()[i] + "\n";
				}
			}
			log.error(message);
		}
	}

}
