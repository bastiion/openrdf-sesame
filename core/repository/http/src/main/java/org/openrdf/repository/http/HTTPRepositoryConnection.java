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
package org.openrdf.repository.http;

import static org.openrdf.rio.RDFFormat.NTRIPLES;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.apache.http.client.HttpClient;

import info.aduna.iteration.CloseableIteratorIteration;

import org.openrdf.OpenRDFException;
import org.openrdf.OpenRDFUtil;
import org.openrdf.http.client.HttpClientDependent;
import org.openrdf.http.client.SesameSession;
import org.openrdf.http.protocol.Protocol;
import org.openrdf.http.protocol.Protocol.Action;
import org.openrdf.http.protocol.transaction.operations.AddStatementOperation;
import org.openrdf.http.protocol.transaction.operations.ClearNamespacesOperation;
import org.openrdf.http.protocol.transaction.operations.ClearOperation;
import org.openrdf.http.protocol.transaction.operations.RemoveNamespaceOperation;
import org.openrdf.http.protocol.transaction.operations.RemoveStatementsOperation;
import org.openrdf.http.protocol.transaction.operations.SPARQLUpdateOperation;
import org.openrdf.http.protocol.transaction.operations.SetNamespaceOperation;
import org.openrdf.http.protocol.transaction.operations.TransactionOperation;
import org.openrdf.model.Literal;
import org.openrdf.model.Model;
import org.openrdf.model.Namespace;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.impl.LinkedHashModel;
import org.openrdf.model.impl.NamespaceImpl;
import org.openrdf.model.vocabulary.SESAME;
import org.openrdf.query.BindingSet;
import org.openrdf.query.BooleanQuery;
import org.openrdf.query.GraphQuery;
import org.openrdf.query.MalformedQueryException;
import org.openrdf.query.Query;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.QueryInterruptedException;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.TupleQuery;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.query.Update;
import org.openrdf.query.parser.QueryParserUtil;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.RepositoryResult;
import org.openrdf.repository.UnknownTransactionStateException;
import org.openrdf.repository.base.RepositoryConnectionBase;
import org.openrdf.rio.ParserConfig;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFHandler;
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.rio.RDFParseException;
import org.openrdf.rio.RDFParserRegistry;
import org.openrdf.rio.Rio;
import org.openrdf.rio.helpers.BasicParserSettings;
import org.openrdf.rio.helpers.StatementCollector;

/**
 * RepositoryConnection that communicates with a server using the HTTP protocol.
 * Methods in this class may throw the specific RepositoryException subclasses
 * UnautorizedException and NotAllowedException, the semantics of which are
 * defined by the HTTP protocol.
 * 
 * @see org.openrdf.http.protocol.UnauthorizedException
 * @see org.openrdf.http.protocol.NotAllowedException
 * @author Arjohn Kampman
 * @author Herko ter Horst
 */
class HTTPRepositoryConnection extends RepositoryConnectionBase implements HttpClientDependent {

	/*-----------*
	 * Variables *
	 *-----------*/

	private List<TransactionOperation> txn = Collections.synchronizedList(new ArrayList<TransactionOperation>());

	private final SesameSession client;

	private boolean active;

	private Model toAdd;

	private Model toRemove;

	/**
	 * Maximum size (in number of statements) allowed for statement buffers
	 * before they are forcibly flushed. TODO: make this setting configurable.
	 */
	private static final long MAX_STATEMENT_BUFFER_SIZE = 200000;

	/*--------------*
	 * Constructors *
	 *--------------*/

	public HTTPRepositoryConnection(HTTPRepository repository, SesameSession client) {
		super(repository);

		this.client = client;

		// parser used for locally processing input data to be sent to the server
		// should be strict, and should preserve bnode ids.
		setParserConfig(new ParserConfig());
		getParserConfig().set(BasicParserSettings.VERIFY_DATATYPE_VALUES, true);
		getParserConfig().set(BasicParserSettings.PRESERVE_BNODE_IDS, true);
	}

	/*---------*
	 * Methods *
	 *---------*/

	public HttpClient getHttpClient() {
		return client.getHttpClient();
	}

	public void setHttpClient(HttpClient httpClient) {
		client.setHttpClient(httpClient);
	}

