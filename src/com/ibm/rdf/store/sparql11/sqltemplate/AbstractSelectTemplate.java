package com.ibm.rdf.store.sparql11.sqltemplate;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.antlr.stringtemplate.StringTemplate;

import com.ibm.rdf.store.Context;
import com.ibm.rdf.store.Store;
import com.ibm.rdf.store.config.Constants;
import com.ibm.rdf.store.runtime.service.types.TypeMap;
import com.ibm.rdf.store.sparql11.model.AggregateExpression;
import com.ibm.rdf.store.sparql11.model.BlankNodeVariable;
import com.ibm.rdf.store.sparql11.model.ConstantExpression;
import com.ibm.rdf.store.sparql11.model.Expression;
import com.ibm.rdf.store.sparql11.model.Pattern;
import com.ibm.rdf.store.sparql11.model.ProjectedVariable;
import com.ibm.rdf.store.sparql11.model.SelectClause;
import com.ibm.rdf.store.sparql11.model.SelectClause.ESelectModifier;
import com.ibm.rdf.store.sparql11.model.SolutionModifiers;
import com.ibm.rdf.store.sparql11.model.Variable;
import com.ibm.rdf.store.sparql11.model.VariableExpression;
import com.ibm.rdf.store.sparql11.sqlwriter.FilterContext;
import com.ibm.rdf.store.sparql11.sqlwriter.SPARQLToSQLExpression;
import com.ibm.rdf.store.sparql11.sqlwriter.SQLWriterException;
import com.ibm.rdf.store.sparql11.stopt.STPlanNode;
import com.ibm.wala.util.collections.HashMapFactory;
import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.collections.Pair;


public abstract class AbstractSelectTemplate extends SolutionModifierBaseTemplate {

	public AbstractSelectTemplate(String templateName, Store store, Context ctx, STPlanWrapper wrapper) {
		super(templateName, store, ctx, wrapper);
	}

	protected abstract Set<Variable> getAllPatternVariables();
	
	protected abstract SelectClause getSelectClause();
	
	protected abstract Pattern getPattern();
	
	protected abstract SolutionModifiers getSolutionModifiers();
	
	protected abstract List<ProjectedVariable> getProjectedVariables();
	
	
	protected HashMap<String, Pair<String, String>> varMap = new HashMap<String, Pair<String, String>>();

	
	Set<SQLMapping> populateMappings() throws Exception{
		HashSet<SQLMapping> mappings = new HashSet<SQLMapping>();
		
		
		List<String> selectDistinctMap = getSelectDistinctMapping();
		if(selectDistinctMap != null){			
			SQLMapping selectDistinctMapping=new SQLMapping("distinct", selectDistinctMap, null);
			mappings.add(selectDistinctMapping);
		}
		
		List<String> projectList = getSelectProjectMapping() != null ? getSelectProjectMapping().fst : null;
		if(projectList == null || projectList.isEmpty()) {
			projectList = new LinkedList<String>();
			projectList.add("*");
		}
		SQLMapping projectMapping=new SQLMapping("project", projectList,null);
		mappings.add(projectMapping);
		
		
		SQLMapping tMapping=new SQLMapping("target", getTargetSQLClause(),null);
		mappings.add(tMapping);
	
		String solnModifiers = getSolutionModifiersMappings();
		if (solnModifiers != null && solnModifiers.length() != 0) {
			mappings.add(new SQLMapping("endModifiers", getSolutionModifiersMappings(), null));
		}
		
		Set<String> outerProject = getSelectProjectMapping() != null ? getSelectProjectMapping().snd : null;
		if (outerProject != null) {
			mappings.add(new SQLMapping("outerProject", outerProject, null));
		}

		
		return mappings;
	}
	
	
	
