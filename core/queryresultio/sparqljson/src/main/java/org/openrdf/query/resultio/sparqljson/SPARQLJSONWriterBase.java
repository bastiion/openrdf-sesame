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
package org.openrdf.query.resultio.sparqljson;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;

import org.openrdf.model.BNode;
import org.openrdf.model.Literal;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.util.Literals;
import org.openrdf.model.vocabulary.XMLSchema;
import org.openrdf.query.Binding;
import org.openrdf.query.BindingSet;
import org.openrdf.query.QueryResultHandlerException;
import org.openrdf.query.TupleQueryResultHandlerException;
import org.openrdf.query.resultio.BasicQueryWriterSettings;
import org.openrdf.query.resultio.QueryResultWriter;
import org.openrdf.query.resultio.QueryResultWriterBase;
import org.openrdf.rio.RioSetting;
import org.openrdf.rio.helpers.BasicWriterSettings;

/**
 * An abstract class to implement the base functionality for both
 * SPARQLBooleanJSONWriter and SPARQLResultsJSONWriter.
 * 
 * @author Peter Ansell
 */
abstract class SPARQLJSONWriterBase extends QueryResultWriterBase implements QueryResultWriter {

	private static final JsonFactory JSON_FACTORY = new JsonFactory();

	static {
		// Disable features that may work for most JSON where the field names are
		// in limited supply,
		// but does not work for RDF/JSON where a wide range of URIs are used for
		// subjects and
		// predicates
		JSON_FACTORY.disable(JsonFactory.Feature.INTERN_FIELD_NAMES);
		JSON_FACTORY.disable(JsonFactory.Feature.CANONICALIZE_FIELD_NAMES);
		JSON_FACTORY.disable(JsonGenerator.Feature.AUTO_CLOSE_TARGET);
	}

	/*-----------*
	 * Variables *
	 *-----------*/

	protected boolean firstTupleWritten = false;

	protected boolean documentOpen = false;

	protected boolean headerOpen = false;

	protected boolean headerComplete = false;

	protected boolean tupleVariablesFound = false;

	protected boolean linksFound = false;

	private final JsonGenerator jg;

	public SPARQLJSONWriterBase(OutputStream out) {
		try {
			jg = JSON_FACTORY.createJsonGenerator(new OutputStreamWriter(out, Charset.forName("UTF-8")));
		}
		catch (IOException e) {
			throw new IllegalArgumentException(e);
		}
	}

	@Override
	public void endHeader()
		throws QueryResultHandlerException
	{
		if (!headerComplete) {
			try {
				jg.writeEndObject();

				if (tupleVariablesFound) {
					// Write results
					jg.writeObjectFieldStart("results");

					jg.writeArrayFieldStart("bindings");
				}

				headerComplete = true;
			}
			catch (IOException e) {
				throw new QueryResultHandlerException(e);
			}
		}
	}

	@Override
	public void startQueryResult(List<String> columnHeaders)
		throws TupleQueryResultHandlerException
	{
		try {
			if (!documentOpen) {
				startDocument();
			}

			if (!headerOpen) {
				startHeader();
			}

			tupleVariablesFound = true;
			jg.writeArrayFieldStart("vars");
			for (String nextColumn : columnHeaders) {
				jg.writeString(nextColumn);
			}
			jg.writeEndArray();
		}
		catch (IOException e) {
			throw new TupleQueryResultHandlerException(e);
		}
		catch (TupleQueryResultHandlerException e) {
			throw e;
		}
		catch (QueryResultHandlerException e) {
			throw new TupleQueryResultHandlerException(e);
		}
	}

	@Override
	public void handleSolution(BindingSet bindingSet)
		throws TupleQueryResultHandlerException
	{
		try {
			if (!documentOpen) {
				startDocument();
			}

			if (!headerOpen) {
				startHeader();
			}

			if (!headerComplete) {
				endHeader();
			}

			if (!tupleVariablesFound) {
				throw new IllegalStateException("Must call startQueryResult before handleSolution");
			}

			firstTupleWritten = true;

			jg.writeStartObject();

			Iterator<Binding> bindingIter = bindingSet.iterator();
			while (bindingIter.hasNext()) {
				Binding binding = bindingIter.next();
				jg.writeFieldName(binding.getName());
				writeValue(binding.getValue());
			}

			jg.writeEndObject();
		}
		catch (IOException e) {
			throw new TupleQueryResultHandlerException(e);
		}
		catch (TupleQueryResultHandlerException e) {
			throw e;
		}
		catch (QueryResultHandlerException e) {
			throw new TupleQueryResultHandlerException(e);
		}
	}

	@Override
	public void endQueryResult()
		throws TupleQueryResultHandlerException
	{
		try {
			if (!documentOpen) {
				startDocument();
			}

			if (!headerOpen) {
				startHeader();
			}

			if (!headerComplete) {
				endHeader();
			}

			if (!tupleVariablesFound) {
				throw new IllegalStateException(
						"Could not end query result as startQueryResult was not called first.");
			}

			// bindings array
			jg.writeEndArray();
			// results braces
			jg.writeEndObject();
			endDocument();
		}
		catch (IOException e) {
			throw new TupleQueryResultHandlerException(e);
		}
		catch (TupleQueryResultHandlerException e) {
			throw e;
		}
		catch (QueryResultHandlerException e) {
			throw new TupleQueryResultHandlerException(e);
		}
	}