	@Override
	public void setParserConfig(ParserConfig parserConfig) {
		super.setParserConfig(parserConfig);
	}

	@Override
	public HTTPRepository getRepository() {
		return (HTTPRepository)super.getRepository();
	}

	public void begin()
		throws RepositoryException
	{
		verifyIsOpen();
		verifyNotTxnActive("Connection already has an active transaction");

		if (this.getRepository().useCompatibleMode()) {
			active = true;
			return;
		}

		try {
			client.beginTransaction(this.getIsolationLevel());
			active = true;
		}
		catch (RepositoryException e) {
			throw e;
		}
		catch (OpenRDFException e) {
			throw new RepositoryException(e);
		}
		catch (IllegalStateException e) {
			throw new RepositoryException(e);
		}
		catch (IOException e) {
			throw new RepositoryException(e);
		}
	}

	/**
	 * Prepares a {@Link Query} for evaluation on this repository. Note
	 * that the preferred way of preparing queries is to use the more specific
	 * {@link #prepareTupleQuery(QueryLanguage, String, String)},
	 * {@link #prepareBooleanQuery(QueryLanguage, String, String)}, or
	 * {@link #prepareGraphQuery(QueryLanguage, String, String)} methods instead.
	 * 
	 * @throws UnsupportedOperationException
	 *         if the method is not supported for the supplied query language.
	 */
	public Query prepareQuery(QueryLanguage ql, String queryString, String baseURI) {
		if (QueryLanguage.SPARQL.equals(ql)) {
			String strippedQuery = QueryParserUtil.removeSPARQLQueryProlog(queryString).toUpperCase();
			if (strippedQuery.startsWith("SELECT")) {
				return prepareTupleQuery(ql, queryString, baseURI);
			}
			else if (strippedQuery.startsWith("ASK")) {
				return prepareBooleanQuery(ql, queryString, baseURI);
			}
			else {
				return prepareGraphQuery(ql, queryString, baseURI);
			}
		}
		else if (QueryLanguage.SERQL.equals(ql)) {
			String strippedQuery = queryString;

			// remove all opening brackets
			strippedQuery = strippedQuery.replace('(', ' ');
			strippedQuery = strippedQuery.trim();

			if (strippedQuery.toUpperCase().startsWith("SELECT")) {
				return prepareTupleQuery(ql, queryString, baseURI);
			}
			else {
				return prepareGraphQuery(ql, queryString, baseURI);
			}
		}
		else {
			throw new UnsupportedOperationException("Operation not supported for query language " + ql);
		}
	}

	public TupleQuery prepareTupleQuery(QueryLanguage ql, String queryString, String baseURI) {
		return new HTTPTupleQuery(this, ql, queryString, baseURI);
	}

	public GraphQuery prepareGraphQuery(QueryLanguage ql, String queryString, String baseURI) {
		return new HTTPGraphQuery(this, ql, queryString, baseURI);
	}

	public BooleanQuery prepareBooleanQuery(QueryLanguage ql, String queryString, String baseURI) {
		return new HTTPBooleanQuery(this, ql, queryString, baseURI);
	}

	public RepositoryResult<Resource> getContextIDs()
		throws RepositoryException
	{
		try {
			List<Resource> contextList = new ArrayList<Resource>();

			TupleQueryResult contextIDs = client.getContextIDs();
			try {
				while (contextIDs.hasNext()) {
					BindingSet bindingSet = contextIDs.next();
					Value context = bindingSet.getValue("contextID");

					if (context instanceof Resource) {
						contextList.add((Resource)context);
					}
				}
			}
			finally {
				contextIDs.close();
			}

			return createRepositoryResult(contextList);
		}
		catch (QueryEvaluationException e) {
			throw new RepositoryException(e);
		}
		catch (IOException e) {
			throw new RepositoryException(e);
		}
	}

	public RepositoryResult<Statement> getStatements(Resource subj, URI pred, Value obj,
			boolean includeInferred, Resource... contexts)
		throws RepositoryException
	{
		try {
			StatementCollector collector = new StatementCollector();
			exportStatements(subj, pred, obj, includeInferred, collector, contexts);
			return createRepositoryResult(collector.getStatements());
		}
		catch (RDFHandlerException e) {
			// found a bug in StatementCollector?
			throw new RuntimeException(e);
		}
	}

