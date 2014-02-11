/* 
 * Licensed to Aduna under one or more contributor license agreements.  
 * See the NOTICE.txt file distributed with this work for additional 
 * information regarding copyright ownership. 
 *
 * Aduna licenses this file to you under the terms of the Aduna BSD 
 * License (the "License"); you may not use this file except in compliance 
 * with the License. See the LICENSE.txt file distributed with this work 
 * for the full License.
 *
 * Unless required by applicable law or agreed to in writing, software 
 * distributed under the License is distributed on an "AS IS" BASIS, 
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or 
 * implied. See the License for the specific language governing permissions
 * and limitations under the License.
 */
package org.openrdf.rio.turtle;

import java.io.InputStream;

import junit.framework.TestSuite;

import org.openrdf.model.Literal;
import org.openrdf.model.URI;
import org.openrdf.query.BindingSet;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.sail.SailRepository;
import org.openrdf.rio.FailureMode;
import org.openrdf.rio.NegativeParserTest;
import org.openrdf.rio.PositiveParserTest;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFParser;
import org.openrdf.sail.memory.MemoryStore;

/**
 * JUnit test for the Turtle parser that uses the tests that are available <a
 * href="http://www.w3.org/2013/TurtleTests/">online</a>.
 */
public abstract class TurtleParserTestCase {

	/*-----------*
	 * Constants *
	 *-----------*/

	/**
	 * Base URL for W3C Tutle tests.
	 */
	protected static String TESTS_W3C_BASE_URL = "http://www.w3.org/2013/TurtleTests/";

	/**
	 * Base directory for W3C Turtle tests
	 */
	private static String TEST_W3C_FILE_BASE_PATH = "/testcases/turtle/tests-ttl-w3c-20131121/";

	private static String TEST_W3C_MANIFEST_URL = TEST_W3C_FILE_BASE_PATH + "manifest.ttl";

	private static String TEST_W3C_MANIFEST_URI_BASE = "http://www.w3.org/2013/TurtleTests/manifest.ttl#";

	private static String TEST_W3C_TEST_URI_BASE = "http://www.w3.org/2013/TurtleTests/";

	private static String NTRIPLES_TEST_URL = "http://www.w3.org/2000/10/rdf-tests/rdfcore/ntriples/test.nt";

	private static String NTRIPLES_TEST_FILE = "/testcases/ntriples/test.nt";

	/**
	 * Base directory for N-Triples compatibility tests that are part of the
	 * Turtle test-suite.
	 */
	private static String TURTLE_NTRIPLES_FILE_BASE_PATH = "/testcases/turtle/tests-nt/";

	/**
	 * Manifest for N-Triples compatibility tests that are part of the Turtle
	 * test-suite.
	 */
	private static String TURTLE_NTRIPLES_MANIFEST_URL = "/testcases/turtle/tests-nt/manifest.ttl";

	private static String TURTLE_NTRIPLES_MANIFEST_URI_BASE = "https://dvcs.w3.org/hg/rdf/raw-file/default/rdf-turtle/tests-nt/manifest.ttl#";

	private static String TURTLE_NTRIPLES_TEST_URI_BASE = "https://dvcs.w3.org/hg/rdf/raw-file/default/rdf-turtle/tests-nt/";

	/*--------------------*
	 * Static initializer *
	 *--------------------*/

	public TestSuite createTestSuite()
		throws Exception
	{
		// Create test suite
		TestSuite suite = new TestSuite(TurtleParserTestCase.class.getName());

		// Add the manifest for W3C test cases to a repository and query it
		Repository w3cRepository = new SailRepository(new MemoryStore());
		w3cRepository.initialize();
		RepositoryConnection w3cCon = w3cRepository.getConnection();

		InputStream inputStream = this.getClass().getResourceAsStream(TEST_W3C_MANIFEST_URL);
		w3cCon.add(inputStream, TEST_W3C_MANIFEST_URI_BASE, RDFFormat.TURTLE);

		parsePositiveTurtleSyntaxTests(suite, TEST_W3C_FILE_BASE_PATH, TESTS_W3C_BASE_URL,
				TEST_W3C_TEST_URI_BASE, w3cCon);
		parseNegativeTurtleSyntaxTests(suite, TEST_W3C_FILE_BASE_PATH, TESTS_W3C_BASE_URL,
				TEST_W3C_TEST_URI_BASE, w3cCon);
		parsePositiveTurtleEvalTests(suite, TEST_W3C_FILE_BASE_PATH, TESTS_W3C_BASE_URL,
				TEST_W3C_TEST_URI_BASE, w3cCon);
		parseNegativeTurtleEvalTests(suite, TEST_W3C_FILE_BASE_PATH, TESTS_W3C_BASE_URL,
				TEST_W3C_TEST_URI_BASE, w3cCon);

		w3cCon.close();
		w3cRepository.shutDown();

		return suite;
	}