	@Override
	public void startDocument()
		throws QueryResultHandlerException
	{
		if (!documentOpen) {
			documentOpen = true;
			headerOpen = false;
			headerComplete = false;
			tupleVariablesFound = false;
			firstTupleWritten = false;
			linksFound = false;

			if (getWriterConfig().get(BasicWriterSettings.PRETTY_PRINT)) {
				// SES-2011: Always use \n for consistency
				// By default Jackson does not pretty print, so enable this unless
				// PRETTY_PRINT setting is disabled
				DefaultPrettyPrinter pp = new DefaultPrettyPrinter().withArrayIndenter(DefaultIndenter.SYSTEM_LINEFEED_INSTANCE).withObjectIndenter(DefaultIndenter.SYSTEM_LINEFEED_INSTANCE);
				jg.setPrettyPrinter(pp);
			}

			try {
				if (getWriterConfig().isSet(BasicQueryWriterSettings.JSONP_CALLBACK)) {
					// SES-1019 : Write the callbackfunction name as a wrapper for
					// the results here
					String callbackName = getWriterConfig().get(BasicQueryWriterSettings.JSONP_CALLBACK);
					jg.writeRaw(callbackName);
					jg.writeRaw("(");
				}
				jg.writeStartObject();
			}
			catch (IOException e) {
				throw new QueryResultHandlerException(e);
			}
		}
	}

	@Override
	public void handleStylesheet(String stylesheetUrl)
		throws QueryResultHandlerException
	{
		// Ignore, as JSON does not support stylesheets
	}

	@Override
	public void startHeader()
		throws QueryResultHandlerException
	{
		if (!documentOpen) {
			startDocument();
		}

		if (!headerOpen) {
			try {
				// Write header
				jg.writeObjectFieldStart("head");

				headerOpen = true;
			}
			catch (IOException e) {
				throw new QueryResultHandlerException(e);
			}
		}
	}

	@Override
	public void handleLinks(List<String> linkUrls)
		throws QueryResultHandlerException
	{
		try {
			if (!documentOpen) {
				startDocument();
			}

			if (!headerOpen) {
				startHeader();
			}

			jg.writeArrayFieldStart("link");
			for (String nextLink : linkUrls) {
				jg.writeString(nextLink);
			}
			jg.writeEndArray();
		}
		catch (IOException e) {
			throw new QueryResultHandlerException(e);
		}
	}

	protected void writeValue(Value value)
		throws IOException, QueryResultHandlerException
	{
		jg.writeStartObject();

		if (value instanceof URI) {
			jg.writeStringField("type", "uri");
			jg.writeStringField("value", ((URI)value).toString());
		}
		else if (value instanceof BNode) {
			jg.writeStringField("type", "bnode");
			jg.writeStringField("value", ((BNode)value).getID());
		}
		else if (value instanceof Literal) {
			Literal lit = (Literal)value;

			if (Literals.isLanguageLiteral(lit)) {
				jg.writeObjectField("xml:lang", lit.getLanguage());
			}
			else {
				URI datatype = lit.getDatatype();
				boolean ignoreDatatype = datatype.equals(XMLSchema.STRING) && xsdStringToPlainLiteral();
				if (!ignoreDatatype) {
					jg.writeObjectField("datatype", lit.getDatatype().stringValue());
				}
			}

			jg.writeObjectField("type", "literal");

			jg.writeObjectField("value", lit.getLabel());
		}
		else {
			throw new TupleQueryResultHandlerException("Unknown Value object type: " + value.getClass());
		}
		jg.writeEndObject();
	}

	@Override
	public void handleBoolean(boolean value)
		throws QueryResultHandlerException
	{
		if (!documentOpen) {
			startDocument();
		}

		if (!headerOpen) {
			startHeader();
		}

		if (!headerComplete) {
			endHeader();
		}

		if (tupleVariablesFound) {
			throw new QueryResultHandlerException("Cannot call handleBoolean after startQueryResults");
		}

		try {
			if (value) {
				jg.writeBooleanField("boolean", Boolean.TRUE);
			}
			else {
				jg.writeBooleanField("boolean", Boolean.FALSE);
			}

			endDocument();
		}
		catch (IOException e) {
			throw new QueryResultHandlerException(e);
		}
	}

	@Override
	public final Collection<RioSetting<?>> getSupportedSettings() {
		Set<RioSetting<?>> result = new HashSet<RioSetting<?>>(super.getSupportedSettings());

		result.add(BasicQueryWriterSettings.JSONP_CALLBACK);
		result.add(BasicWriterSettings.PRETTY_PRINT);
		result.add(BasicWriterSettings.XSD_STRING_TO_PLAIN_LITERAL);

		return result;
	}

	@Override
	public void handleNamespace(String prefix, String uri)
		throws QueryResultHandlerException
	{
		// Ignored by SPARQLJSONWriterBase
	}

	protected void endDocument()
		throws IOException
	{
		jg.writeEndObject();
		if (getWriterConfig().isSet(BasicQueryWriterSettings.JSONP_CALLBACK)) {
			jg.writeRaw(");");
		}
		jg.flush();
		documentOpen = false;
		headerOpen = false;
		headerComplete = false;
		tupleVariablesFound = false;
		firstTupleWritten = false;
		linksFound = false;
	}

}
