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
package org.openrdf.query.resultio;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.openrdf.model.URI;
import org.openrdf.model.impl.ValueFactoryImpl;

/**
 * Represents the concept of a boolean query result serialization format.
 * Boolean query result formats are identified by a {@link #getName() name} and
 * can have one or more associated MIME types, zero or more associated file
 * extensions and can specify a (default) character encoding.
 * 
 * @author Arjohn Kampman
 */
public class BooleanQueryResultFormat extends QueryResultFormat {

	/*-----------*
	 * Constants *
	 *-----------*/

	/**
	 * SPARQL Query Results XML Format.
	 */
	public static final BooleanQueryResultFormat SPARQL = new BooleanQueryResultFormat("SPARQL/XML",
			Arrays.asList("application/sparql-results+xml", "application/xml"), Charset.forName("UTF-8"),
			Arrays.asList("srx", "xml"), SPARQL_RESULTS_XML_URI);

	/**
	 * SPARQL Query Results JSON Format.
	 */
	public static final BooleanQueryResultFormat JSON = new BooleanQueryResultFormat("SPARQL/JSON",
			Arrays.asList("application/sparql-results+json", "application/json"), Charset.forName("UTF-8"),
			Arrays.asList("srj", "json"), SPARQL_RESULTS_JSON_URI);

	/**
	 * Plain text encoding using values "true" and "false" (case-insensitive).
	 */
	public static final BooleanQueryResultFormat TEXT = new BooleanQueryResultFormat("TEXT", "text/boolean",
			Charset.forName("US-ASCII"), "txt");

	/*------------------*
	 * Static variables *
	 *------------------*/

	/**
	 * List of known boolean query result formats.
	 */
	private static List<BooleanQueryResultFormat> VALUES = new ArrayList<BooleanQueryResultFormat>(8);

	/*--------------------*
	 * Static initializer *
	 *--------------------*/

	static {
		register(TEXT);
		register(JSON);
		register(SPARQL);
	}

	/*----------------*
	 * Static methods *
	 *----------------*/

	/**
	 * Returns all known/registered boolean query result formats.
	 */
	public static Collection<BooleanQueryResultFormat> values() {
		return Collections.unmodifiableList(VALUES);
	}

	/**
	 * Registers the specified boolean query result format.
	 * 
	 * @param name
	 *        The name of the format, e.g. "SPARQL/XML".
	 * @param mimeType
	 *        The MIME type of the format, e.g.
	 *        <tt>application/sparql-results+xml</tt> for the SPARQL/XML file
	 *        format.
	 * @param fileExt
	 *        The (default) file extension for the format, e.g. <tt>srx</tt> for
	 *        SPARQL/XML files.
	 */
	public static BooleanQueryResultFormat register(String name, String mimeType, String fileExt) {
		BooleanQueryResultFormat format = new BooleanQueryResultFormat(name, mimeType, fileExt);
		register(format);
		return format;
	}

	/**
	 * Registers the specified boolean query result format.
	 */
	public static void register(BooleanQueryResultFormat format) {
		VALUES.add(format);
	}

	/**
	 * Tries to determine the appropriate boolean file format based on the a MIME
	 * type that describes the content type.
	 * <p>
	 * NOTE: This method may not take into account dynamically loaded formats.
	 * Use {@link QueryResultIO#getBooleanParserFormatForMIMEType(String)} and
	 * {@link QueryResultIO#getBooleanWriterFormatForMIMEType(String)} to find
	 * all dynamically loaded parser and writer formats, respectively.
	 * 
	 * @param mimeType
	 *        A MIME type, e.g. "application/sparql-results+xml".
	 * @return A TupleQueryResultFormat object if the MIME type was recognized,
	 *         or <tt>null</tt> otherwise.
	 * @see #forMIMEType(String,BooleanQueryResultFormat)
	 * @see #getMIMETypes
	 */
	public static BooleanQueryResultFormat forMIMEType(String mimeType) {
		return forMIMEType(mimeType, null);
	}

	/**
	 * Tries to determine the appropriate boolean file format based on the a MIME
	 * type that describes the content type. The supplied fallback format will be
	 * returned when the MIME type was not recognized.
	 * <p>
	 * NOTE: This method may not take into account dynamically loaded formats.
	 * Use
	 * {@link QueryResultIO#getBooleanParserFormatForMIMEType(String, BooleanQueryResultFormat)}
	 * and
	 * {@link QueryResultIO#getBooleanWriterFormatForMIMEType(String, BooleanQueryResultFormat)}
	 * to find all dynamically loaded parser and writer formats, respectively.
	 * 
	 * @param mimeType
	 *        a MIME type, e.g. "application/sparql-results+xml"
	 * @param fallback
	 *        a fallback TupleQueryResultFormat that will be returned by the
	 *        method if no match for the supplied MIME type can be found.
	 * @return A TupleQueryResultFormat that matches the MIME type, or the
	 *         fallback format if the extension was not recognized.
	 * @see #forMIMEType(String)
	 * @see #getMIMETypes
	 */
	public static BooleanQueryResultFormat forMIMEType(String mimeType, BooleanQueryResultFormat fallback) {
		return matchMIMEType(mimeType, VALUES, fallback);
	}