	private Map<String, Expression> getExpressionsInSolutionModifiers() {
		Map<String, Expression> ret = HashMapFactory.make();
		SolutionModifiers mods = getSolutionModifiers();
		if (mods == null) {
			return Collections.emptyMap();
		}
		if (mods.getGroupClause() != null && mods.getGroupClause().getConditions() != null) {
			for (Expression e : mods.getGroupClause().getConditions()) {
				extractExpressionFromSolutionsClause(ret, e);
			}
		}
		if (mods.getHavingClause() != null && mods.getHavingClause().getConstraints() != null) {
			for (Expression e : mods.getHavingClause().getConstraints()) {
				extractExpressionFromSolutionsClause(ret, e);
			}
		}
		
		return ret;
	}

	private void extractExpressionFromSolutionsClause(
			Map<String, Expression> ret, Expression e) {
		if (e instanceof VariableExpression) {
			if (((VariableExpression) e).getExpression() != null) {
				ret.put(((VariableExpression) e).getVariable(), ((VariableExpression) e).getExpression());
			}
		}
	}


	
	private Pair<List<String>, Set<String>> getSelectProjectMapping() throws Exception{
		List<ProjectedVariable> projectedVariables = getProjectedVariables();
		if (projectedVariables == null || projectedVariables.isEmpty()) {
			return null;
		}

		Pair<List<String>, Set<AggregateExpression>> exps = getProjectionMapping(projectedVariables);
		List<String> projectMapping = exps.fst; 
		Set<AggregateExpression> aggregateExpressions = exps.snd;
		
		Set<String> aggs = HashSetFactory.make();
		if (aggregateExpressions != null && !aggregateExpressions.isEmpty()) {
			for (Expression e: aggregateExpressions) {
				for (Variable v : e.gatherVariables()) {
					assert varMap.containsKey(v.getName());
					aggs.add("MIN(" + varMap.get(v.getName()).snd + ") AS " + varMap.get(v.getName()).snd + "_MIN");
					aggs.add("MAX(" + varMap.get(v.getName()).snd + ") AS "+ varMap.get(v.getName()).snd + "_MAX");				
				}
			}
		}
		
		for (String s: aggs) {
			projectMapping.add(s);
		}
		
		Set<String> outerProjections = null;
		if (aggregateExpressions != null && !aggregateExpressions.isEmpty()) {
			outerProjections = getOuterProjectionMapping(projectedVariables, aggregateExpressions);
		}
		
		return Pair.make(projectMapping, outerProjections);
	}
	
	private Set<String> getOuterProjectionMapping(List<ProjectedVariable> projectedVariables, Set<AggregateExpression> aggregateExpressions) {
		Set<String> ret = HashSetFactory.make();
		Map<String, Expression> expressionsInSolutions = getExpressionsInSolutionModifiers();

		for (ProjectedVariable v : projectedVariables) {
			Variable renamedpv = wrapper.getRenamedVariableFor(v.getVariable());

			if (v.getExpression() != null && aggregateExpressions.contains(v.getExpression())) {
				ret.add(getOuterAggregate(renamedpv, v.getExpression()));
			} else if (expressionsInSolutions.containsKey(v.getVariable().getName())) {
				Expression e = expressionsInSolutions.get(v.getVariable().getName());
				if (aggregateExpressions.contains(e)) {
					getOuterAggregate(renamedpv, e);
				}
			} else {
				ret.add(renamedpv.getName());
			} 
		}
		return ret;
	}
		
	private String getOuterAggregate(Variable renamedpv, Expression e) {
		// KAVITHA: we need to wrap an aggregate in a case if its a numeric and there has been an 'error' in processing it meaning we have different types
		StringTemplate t = SPARQLToSQLExpression.getInstanceOf("outer_aggregate_function_type_check");
		List<String> typeCheck = new LinkedList<String>();
		for (Variable v :e.gatherVariables()) {
			typeCheck.add(varMap.get(v.getName()).snd + "_MIN >= " + TypeMap.DATATYPE_NUMERICS_IDS_START + " AND " + varMap.get(v.getName()).snd + "_MAX <= " + TypeMap.DATATYPE_NUMERICS_IDS_END);
		}
		t.setAttribute("typecheck", typeCheck);
		t.setAttribute("var", renamedpv.getName());
		return t.toString();
	}

