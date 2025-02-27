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
package org.openrdf.query.algebra.evaluation.function;

import java.math.BigDecimal;

import org.openrdf.model.Literal;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.datatypes.XMLDatatypeUtil;
import org.openrdf.model.vocabulary.XMLSchema;
import org.openrdf.query.algebra.evaluation.ValueExprEvaluationException;
import org.openrdf.query.algebra.evaluation.util.QueryEvaluationUtil;

/**
 * A {@link Function} that tries to cast its argument to an <tt>xsd:decimal</tt>.
 * 
 * @author Arjohn Kampman
 */
public class DecimalCast implements Function {

	public String getURI() {
		return XMLSchema.DECIMAL.toString();
	}

	public Literal evaluate(ValueFactory valueFactory, Value... args)
		throws ValueExprEvaluationException
	{
		if (args.length != 1) {
			throw new ValueExprEvaluationException("xsd:decimal cast requires exactly 1 argument, got "
					+ args.length);
		}

		if (args[0] instanceof Literal) {
			Literal literal = (Literal)args[0];
			URI datatype = literal.getDatatype();

			if (QueryEvaluationUtil.isStringLiteral(literal)) {
				String decimalValue = XMLDatatypeUtil.collapseWhiteSpace(literal.getLabel());
				if (XMLDatatypeUtil.isValidDecimal(decimalValue)) {
					return valueFactory.createLiteral(decimalValue, XMLSchema.DECIMAL);
				}
			}
			else if (datatype != null) {
				if (datatype.equals(XMLSchema.DECIMAL)) {
					return literal;
				}
				else if (XMLDatatypeUtil.isNumericDatatype(datatype)) {
					// FIXME: floats and doubles must be processed separately, see
					// http://www.w3.org/TR/xpath-functions/#casting-from-primitive-to-primitive
					try {
						BigDecimal decimalValue = literal.decimalValue();
						return valueFactory.createLiteral(decimalValue.toPlainString(), XMLSchema.DECIMAL);
					}
					catch (NumberFormatException e) {
						throw new ValueExprEvaluationException(e.getMessage(), e);
					}
				}
				else if (datatype.equals(XMLSchema.BOOLEAN)) {
					try {
						return valueFactory.createLiteral(literal.booleanValue() ? "1.0" : "0.0", XMLSchema.DECIMAL);
					}
					catch (IllegalArgumentException e) {
						throw new ValueExprEvaluationException(e.getMessage(), e);
					}
				}
			}
		}

		throw new ValueExprEvaluationException("Invalid argument for xsd:decimal cast: " + args[0]);
	}
}
