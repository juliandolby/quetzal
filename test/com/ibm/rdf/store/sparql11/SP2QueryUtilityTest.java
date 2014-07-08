package com.ibm.rdf.store.sparql11;

import static com.ibm.rdf.store.sparql11.TestRunner.DB2TestData.getStore;

import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.rdf.store.sparql11.TestRunner.SharkTestData;
import com.ibm.rdf.store.testing.RandomizedRepeat;

//@RunWith(com.ibm.rdf.store.testing.RandomizedRepeatRunner.class)
//@RandomizedRepeat(1)
public class SP2QueryUtilityTest<D> extends TestRunner<D>
   {
   private final String queryDir;

   public SP2QueryUtilityTest(DatabaseEngine<D> engine, D data, int[] answers, String queryDir)
      {
      super(data, engine, answers);
      this.queryDir = queryDir;
      }

   protected final static int[] sp2b1MAnswers   = new int[]
                                                   { 1, 32770, 52676, 379, 0, 2586733, 35241, 35241, 62795, 292, 400, 4, 572, 10, 1, 1, 0                                  };
   protected final static int[] sp2b10MAnswers  = new int[]
                                                   { 1, 613729, 323456, 2209, 0, -1, 404903, 404903, -1, -1, 493, 4, 656, 10, 1, 1, 0 };
   protected final static int[] sp2b100MAnswers = new int[]
                                                   { 1, 9050604, 1466402, 10143, 0, -1, 2016996, 2016996, 9812030, 14645, 493, 4, 656, 10, 1, 1, 0                         };

   public static class DB2SP2B10MHelix1 extends SP2QueryUtilityTest<DB2TestData>
      {
      private static final DB2TestData data = DB2TestData.getStore("jdbc:db2://helix1.pok.ibm.com:50001/sp2b10m", "sp2b10m", "db2inst1",
                                                  "db2inst1", "db2inst1", false);

      public DB2SP2B10MHelix1()
         {
         super(new DB2Engine(), data, sp2b10MAnswers, "../rdfstore-data/sp2b_queries_rev/");
         }
      }

   public static class PSQLSP2B10MHelix1 extends SP2QueryUtilityTest<PSQLTestData>
      {
      private static final PSQLTestData data = PSQLTestData.getStore("jdbc:postgresql://helix1.pok.ibm.com:24973/sp2b10m", "sp2b10m", "akement",
                                                  "passw0rd", "db2inst1", false);

      public PSQLSP2B10MHelix1()
         {
         super(new PSQLEngine(), data, sp2b10MAnswers, "../rdfstore-data/sp2b_queries_rev/");
         }
      }

   public static class PSQLSP2B100MHelix1 extends SP2QueryUtilityTest<PSQLTestData>
      {
      private static final PSQLTestData data = PSQLTestData.getStore("jdbc:postgresql://helix1.pok.ibm.com:24973/sp2b100m", "sp2b100m", "akement",
                                                  "passw0rd", "db2inst1", false);

      public PSQLSP2B100MHelix1()
         {
         super(new PSQLEngine(), data, sp2b100MAnswers, "../rdfstore-data/sp2b_queries_rev/");
         }
      }

   public static class DB2SP2B1MHelix1 extends SP2QueryUtilityTest<DB2TestData>
   {
   private static final DB2TestData data = getStore("jdbc:db2://helix1.pok.ibm.com:50001/sp2b", "sp2b1m", "db2inst1",
                                               "db2inst1", "db2inst1", false);

   public DB2SP2B1MHelix1()
      {
      super(new DB2Engine(), data, sp2b1MAnswers, "../rdfstore-data/sp2b_queries_rev/");
      }
   }

   public static class SharkSP2B1MVM9_12_196_243 extends SP2QueryUtilityTest<SharkTestData>
      {
	   private static final SharkTestData data =  SharkTestData.getStore("jdbc:hive2://9.12.196.243:10000/default", "sp2b1m", "root", "nkoutche",
    		   "default", false);
 

        public SharkSP2B1MVM9_12_196_243()
         {
         super(new SharkEngine(), data, sp2b1MAnswers, "../rdfstore-data/sp2b_queries_rev/");
         }
      }

   public static class PSQLSP2B1MHelix1 extends SP2QueryUtilityTest<PSQLTestData>
      {
      private static final PSQLTestData data = PSQLTestData.getStore("jdbc:postgresql://helix1.pok.ibm.com:24973/sp2b1m", "sp2b1m", "akement",
                                                  "passw0rd", "db2inst1", false);

      public PSQLSP2B1MHelix1()
         {
         super(new PSQLEngine(), data, sp2b1MAnswers, "../rdfstore-data/sp2b_queries_rev/");
         }
      }

   public static class SP2B10M_R_MIN1 extends SP2QueryUtilityTest<DB2TestData>
      {
      private static final DB2TestData data = getStore("jdbc:db2://min-1.watson.ibm.com:50001/testrev", "sp2b10m_r", "db2inst1",
                                                  "sheruser", "db2inst1", false);

      public SP2B10M_R_MIN1()
         {
         super(new DB2Engine(), data, null, "../rdfstore-data/sp2b_queries_rev/");
         }
      }

   public static class SP2B10K_R_MIN1 extends SP2QueryUtilityTest<DB2TestData>
      {
      private static final DB2TestData data = getStore("jdbc:db2://min-1.watson.ibm.com:50001/testrev", "sp2b10k_r", "db2inst1",
                                                  "sheruser", "db2inst1", false);

      public SP2B10K_R_MIN1()
         {
         super(new DB2Engine(), data, null, "../rdfstore-data/sp2b_queries_rev/");
         }
      }

   public static class SP2B100M_R_AMAZON extends SP2QueryUtilityTest<DB2TestData>
      {
      private static final DB2TestData data = getStore("jdbc:db2://localhost:50000/testrev", "sp2b5m_r", "db2inst1", "ihaterc2",
                                                  "db2inst1", false);

      public SP2B100M_R_AMAZON()
         {
         super(new DB2Engine(), data, sp2b100MAnswers, "../rdfstore-data/sp2b_queries_rev/");
         }
      }

   public static class SP2B100M_R_RC2 extends SP2QueryUtilityTest<DB2TestData>
      {
      private static final DB2TestData data = getStore("jdbc:db2://9.47.202.45:50001/sp2b", "sp2b100m", "db2inst2", "db2admin",
                                                  "db2inst2", false);

      public SP2B100M_R_RC2()
         {
         super(new DB2Engine(), data, sp2b100MAnswers, "../rdfstore-data/sp2b_queries_rev/");
         }
      }

   public static class SP2B10M_R_RC2 extends SP2QueryUtilityTest<DB2TestData>
      {
      private static final DB2TestData data = getStore("jdbc:db2://9.47.202.45:50001/sp2b", "sp2b10m", "db2inst2", "db2admin",
                                                  "db2inst2", false);

      public SP2B10M_R_RC2()
         {
         super(new DB2Engine(), data, null, "../rdfstore-data/sp2b_queries_rev/");
         }
      }

   public static class SP2B100K_R_RC2 extends SP2QueryUtilityTest<DB2TestData>
      {
      private static final DB2TestData data = getStore("jdbc:db2://9.47.202.45:50001/sp2b", "sp2b100k", "db2inst2", "db2admin",
                                                  "db2inst2", false);

      public SP2B100K_R_RC2()
         {
         super(new DB2Engine(), data, null, "../rdfstore-data/sp2b_queries_rev/");
         }
      }

   @Test
   public void testQueryQ1() throws Exception
      {
      String file = queryDir + "q1.sparql";
      System.err.println("Testing:" + file);
      executeQuery(file, 0);
      }

   @Test
   public void testQueryQ2() throws Exception
      {
      String file = queryDir + "q2.sparql";
      System.err.println("Testing:" + file);
      executeQuery(file, 1);
      }

   @Test
   public void testQueryQ3a() throws Exception
      {
      String file = queryDir + "q3a.sparql";
      System.err.println("Testing:" + file);
      executeQuery(file, 2);
      }

   @Test
   public void testQueryQ3b() throws Exception
      {
      String file = queryDir + "q3b.sparql";
      System.err.println("Testing:" + file);
      executeQuery(file, 3);
      }

   @Test
   public void testQueryQ3c() throws Exception
      {
      String file = queryDir + "q3c.sparql";
      System.err.println("Testing:" + file);
      executeQuery(file, 4);
      }

   //@Test
   public void testQueryQ4() throws Exception
      {
      String file = queryDir + "q4.sparql";
      System.err.println("Testing:" + file);
      int result = executeQuery(file);
      executeQuery(file, 5);
      System.out.println(file + " has : " + result + " rows");
      }

   @Test
   public void testQueryQ5a() throws Exception
      {
      String file = queryDir + "q5a.sparql";
      System.err.println("Testing:" + file);
      executeQuery(file, 6);
      }

   @Test
   public void testQueryQ5b() throws Exception
      {
      String file = queryDir + "q5b.sparql";
      System.err.println("Testing:" + file);
      executeQuery(file, 7);
      }

   @Test
   public void testQueryQ6() throws Exception
      {
      String file = queryDir + "q6.sparql";
      System.err.println("Testing:" + file);
      executeQuery(file, 8);
      }

   @Test
   public void testQueryQ7() throws Exception
      {
      String file = queryDir + "q7.sparql";
      System.err.println("Testing:" + file);
      executeQuery(file, 9);
      }

   @Test
   public void testQueryQ8() throws Exception
      {
      String file = queryDir + "q8.sparql";
      System.err.println("Testing:" + file);
      executeQuery(file, 10);
      }

   @Test
   public void testQueryQ9() throws Exception
      {
      String file = queryDir + "q9.sparql";
      System.err.println("Testing:" + file);
      executeQuery(file, 11);
      }

   @Test
   public void testQueryQ10() throws Exception
      {
      String file = queryDir + "q10.sparql";
      System.err.println("Testing:" + file);
      executeQuery(file, 12);
      }

   @Test
   public void testQueryQ11() throws Exception
      {
      String file = queryDir + "q11.sparql";
      System.err.println("Testing:" + file);
      executeQuery(file, 13);
      }

    @Test
   public void testQueryQ12a() throws Exception
      {
      String file = queryDir + "q12a.sparql";
      System.err.println("Testing:" + file);
      executeQuery(file, 14);
      }

   @Test
   public void testQueryQ12b() throws Exception
      {
      String file = queryDir + "q12b.sparql";
      System.err.println("Testing:" + file);
      executeQuery(file, 15);
      }

   @Test
   public void testQueryQ12c() throws Exception
      {
      String file = queryDir + "q12c.sparql";
      System.err.println("Testing:" + file);
      executeQuery(file, 16);
      }

   @RunWith(com.ibm.rdf.store.testing.RandomizedRepeatRunner.class)
   @RandomizedRepeat(5)
   public static class Driver extends SP2QueryUtilityTest<DB2TestData>
      {
      public Driver()
         {
         super(new DB2Engine(), junitHackData, sp2b100MAnswers, junitHackDirectory);
         }
      }

   public static void main(String[] args)
      {
      main(Driver.class, "sp2b100m_r", args);
      }
   }
