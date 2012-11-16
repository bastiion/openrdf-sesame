/*
 * Copyright Aduna (http://www.aduna-software.com/) (c) 1997-2007.
 *
 * Licensed under the Aduna BSD-style license.
 */
package org.openrdf.query.parser.sparql;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.openrdf.model.impl.ValueFactoryImpl;
import org.openrdf.query.Dataset;
import org.openrdf.query.IncompatibleOperationException;
import org.openrdf.query.MalformedQueryException;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.algebra.TupleExpr;
import org.openrdf.query.algebra.UpdateExpr;
import org.openrdf.query.parser.ParsedBooleanQuery;
import org.openrdf.query.parser.ParsedGraphQuery;
import org.openrdf.query.parser.ParsedOperation;
import org.openrdf.query.parser.ParsedQuery;
import org.openrdf.query.parser.ParsedTupleQuery;
import org.openrdf.query.parser.ParsedUpdate;
import org.openrdf.query.parser.QueryParser;
import org.openrdf.query.parser.QueryParserUtil;
import org.openrdf.query.parser.sparql.ast.ASTAskQuery;
import org.openrdf.query.parser.sparql.ast.ASTConstructQuery;
import org.openrdf.query.parser.sparql.ast.ASTDescribeQuery;
import org.openrdf.query.parser.sparql.ast.ASTPrefixDecl;
import org.openrdf.query.parser.sparql.ast.ASTQuery;
import org.openrdf.query.parser.sparql.ast.ASTQueryContainer;
import org.openrdf.query.parser.sparql.ast.ASTSelectQuery;
import org.openrdf.query.parser.sparql.ast.ASTUpdate;
import org.openrdf.query.parser.sparql.ast.ASTUpdateContainer;
import org.openrdf.query.parser.sparql.ast.ASTUpdateSequence;
import org.openrdf.query.parser.sparql.ast.Node;
import org.openrdf.query.parser.sparql.ast.ParseException;
import org.openrdf.query.parser.sparql.ast.SyntaxTreeBuilder;
import org.openrdf.query.parser.sparql.ast.TokenMgrError;
import org.openrdf.query.parser.sparql.ast.VisitorException;

public class SPARQLParser implements QueryParser {
	private static final Logger logger = LoggerFactory.getLogger(SPARQLParser.class);

	public ParsedUpdate parseUpdate(String updateStr, String baseURI)
		throws MalformedQueryException
	{
		try {

			ParsedUpdate update = new ParsedUpdate();

			ASTUpdateSequence updateSequence = SyntaxTreeBuilder.parseUpdateSequence(updateStr);

			List<ASTUpdateContainer> updateOperations = updateSequence.getUpdateContainers();

			List<ASTPrefixDecl> sharedPrefixDeclarations = null;

			Node node = updateSequence.jjtGetChild(0);
			if (node instanceof ASTPrefixDecl) {

			}

			for (int i = 0; i < updateOperations.size(); i++) {

				ASTUpdateContainer uc = updateOperations.get(i);

				if (uc.jjtGetNumChildren() == 0 && i > 0 && i < updateOperations.size() - 1) {
					// empty update in the middle of the sequence
					throw new MalformedQueryException("empty update in sequence not allowed");
				}

				StringEscapesProcessor.process(uc);
				BaseDeclProcessor.process(uc, baseURI);

				// do a special dance to handle prefix declarations in sequences: if
				// the current
				// operation has its own prefix declarations, use those. Otherwise,
				// try and use
				// prefix declarations from a previous operation in this sequence.
				List<ASTPrefixDecl> prefixDeclList = uc.getPrefixDeclList();
				if (prefixDeclList == null || prefixDeclList.size() == 0) {
					if (sharedPrefixDeclarations != null) {
						for (ASTPrefixDecl prefixDecl : sharedPrefixDeclarations) {
							uc.jjtAppendChild(prefixDecl);
						}
					}
				}
				else {
					sharedPrefixDeclarations = prefixDeclList;
				}

				PrefixDeclProcessor.process(uc);
				BlankNodeVarProcessor.process(uc);

				UpdateExprBuilder updateExprBuilder = new UpdateExprBuilder(new ValueFactoryImpl());

				ASTUpdate updateNode = uc.getUpdate();
				if (updateNode != null) {
					UpdateExpr updateExpr = (UpdateExpr)updateNode.jjtAccept(updateExprBuilder, null);
					
					// add individual update expression to ParsedUpdate sequence container
					update.addUpdateExpr(updateExpr);

					// associate updateExpr with the correct dataset (if any)
					Dataset dataset = DatasetDeclProcessor.process(uc);
					update.map(updateExpr, dataset);
				}
			} // end for

			return update;
		}
		catch (ParseException e) {
			throw new MalformedQueryException(e.getMessage(), e);
		}
		catch (TokenMgrError e) {
			throw new MalformedQueryException(e.getMessage(), e);
		}
		catch (VisitorException e) {
			throw new MalformedQueryException(e.getMessage(), e);
		}

	}

