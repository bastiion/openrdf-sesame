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
package org.openrdf.query.impl;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.openrdf.model.Value;
import org.openrdf.query.AbstractBindingSet;
import org.openrdf.query.Binding;
import org.openrdf.query.BindingSet;

/**
 * A Map-based implementation of the {@link BindingSet} interface.
 */
public class MapBindingSet extends AbstractBindingSet {

	private static final long serialVersionUID = -8857324525220429607L;

	private final Map<String, Binding> bindings;

	public MapBindingSet() {
		this(8);
	}

	/**
	 * Creates a new Map-based BindingSet with the specified initial capacity.
	 * Bindings can be added to this binding set using the {@link #addBinding}
	 * methods.
	 * 
	 * @param capacity
	 *        The initial capacity of the created BindingSet object.
	 */
	public MapBindingSet(int capacity) {
		// Create bindings map, compensating for HashMap's load factor
		bindings = new LinkedHashMap<String, Binding>(capacity * 2);
	}

	/**
	 * Adds a binding to the binding set.
	 * 
	 * @param name
	 *        The binding's name.
	 * @param value
	 *        The binding's value.
	 */
	public void addBinding(String name, Value value) {
		addBinding(new BindingImpl(name, value));
	}

	/**
	 * Adds a binding to the binding set.
	 * 
	 * @param binding
	 *        The binding to add to the binding set.
	 */
	public void addBinding(Binding binding) {
		bindings.put(binding.getName(), binding);
	}

	/**
	 * Removes a binding from the binding set.
	 * 
	 * @param name
	 *        The binding's name.
	 */
	public void removeBinding(String name) {
		bindings.remove(name);
	}

	/**
	 * Removes all bindings from the binding set.
	 */
	public void clear() {
		bindings.clear();
	}

	public Iterator<Binding> iterator() {
		return bindings.values().iterator();
	}

	public Set<String> getBindingNames() {
		return bindings.keySet();
	}

	public Binding getBinding(String bindingName) {
		return bindings.get(bindingName);
	}

	public boolean hasBinding(String bindingName) {
		return bindings.containsKey(bindingName);
	}

	public Value getValue(String bindingName) {
		Binding binding = getBinding(bindingName);

		if (binding != null) {
			return binding.getValue();
		}

		return null;
	}

	public int size() {
		return bindings.size();
	}

}
