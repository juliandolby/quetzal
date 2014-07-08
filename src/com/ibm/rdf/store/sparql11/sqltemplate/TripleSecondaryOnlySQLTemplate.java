package com.ibm.rdf.store.sparql11.sqltemplate;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import com.ibm.rdf.store.Context;
import com.ibm.rdf.store.Store;
import com.ibm.rdf.store.Store.Db2Type;
import com.ibm.rdf.store.Store.PredicateTable;
import com.ibm.rdf.store.config.Constants;
import com.ibm.rdf.store.hashing.HashingException;
import com.ibm.rdf.store.hashing.HashingHelper;
import com.ibm.rdf.store.runtime.service.types.TypeMap;
import com.ibm.rdf.store.sparql11.model.BinaryUnion;
import com.ibm.rdf.store.sparql11.model.Expression;
import com.ibm.rdf.store.sparql11.model.IRI;
import com.ibm.rdf.store.sparql11.model.PropertyTerm;
import com.ibm.rdf.store.sparql11.model.QueryTriple;
import com.ibm.rdf.store.sparql11.model.QueryTripleTerm;
import com.ibm.rdf.store.sparql11.model.Variable;
import com.ibm.rdf.store.sparql11.sqlwriter.SQLWriterException;
import com.ibm.rdf.store.sparql11.stopt.STAccessMethod;
import com.ibm.rdf.store.sparql11.stopt.STEAccessMethodType;
import com.ibm.rdf.store.sparql11.stopt.STPlanNode;
import com.ibm.wala.util.collections.Pair;

/**
 * @author mbornea
 *
 */
public class TripleSecondaryOnlySQLTemplate extends SimplePatternSQLTemplate {

	Set<Variable> projectedInSecondary = null;
	public TripleSecondaryOnlySQLTemplate(String templateName, STPlanNode planNode, Store store, Context ctx, STPlanWrapper wrapper) {
		super(templateName, store, ctx, wrapper, planNode);
	}

	@Override
	Set<SQLMapping> populateMappings() throws SQLWriterException {
		
		HashSet<SQLMapping> mappings = new HashSet<SQLMapping>();
		projectedInSecondary = new HashSet<Variable>();
		varMap = new HashMap<String, Pair<String, String>>();
		
		List<String> qidSqlParam = new LinkedList<String>();
		qidSqlParam.add(getQIDMapping());		
		SQLMapping qidMapping=new SQLMapping("sql_id", qidSqlParam,null);
		mappings.add(qidMapping);
		
	
		List<String> projectSqlParams = getProjectedSQLClause();
		if(projectSqlParams.size()==0)projectSqlParams.add("*");
		SQLMapping pMapping=new SQLMapping("project", projectSqlParams,null);
		mappings.add(pMapping);
		
		SQLMapping tMapping=new SQLMapping("target", getTargetSQLClause(),null);
		mappings.add(tMapping);
		
		SQLMapping eMapping=new SQLMapping("entry_constraint",getEntrySQLConstraint(),null);
		mappings.add(eMapping);
		
		SQLMapping gMapping=new SQLMapping("graph_constraint", getGraphSQLConstraint(),null);
		mappings.add(gMapping);
	
		SQLMapping vMapping=new SQLMapping("val_constraint",getValueSQLConstraint(),null);
		mappings.add(vMapping);
		
		SQLMapping predicateMapping=new SQLMapping("predicate_constraint",getPropSQLConstraint(),null);
		mappings.add(predicateMapping);
		
		List<String> filterConstraint = getFilterSQLConstraint();
		SQLMapping filterMapping = new SQLMapping("filter_constraint", filterConstraint,null);
		mappings.add(filterMapping);
		
		List<String> sep = new LinkedList<String>();
		sep.add(" OR ");
		SQLMapping sepMapping=new SQLMapping("sep",sep,null);
		mappings.add(sepMapping);
		
		
		return mappings;
	}

	protected List<String> getProjectedSQLClause() throws SQLWriterException {
		List<String> projectSQLClause = super.getProjectedSQLClause();
		List<String> prop = mapPropForProject();
		if(prop != null)projectSQLClause.addAll(prop);
		return projectSQLClause;
	}