	private Pair<List<String>, Set<AggregateExpression>> getProjectionMapping(List<ProjectedVariable> projectedVariables) throws SQLWriterException {
		if (projectedVariables == null || projectedVariables.isEmpty()) {
			return null;
		}
		Set<Variable> queryPatternVariables = HashSetFactory.make();
		Set<AggregateExpression> aggregateExpressions = HashSetFactory.make();
		// eliminate blank nodes
		Set<Variable> patternVariables = getAllPatternVariables();
		if (patternVariables != null) {
			for (Variable v : patternVariables) {
				if (!(v instanceof BlankNodeVariable))
				queryPatternVariables.add(v);
			}
		}

		List<String> projectMapping = new LinkedList<String>();
		Set<Variable> iriBoundVariables = getPattern().gatherIRIBoundVariables();
		
		Map<String, Expression> expressionsInSolutions = getExpressionsInSolutionModifiers();
		
		if (projectedVariables != null) {
			for (ProjectedVariable pv : projectedVariables) {
				Variable renamedpv = wrapper.getRenamedVariableFor(pv.getVariable());

				if (queryPatternVariables != null && queryPatternVariables.contains(pv.getVariable())) {
					String sqlVarName = wrapper.getLastVarMappingForQueryInfo(pv.getVariable());
					projectMapping.add(sqlVarName+" AS "+ renamedpv.getName());
					if( !iriBoundVariables.contains(pv.getVariable())){
						String sqlVarTypeName = (sqlVarName != null)?sqlVarName+Constants.TYP_COLUMN_SUFFIX_IN_SPARQL_RS:null; 
						projectMapping.add(sqlVarTypeName+" AS "+ renamedpv.getName()+Constants.TYP_COLUMN_SUFFIX_IN_SPARQL_RS);
					}
				}
				else if (pv.getExpression() != null) {
					handleProjectedExpression(pv.getExpression(), renamedpv, projectMapping, aggregateExpressions);
				} else if (expressionsInSolutions.containsKey(pv.getVariable().getName())) {
					Expression e = expressionsInSolutions.get(pv.getVariable().getName());
					handleProjectedExpression(e, renamedpv, projectMapping, aggregateExpressions);
				} else {
					projectMapping.add(renamedpv.getName());
				}
			}
		}
		return Pair.make(projectMapping, aggregateExpressions);
	}
	
	protected List<String> getProjectMapping() throws Exception{
		return getSelectProjectMapping().fst;
	}
	
	private void handleProjectedExpression(Expression e, Variable renamedpv, List<String> projectMapping, Set<AggregateExpression> aggregateExpressions) throws SQLWriterException {

		FilterContext context = new FilterContext(varMap,  wrapper.getPropertyValueTypes(), planNode);
		if (wrapper.getPlan() != null) {
			STPlanNode pn = planNode != null ? planNode : wrapper.getPlan().planTreeRoot;
			if (!hasAvailableVariables(pn, e)) {
				return;															// SPARQL has this annoying habit of asking about variables that are never bound
			}
		}
		populateVarMap(e);

		if (needCaseForNumeric(e, context)) {
			context.addConstantNeedsTypeCheck(e);
			SPARQLToSQLExpression.gatherAggregates(e, aggregateExpressions);
		}
		
		String str = expGenerator.getSQLExpression(e, context);

		if (e.getReturnType() == TypeMap.BOOLEAN_ID) {
			// boolean expressions cannot be projected directly  They need to be wrapped in a case statement
			projectMapping.add(expGenerator.wrapBooleanExpressionInCase(str) +" AS "+renamedpv.getName());
		}  else {
			projectMapping.add(str + " AS " + renamedpv.getName());
		}
		
		String vtyp = null;
		if (e instanceof AggregateExpression) {
			Set<Variable> vars = e.gatherVariables();
			if (vars.size() == 1) {
				Variable v = vars.iterator().next();
				vtyp = varMap.get(v.getName()).snd;
				projectMapping.add("MAX(" + vtyp + ") AS " + renamedpv.getName() + "_TYP");
			}
		} else if (!(e instanceof VariableExpression) && !(e instanceof ConstantExpression)){
			projectMapping.add(e.getReturnType() + " AS " + renamedpv.getName() + "_TYP");
		}
		// done with this expression.  Add the projected variable in to varmap in case it gets referenced later in another expression
		if (!varMap.containsKey(renamedpv.getName())) {
			varMap.put(renamedpv.getName(), Pair.make(str, vtyp));
		}	

	}