	private void parsePositiveTurtleSyntaxTests(TestSuite suite, String fileBasePath, String testBaseUrl,
			String testLocationBaseUri, RepositoryConnection con)
		throws Exception
	{
		StringBuilder positiveQuery = new StringBuilder();
		positiveQuery.append(" PREFIX mf:   <http://www.w3.org/2001/sw/DataAccess/tests/test-manifest#>\n");
		positiveQuery.append(" PREFIX qt:   <http://www.w3.org/2001/sw/DataAccess/tests/test-query#>\n");
		positiveQuery.append(" PREFIX rdft: <http://www.w3.org/ns/rdftest#>\n");
		positiveQuery.append(" SELECT ?test ?testName ?inputURL ?outputURL \n");
		positiveQuery.append(" WHERE { \n");
		positiveQuery.append("     ?test a rdft:TestTurtlePositiveSyntax . ");
		positiveQuery.append("     ?test mf:name ?testName . ");
		positiveQuery.append("     ?test mf:action ?inputURL . ");
		positiveQuery.append(" }");

		TupleQueryResult queryResult = con.prepareTupleQuery(QueryLanguage.SPARQL, positiveQuery.toString()).evaluate();

		// Add all positive parser tests to the test suite
		while (queryResult.hasNext()) {
			BindingSet bindingSet = queryResult.next();
			URI nextTestUri = (URI)bindingSet.getValue("test");
			String nextTestName = ((Literal)bindingSet.getValue("testName")).getLabel();
			String nextTestFile = removeBase(((URI)bindingSet.getValue("inputURL")).toString(),
					testLocationBaseUri);
			String nextInputURL = fileBasePath + nextTestFile;

			String nextBaseUrl = testBaseUrl + nextTestFile;

			suite.addTest(new PositiveParserTest(nextTestUri, nextTestName, nextInputURL, null,
					nextBaseUrl, createTurtleParser(), createNTriplesParser()));
		}

		queryResult.close();

	}

	private void parseNegativeTurtleSyntaxTests(TestSuite suite, String fileBasePath, String testBaseUrl,
			String manifestBaseUrl, RepositoryConnection con)
		throws Exception
	{
		StringBuilder negativeQuery = new StringBuilder();
		negativeQuery.append(" PREFIX mf:   <http://www.w3.org/2001/sw/DataAccess/tests/test-manifest#>\n");
		negativeQuery.append(" PREFIX qt:   <http://www.w3.org/2001/sw/DataAccess/tests/test-query#>\n");
		negativeQuery.append(" PREFIX rdft: <http://www.w3.org/ns/rdftest#>\n");
		negativeQuery.append(" SELECT ?test ?testName ?inputURL ?outputURL \n");
		negativeQuery.append(" WHERE { \n");
		negativeQuery.append("     ?test a rdft:TestTurtleNegativeSyntax . ");
		negativeQuery.append("     ?test mf:name ?testName . ");
		negativeQuery.append("     ?test mf:action ?inputURL . ");
		negativeQuery.append(" }");

		TupleQueryResult queryResult = con.prepareTupleQuery(QueryLanguage.SPARQL, negativeQuery.toString()).evaluate();

		// Add all negative parser tests to the test suite
		while (queryResult.hasNext()) {
			BindingSet bindingSet = queryResult.next();
			URI nextTestUri = (URI)bindingSet.getValue("test");
			String nextTestName = ((Literal)bindingSet.getValue("testName")).toString();
			String nextTestFile = removeBase(((URI)bindingSet.getValue("inputURL")).toString(), manifestBaseUrl);
			String nextInputURL = fileBasePath + nextTestFile;

			String nextBaseUrl = testBaseUrl + nextTestFile;

			suite.addTest(new NegativeParserTest(nextTestUri, nextTestName, nextInputURL, nextBaseUrl,
					createTurtleParser(), FailureMode.IGNORE_FAILURE));
		}

		queryResult.close();

	}