	protected List<String> mapEntryForProject(){
		QueryTriple qt = planNode.getTriple();
		QueryTripleTerm entryTerm = null;
		STAccessMethod am = planNode.getMethod();
		boolean entryHasSqlType = false;
		if(STEAccessMethodType.isDirectAccess(am.getType())){
			entryTerm = qt.getSubject();
		}
		else{
			entryTerm = qt.getObject();
			entryHasSqlType = true;
		}
		if(entryTerm.isVariable()){
			Variable entryVariable = entryTerm.getVariable();
			if(projectedInSecondary.contains(entryVariable))return null;
			projectedInSecondary.add(entryVariable);
			List<String> entrySqlToSparql =  new LinkedList<String>();
			entrySqlToSparql.add( Constants.NAME_COLUMN_ENTITY+" AS "+entryVariable.getName());
			String entryType = null;
			Set<Variable> iriBoundVariables = wrapper.getIRIBoundVariables();
			if(!iriBoundVariables.contains(entryVariable)){
				entryType = (entryHasSqlType)?Constants.NAME_COLUMN_PREFIX_TYPE:new Short(TypeMap.IRI_ID).toString();
				entrySqlToSparql.add( entryType+" AS "+entryTerm.getVariable().getName()+"_"+Constants.NAME_COLUMN_PREFIX_TYPE);
			}
			return entrySqlToSparql;
		}
		else{
			return null;
		}
	}	
	
	
	private List<String> mapPropForProject(){ 
		QueryTriple qt = planNode.getTriple();
		// TODO [Property Path]: Double check with Mihaela that it is fine for propTerm.toSqlDataString() to return null for complex path (ie., same behavior as variable)
		PropertyTerm propTerm = qt.getPredicate();
		if(propTerm.isVariable()){
			Variable propVariable = propTerm.getVariable();
			if(projectedInSecondary.contains(propVariable))return null;
			projectedInSecondary.add(propVariable);
			List<String> propSqlToSparql =  new LinkedList<String>();
			propSqlToSparql.add(Constants.NAME_COLUMN_PREFIX_PREDICATE+" AS "+propVariable.getName());
			return propSqlToSparql;
		}
		else{
			return null;
		}
	}
	
	/**
	 * @return
	 */
	protected List<String> mapValForProject(){
		QueryTriple qt = planNode.getTriple();
		QueryTripleTerm valTerm = null;
		STAccessMethod am = planNode.getMethod();
		PredicateTable predicateTable = null;
		boolean valueHasSqlType = false;
		if(STEAccessMethodType.isDirectAccess(am.getType())){
			valTerm = qt.getObject();
			valueHasSqlType = true;
			predicateTable = store.getDirectPredicates();
		}
		else{
			valTerm = qt.getSubject();
			predicateTable = store.getReversePredicates();			
		}
		if(valTerm.isVariable()){
			Variable valueVariable = valTerm.getVariable();
			// if the predicate is variable it is possible that there is a binding to multiple predicate values in a filter
			// possible optimization might look at the type of all the predicates and decide if they all map to the same type
			Db2Type pType=(qt.getPredicate().isVariable())?Db2Type.MIXED:predicateTable.getType(qt.getPredicate().getIRI().getValue());
			wrapper.addProperyValueType(valueVariable.getName(), pType);
			if(projectedInSecondary.contains(valueVariable))return null;
			projectedInSecondary.add(valueVariable);
			List<String> valSqlToSparql =  new LinkedList<String>();
			valSqlToSparql.add(Constants.NAME_COLUMN_PREFIX_LIST_ELEMENT+" AS " + valueVariable.getName());
			String valType = null;
			Set<Variable> iriBoundVariables = wrapper.getIRIBoundVariables();
			if(!iriBoundVariables.contains(valueVariable)){
				valType = (valueHasSqlType)?Constants.NAME_COLUMN_PREFIX_TYPE:new Short(TypeMap.IRI_ID).toString();
				valSqlToSparql.add(valType+" AS "+valueVariable.getName()+Constants.TYP_COLUMN_SUFFIX_IN_SPARQL_RS);
				
			}
			return valSqlToSparql;
		}
		else{
			return null;
		}
	}
	