	/**
	 * Tries to determine the appropriate boolean file format for a file, based
	 * on the extension specified in a file name.
	 * <p>
	 * NOTE: This method may not take into account dynamically loaded formats.
	 * Use {@link QueryResultIO#getBooleanParserFormatForFileName(String)} and
	 * {@link QueryResultIO#getBooleanWriterFormatForFileName(String)} to find
	 * all dynamically loaded parser and writer formats, respectively.
	 * 
	 * @param fileName
	 *        A file name.
	 * @return A TupleQueryResultFormat object if the file extension was
	 *         recognized, or <tt>null</tt> otherwise.
	 * @see #forFileName(String,BooleanQueryResultFormat)
	 * @see #getFileExtensions
	 */
	public static BooleanQueryResultFormat forFileName(String fileName) {
		return forFileName(fileName, null);
	}

	/**
	 * Tries to determine the appropriate boolean file format for a file, based
	 * on the extension specified in a file name. The supplied fallback format
	 * will be returned when the file name extension was not recognized.
	 * <p>
	 * NOTE: This method may not take into account dynamically loaded formats.
	 * Use
	 * {@link QueryResultIO#getBooleanParserFormatForFileName(String, BooleanQueryResultFormat)}
	 * and
	 * {@link QueryResultIO#getBooleanWriterFormatForFileName(String, BooleanQueryResultFormat)}
	 * to find all dynamically loaded parser and writer formats, respectively.
	 * 
	 * @param fileName
	 *        A file name.
	 * @return A TupleQueryResultFormat that matches the file name extension, or
	 *         the fallback format if the extension was not recognized.
	 * @see #forFileName(String)
	 * @see #getFileExtensions
	 */
	public static BooleanQueryResultFormat forFileName(String fileName, BooleanQueryResultFormat fallback) {
		return matchFileName(fileName, VALUES, fallback);
	}

	/*--------------*
	 * Constructors *
	 *--------------*/

	/**
	 * Creates a new BooleanQueryResultFormat object.
	 * 
	 * @param name
	 *        The name of the format, e.g. "SPARQL/XML".
	 * @param mimeType
	 *        The MIME type of the format, e.g.
	 *        <tt>application/sparql-results+xml</tt> for the SPARQL/XML format.
	 * @param fileExt
	 *        The (default) file extension for the format, e.g. <tt>srx</tt> for
	 *        SPARQL/XML.
	 */
	public BooleanQueryResultFormat(String name, String mimeType, String fileExt) {
		this(name, mimeType, null, fileExt);
	}

	/**
	 * Creates a new BooleanQueryResultFormat object.
	 * 
	 * @param name
	 *        The name of the format, e.g. "SPARQL/XML".
	 * @param mimeType
	 *        The MIME type of the format, e.g.
	 *        <tt>application/sparql-results+xml</tt> for the SPARQL/XML format.
	 * @param charset
	 *        The default character encoding of the format. Specify <tt>null</tt>
	 *        if not applicable.
	 * @param fileExt
	 *        The (default) file extension for the format, e.g. <tt>srx</tt> for
	 *        SPARQL/XML.
	 */
	public BooleanQueryResultFormat(String name, String mimeType, Charset charset, String fileExt) {
		super(name, mimeType, charset, fileExt);
	}

	/**
	 * Creates a new BooleanQueryResultFormat object.
	 * 
	 * @param name
	 *        The name of the format, e.g. "SPARQL/XML".
	 * @param mimeTypes
	 *        The MIME types of the format, e.g.
	 *        <tt>application/sparql-results+xml</tt> for the SPARQL/XML format.
	 *        The first item in the list is interpreted as the default MIME type
	 *        for the format.
	 * @param charset
	 *        The default character encoding of the format. Specify <tt>null</tt>
	 *        if not applicable.
	 * @param fileExtensions
	 *        The format's file extensions, e.g. <tt>srx</tt> for SPARQL/XML
	 *        files. The first item in the list is interpreted as the default
	 *        file extension for the format.
	 */
	public BooleanQueryResultFormat(String name, Collection<String> mimeTypes, Charset charset,
			Collection<String> fileExtensions)
	{
		super(name, mimeTypes, charset, fileExtensions);
	}

	/**
	 * Creates a new BooleanQueryResultFormat object.
	 * 
	 * @param name
	 *        The name of the format, e.g. "SPARQL/XML".
	 * @param mimeTypes
	 *        The MIME types of the format, e.g.
	 *        <tt>application/sparql-results+xml</tt> for the SPARQL/XML format.
	 *        The first item in the list is interpreted as the default MIME type
	 *        for the format.
	 * @param charset
	 *        The default character encoding of the format. Specify <tt>null</tt>
	 *        if not applicable.
	 * @param fileExtensions
	 *        The format's file extensions, e.g. <tt>srx</tt> for SPARQL/XML
	 *        files. The first item in the list is interpreted as the default
	 *        file extension for the format.
	 * @param standardURI
	 *        The standard URI that has been assigned to this format by a
	 *        standards organisation or null if it does not currently have a
	 *        standard URI.
	 * @since 2.8.0
	 */
	public BooleanQueryResultFormat(String name, Collection<String> mimeTypes, Charset charset,
			Collection<String> fileExtensions, URI standardURI)
	{
		super(name, mimeTypes, charset, fileExtensions, standardURI);
	}
}
