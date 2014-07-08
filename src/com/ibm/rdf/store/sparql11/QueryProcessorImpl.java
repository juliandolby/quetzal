package com.ibm.rdf.store.sparql11;

import java.io.File;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.hpl.jena.query.QueryFactory;
import com.ibm.rdf.store.Context;
import com.ibm.rdf.store.Store;
import com.ibm.rdf.store.query.QueryProcessor;
import com.ibm.rdf.store.runtime.service.sql.StoreImpl;
import com.ibm.rdf.store.runtime.service.types.LiteralInfoResultSet;
import com.ibm.rdf.store.sparql11.merger.QueryInfo;
import com.ibm.rdf.store.sparql11.model.Query;
import com.ibm.rdf.store.sparql11.optimizer.SPARQLOptimizerStatistics;
import com.ibm.rdf.store.sparql11.sqltemplate.SQLGenerator;
import com.ibm.rdf.store.sparql11.sqltemplate.SQLMapping;
import com.ibm.rdf.store.sparql11.sqltemplate.SQLTemplateManager;
import com.ibm.rdf.store.sparql11.sqlwriter.SPARQLToSQLExpression;
import com.ibm.rdf.store.sparql11.sqlwriter.SQLWriterException;
import com.ibm.rdf.store.sparql11.stopt.Planner;
import com.ibm.rdf.store.sparql11.stopt.STPlan;
import com.ibm.rdf.store.utilities.MinusRewriter;
import com.ibm.research.owlql.ruleref.OWLQLSPARQLCompiler;
import com.ibm.research.proppaths.CTENameMgr;
import com.ibm.research.proppaths.CTEToNestedQueryConverter;
import com.ibm.research.proppaths.CodeGenerator;
import com.ibm.research.proppaths.DefaultStoreProcedureManager;
import com.ibm.research.proppaths.DefaultTemporaryTableMgr;
import com.ibm.research.proppaths.PropertyPathRewrite;
import com.ibm.research.proppaths.SQLCommand;
import com.ibm.research.proppaths.StoreProcedure;
import com.ibm.research.proppaths.TemporaryTableMgr;
import com.ibm.wala.util.WalaException;
import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.collections.Pair;