	protected List<String> mapExternalVarForProject(){		
		List<String> varList = new LinkedList<String>();
		STPlanNode predecessor = planNode.getPredecessor(wrapper.getPlan());
			
		if(predecessor!=null){
			String predecessorCTE = wrapper.getPlanNodeCTE(predecessor);
			Set<Variable> predecessorVars = predecessor.getAvailableVariables();
			Set<Variable> iriBoundVariables = wrapper.getIRIBoundVariables();
			if(predecessorVars!=null){
				for(Variable v : predecessorVars){
					if(projectedInSecondary.contains(v))continue;
					projectedInSecondary.add(v);
					String vPredName = wrapper.getPlanNodeVarMapping(predecessor,v.getName());
					varList.add(predecessorCTE+"."+vPredName+" AS "+v.getName());
					String vType = null;
					if(!iriBoundVariables.contains(v)){
						vType = predecessorCTE+"."+vPredName+Constants.TYP_COLUMN_SUFFIX_IN_SPARQL_RS;
						varList.add(vType+" AS "+v.getName()+Constants.TYP_COLUMN_SUFFIX_IN_SPARQL_RS);							
					}
					varMap.put(v.getName(), Pair.make(predecessorCTE+"."+vPredName, vType));
				}				
			}

		}
		return varList;
	}
		
	private List<String> getTargetSQLClause(){
		List<String> targetSQLClause = new LinkedList<String>();
		if(STEAccessMethodType.isDirectAccess(planNode.getMethod().getType())){
			targetSQLClause.add(store.getDirectSecondary()+" AS T");
		}
		else{
			targetSQLClause.add(store.getReverseSecondary()+" AS T");
		}
		STPlanNode predecessor = planNode.getPredecessor(wrapper.getPlan());
		if(predecessor!=null){
			targetSQLClause.add( wrapper.getPlanNodeCTE(predecessor));
		}
		return targetSQLClause;		
	}
	
	
	private List<String> getEntrySQLConstraint(){
		List<String> entrySQLConstraint = new LinkedList<String>();
		QueryTriple qt = planNode.getTriple();
		QueryTripleTerm entryTerm = null;
		STAccessMethod am = planNode.getMethod();
		boolean hasSqlType = false;
		if(STEAccessMethodType.isDirectAccess(am.getType())){
			entryTerm = qt.getSubject();
		}
		else{
			entryTerm = qt.getObject();
			hasSqlType = true;
		}
		if(entryTerm.isVariable()){
			Variable entryVariable = entryTerm.getVariable();
			STPlanNode predecessor = planNode.getPredecessor(wrapper.getPlan());
			boolean typConstraint = false;
			if(hasSqlType && wrapper.getIRIBoundVariables().contains(entryVariable)){
				entrySQLConstraint.add("T."+Constants.NAME_COLUMN_PREFIX_TYPE + " <= " + TypeMap.IRI_ID);
			}
			else if(hasSqlType && !wrapper.getIRIBoundVariables().contains(entryVariable)){
				typConstraint = true;
			}
			boolean entryHasConstraintWithPredecessor = false;
			if(predecessor!=null ){
				entryHasConstraintWithPredecessor= super.getPredecessorConstraint(entrySQLConstraint, entryVariable, predecessor, "T."+Constants.NAME_COLUMN_ENTITY,"T."+ Constants.NAME_COLUMN_PREFIX_TYPE, typConstraint);
			}
			/*if(!entryHasConstraintWithPredecessor){
				if(varMap.containsKey(entryVariable.getName())){
					entrySQLConstraint.add("T."+Constants.NAME_COLUMN_ENTITY + " = " + varMap.get(entryVariable.getName()).fst);
				}
			} */
			String typeSQL = (typConstraint) ? "T."+Constants.NAME_COLUMN_PREFIX_TYPE : null;
			varMap.put(entryVariable.getName(), Pair.make("T."+Constants.NAME_COLUMN_ENTITY, typeSQL));
		}
		else{
			super.addConstantEntrySQLConstraint(entryTerm, entrySQLConstraint, hasSqlType, "T."+Constants.NAME_COLUMN_ENTITY);
		}
		return entrySQLConstraint;
	}

