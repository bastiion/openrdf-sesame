/*
 * Copyright Aduna (http://www.aduna-software.com/) (c) 2013.
 *
 * Licensed under the Aduna BSD-style license.
 */
package org.openrdf.console;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Prints help to the console.
 * 
 * @author Dale Visser
 */
class PrintHelp implements Command {
	;

	private static final String USAGE = "Usage:\n";

	protected static final String CONNECT = USAGE
			+ "connect default                         Opens the default repository set for this console\n"
			+ "connect <dataDirectory>                 Opens the repository set in the specified data dir\n"
			+ "connect <serverURL> [user [password]]   Connects to a Sesame server with optional credentials\n";

	protected static final String CREATE = USAGE + "create <template-name>\n"
			+ "  <template-name>   The name of a repository configuration template\n";

	private static final String DISCONNECT = USAGE
			+ "disconnect   Disconnects from the current set of repositories or server\n";

	protected static final String DROP = USAGE
			+ "drop <repositoryID>   Drops the repository with the specified id\n";

	private final Map<String, String> topics = new HashMap<String, String>();

	private final ConsoleIO consoleIO;

	protected static final String OPEN = USAGE
			+ "open <repositoryID>   Opens the repository with the specified ID\n";

	protected static final String CLOSE = USAGE + "close   Closes the current repository\n";

	protected static final String SHOW = USAGE + "show {r, repositories}   Shows all available repositories\n"
			+ "show {n, namespaces}     Shows all namespaces\n"
			+ "show {c, contexts}       Shows all context identifiers\n";

	protected static final String LOAD = USAGE
			+ "load <file-or-url> [from <base-uri>] [into <context-id>]\n"
			+ "  <file-or-url>   The path or URL identifying the data file\n"
			+ "  <base-uri>      The base URI to use for resolving relative references, defaults to <file-or-url>\n"
			+ "  <context-id>    The ID of the context to add the data to, e.g. foo:bar or _:n123\n"
			+ "Loads the specified data file into the current repository\n";

	protected static final String VERIFY = USAGE + "verify <file-or-url>\n"
			+ "  <file-or-url>   The path or URL identifying the data file\n"
			+ "Verifies the validity of the specified data file\n";

	protected static final String CLEAR = USAGE + "clear                   Clears the entire repository\n"
			+ "clear (<uri>|null)...   Clears the specified context(s)\n";

	protected static final String SET = USAGE
			+ "set                            Shows all parameter values\n"
			+ "set width=<number>             Set the width for query result tables\n"
			+ "set log=<level>                Set the logging level (none, error, warning, info or debug)\n"
			+ "set showPrefix=<true|false>    Toggles use of prefixed names in query results\n"
			+ "set queryPrefix=<true|false>   Toggles automatic use of known namespace prefixes in queries (warning: buggy!)\n";

	PrintHelp(ConsoleIO consoleIO) {
		super();
		this.consoleIO = consoleIO;
		topics.put("connect", CONNECT);
		topics.put("disconnect", DISCONNECT);
		topics.put("create", CREATE);
		topics.put("drop", DROP);
		topics.put("open", OPEN);
		topics.put("close", CLOSE);
		topics.put("show", SHOW);
		topics.put("load", LOAD);
		topics.put("verify", VERIFY);
		topics.put("clear", CLEAR);
		topics.put("set", SET);
	}

	public void execute(String... parameters) {
		if (parameters.length < 2) {
			printCommandOverview();
		}
		else {
			final String target = parameters[1].toLowerCase(Locale.ENGLISH);
			if (topics.containsKey(target)) {
				consoleIO.writeln(topics.get(target));
			}
			else {
				consoleIO.writeln("No info available for command " + parameters[1]);
			}
		}
	}

	private void printCommandOverview() {
		consoleIO.writeln("For more information on a specific command, try 'help <command>.'");
		consoleIO.writeln("List of all commands:");
		consoleIO.writeln("help        Displays this help message");
		consoleIO.writeln("info        Shows info about the console");
		consoleIO.writeln("connect     Connects to a (local or remote) set of repositories");
		consoleIO.writeln("disconnect  Disconnects from the current set of repositories");
		consoleIO.writeln("create      Creates a new repository");
		consoleIO.writeln("drop        Drops a repository");
		consoleIO.writeln("open        Opens a repository to work on, takes a repository ID as argument");
		consoleIO.writeln("close       Closes the current repository");
		consoleIO.writeln("show        Displays an overview of various resources");
		consoleIO.writeln("load        Loads a data file into a repository, takes a file path or URL as argument");
		consoleIO.writeln("verify      Verifies the syntax of an RDF data file, takes a file path or URL as argument");
		consoleIO.writeln("clear       Removes data from a repository");
		consoleIO.writeln("serql       Evaluates the SeRQL query, takes a query as argument");
		consoleIO.writeln("sparql      Evaluates the SPARQL query, takes a query as argument");
		consoleIO.writeln("set         Allows various console parameters to be set");
		consoleIO.writeln("exit, quit  Exit the console");
	}
}
