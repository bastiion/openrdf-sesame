/* Generated By:JJTree: Do not edit this line. ASTIRI.java */

package org.openrdf.query.parser.sparql.ast;

public class ASTIRI extends SimpleNode {

	private String value;

	public ASTIRI(int id) {
		super(id);
	}

	public ASTIRI(SyntaxTreeBuilder p, int id) {
		super(p, id);
	}

	@Override
	public Object jjtAccept(SyntaxTreeBuilderVisitor visitor, Object data)
		throws VisitorException
	{
		return visitor.visit(this, data);
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	@Override
	public String toString()
	{
		return super.toString() + " (" + value + ")";
	}
}