public class QueryProcessorImpl implements QueryProcessor
   {
   final static Logger          logger          = LoggerFactory.getLogger(QueryProcessorImpl.class);
   private String               sparql;
   private Store                store;
   private Context              ctx;
   private OWLQLSPARQLCompiler  compiler;
   Connection                   connection;
   Query                        query           = null;
   long                         optimizerTime   = 0;
   long                         translatorTime  = 0;
   long                         dbTime          = 0;
   String                       sql             = null;

   private static final boolean USE_NEW_PLANNER = true;
   private static final boolean TEST_LP_PLANNER = false;

   public QueryProcessorImpl(String sparql, Connection connection, Store store, Context ctx, OWLQLSPARQLCompiler compiler)
      {
      super();
      this.sparql = sparql;
      this.store = store;
      this.ctx = ctx;
      this.connection = connection;
      this.compiler = compiler;

      // query = SparqlParserUtilities.parseSparqlString(sparql, Collections.<String,String>emptyMap());
      query = compile(sparql);
      }

   public QueryProcessorImpl(File sparql_file, Connection connection, Store store, Context ctx, OWLQLSPARQLCompiler compiler)
      {
      super();
      this.store = store;
      this.ctx = ctx;
      this.connection = connection;
      this.compiler = compiler;
      query = SparqlParserUtilities.parseSparql(sparql_file, Collections.<String, String> emptyMap());
      query = query.getOrigQueryString() == null ? compile(query) : compiler == null ? query
            : compile(query.getOrigQueryString());
      logger.debug("Query: {}", query);

      }

   public QueryProcessorImpl(Query query, Connection connection, Store store, Context ctx, OWLQLSPARQLCompiler compiler)
      {
      super();
      this.store = store;
      this.ctx = ctx;
      this.query = query;
      this.connection = connection;
      this.compiler = compiler;
      this.sparql = query.getOrigQueryString();
     // logger.debug("Query: {}", query);
      query = compile(query);
      logger.debug("Query: {}", query);
      }

   protected Query compile(Query query)
      {
      return compile(compiler, query);
      }

   protected Query compile(String query)
      {
      if (compiler != null)
         {
         return compile(compiler, query);
         }
      else
         {
         return SparqlParserUtilities.parseSparqlString(query);
         }
      }

   public static Query compile(OWLQLSPARQLCompiler compiler, Query query)
      {
      return compiler != null ? compile(compiler, query.toString()) : query;
      }

   public static Query compile(OWLQLSPARQLCompiler compiler, String query)
      {
      com.hp.hpl.jena.query.Query jq = QueryFactory.create(query);
      com.hp.hpl.jena.query.Query compiledJQ = compiler.compile(jq);
      logger.debug("QL query compilation:\n\t OriginalQuery: {}\n\t CompiledQuery:\n\t{}", jq, compiledJQ);
      Query ret = SparqlParserUtilities.parseSparqlString(compiledJQ.toString());
      return ret;
      }

   public boolean execAsk()
      {
      LiteralInfoResultSet askRS = getResultSet(query.getAskQuery().getPattern() != null);

      try
         {
         ResultSet rs = askRS.getResultSet();
         /*
          * if (rs.next()) { String retVal = rs.getString(1); return Boolean.parseBoolean(retVal); }
          */
         boolean theReturn = false;
         if (rs.next())
            theReturn = true;
         Statement s = askRS.getStatement();
         rs.close();
         s.close();
         return theReturn;
         }
      catch (SQLException e)
         {
         e.printStackTrace();
         throw new IllegalArgumentException("execask failed");
         }
      }

   public LiteralInfoResultSet execConstruct()
      {
      return getResultSet(query.getConstructQuery().getPattern() != null && !query.getConstructQuery().getPattern().isEmpty());

      }

   public LiteralInfoResultSet execDescribe()
      {
      return getResultSet(query.getDescribeQuery().getPattern() != null);

      }

   public LiteralInfoResultSet execSelect()
      {
      return getResultSet(query.getSelectQuery().getPattern() != null && !query.getSelectQuery().getPattern().isEmpty());
      }

   public LiteralInfoResultSet runTestSQL(String sqlQuery)
      {
      QueryInfo qi = new QueryInfo(query);
      try
         {
         Statement st = connection.createStatement();
         ResultSet rs = st.executeQuery(sqlQuery);
         return new LiteralInfoResultSet(st, rs, qi.getLiteralBoundVariables());
         }
      catch (SQLException e)
         {
         assert false : e.toString();
         }
      return null;
      }

   private LiteralInfoResultSet getResultSet(boolean condition)
      {
	  Statement st = null;
      ResultSet rs = null;
      QueryInfo qi = null;
      try
         {
         long start = System.currentTimeMillis();
         SPARQLOptimizerStatistics stats = new SPARQLOptimizerStatistics();
         stats.globalStatistics = store.getOverallStatistics();
         stats.perGraphSingleStatistic = store.getPerGraphStatistics();
         new SPARQLToSQLExpression().setStore(this.store);
         SQLTemplateManager.setStoreTemplate(store);
         
         qi = new QueryInfo(query);
         if (condition)
            {
            while (new MinusRewriter.OptionalAsFilterRewriter(query).rewrite())
               ;
            while (new MinusRewriter.FilterAsMinusRewriter(query).rewrite())
               ;

            System.out.println("Query after negation rewrites:" + query);
            
            STPlan plan;
            if (USE_NEW_PLANNER)
            {
               long time = System.currentTimeMillis();
               PropertyPathRewrite rewrite = new PropertyPathRewrite();
               boolean hasNoPropertyPath = rewrite.rewrite(query, true, null, null);
               logger.debug("Rewritten query (after property path rewrite): {}", query);
               STPlan greedyPlan = (new Planner()).plan(query, store, stats);
               logger.debug("Plan : {}", greedyPlan);
               plan = greedyPlan;
               if (plan.getPlanRoot() == null)
                  {
            	  Set<SQLMapping> mappings = HashSetFactory.make();
            	  mappings.add(new SQLMapping("store_name", store.getStoreName(), null));
                  sql = SQLTemplateManager.getSQLString("dummy", mappings);
                  }
               else
                  {

                 

                  /*
                   * logger.info("Clean up"); for (SQLCommand cmd: gen.releaseAllCreatedTemporaryStructures()) { logger.info("{}",cmd.toSQL()); }
                   */
                  if (!hasNoPropertyPath)
                     {
                	 TemporaryTableMgr tmptableMgr = store.getStoreBackend().equals("db2") ? DefaultTemporaryTableMgr
                              .get("tmpspace") : new CTENameMgr("cte");
                     DefaultStoreProcedureManager procMgr = new DefaultStoreProcedureManager("genproc");
                     CodeGenerator gen = new CodeGenerator(store, stats, ctx, query, greedyPlan, tmptableMgr, procMgr, 0);
                     assert gen.isRecursiveQueryOrHasZeroOrOne();
                     if (gen.isDB2Backend())
                        {
                        List<Pair<StoreProcedure, List<SQLCommand>>> dependentProcedures = new LinkedList<Pair<StoreProcedure, List<SQLCommand>>>();
                        StoreProcedure proc = gen.toStoreProcedureSQL(dependentProcedures, null);
                        logger.debug("Code Gen Result:\n");
                        st = connection.createStatement();
                        for (Pair<StoreProcedure, List<SQLCommand>> pair : dependentProcedures)
                           {
                           for (SQLCommand cmd : pair.snd)
                              {
                              if (logger.isDebugEnabled())
                                 {
                                 logger.debug(cmd.toSQL());
                                 }
                              // st.addBatch(cmd.toSQL());
                              boolean exres = st.execute(cmd.toSQL());
                              if (logger.isDebugEnabled())
                              {
                            	  logger.debug("Command execution successful? {} ", exres);
                              }
                              }
                           if (logger.isDebugEnabled())
                              {
                              logger.debug(pair.fst.getSqlDeclarationCode(true));
                              }
                           // st.addBatch(pair.fst.getSqlDeclarationCode(true));
                           st.execute(pair.fst.getSqlDeclarationCode(true));
                           }
                        if (logger.isDebugEnabled())
                           {
                           logger.debug(proc.getSqlDeclarationCode(true));
                           }
                        // st.addBatch(proc.getSqlDeclarationCode(true));
                        st.execute(proc.getSqlDeclarationCode(true));                        
                        // st.executeBatch();
                        sql = proc.getSqlInvocatiionCode();
                        }
                     else if (store.getStoreBackend().equalsIgnoreCase(Store.Backend.shark.name())) {
                    	 //TODO
                    	 throw new RuntimeException("Recursive Property Paths not supported yet in Hive/Shark backend!"); 
                     } else {
                        assert gen.isPostGresBackend() : store.getStoreBackend();
                        sql = gen.toRecursiveSQL();
                     }
                     logger.debug("SQL: {}", sql);
                     }
                  else
                     {
                     //sql = gen.toSQLWithoutStoreProcedureSQL();
                	  SQLGenerator gen = new SQLGenerator(plan,query,store,ctx);
                      sql=gen.toSQL();
                      if (store.getStoreBackend().equalsIgnoreCase(Store.Backend.shark.name())) {
                    	  sql = new CTEToNestedQueryConverter(Store.Backend.shark).transform(sql);
                      }
                     }
                  // SQLGenerator gen = new SQLGenerator(plan,query,store,ctx);
                  // sql=gen.toSQL();

                  }

               time = System.currentTimeMillis() - time;
               System.out.println("Plan:" + plan);
               /*
                * if (TEST_LP_PLANNER) { LPSolver lpsolv = new LPSolver(DefaultPatternComponentFactory.getInstance()); try { greedyPlan =
                * GreedyPlanConverter.convert(greedyPlan); STPlan lpplan = lpsolv.plan(store,stats, query, greedyPlan); if
                * (!lpsolv.isTree(lpplan.getPlanTree())) { logger.warn("Non tree plan!"); } plan = lpplan; logger.debug("LP STPlan:\n{}", lpplan);
                * //logger.debug("Optimal flow: \n"+ comp.getInternalView()); } catch (Exception e) { e.printStackTrace(); throw new
                * RuntimeException(e); }
                * 
                * }
                */
               // logger.debug("Greedy STPLan:\n{}", greedyPlan);
               // logger.info("Greedy plan built in "+ time+" ms");
               }
  
            if (sql == null)
               {
               if (plan.getPlanRoot() == null)
                  {
            	  Set<SQLMapping> mappings = HashSetFactory.make();
             	  mappings.add(new SQLMapping("store_name", store.getStoreName(), null));
                  sql = SQLTemplateManager.getSQLString ("dummy", mappings);
                  }
               else
                  {

                  SQLGenerator gen = new SQLGenerator(plan, query, store, ctx);
                  sql = gen.toSQL();

                  }
               }

            }
         else
            {
            SQLGenerator gen = new SQLGenerator(null, query, store, ctx);
            sql = gen.toSQL();
            }

         optimizerTime = System.currentTimeMillis();

         translatorTime = System.currentTimeMillis();
         logger.info("SPARQL -> " + sparql + "\nGenerated SQL -> " + sql);

         
         // connection = null;
         try
            {
        	if (st == null) {
        		 st = connection.createStatement();
        	}
            rs = st.executeQuery(sql);

            // set times
            dbTime = System.currentTimeMillis() - translatorTime;
            translatorTime = translatorTime - optimizerTime;
            optimizerTime = optimizerTime - start;

            }
         catch (SQLException e)
            {

            dbTime = System.currentTimeMillis() - translatorTime;
            translatorTime = translatorTime - optimizerTime;
            optimizerTime = optimizerTime - start;

            String logmsg = sparql + " | " + optimizerTime + " | " + translatorTime + " | SQLException | " + sql;
            ((StoreImpl) store).logQueryInfo(logmsg);

            try
               {
               if (st != null)
                  {
                  // Closing the statement in case of exception
                  st.close();
                  }
               }
            catch (SQLException e1)
               {
               }
        	throw new RuntimeException(e);
            }

         }
      catch (SQLWriterException e)
         {
       	  throw new RuntimeException(e);
         }
      catch (WalaException e)
         {
    	  throw new RuntimeException(e);
         }
      catch (Exception e)
         {
    	  throw new RuntimeException(e);
         }
      // return rs;
      return new LiteralInfoResultSet(st, rs, qi.getLiteralBoundVariables());
      }

   public com.ibm.rdf.store.query.Query getQuery()
      {
      return query;
      }

   public String getSparql()
      {
      return sparql;
      }

   public long getOptimizerTime()
      {
      return optimizerTime;
      }

   public long getTranslatorTime()
      {
      return translatorTime;
      }

   public long getDbTime()
      {
      return dbTime;
      }

   public String getSql()
      {
      return sql;
      }

   }