	public ParsedQuery parseQuery(String queryStr, String baseURI)
		throws MalformedQueryException
	{
		try {
			ASTQueryContainer qc = SyntaxTreeBuilder.parseQuery(queryStr);
			StringEscapesProcessor.process(qc);
			BaseDeclProcessor.process(qc, baseURI);
			Map<String, String> prefixes = PrefixDeclProcessor.process(qc);
			WildcardProjectionProcessor.process(qc);
			BlankNodeVarProcessor.process(qc);

			if (qc.containsQuery()) {

				// handle query operation

				TupleExpr tupleExpr = buildQueryModel(qc);

				ParsedQuery query;

				ASTQuery queryNode = qc.getQuery();
				if (queryNode instanceof ASTSelectQuery) {
					query = new ParsedTupleQuery(queryStr, tupleExpr);
				}
				else if (queryNode instanceof ASTConstructQuery) {
					query = new ParsedGraphQuery(queryStr, tupleExpr, prefixes);
				}
				else if (queryNode instanceof ASTAskQuery) {
					query = new ParsedBooleanQuery(queryStr, tupleExpr);
				}
				else if (queryNode instanceof ASTDescribeQuery) {
					query = new ParsedGraphQuery(queryStr, tupleExpr, prefixes);
				}
				else {
					throw new RuntimeException("Unexpected query type: " + queryNode.getClass());
				}

				// Handle dataset declaration
				Dataset dataset = DatasetDeclProcessor.process(qc);
				if (dataset != null) {
					query.setDataset(dataset);
				}

				return query;
			}
			else {
				throw new IncompatibleOperationException("supplied string is not a query operation");
			}
		}
		catch (ParseException e) {
			throw new MalformedQueryException(e.getMessage(), e);
		}
		catch (TokenMgrError e) {
			throw new MalformedQueryException(e.getMessage(), e);
		}
	}

	private TupleExpr buildQueryModel(Node qc)
		throws MalformedQueryException
	{
		TupleExprBuilder tupleExprBuilder = new TupleExprBuilder(new ValueFactoryImpl());
		try {
			return (TupleExpr)qc.jjtAccept(tupleExprBuilder, null);
		}
		catch (VisitorException e) {
			throw new MalformedQueryException(e.getMessage(), e);
		}
	}

	public static void main(String[] args)
		throws java.io.IOException
	{
		logger.info("Your SPARQL query:");

		BufferedReader in = new BufferedReader(new InputStreamReader(System.in));

		StringBuilder buf = new StringBuilder();
		String line = null;
		while ((line = in.readLine()) != null) {
			if (line.length() > 0) {
				buf.append(' ').append(line).append('\n');
			}
			else {
				String queryStr = buf.toString().trim();
				if (queryStr.length() > 0) {
					try {
						ParsedOperation parsedQuery = QueryParserUtil.parseOperation(QueryLanguage.SPARQL,
								queryStr, null);

						logger.info("Parsed query: ");
						logger.info(parsedQuery.toString());
					}
					catch (Exception e) {
						logger.error(e.getMessage(), e);
					}
				}
				buf.setLength(0);
			}
		}
	}
}
