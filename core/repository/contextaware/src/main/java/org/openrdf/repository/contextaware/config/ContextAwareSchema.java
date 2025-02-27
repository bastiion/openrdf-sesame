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
package org.openrdf.repository.contextaware.config;

import org.openrdf.model.URI;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.impl.ValueFactoryImpl;

/**
 * @author James Leigh
 */
public class ContextAwareSchema {

	/**
	 * The ContextAwareRepository schema namespace (
	 * <tt>http://www.openrdf.org/config/repository/contextaware#</tt>).
	 */
	public static final String NAMESPACE = "http://www.openrdf.org/config/repository/contextaware#";

	/** <tt>http://www.openrdf.org/config/repository/contextaware#includeInferred</tt> */
	public final static URI INCLUDE_INFERRED;

	/** <tt>http://www.openrdf.org/config/repository/contextaware#maxQueryTime</tt> */
	public final static URI MAX_QUERY_TIME;

	/** <tt>http://www.openrdf.org/config/repository/contextaware#queryLanguage</tt> */
	public final static URI QUERY_LANGUAGE;

	/** <tt>http://www.openrdf.org/config/repository/contextaware#base</tt> */
	public final static URI BASE_URI;

	/** <tt>http://www.openrdf.org/config/repository/contextaware#readContext</tt> */
	public final static URI READ_CONTEXT;

	/** <tt>http://www.openrdf.org/config/repository/contextaware#addContext</tt> */
	@Deprecated
	public final static URI ADD_CONTEXT;

	/** <tt>http://www.openrdf.org/config/repository/contextaware#removeContext</tt> */
	public final static URI REMOVE_CONTEXT;

	/** <tt>http://www.openrdf.org/config/repository/contextaware#archiveContext</tt> */
	@Deprecated
	public final static URI ARCHIVE_CONTEXT;

	/** <tt>http://www.openrdf.org/config/repository/contextaware#insertContext</tt> */
	public final static URI INSERT_CONTEXT;

	static {
		ValueFactory factory = ValueFactoryImpl.getInstance();
		INCLUDE_INFERRED = factory.createURI(NAMESPACE, "includeInferred");
		QUERY_LANGUAGE = factory.createURI(NAMESPACE, "ql");
		BASE_URI = factory.createURI(NAMESPACE, "base");
		READ_CONTEXT = factory.createURI(NAMESPACE, "readContext");
		ADD_CONTEXT = factory.createURI(NAMESPACE, "addContext");
		REMOVE_CONTEXT = factory.createURI(NAMESPACE, "removeContext");
		ARCHIVE_CONTEXT = factory.createURI(NAMESPACE, "archiveContext");
		INSERT_CONTEXT = factory.createURI(NAMESPACE, "insertContext");
		MAX_QUERY_TIME = factory.createURI(NAMESPACE, "maxQueryTime");
	}
}
