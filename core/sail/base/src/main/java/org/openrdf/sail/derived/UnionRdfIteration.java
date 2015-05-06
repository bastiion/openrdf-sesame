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
package org.openrdf.sail.derived;

import info.aduna.iteration.Iteration;
import info.aduna.iteration.UnionIteration;

import org.openrdf.sail.SailException;

/**
 *
 * @author James Leigh
 */
public class UnionRdfIteration<T> extends UnionIteration<T, SailException> implements RdfIteration<T> {

	public UnionRdfIteration(Iterable<? extends Iteration<? extends T, SailException>> args) {
		super(args);
	}

	public UnionRdfIteration(Iteration<? extends T, SailException>... args) {
		super(args);
	}
}
