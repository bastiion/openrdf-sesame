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
package org.openrdf.query;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Test;

import org.openrdf.model.BNode;
import org.openrdf.model.Literal;
import org.openrdf.model.Model;
import org.openrdf.model.Statement;
import org.openrdf.model.IRI;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.impl.SimpleValueFactory;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.query.impl.EmptyBindingSet;
import org.openrdf.query.impl.ListBindingSet;
import org.openrdf.query.impl.MutableTupleQueryResult;

/**
 * @author Arjohn Kampman
 */
public class QueryResultsTest {

	private MutableTupleQueryResult tqr1;

	private MutableTupleQueryResult tqr2;

	private MutableTupleQueryResult tqr3;

	/** a stub GraphQueryResult, containing a number of duplicate statements */
	private GraphQueryResult gqr;

	private static ValueFactory VF = SimpleValueFactory.getInstance();

	private List<String> twoBindingNames = Arrays.asList("a", "b");

	private List<String> threeBindingNames = Arrays.asList("a", "b", "c");

	private IRI foo;

	private IRI bar;

	private BNode bnode1;

	private BNode bnode2;

	private Literal lit1;

	private Literal lit2;

	private IRI a = VF.createIRI("urn:a");

	private IRI b = VF.createIRI("urn:b");

	private IRI c = VF.createIRI("urn:c");

	private IRI p = VF.createIRI("urn:p");

	private IRI q = VF.createIRI("urn:q");

	@Before
	public void setUp() {
		tqr1 = new MutableTupleQueryResult(twoBindingNames);
		tqr2 = new MutableTupleQueryResult(twoBindingNames);
		tqr3 = new MutableTupleQueryResult(threeBindingNames);

		gqr = new StubGraphQueryResult();

		foo = VF.createIRI("http://example.org/foo");
		bar = VF.createIRI("http://example.org/bar");

		bnode1 = VF.createBNode();
		bnode2 = VF.createBNode();

		lit1 = VF.createLiteral(1);
		lit2 = VF.createLiteral("test", "en");
	}

	@Test
	public void testAsModel()
		throws QueryEvaluationException
	{
		Model model = QueryResults.asModel(gqr);

		assertFalse(gqr.hasNext());
		assertNotNull(model);
		assertTrue(model.contains(VF.createStatement(a, p, b)));
	}

	@Test
	public void testStreamGraphResult() {
		List<Statement> aboutA = QueryResults.stream(gqr).filter(s -> s.getSubject().equals(a)).collect(
				Collectors.toList());

		assertFalse(aboutA.isEmpty());

		for (Statement st : aboutA) {
			assertTrue(st.getSubject().equals(a));
		}
	}

	@Test
	public void testStreamTupleResult() {
		BindingSet a = new ListBindingSet(twoBindingNames, foo, lit1);
		BindingSet b = new ListBindingSet(twoBindingNames, bar, lit2);
		tqr1.append(a);
		tqr1.append(b);
		tqr1.append(a);
		tqr1.append(b);
		tqr1.append(b);

		List<BindingSet> list = QueryResults.stream(tqr1).filter(bs -> bs.getValue("a").equals(foo)).collect(
				Collectors.toList());

		assertNotNull(list);
		assertFalse(list.isEmpty());
		for (BindingSet bs : list) {
			assertTrue(bs.getValue("a").equals(foo));
		}
	}

	@Test
	public void testGraphQueryResultEquals() throws QueryEvaluationException {
		
		StubGraphQueryResult toCompare = new StubGraphQueryResult();
		
		assertTrue(QueryResults.equals(gqr, toCompare));
		gqr = new StubGraphQueryResult();
		toCompare = new StubGraphQueryResult();
		toCompare.statements.add(VF.createStatement(VF.createURI("urn:test-gqr-equals"), RDF.TYPE, RDF.PROPERTY));
		
		assertFalse(QueryResults.equals(gqr, toCompare));
	}
	