	public void exportStatements(Resource subj, URI pred, Value obj, boolean includeInferred,
			RDFHandler handler, Resource... contexts)
		throws RDFHandlerException, RepositoryException
	{
		flushTransactionState(Action.GET);
		try {
			client.getStatements(subj, pred, obj, includeInferred, handler, contexts);
		}
		catch (IOException e) {
			throw new RepositoryException(e);
		}
		catch (QueryInterruptedException e) {
			throw new RepositoryException(e);
		}
	}

	public long size(Resource... contexts)
		throws RepositoryException
	{
		flushTransactionState(Action.SIZE);
		try {
			return client.size(contexts);
		}
		catch (IOException e) {
			throw new RepositoryException(e);
		}
	}

	public void commit()
		throws RepositoryException
	{

		if (this.getRepository().useCompatibleMode()) {
			synchronized (txn) {
				if (txn.size() > 0) {
					try {
						client.sendTransaction(txn);
						txn.clear();
					}
					catch (IOException e) {
						throw new RepositoryException(e);
					}
				}
				active = false;
			}
			return;
		}

		flushTransactionState(Action.COMMIT);
		try {
			client.commitTransaction();
			active = false;
		}
		catch (OpenRDFException e) {
			throw new RepositoryException(e);
		}
		catch (IllegalStateException e) {
			throw new RepositoryException(e);
		}
		catch (IOException e) {
			throw new RepositoryException(e);
		}
	}

	public void rollback()
		throws RepositoryException
	{
		if (this.getRepository().useCompatibleMode()) {
			txn.clear();
			active = false;
			return;
		}

		flushTransactionState(Action.ROLLBACK);
		try {
			client.rollbackTransaction();
			active = false;
		}
		catch (OpenRDFException e) {
			throw new RepositoryException(e);
		}
		catch (IllegalStateException e) {
			throw new RepositoryException(e);
		}
		catch (IOException e) {
			throw new RepositoryException(e);
		}
	}

	@Override
	public void close()
		throws RepositoryException
	{
		if (isActive()) {
			logger.warn("Rolling back transaction due to connection close", new Throwable());
			rollback();
		}

		super.close();
	}

	public void add(File file, String baseURI, RDFFormat dataFormat, Resource... contexts)
		throws IOException, RDFParseException, RepositoryException
	{
		if (baseURI == null) {
			// default baseURI to file
			baseURI = file.toURI().toString();
		}
		if (dataFormat == null) {
			dataFormat = Rio.getParserFormatForFileName(file.getName());
		}

		InputStream in = new FileInputStream(file);
		try {
			add(in, baseURI, dataFormat, contexts);
		}
		finally {
			in.close();
		}
	}

	public void add(URL url, String baseURI, RDFFormat dataFormat, Resource... contexts)
		throws IOException, RDFParseException, RepositoryException
	{
		if (baseURI == null) {
			baseURI = url.toExternalForm();
		}

		URLConnection con = url.openConnection();

		// Set appropriate Accept headers
		if (dataFormat != null) {
			for (String mimeType : dataFormat.getMIMETypes()) {
				con.addRequestProperty("Accept", mimeType);
			}
		}
		else {
			Set<RDFFormat> rdfFormats = RDFParserRegistry.getInstance().getKeys();
			List<String> acceptParams = RDFFormat.getAcceptParams(rdfFormats, true, null);
			for (String acceptParam : acceptParams) {
				con.addRequestProperty("Accept", acceptParam);
			}
		}

		InputStream in = con.getInputStream();

		if (dataFormat == null) {
			// Try to determine the data's MIME type
			String mimeType = con.getContentType();
			int semiColonIdx = mimeType.indexOf(';');
			if (semiColonIdx >= 0) {
				mimeType = mimeType.substring(0, semiColonIdx);
			}
			dataFormat = Rio.getParserFormatForMIMEType(mimeType);

			// Fall back to using file name extensions
			if (dataFormat == null) {
				dataFormat = Rio.getParserFormatForFileName(url.getPath());
			}
		}

		try {
			add(in, baseURI, dataFormat, contexts);
		}
		finally {
			in.close();
		}
	}

