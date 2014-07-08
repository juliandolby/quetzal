package com.ibm.rdf.store.sparql11.sqltemplate;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.ibm.rdf.store.Context;
import com.ibm.rdf.store.Store;
import com.ibm.rdf.store.sparql11.model.BindPattern;
import com.ibm.rdf.store.sparql11.model.Expression;
import com.ibm.rdf.store.sparql11.model.QueryTriple;
import com.ibm.rdf.store.sparql11.model.Variable;
import com.ibm.rdf.store.sparql11.sqlwriter.SQLWriterException;
import com.ibm.rdf.store.sparql11.stopt.STEAccessMethodType;
import com.ibm.rdf.store.sparql11.stopt.STPlanNode;
import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.collections.Pair;

public abstract class SimplePatternBothSQLTemplate extends SimplePatternSQLTemplate {
	
	protected Map<String, Pair<String, String>> sVarMap;
	
	public SimplePatternBothSQLTemplate(String templateName, Store store,
			Context ctx, STPlanWrapper wrapper, STPlanNode planNode) {
		super(templateName, store, ctx, wrapper, planNode);
		
	}
	
	@Override
	// ApplyBind gets called when creating the project clause for the primary table
	// in SQLBoth templates.  So this pattern should evaluate if we can apply bind in 
	// primary
	protected boolean applyBind() {
		return applyBindForPrimary();
	}
	
	// When we have access to both primary and secondary, the decision on whether one needs
	// to project bind is complicated by the fact that bind patterns may be projected in
	// the primary only if they are in the subject position and the access is by direct
	// (if its DPH) or if its reverse access and its in the object position (RPH)
	protected boolean applyBindForPrimary() {
		if (planNode.getBindPatterns() == null) {
			return false;
		}
		boolean canApply = true;
		for (Variable v : getBindDependsOnVariables()) {
			canApply = !isApplicableInSecondaryOnly(v) && canApply;
			if (!canApply) {
				break;
			}
		}
		return canApply;
	}
	
	protected boolean isApplicableInSecondaryOnly(Variable v) {
		QueryTriple t = planNode.getTriple();
		if (STEAccessMethodType.isDirectAccess(planNode.getMethod().getType())) {
			if (t.getObject().isVariable() && t.getObject().getVariable().equals(v)) {
				return true;
			} else {
				return false;
			}
		} else if (STEAccessMethodType.isReverseAccess(planNode.getMethod().getType())) {
			if (t.getSubject().isVariable() && t.getSubject().getVariable().equals(v)) {
				return true;
			} else {
				return false;
			}
		} else {
			throw new RuntimeException("Unknown access type");
		}
	}

	
	protected Set<Variable> getBindDependsOnVariables() {
		Set<Variable> bindVariables = HashSetFactory.make();
		if (planNode.getBindPatterns() != null) {
			for (BindPattern p : planNode.getBindPatterns()) {
				bindVariables.addAll(p.getExpression().gatherVariables());
			}
		}
		return bindVariables;
	}
	
	protected Set<Variable> getBindProjectedVariables() {
		Set<Variable> bindVariables = HashSetFactory.make();
		if (planNode.getBindPatterns() != null) {
			for (BindPattern p : planNode.getBindPatterns()) {
				bindVariables.add(p.getVar());
			}
		}
		return bindVariables;
	}
	
	protected boolean applyFilterInPrimary(Expression e) {
		Set<Variable> bindDependsOn = getBindDependsOnVariables();
		Set<Variable> bindProjects = getBindProjectedVariables();
		for (Variable v: e.gatherVariables()) {
			if (bindProjects.contains(v)) {
				for (Variable d: bindDependsOn) {
					if (isApplicableInSecondaryOnly(d)) {
						return false;
					}
				}
			} else {
				if (isApplicableInSecondaryOnly(v)) {
					return false;
				}
			}
		}
		return true;
		
	}
	
	
	protected List<String> mapBindForProjectInSecondary() throws SQLWriterException {
		return mapBindForProject(sVarMap);
	}
}
