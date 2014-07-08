package com.ibm.rdf.store.sparql;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * @author oudrea
 * 
 * Creates connections to the RDFStore test database.
 */
public class ConnectionFactory {
	
//	private static final String JDBC_URL = "jdbc:db2:RDFTEST";
//	private static String JDBC_USER = null;
//	private static final String JDBC_PASSWORD = null;

	private static final String JDBC_URL = "jdbc:db2://pasta-dev.watson.ibm.com/RDFTEST";
	private static String JDBC_USER = "db2inst4";
	private static final String JDBC_PASSWORD = "sheruser";
	
	private static boolean driverLoaded = false;
	
	public static Connection getConnection() throws ClassNotFoundException, SQLException {
		if(!driverLoaded) {
			 Class.forName("com.ibm.db2.jcc.DB2Driver");                             
		     //System.out.println("**** Loaded the JDBC driver");
			 driverLoaded = true;
		}
		if(JDBC_USER == null)
			return DriverManager.getConnection(JDBC_URL);
		else return DriverManager.getConnection(JDBC_URL, JDBC_USER, JDBC_PASSWORD);
	}
	
	public static Connection getConnection(String db, String usr, String passwd) throws ClassNotFoundException, SQLException {
		if(!driverLoaded) {
			 Class.forName("com.ibm.db2.jcc.DB2Driver");                             
		     //System.out.println("**** Loaded the JDBC driver");
			 driverLoaded = true;
		}
		
		return DriverManager.getConnection("jdbc:db2:" + db, usr, passwd);
	}
}