	public void add(InputStream in, String baseURI, RDFFormat dataFormat, Resource... contexts)
		throws IOException, RDFParseException, RepositoryException
	{
		if (this.getRepository().useCompatibleMode()) {
		
			dataFormat = getBackwardCompatibleFormat(dataFormat);
			
			if (!isActive()) {
				// Send bytes directly to the server
				client.upload(in, baseURI, dataFormat, false, false, contexts);
			}
			else {
				// Parse files locally
				super.add(in, baseURI, dataFormat, contexts);

			}
			return;
		}

		flushTransactionState(Action.ADD);
		// Send bytes directly to the server
		client.upload(in, baseURI, dataFormat, false, false, contexts);
	}

	private RDFFormat getBackwardCompatibleFormat(RDFFormat format) {
		// In Sesame 2.8, the default MIME-type for N-Triples changed. To stay backward compatible, we 'fake' the 
		// default MIME-type back to the older value (text/plain) when running in compatibility mode.  
		if (NTRIPLES.equals(format)) {
			// create a new format constant with identical properties as the N-Triples format, just with a different
			// default MIME-type.
			return new RDFFormat(NTRIPLES.getName(), Arrays.asList("text/plain"), NTRIPLES.getCharset(),
					NTRIPLES.getFileExtensions(), NTRIPLES.supportsNamespaces(), NTRIPLES.supportsContexts());
		}
		
		return format;
	}
	
	public void add(Reader reader, String baseURI, RDFFormat dataFormat, Resource... contexts)
		throws IOException, RDFParseException, RepositoryException
	{

		if (this.getRepository().useCompatibleMode()) {

			dataFormat = getBackwardCompatibleFormat(dataFormat);
			
			if (!isActive()) {
				// Send bytes directly to the server
				client.upload(reader, baseURI, dataFormat, false, false, contexts);
			}
			else {
				// Parse files locally
				super.add(reader, baseURI, dataFormat, contexts);

			}
			return;
		}

		flushTransactionState(Action.ADD);
		client.upload(reader, baseURI, dataFormat, false, false, contexts);
	}

	@Override
	public void add(Statement st, Resource... contexts)
		throws RepositoryException
	{
		if (!isActive()) {
			// operation is not part of a transaction - just send directly
			OpenRDFUtil.verifyContextNotNull(contexts);

			final Model m = new LinkedHashModel();

			if (contexts.length == 0) {
				// if no context is specified in the method call, statement's own
				// context (if any) is used.
				m.add(st.getSubject(), st.getPredicate(), st.getObject(), st.getContext());
			}
			else {
				m.add(st.getSubject(), st.getPredicate(), st.getObject(), contexts);
			}
			addModel(m);
		}
		else {
			super.add(st, contexts);
		}
	}

	@Override
	public void add(Resource subject, URI predicate, Value object, Resource... contexts)
		throws RepositoryException
	{
		if (!isActive()) {
			logger.debug("adding statement directly: {} {} {} {}", new Object[] {
					subject,
					predicate,
					object,
					contexts });
			// operation is not part of a transaction - just send directly
			OpenRDFUtil.verifyContextNotNull(contexts);
			final Model m = new LinkedHashModel();
			m.add(subject, predicate, object, contexts);
			addModel(m);
		}
		else {
			logger.debug("adding statement in txn: {} {} {} {}", new Object[] {
					subject,
					predicate,
					object,
					contexts });
			super.add(subject, predicate, object, contexts);
		}
	}

	/*
	@Override
	public void remove(Resource subject, URI predicate, Value object, Resource... contexts)
		throws RepositoryException
	{
		if (!isActive()) {
			// operation is not part of a transaction - just send directly
			OpenRDFUtil.verifyContextNotNull(contexts);
			if (subject == null) {
				subject = SESAME.WILDCARD;
			}
			if (predicate == null) {
				predicate = SESAME.WILDCARD;
			}
			if (object == null) {
				object = SESAME.WILDCARD;
			}
			final Model m = new LinkedHashModel();
			m.add(subject, predicate, object, contexts);
			removeModel(m);
		}
		else {
			super.remove(subject, predicate, object, contexts);
		}
	}
	*/

	@Override
	protected void addWithoutCommit(Resource subject, URI predicate, Value object, Resource... contexts)
		throws RepositoryException
	{
		if (this.getRepository().useCompatibleMode()) {
			txn.add(new AddStatementOperation(subject, predicate, object, contexts));
			return;
		}

		flushTransactionState(Protocol.Action.ADD);

		if (toAdd == null) {
			toAdd = new LinkedHashModel();
		}
		toAdd.add(subject, predicate, object, contexts);
	}

