package com.ibm.rdf.store.sparql11.model;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.ibm.rdf.store.runtime.service.types.TypeMap;
import com.ibm.wala.util.collections.HashSetFactory;

public class NumericExpression extends Expression {
	public static enum ENEType {VALUE, PLUS, MINUS, MUL, DIV};
	private Expression lhs;
	private Expression rhs;
	private ENEType ntype;
	
	public NumericExpression() 
	{
		super(EExpressionType.NUMERIC);
		this.ntype = ENEType.VALUE;
		this.lhs = null;
		this.rhs = null;
	}

	public NumericExpression(ENEType t, Expression e, Expression r) 
	{
		super(EExpressionType.NUMERIC);
		this.ntype = t;
		this.lhs = e;
		this.rhs = r;
	}

	public Expression getLHS()
	{
		return this.lhs;
	}
	
	public void setLHS(Expression e)
	{
		this.lhs = e;
	}
	
	public Expression getRHS()
	{
		return this.rhs;
	}
	
	public NumericExpression setRHS(ENEType t, Expression e)
	{
		if (this.rhs == null) {
			this.ntype = t;
			this.rhs = e;
			return this;
		}
		
		NumericExpression n = new NumericExpression();
		n.setLHS(this);
		n.setRHS(t, e);
		return n;
	}
	
	public ENEType getNumericExpressionType() 
	{
		return this.ntype;
	}
	
	
	@Override
	public Short getReturnType() 
	{
		return TypeMap.DOUBLE_ID;
	}
	
	public short getTypeEquality(Variable v) {
		return TypeMap.NONE_ID;
	}
	
	
	public String getOperatorString()
	{
		if (this.ntype == ENEType.PLUS) {
			return "+";
		} else if (this.ntype == ENEType.MINUS) {
			return "-";
		} else if (this.ntype == ENEType.MUL) {
			return "*";
		} else if (this.ntype == ENEType.DIV) {
			return "/";
		}
		
		return "";
	}
	public String toString() {
		StringBuilder sb = new StringBuilder();
		
		if (this.ntype == ENEType.VALUE) {
			sb.append(this.lhs.toString());
			return sb.toString();
		}
		
		sb.append("(");
		sb.append(this.lhs.toString());
		if (this.ntype == ENEType.PLUS) {
			sb.append(" + ");
		} else if (this.ntype == ENEType.MINUS) {
			sb.append(" - ");
		} else if (this.ntype == ENEType.MUL) {
			sb.append(" * ");
		} else if (this.ntype == ENEType.DIV) {
			sb.append(" / ");
		} 
		sb.append(rhs.toString());
		sb.append(")");
		
		return sb.toString();
	}
	
	public String getStringWithVarName() {
		StringBuilder sb = new StringBuilder();
		
		if (this.ntype == ENEType.VALUE) {
			sb.append(this.lhs.getStringWithVarName());
			return sb.toString();
		}
		
		sb.append("(");
		sb.append(this.lhs.getStringWithVarName());
		if (this.ntype == ENEType.PLUS) {
			sb.append(" + ");
		} else if (this.ntype == ENEType.MINUS) {
			sb.append(" - ");
		} else if (this.ntype == ENEType.MUL) {
			sb.append(" * ");
		} else if (this.ntype == ENEType.DIV) {
			sb.append(" / ");
		} 
		sb.append(rhs.getStringWithVarName());
		sb.append(")");
		
		return sb.toString();
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + this.lhs.hashCode();
		if (this.ntype != ENEType.VALUE) {
			result = prime * result + this.rhs.hashCode();
		}
		return result;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		NumericExpression other = (NumericExpression) obj;
		if (!this.lhs.equals(other.lhs))
			return false;
		if (this.ntype != other.ntype)  
			return false;
		if (this.ntype != ENEType.VALUE) {
			if (!this.rhs.equals(other.rhs))
				return false;
		}
		return true;
	}
	
	/* (non-Javadoc)
	 * @see com.ibm.rdf.store.sparql11.model.Expression#renamePrefixes(java.lang.String, java.util.Map, java.util.Map)
	 */
	@Override
	public void renamePrefixes(String base, Map<String, String> declared,
			Map<String, String> internal) 
	{
		if (lhs != null) lhs.renamePrefixes(base, declared, internal);
		if (rhs != null) rhs.renamePrefixes(base, declared, internal);
	}
	
	@Override
	public void reverseIRIs() 
	{
		if (lhs != null) lhs.reverseIRIs();
		if (rhs != null) rhs.reverseIRIs();
	}
	
	/* (non-Javadoc)
	 * @see com.ibm.rdf.store.sparql11.model.Expression#gatherBlankNodes()
	 */
	@Override
	public Set<BlankNodeVariable> gatherBlankNodes() 
	{
		Set<BlankNodeVariable> ret = new HashSet<BlankNodeVariable>();
		if (lhs != null) ret.addAll(lhs.gatherBlankNodes());
		if (rhs != null) ret.addAll(rhs.gatherBlankNodes());
		return ret;
	}
	
	/* (non-Javadoc)
	 * @see com.ibm.rdf.store.sparql11.model.Expression#gatherVariables()
	 */
	@Override
	public Set<Variable> gatherVariables() 
	{
		Set<Variable> ret = new HashSet<Variable>();
		if (lhs != null) ret.addAll(lhs.gatherVariables());
		if (rhs != null) ret.addAll(rhs.gatherVariables());
		return ret;
	}

	@Override
	public Set<Variable> getVariables() {
		Set<Variable> vars = HashSetFactory.make();
		if (lhs != null) {
			vars.addAll(lhs.gatherVariables());
		}
		if (rhs != null) {
			vars.addAll(rhs.gatherVariables());
		}
		return vars; 
	}

	public void traverse(IExpressionTraversalListener l) {
		l.startExpression(this);
		if (lhs != null) lhs.traverse(l);
		if (rhs != null) rhs.traverse(l);
		l.endExpression(this);
	}
	
	
	public TypeMap.TypeCategory getTypeRestriction(Variable v){
		if(!this.gatherVariables().contains(v))
			return TypeMap.TypeCategory.NONE;
		else return TypeMap.TypeCategory.NUMERIC;
	}
	
	public boolean containsEBV(){
		return false;
	}
	
	public boolean containsBound(){
		return false;
	}
	
	public boolean containsNotBound(){
		return false;
	}
	
	@Override
	public boolean containsCast(Variable v) {		
		if(lhs!=null && lhs.containsCast(v))return true;
		if(rhs!=null && rhs.containsCast(v))return true;
		return false;
	}
}