	private void parsePositiveTurtleEvalTests(TestSuite suite, String fileBasePath, String testBaseUrl,
			String manifestBaseUrl, RepositoryConnection con)
		throws Exception
	{
		StringBuilder positiveEvalQuery = new StringBuilder();
		positiveEvalQuery.append(" PREFIX mf:   <http://www.w3.org/2001/sw/DataAccess/tests/test-manifest#>\n");
		positiveEvalQuery.append(" PREFIX qt:   <http://www.w3.org/2001/sw/DataAccess/tests/test-query#>\n");
		positiveEvalQuery.append(" PREFIX rdft: <http://www.w3.org/ns/rdftest#>\n");
		positiveEvalQuery.append(" SELECT ?test ?testName ?inputURL ?outputURL \n");
		positiveEvalQuery.append(" WHERE { \n");
		positiveEvalQuery.append("     ?test a rdft:TestTurtleEval . ");
		positiveEvalQuery.append("     ?test mf:name ?testName . ");
		positiveEvalQuery.append("     ?test mf:action ?inputURL . ");
		positiveEvalQuery.append("     ?test mf:result ?outputURL . ");
		positiveEvalQuery.append(" }");

		TupleQueryResult queryResult = con.prepareTupleQuery(QueryLanguage.SPARQL, positiveEvalQuery.toString()).evaluate();

		// Add all positive eval tests to the test suite
		while (queryResult.hasNext()) {
			BindingSet bindingSet = queryResult.next();
			URI nextTestUri = (URI)bindingSet.getValue("test");
			String nextTestName = ((Literal)bindingSet.getValue("testName")).getLabel();
			String nextTestFile = removeBase(((URI)bindingSet.getValue("inputURL")).toString(), manifestBaseUrl);
			String nextInputURL = fileBasePath + nextTestFile;
			String nextOutputURL = fileBasePath
					+ removeBase(((URI)bindingSet.getValue("outputURL")).toString(), manifestBaseUrl);

			String nextBaseUrl = testBaseUrl + nextTestFile;

			if (nextTestName.contains("CARRIAGE_RETURN")) {
				// FIXME: Sesame seems not to preserve the CARRIAGE_RETURN character
				// right now
				System.err.println("Ignoring Turtle Positive Parser Eval Test: " + nextInputURL);
				continue;
			}
			else if (nextTestName.contains("UTF8_boundaries")
					|| nextTestName.contains("PN_CHARS_BASE_character_boundaries")
					|| nextTestName.contains("localName_with_non_leading_extras"))
			{
				// FIXME: UTF8 support not implemented yet
				System.err.println("Ignoring Turtle Positive Parser Eval Test: " + nextInputURL);
				continue;
			}

			suite.addTest(new PositiveParserTest(nextTestUri, nextTestName, nextInputURL, nextOutputURL,
					nextBaseUrl, createTurtleParser(), createNTriplesParser()));
		}

		queryResult.close();
	}

	private void parseNegativeTurtleEvalTests(TestSuite suite, String fileBasePath, String testBaseUrl,
			String manifestBaseUrl, RepositoryConnection con)
		throws Exception
	{
		StringBuilder negativeEvalQuery = new StringBuilder();
		negativeEvalQuery.append(" PREFIX mf:   <http://www.w3.org/2001/sw/DataAccess/tests/test-manifest#>\n");
		negativeEvalQuery.append(" PREFIX qt:   <http://www.w3.org/2001/sw/DataAccess/tests/test-query#>\n");
		negativeEvalQuery.append(" PREFIX rdft: <http://www.w3.org/ns/rdftest#>\n");
		negativeEvalQuery.append(" SELECT ?test ?testName ?inputURL ?outputURL \n");
		negativeEvalQuery.append(" WHERE { \n");
		negativeEvalQuery.append("     ?test a rdft:TestTurtleNegativeEval . ");
		negativeEvalQuery.append("     ?test mf:name ?testName . ");
		negativeEvalQuery.append("     ?test mf:action ?inputURL . ");
		negativeEvalQuery.append(" }");

		TupleQueryResult queryResult = con.prepareTupleQuery(QueryLanguage.SPARQL, negativeEvalQuery.toString()).evaluate();

		// Add all negative eval tests to the test suite
		while (queryResult.hasNext()) {
			BindingSet bindingSet = queryResult.next();
			URI nextTestUri = (URI)bindingSet.getValue("test");
			String nextTestName = ((Literal)bindingSet.getValue("testName")).toString();
			String nextTestFile = removeBase(((URI)bindingSet.getValue("inputURL")).toString(), manifestBaseUrl);
			String nextInputURL = fileBasePath + nextTestFile;

			String nextBaseUrl = testBaseUrl + nextTestFile;

			suite.addTest(new NegativeParserTest(nextTestUri, nextTestName, nextInputURL, nextBaseUrl,
					createTurtleParser(), FailureMode.IGNORE_FAILURE));
		}

		queryResult.close();
	}

