/*
 * Copyright Aduna (http://www.aduna-software.com/) (c) 2008-2009.
 *
 * Licensed under the Aduna BSD-style license.
 */
package org.openrdf.http.server.resources;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.restlet.data.Form;
import org.restlet.representation.Representation;
import org.restlet.representation.Variant;
import org.restlet.resource.ResourceException;

import org.openrdf.http.protocol.Protocol;
import org.openrdf.http.server.helpers.ServerUtil;
import org.openrdf.http.server.resources.helpers.TupleResultResource;
import org.openrdf.model.BNode;
import org.openrdf.model.ValueFactory;
import org.openrdf.query.BindingSet;
import org.openrdf.query.impl.ListBindingSet;
import org.openrdf.result.TupleResult;
import org.openrdf.result.impl.TupleResultImpl;

/**
 * @author Arjohn Kampman
 */
public class BNodesResource extends TupleResultResource {

	@Override
	protected Representation post(Representation representation, Variant variant)
		throws ResourceException
	{
		return get(variant);
	}

	@Override
	public TupleResult getTupleResult()
		throws ResourceException
	{
		Form params = getQuery();
		int amount = ServerUtil.parseIntegerParam(params, Protocol.AMOUNT, 1);
		String nodeID = params.getFirstValue(Protocol.NODE_ID);

		ValueFactory vf = getConnection().getValueFactory();

		List<String> columns = Arrays.asList(Protocol.BNODE);
		List<BindingSet> bnodes = new ArrayList<BindingSet>(amount);
		for (int i = 0; i < amount; i++) {
			BNode bnode = createBNode(vf, nodeID, i);
			bnodes.add(new ListBindingSet(columns, bnode));
		}

		return new TupleResultImpl(columns, bnodes);
	}

	private BNode createBNode(ValueFactory vf, String nodeID, int i) {
		if (i == 0 && nodeID != null) {
			return vf.createBNode(nodeID);
		}
		return vf.createBNode();
	}

	@Override
	protected String getFilenamePrefix() {
		return "bnodes";
	}
}