	private void addModel(Model m)
		throws RepositoryException
	{
		// TODO we should dynamically pick a format from the available writers
		// perhaps?
		RDFFormat format = RDFFormat.BINARY;
		try {
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			Rio.write(m, out, format);
			ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
			client.addData(in, null, format);

		}
		catch (RDFHandlerException e) {
			throw new RepositoryException("error while writing statement", e);
		}
		catch (RDFParseException e) {
			throw new RepositoryException(e);
		}
		catch (IOException e) {
			throw new RepositoryException(e);
		}
	}

	private void removeModel(Model m)
		throws RepositoryException
	{
		RDFFormat format = RDFFormat.BINARY;
		try {
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			Rio.write(m, out, format);
			ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
			client.removeData(in, null, format);

		}
		catch (RDFHandlerException e) {
			throw new RepositoryException("error while writing statement", e);
		}
		catch (RDFParseException e) {
			throw new RepositoryException(e);
		}
		catch (IOException e) {
			throw new RepositoryException(e);
		}
	}

	protected void flushTransactionState(Action action)
		throws RepositoryException
	{
		if (this.getRepository().useCompatibleMode()) {
			// no need to flush, using old-style transactions.
			return;
		}

		if (isActive()) {
			switch (action) {
				case ADD:
					if (toRemove != null) {
						removeModel(toRemove);
						toRemove = null;
					}
					if (toAdd != null && MAX_STATEMENT_BUFFER_SIZE <= toAdd.size()) {
						addModel(toAdd);
						toAdd = null;
					}
					break;
				case DELETE:
					if (toAdd != null) {
						addModel(toAdd);
						toAdd = null;
					}
					if (toRemove != null && MAX_STATEMENT_BUFFER_SIZE <= toRemove.size()) {
						removeModel(toRemove);
						toRemove = null;
					}
					break;
				case GET:
				case UPDATE:
				case COMMIT:
				case QUERY:
				case SIZE:
					if (toAdd != null) {
						addModel(toAdd);
						toAdd = null;
					}
					if (toRemove != null) {
						removeModel(toRemove);
						toRemove = null;
					}
					break;
				case ROLLBACK:
					toAdd = null;
					toRemove = null;
					break;

			}
		}
	}

	@Override
	protected void removeWithoutCommit(Resource subject, URI predicate, Value object, Resource... contexts)
		throws RepositoryException
	{
		if (this.getRepository().useCompatibleMode()) {
			txn.add(new RemoveStatementsOperation(subject, predicate, object, contexts));
			return;
		}

		flushTransactionState(Protocol.Action.DELETE);

		if (toRemove == null) {
			toRemove = new LinkedHashModel();
		}
		if (subject == null) {
			subject = SESAME.WILDCARD;
		}
		if (predicate == null) {
			predicate = SESAME.WILDCARD;
		}
		if (object == null) {
			object = SESAME.WILDCARD;
		}
		toRemove.add(subject, predicate, object, contexts);
	}

	@Override
	public void clear(Resource... contexts)
		throws RepositoryException
	{
		boolean localTransaction = startLocalTransaction();

		if (this.getRepository().useCompatibleMode()) {
			txn.add(new ClearOperation(contexts));
		}
		else {
			remove(null, null, null, contexts);
		}

		conditionalCommit(localTransaction);
	}

	public void removeNamespace(String prefix)
		throws RepositoryException
	{
		if (prefix == null) {
			throw new NullPointerException("prefix must not be null");
		}

		boolean localTransaction = startLocalTransaction();

		try {
			if (this.getRepository().useCompatibleMode()) {
				txn.add(new RemoveNamespaceOperation(prefix));
			}
			else {
				client.removeNamespacePrefix(prefix);
			}
			conditionalCommit(localTransaction);
		}
		catch (IOException e) {
			// TODO if rollback throws an exception too, the original ioexception
			// is silently ignored. Should we throw the rollback exception or the
			// original exception (and/or should we log one of the exceptions?)
			conditionalRollback(localTransaction);
			throw new RepositoryException(e);
		}

	}