	@Test
	public void testDistinctGraphQueryResults()
		throws QueryEvaluationException
	{

		GraphQueryResult filtered = QueryResults.distinctResults(gqr);

		List<Statement> processed = new ArrayList<Statement>();
		while (filtered.hasNext()) {
			Statement result = filtered.next();
			assertFalse(processed.contains(result));
			processed.add(result);
		}
	}

	@Test
	public void testDistinctTupleQueryResults()
		throws QueryEvaluationException
	{

		BindingSet a = new ListBindingSet(twoBindingNames, foo, lit1);
		BindingSet b = new ListBindingSet(twoBindingNames, bar, lit2);
		tqr1.append(a);
		tqr1.append(b);
		tqr1.append(a);
		tqr1.append(b);
		tqr1.append(b);

		TupleQueryResult filtered = QueryResults.distinctResults(tqr1);

		List<BindingSet> processed = new ArrayList<BindingSet>();
		while (filtered.hasNext()) {
			BindingSet result = filtered.next();
			assertFalse(processed.contains(result));
			processed.add(result);
		}
	}

	private class StubGraphQueryResult implements GraphQueryResult {

		private List<Statement> statements = new ArrayList<Statement>();

		public StubGraphQueryResult() {
			statements.add(VF.createStatement(a, p, b));
			statements.add(VF.createStatement(b, q, c));
			statements.add(VF.createStatement(c, q, a));
			statements.add(VF.createStatement(c, q, a));
			statements.add(VF.createStatement(a, p, b));
		}

		public void close()
			throws QueryEvaluationException
		{
			// TODO Auto-generated method stub
		}

		public boolean hasNext()
			throws QueryEvaluationException
		{
			return !statements.isEmpty();
		}

		public Statement next()
			throws QueryEvaluationException
		{
			return statements.remove(0);
		}

		public void remove()
			throws QueryEvaluationException
		{
			statements.remove(0);
		}

		public Map<String, String> getNamespaces()
			throws QueryEvaluationException
		{
			// TODO Auto-generated method stub
			return null;
		}

	}

	@Test
	public void testEmptyQueryResult()
		throws QueryEvaluationException
	{
		tqr1.append(EmptyBindingSet.getInstance());
		tqr2.append(EmptyBindingSet.getInstance());
		tqr3.append(EmptyBindingSet.getInstance());

		assertTrue(QueryResults.equals(tqr1, tqr2));
	}

	@Test
	public void testEmptyQueryResult2()
		throws QueryEvaluationException
	{
		tqr1.append(EmptyBindingSet.getInstance());
		tqr3.append(EmptyBindingSet.getInstance());

		assertTrue(QueryResults.equals(tqr1, tqr3));
	}

	@Test
	public void testEmptyQueryResult3()
		throws QueryEvaluationException
	{
		tqr1.append(EmptyBindingSet.getInstance());
		tqr3.append(EmptyBindingSet.getInstance());

		assertTrue(QueryResults.equals(tqr3, tqr1));
	}

	@Test
	public void testEmptyBindingSet()
		throws QueryEvaluationException
	{
		tqr1.append(EmptyBindingSet.getInstance());
		tqr2.append(EmptyBindingSet.getInstance());
		assertTrue(QueryResults.equals(tqr1, tqr2));
	}

	@Test
	public void testNonBNodeBindingSet1()
		throws QueryEvaluationException
	{
		tqr1.append(new ListBindingSet(twoBindingNames, foo, lit1));
		tqr1.append(new ListBindingSet(twoBindingNames, bar, lit2));

		tqr2.append(new ListBindingSet(twoBindingNames, bar, lit2));
		tqr2.append(new ListBindingSet(twoBindingNames, foo, lit1));

		assertTrue(QueryResults.equals(tqr1, tqr2));
	}

	@Test
	public void testNonBNodeBindingSet2()
		throws QueryEvaluationException
	{
		tqr1.append(new ListBindingSet(twoBindingNames, foo, lit1));
		tqr2.append(new ListBindingSet(twoBindingNames, foo, lit2));

		assertFalse(QueryResults.equals(tqr1, tqr2));
	}