	private List<String> getValueSQLConstraint(){
		List<String> valueSQLConstraint = new LinkedList<String>();
		QueryTriple qt = planNode.getTriple();
		QueryTripleTerm valueTerm = null;
		STAccessMethod am = planNode.getMethod();	
		boolean hasSqlType = false;
		if(STEAccessMethodType.isDirectAccess(am.getType())){
			valueTerm = qt.getObject();
			hasSqlType = true;
		}
		else{
			valueTerm = qt.getSubject();
		}
		
		String valueSQLName = "T."+Constants.NAME_COLUMN_PREFIX_LIST_ELEMENT;
		
		if(valueTerm.isVariable()){
			Variable valueVariable = valueTerm.getVariable();
			STPlanNode predecessor = planNode.getPredecessor(wrapper.getPlan());
			boolean typConstraint = false;
			if(hasSqlType && wrapper.getIRIBoundVariables().contains(valueVariable)){
				valueSQLConstraint.add("T."+Constants.NAME_COLUMN_PREFIX_TYPE + " <= " + TypeMap.IRI_ID);
			}
			if(hasSqlType && !wrapper.getIRIBoundVariables().contains(valueVariable)){
				typConstraint = true;
			}
			boolean hasValueConstraintWithPredecessor = false;
			if(predecessor!=null ){
				Set<Variable> availableVariables = predecessor.getAvailableVariables();
				if(availableVariables != null){
					if(availableVariables.contains(valueVariable)){
						String valPredName = wrapper.getPlanNodeVarMapping(predecessor,valueVariable.getName());
						valueSQLConstraint.add(valueSQLName+ 
								" = "+ wrapper.getPlanNodeCTE(predecessor)+ "." + valPredName);
						if(typConstraint){
							valueSQLConstraint.add("T."+Constants.NAME_COLUMN_PREFIX_TYPE + 
									" = " + wrapper.getPlanNodeCTE(predecessor) + "." + valPredName + Constants.TYP_COLUMN_SUFFIX_IN_SPARQL_RS);
						}
						hasValueConstraintWithPredecessor = true;
					}
				}
			}
			if(!hasValueConstraintWithPredecessor){
				if(varMap.containsKey(valueVariable.getName())){
					valueSQLConstraint.add(valueSQLName + " = "+ varMap.get(valueVariable.getName()).fst);
				}
			}
			String valType = (typConstraint) ?"T."+ Constants.NAME_COLUMN_PREFIX_TYPE : null;
			varMap.put(valueVariable.getName(), Pair.make("T."+Constants.NAME_COLUMN_PREFIX_LIST_ELEMENT, valType));
		}
		else{
			valueSQLConstraint.add(valueSQLName+ " = '"+valueTerm.toSqlDataString()+"'");
		}
		return valueSQLConstraint;
	}
	
	private List<String> getPropSQLConstraint(){
		List<String> propSQLConstraint = new LinkedList<String>();
		QueryTriple qt = planNode.getTriple();
		// TODO [Property Path]: Double check with Mihaela that it is fine for propTerm.toSqlDataString() to return null for complex path (ie., same behavior as variable)
		PropertyTerm propTerm = qt.getPredicate();
		STPlanNode predecessor = planNode.getPredecessor(wrapper.getPlan());
		if(propTerm.isVariable()){
			if(predecessor!=null){
				Set<Variable> availableVariables = predecessor.getAvailableVariables();
				if(availableVariables != null){
					if(availableVariables.contains(propTerm.getVariable())){
						String propPredName = wrapper.getPlanNodeVarMapping(predecessor,propTerm.getVariable().getName());	
						propSQLConstraint.add("T."+Constants.NAME_COLUMN_PREFIX_PREDICATE+
								" = " +  wrapper.getPlanNodeCTE(predecessor) + "." + propPredName);
					}
				}
			}
			varMap.put(propTerm.getVariable().getName(), Pair.make("T."+Constants.NAME_COLUMN_PREFIX_PREDICATE, (String)null));			
			
		}
		else{
			propSQLConstraint.add("T."+Constants.NAME_COLUMN_PREFIX_PREDICATE+ " = '"+propTerm.toSqlDataString()+"'");
		}
		
		return propSQLConstraint;
	}
	

	public static String getSID(String value, int maxLength) {
		if (value.length() > maxLength) {
			try {
				return Constants.PREFIX_SHORT_STRING
						+ HashingHelper.hashLongString(value);
			} catch (HashingException e) {
			}
		}
		return value;
	}

	@Override
	protected List<String> mapGraphForProject(){
		BinaryUnion<Variable,IRI> graphRestriction = planNode.getGraphRestriction();
		if(graphRestriction !=null){	
			if(graphRestriction.isFirstType()){
				Variable graphVariable = graphRestriction.getFirst();
				if(projectedInSecondary.contains(graphVariable))return null;
				projectedInSecondary.add(graphVariable);
				List<String> graphSqlToSparql =  new LinkedList<String>();
				graphSqlToSparql.add(Constants.NAME_COLUMN_GRAPH_ID+" AS "+graphVariable.getName());			
				return graphSqlToSparql;
			}			
		}
		return null;
	}	
	
	@Override
	protected boolean applyBind() {
		return true;
	}
}
