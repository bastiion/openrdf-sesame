/*
 * Copyright Aduna (http://www.aduna-software.com/) (c) 2007.
 *
 * Licensed under the Aduna BSD-style license.
 */
package org.openrdf.query.dawg;

import static org.openrdf.query.dawg.DAWGTestResultSetSchema.BINDING;
import static org.openrdf.query.dawg.DAWGTestResultSetSchema.RESULTSET;
import static org.openrdf.query.dawg.DAWGTestResultSetSchema.RESULTVARIABLE;
import static org.openrdf.query.dawg.DAWGTestResultSetSchema.SOLUTION;
import static org.openrdf.query.dawg.DAWGTestResultSetSchema.VALUE;
import static org.openrdf.query.dawg.DAWGTestResultSetSchema.VARIABLE;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.openrdf.model.Literal;
import org.openrdf.model.Model;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.Value;
import org.openrdf.model.impl.ModelImpl;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.query.Binding;
import org.openrdf.query.TupleQueryResultHandler;
import org.openrdf.query.TupleQueryResultHandlerException;
import org.openrdf.query.impl.BindingImpl;
import org.openrdf.query.impl.MapBindingSet;
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.rio.helpers.RDFHandlerBase;

/**
 * @author Arjohn Kampman
 */
public class DAWGTestResultSetParser extends RDFHandlerBase {

	/*-----------*
	 * Constants *
	 *-----------*/

	/**
	 * RDFHandler to report the generated statements to.
	 */
	private final TupleQueryResultHandler tqrHandler;

	/*-----------*
	 * Variables *
	 *-----------*/

	private Model model = new ModelImpl();

	/*--------------*
	 * Constructors *
	 *--------------*/

	public DAWGTestResultSetParser(TupleQueryResultHandler tqrHandler) {
		this.tqrHandler = tqrHandler;
	}

	/*---------*
	 * Methods *
	 *---------*/

	@Override
	public void startRDF()
		throws RDFHandlerException
	{
		model.clear();
	}

	@Override
	public void handleStatement(Statement st)
		throws RDFHandlerException
	{
		model.add(st);
	}

	@Override
	public void endRDF()
		throws RDFHandlerException
	{
		try {
			Resource resultSetNode = model.subjects(RDF.TYPE, RESULTSET).iterator().next();

			List<String> bindingNames = getBindingNames(resultSetNode);
			tqrHandler.startQueryResult(bindingNames);

			Iterator<Value> solIter = model.objects(resultSetNode, SOLUTION).iterator();
			while (solIter.hasNext()) {
				Value solutionNode = solIter.next();

				if (solutionNode instanceof Resource) {
					reportSolution((Resource)solutionNode, bindingNames);
				}
				else {
					new RDFHandlerException("Value for " + SOLUTION + " is not a resource: " + solutionNode);
				}
			}

			tqrHandler.endQueryResult();
		}
		catch (RDFHandlerException e) {
			throw e;
		}
		catch (TupleQueryResultHandlerException e) {
			throw new RDFHandlerException(e.getMessage(), e);
		}
		catch (Exception e) {
			throw new RDFHandlerException(e.getMessage(), e);
		}
	}

	private List<String> getBindingNames(Resource resultSetNode)
		throws RDFHandlerException
	{
		List<String> bindingNames = new ArrayList<String>(16);

		Iterator<Value> varIter = model.objects(resultSetNode, RESULTVARIABLE).iterator();

		while (varIter.hasNext()) {
			Value varName = varIter.next();

			if (varName instanceof Literal) {
				bindingNames.add(((Literal)varName).getLabel());
			}
			else {
				throw new RDFHandlerException("Value for " + RESULTVARIABLE + " is not a literal: " + varName);
			}
		}

		return bindingNames;
	}

	private void reportSolution(Resource solutionNode, List<String> bindingNames)
		throws RDFHandlerException
	{
		MapBindingSet bindingSet = new MapBindingSet(bindingNames.size());

		Iterator<Value> bindingIter = model.objects(solutionNode, BINDING).iterator();
		while (bindingIter.hasNext()) {
			Value bindingNode = bindingIter.next();

			if (bindingNode instanceof Resource) {
				Binding binding = getBinding((Resource)bindingNode);
				bindingSet.addBinding(binding);
			}
			else {
				throw new RDFHandlerException("Value for " + BINDING + " is not a resource: " + bindingNode);
			}
		}

		try {
			tqrHandler.handleSolution(bindingSet);
		}
		catch (TupleQueryResultHandlerException e) {
			throw new RDFHandlerException(e.getMessage(), e);
		}
	}

	private Binding getBinding(Resource bindingNode) {
		Literal name = (Literal)model.objects(bindingNode, VARIABLE).iterator().next();
		Value value = model.objects(bindingNode, VALUE).iterator().next();
		return new BindingImpl(name.getLabel(), value);
	}
}
