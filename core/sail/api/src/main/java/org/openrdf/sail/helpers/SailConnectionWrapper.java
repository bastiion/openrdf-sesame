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
package org.openrdf.sail.helpers;

import info.aduna.iteration.CloseableIteration;

import org.openrdf.IsolationLevel;
import org.openrdf.model.Namespace;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.query.BindingSet;
import org.openrdf.query.Dataset;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.algebra.TupleExpr;
import org.openrdf.query.algebra.evaluation.federation.FederatedServiceResolver;
import org.openrdf.query.algebra.evaluation.federation.FederatedServiceResolverClient;
import org.openrdf.sail.SailConnection;
import org.openrdf.sail.SailException;
import org.openrdf.sail.UnknownSailTransactionStateException;
import org.openrdf.sail.UpdateContext;

/**
 * An implementation of the SailConnection interface that wraps another
 * SailConnection object and forwards any method calls to the wrapped
 * connection.
 * 
 * @author Jeen Broekstra
 */
public class SailConnectionWrapper implements SailConnection, FederatedServiceResolverClient {

	/*-----------*
	 * Variables *
	 *-----------*/

	/**
	 * The wrapped SailConnection.
	 */
	private SailConnection wrappedCon;

	/*--------------*
	 * Constructors *
	 *--------------*/

	/**
	 * Creates a new TransactionWrapper object that wraps the supplied
	 * connection.
	 */
	public SailConnectionWrapper(SailConnection wrappedCon) {
		this.wrappedCon = wrappedCon;
	}

	/*---------*
	 * Methods *
	 *---------*/

	/**
	 * Gets the connection that is wrapped by this object.
	 * 
	 * @return The SailConnection object that was supplied to the constructor of
	 *         this class.
	 */
	public SailConnection getWrappedConnection() {
		return wrappedCon;
	}

	@Override
	public void setFederatedServiceResolver(FederatedServiceResolver resolver) {
		if (wrappedCon instanceof FederatedServiceResolverClient) {
			((FederatedServiceResolverClient)wrappedCon).setFederatedServiceResolver(resolver);
		}
	}

	@Override
	public boolean isOpen()
		throws SailException
	{
		return wrappedCon.isOpen();
	}

	@Override
	public void close()
		throws SailException
	{
		wrappedCon.close();
	}

	@Override
	public CloseableIteration<? extends BindingSet, QueryEvaluationException> evaluate(TupleExpr tupleExpr,
			Dataset dataset, BindingSet bindings, boolean includeInferred)
		throws SailException
	{
		return wrappedCon.evaluate(tupleExpr, dataset, bindings, includeInferred);
	}

	@Override
	public CloseableIteration<? extends Resource, SailException> getContextIDs()
		throws SailException
	{
		return wrappedCon.getContextIDs();
	}

	@Override
	public CloseableIteration<? extends Statement, SailException> getStatements(Resource subj, URI pred,
			Value obj, boolean includeInferred, Resource... contexts)
		throws SailException
	{
		return wrappedCon.getStatements(subj, pred, obj, includeInferred, contexts);
	}

	@Override
	public long size(Resource... contexts)
		throws SailException
	{
		return wrappedCon.size(contexts);
	}

	/*
	 * Not in the API, preserving for binary compatibility. Will be removed in future.
	 * 
	 * Should use {@link #size(Resource...)} instead, which is called by this method.
	 */
	public long size(Resource context)
		throws SailException
	{
		return wrappedCon.size(context);
	}

	@Override
	public void commit()
		throws SailException
	{
		wrappedCon.commit();
	}

	@Override
	public void rollback()
		throws SailException
	{
		wrappedCon.rollback();
	}

	@Override
	public void addStatement(Resource subj, URI pred, Value obj, Resource... contexts)
		throws SailException
	{
		wrappedCon.addStatement(subj, pred, obj, contexts);
	}

	@Override
	public void removeStatements(Resource subj, URI pred, Value obj, Resource... contexts)
		throws SailException
	{
		wrappedCon.removeStatements(subj, pred, obj, contexts);
	}

	@Override
	public void startUpdate(UpdateContext modify)
		throws SailException
	{
		wrappedCon.startUpdate(modify);
	}

	@Override
	public void addStatement(UpdateContext modify, Resource subj, URI pred, Value obj, Resource... contexts)
		throws SailException
	{
		wrappedCon.addStatement(modify, subj, pred, obj, contexts);
	}

	@Override
	public void removeStatement(UpdateContext modify, Resource subj, URI pred, Value obj, Resource... contexts)
		throws SailException
	{
		wrappedCon.removeStatement(modify, subj, pred, obj, contexts);
	}

	@Override
	public void endUpdate(UpdateContext modify)
		throws SailException
	{
		wrappedCon.endUpdate(modify);
	}

	@Override
	public void clear(Resource... contexts)
		throws SailException
	{
		wrappedCon.clear(contexts);
	}

	@Override
	public CloseableIteration<? extends Namespace, SailException> getNamespaces()
		throws SailException
	{
		return wrappedCon.getNamespaces();
	}

	@Override
	public String getNamespace(String prefix)
		throws SailException
	{
		return wrappedCon.getNamespace(prefix);
	}

	@Override
	public void setNamespace(String prefix, String name)
		throws SailException
	{
		wrappedCon.setNamespace(prefix, name);
	}

	@Override
	public void removeNamespace(String prefix)
		throws SailException
	{
		wrappedCon.removeNamespace(prefix);
	}

	@Override
	public void clearNamespaces()
		throws SailException
	{
		wrappedCon.clearNamespaces();
	}

	@Override
	public void begin()
		throws SailException
	{
		wrappedCon.begin();
	}

	@Override
	public void begin(IsolationLevel level)
		throws SailException
	{
		wrappedCon.begin(level);
	}

	@Override
	public void flush()
		throws SailException
	{
		wrappedCon.flush();
	}

	@Override
	public void prepare()
		throws SailException
	{
		wrappedCon.prepare();
	}

	@Override
	public boolean isActive()
		throws UnknownSailTransactionStateException
	{
		return wrappedCon.isActive();
	}
}