	public void clearNamespaces()
		throws RepositoryException
	{
		if (this.getRepository().useCompatibleMode()) {
			boolean localTransaction = startLocalTransaction();
			txn.add(new ClearNamespacesOperation());
			conditionalCommit(localTransaction);
			return;
		}

		try {
			client.clearNamespaces();
		}
		catch (IOException e) {
			throw new RepositoryException(e);
		}
	}

	public void setNamespace(String prefix, String name)
		throws RepositoryException
	{
		if (prefix == null) {
			throw new NullPointerException("prefix must not be null");
		}
		if (name == null) {
			throw new NullPointerException("name must not be null");
		}

		if (this.getRepository().useCompatibleMode()) {
			boolean localTransaction = startLocalTransaction();
			txn.add(new SetNamespaceOperation(prefix, name));
			conditionalCommit(localTransaction);
			return;
		}

		try {
			client.setNamespacePrefix(prefix, name);
		}
		catch (IOException e) {
			throw new RepositoryException(e);
		}
	}

	public RepositoryResult<Namespace> getNamespaces()
		throws RepositoryException
	{
		try {
			List<Namespace> namespaceList = new ArrayList<Namespace>();

			TupleQueryResult namespaces = client.getNamespaces();
			try {
				while (namespaces.hasNext()) {
					BindingSet bindingSet = namespaces.next();
					Value prefix = bindingSet.getValue("prefix");
					Value namespace = bindingSet.getValue("namespace");

					if (prefix instanceof Literal && namespace instanceof Literal) {
						String prefixStr = ((Literal)prefix).getLabel();
						String namespaceStr = ((Literal)namespace).getLabel();
						namespaceList.add(new NamespaceImpl(prefixStr, namespaceStr));
					}
				}
			}
			finally {
				namespaces.close();
			}

			return createRepositoryResult(namespaceList);
		}
		catch (QueryEvaluationException e) {
			throw new RepositoryException(e);
		}
		catch (IOException e) {
			throw new RepositoryException(e);
		}
	}

	public String getNamespace(String prefix)
		throws RepositoryException
	{
		if (prefix == null) {
			throw new NullPointerException("prefix must not be null");
		}
		try {
			return client.getNamespace(prefix);
		}
		catch (IOException e) {
			throw new RepositoryException(e);
		}
	}

	protected void scheduleUpdate(HTTPUpdate update) {
		SPARQLUpdateOperation op = new SPARQLUpdateOperation();
		op.setUpdateString(update.getQueryString());
		op.setBaseURI(update.getBaseURI());
		op.setBindings(update.getBindingsArray());
		op.setIncludeInferred(update.getIncludeInferred());
		op.setDataset(update.getDataset());
		txn.add(op);
	}

	/**
	 * Creates a RepositoryResult for the supplied element set.
	 */
	protected <E> RepositoryResult<E> createRepositoryResult(Iterable<? extends E> elements) {
		return new RepositoryResult<E>(new CloseableIteratorIteration<E, RepositoryException>(
				elements.iterator()));
	}

	public Update prepareUpdate(QueryLanguage ql, String update, String baseURI)
		throws RepositoryException, MalformedQueryException
	{
		return new HTTPUpdate(this, ql, update, baseURI);
	}

	/**
	 * Verifies that the connection is open, throws a {@link StoreException} if
	 * it isn't.
	 */
	protected void verifyIsOpen()
		throws RepositoryException
	{
		if (!isOpen()) {
			throw new RepositoryException("Connection has been closed");
		}
	}

	/**
	 * Verifies that the connection has an active transaction, throws a
	 * {@link StoreException} if it hasn't.
	 */
	protected void verifyTxnActive()
		throws RepositoryException
	{
		if (!isActive()) {
			throw new RepositoryException("Connection does not have an active transaction");
		}
	}

	/**
	 * Verifies that the connection does not have an active transaction, throws a
	 * {@link RepositoryException} if it has.
	 */
	protected void verifyNotTxnActive(String msg)
		throws RepositoryException
	{
		if (isActive()) {
			throw new RepositoryException(msg);
		}
	}

	public boolean isActive()
		throws UnknownTransactionStateException, RepositoryException
	{
		return active;
	}

	/**
	 * @return
	 */
	protected SesameSession getSesameSession() {
		return client;
	}
}
