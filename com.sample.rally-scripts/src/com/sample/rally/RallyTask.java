package com.sample.rally;

import java.util.Date;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.sample.rally.controller.RallyController;

public abstract class RallyTask
{
	protected Logger			log					= LogManager.getLogger(this.getClass());
	protected String			rallyUrl			= "";
	protected String			username			= "";
	protected String			password			= "";
	protected Date				start				= new Date();
	protected static Options	commandLineOptions	= new Options();

	public abstract void addTaskCommandLineOptions();

	public abstract void performTask() throws Exception;

	public abstract void parseTaskCommandLineOptions(CommandLine commands);

	protected void parseCommandLine(String[] args, String className) throws Exception
	{
		CommandLineParser parser = new BasicParser();
		CommandLine commandLine = null;

		try
		{
			commandLine = parser.parse(commandLineOptions, args);
		}
		catch (Exception e)
		{
			System.out.println(e.getMessage());
			HelpFormatter help = new HelpFormatter();
			help.printHelp(className, commandLineOptions);
			System.exit(0);

		}

		if (commandLine.hasOption("h"))
		{
			HelpFormatter help = new HelpFormatter();
			help.printHelp(className, commandLineOptions);
			System.exit(0);
		}
		if (commandLine.hasOption("url"))
		{
			rallyUrl = commandLine.getOptionValue("url");
		}
		if (commandLine.hasOption("u"))
		{
			username = commandLine.getOptionValue("u");
		}
		if (commandLine.hasOption("p"))
		{
			password = commandLine.getOptionValue("p");
		}
		parseTaskCommandLineOptions(commandLine);
	}

	public void addCommandLineOptions()
	{
		commandLineOptions.addOption("h", "help", false, "Print this message");

		Option url = new Option("url", "rallyUrl", true, "Rally URL instance");
		url.setRequired(true);
		commandLineOptions.addOption(url);

		Option user = new Option("u", "user", true, "username");
		user.setRequired(true);
		commandLineOptions.addOption(user);

		Option pass = new Option("p", "pass", true, "password");
		pass.setRequired(true);
		commandLineOptions.addOption(pass);

		addTaskCommandLineOptions();
	}

	protected void doIt(String[] args)
	{
		try
		{
			addCommandLineOptions();
			parseCommandLine(args, getClass().getSimpleName());
			RallyController.connectToRally(rallyUrl, username, password, this.getClass().getSimpleName());
			performTask();
			Date end = new Date();
			Long elapsed = (end.getTime() - start.getTime()) / 1000;
			log.info("Completed task, Total time: " + elapsed + " seconds");
		}
		catch (Throwable e)
		{
			log.error(e);
		}
		finally
		{
			RallyController.close();
		}

	}
}