	private boolean hasAvailableVariables(STPlanNode planNode, Expression e) {
		for (Variable v : e.gatherVariables()) {
			if (!(planNode.getAvailableVariables().contains(v) ||
				  (planNode.getOperatorVariables() != null && planNode.getOperatorVariables().contains(v)) 
			     ) && !varMap.containsKey(v.getName())) {
				return false;
			}
		}
		return true;		
	}
	
	
	private boolean needCaseForNumeric(Expression e, FilterContext context) {

		// KAVITHA: We need something special for aggregate handling. DAWG has the 'open world semantics' of XML and unfortunately
		// doing XML here will cause ridiculous performance problems
		boolean hasSingleType = true;

		for (Variable v : e.gatherVariables()) {
			if (e.getTypeRestriction(v) == TypeMap.TypeCategory.NONE) {
				return false;
			}
			if (!context.getVarMap().containsKey(v.getName())) {
				return false;			// never encountered this variable before, its a new var in project
			}
			if (context.getVarMap().containsKey(v.getName()) && context.getVarMap().get(v.getName()).snd == null) {
				return false;			// we don't even have a type, so unclear we can make any sort of case check
			}
			if (wrapper.getPlan() != null && wrapper.getPlan().isVariableOfSingleType(store, v)) {
				hasSingleType = hasSingleType && true;
			} else {
				return true;			// of mixed type, needs case
			}
		}
		if (hasSingleType) {
			return false;			// all variables in this numeric have a single type, we should be fine with just a type check
		}
		return true;
	}

	
	private List<String> getSelectDistinctMapping(){
		SelectClause sc = getSelectClause();
		if(sc != null){
			if (getSelectClause().getSelectModifier() == ESelectModifier.DISTINCT) {
				List<String> selectDistinctMapping = new LinkedList<String>();		
				selectDistinctMapping.add("DISTINCT");
				return selectDistinctMapping;
			}
		}
		return null;
	}

	
	protected String getSolutionModifiersMappings() throws Exception {
		SolutionModifiers modifiers = getSolutionModifiers();
		if (modifiers == null) {
			return null;
		}
		
		AbstractSQLTemplate solnModiferTemplate = new SolutionModifierTemplate("solnModifier", modifiers ,store, ctx, wrapper, this);


		return solnModiferTemplate.createSQLString();
	}

		
	protected void populateVarMap(Expression e) {
		for (Variable v : e.gatherVariables()){

			String vSqlName = wrapper.getLastVarMappingForQueryInfo(v);
			if (vSqlName == null) {
				vSqlName = v.getName();
			}
			
			if (varMap.containsKey(vSqlName)) {
				continue;
			}

			if(wrapper.getIRIBoundVariables().contains(v)) {
				varMap.put(vSqlName, Pair.make(v.getName(),(String) null));
			} else {
				varMap.put(vSqlName, Pair.make(v.getName(),v.getName()+Constants.TYP_COLUMN_SUFFIX_IN_SPARQL_RS));
			} 
		}
		
	}
}