	@Test
	public void testNonBNodeBindingSet3()
		throws QueryEvaluationException
	{
		tqr3.append(new ListBindingSet(threeBindingNames, foo, lit1, bar));
		tqr2.append(new ListBindingSet(twoBindingNames, foo, lit1));

		assertFalse(QueryResults.equals(tqr3, tqr2));
	}

	@Test
	public void testNonBNodeBindingSet()
		throws QueryEvaluationException
	{
		tqr1.append(new ListBindingSet(twoBindingNames, foo, lit1));
		tqr2.append(new ListBindingSet(twoBindingNames, foo, lit2));

		assertFalse(QueryResults.equals(tqr1, tqr2));
	}

	@Test
	public void testBNodeBindingSet1()
		throws QueryEvaluationException
	{
		tqr1.append(new ListBindingSet(twoBindingNames, foo, bnode1));
		tqr1.append(new ListBindingSet(twoBindingNames, bar, bnode2));

		tqr2.append(new ListBindingSet(twoBindingNames, foo, bnode2));
		tqr2.append(new ListBindingSet(twoBindingNames, bar, bnode1));

		assertTrue(QueryResults.equals(tqr1, tqr2));
	}

	@Test
	public void testBNodeBindingSet2()
		throws QueryEvaluationException
	{
		tqr1.append(new ListBindingSet(twoBindingNames, foo, bnode1));
		tqr2.append(new ListBindingSet(twoBindingNames, foo, lit1));

		assertFalse(QueryResults.equals(tqr1, tqr2));
	}

	@Test
	public void testBNodeBindingSet3()
		throws QueryEvaluationException
	{
		tqr1.append(new ListBindingSet(twoBindingNames, foo, bnode1));
		tqr1.append(new ListBindingSet(twoBindingNames, foo, bnode2));

		tqr2.append(new ListBindingSet(twoBindingNames, foo, bnode1));
		tqr2.append(new ListBindingSet(twoBindingNames, foo, bnode1));

		assertFalse(QueryResults.equals(tqr1, tqr2));
	}

	@Test
	public void testBNodeBindingSet4()
		throws QueryEvaluationException
	{
		tqr1.append(new ListBindingSet(twoBindingNames, bnode1, bnode2));
		tqr1.append(new ListBindingSet(twoBindingNames, foo, bnode2));

		tqr2.append(new ListBindingSet(twoBindingNames, bnode2, bnode1));
		tqr2.append(new ListBindingSet(twoBindingNames, foo, bnode1));

		assertTrue(QueryResults.equals(tqr1, tqr2));
	}

	@Test
	public void testBNodeBindingSet5()
		throws QueryEvaluationException
	{
		tqr1.append(new ListBindingSet(twoBindingNames, bnode1, bnode2));
		tqr1.append(new ListBindingSet(twoBindingNames, foo, bnode2));

		tqr2.append(new ListBindingSet(twoBindingNames, bnode2, bnode1));
		tqr2.append(new ListBindingSet(twoBindingNames, foo, bnode2));

		assertFalse(QueryResults.equals(tqr1, tqr2));
	}

	@Test
	public void testBNodeBindingSet6()
		throws QueryEvaluationException
	{
		tqr3.append(new ListBindingSet(threeBindingNames, foo, bnode2, bnode1));
		tqr1.append(new ListBindingSet(twoBindingNames, foo, bnode2));

		assertFalse(QueryResults.equals(tqr3, tqr1));
	}

	@Test
	public void testBNodeBindingSet7()
		throws QueryEvaluationException
	{
		tqr3.append(new ListBindingSet(threeBindingNames, foo, bnode2, bnode1));
		tqr1.append(new ListBindingSet(twoBindingNames, foo, bnode2));

		assertFalse(QueryResults.equals(tqr1, tqr3));
	}
}