	private void parsePositiveNTriplesSyntaxTests(TestSuite suite, String fileBasePath, String testBaseUrl,
			String manifestBaseUrl, RepositoryConnection con)
		throws Exception
	{
		StringBuilder positiveQuery = new StringBuilder();
		positiveQuery.append(" PREFIX mf:   <http://www.w3.org/2001/sw/DataAccess/tests/test-manifest#>\n");
		positiveQuery.append(" PREFIX qt:   <http://www.w3.org/2001/sw/DataAccess/tests/test-query#>\n");
		positiveQuery.append(" PREFIX rdft: <http://www.w3.org/ns/rdftest#>\n");
		positiveQuery.append(" SELECT ?test ?testName ?inputURL ?outputURL \n");
		positiveQuery.append(" WHERE { \n");
		positiveQuery.append("     ?test a rdft:TestNTriplesPositiveSyntax . ");
		positiveQuery.append("     ?test mf:name ?testName . ");
		positiveQuery.append("     ?test mf:action ?inputURL . ");
		positiveQuery.append(" }");

		TupleQueryResult queryResult = con.prepareTupleQuery(QueryLanguage.SPARQL, positiveQuery.toString()).evaluate();

		// Add all positive parser tests to the test suite
		while (queryResult.hasNext()) {
			BindingSet bindingSet = queryResult.next();
			URI nextTestUri = (URI)bindingSet.getValue("test");
			String nextTestName = ((Literal)bindingSet.getValue("testName")).getLabel();
			String nextTestFile = removeBase(((URI)bindingSet.getValue("inputURL")).toString(), manifestBaseUrl);
			String nextInputURL = fileBasePath + nextTestFile;

			String nextBaseUrl = testBaseUrl + nextTestFile;

			suite.addTest(new PositiveParserTest(nextTestUri, nextTestName, nextInputURL, null,
					nextBaseUrl, createNTriplesParser(), createNTriplesParser()));
		}

		queryResult.close();

	}

	private void parseNegativeNTriplesSyntaxTests(TestSuite suite, String fileBasePath, String testBaseUrl,
			String manifestBaseUrl, RepositoryConnection con)
		throws Exception
	{
		StringBuilder negativeQuery = new StringBuilder();
		negativeQuery.append(" PREFIX mf:   <http://www.w3.org/2001/sw/DataAccess/tests/test-manifest#>\n");
		negativeQuery.append(" PREFIX qt:   <http://www.w3.org/2001/sw/DataAccess/tests/test-query#>\n");
		negativeQuery.append(" PREFIX rdft: <http://www.w3.org/ns/rdftest#>\n");
		negativeQuery.append(" SELECT ?test ?testName ?inputURL ?outputURL \n");
		negativeQuery.append(" WHERE { \n");
		negativeQuery.append("     ?test a rdft:TestNTriplesNegativeSyntax . ");
		negativeQuery.append("     ?test mf:name ?testName . ");
		negativeQuery.append("     ?test mf:action ?inputURL . ");
		negativeQuery.append(" }");

		TupleQueryResult queryResult = con.prepareTupleQuery(QueryLanguage.SPARQL, negativeQuery.toString()).evaluate();

		// Add all negative parser tests to the test suite
		while (queryResult.hasNext()) {
			BindingSet bindingSet = queryResult.next();
			URI nextTestUri = (URI)bindingSet.getValue("test");
			String nextTestName = ((Literal)bindingSet.getValue("testName")).toString();
			String nextTestFile = removeBase(((URI)bindingSet.getValue("inputURL")).toString(), manifestBaseUrl);
			String nextInputURL = fileBasePath + nextTestFile;

			String nextBaseUrl = testBaseUrl + nextTestFile;

			suite.addTest(new NegativeParserTest(nextTestUri, nextTestName, nextInputURL, nextBaseUrl,
					createNTriplesParser(), FailureMode.IGNORE_FAILURE));
		}

		queryResult.close();

	}

	/**
	 * @return An implementation of a Turtle parser to test compliance with the
	 *         Turtle Test Suite Turtle tests.
	 */
	protected abstract RDFParser createTurtleParser();

	/**
	 * @return An implementation of an N-Triples parser to test compliance with
	 *         the Turtle Test Suite N-Triples tests.
	 */
	protected abstract RDFParser createNTriplesParser();

	private String removeBase(String baseUrl, String redundantBaseUrl) {
		if (baseUrl.startsWith(redundantBaseUrl)) {
			return baseUrl.substring(redundantBaseUrl.length());
		}

		return baseUrl;
	}

}
