package com.ibm.rdf.store.sparql11;

import java.io.File;
import java.io.FileFilter;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.ibm.rdf.store.sparql11.model.Query;

@RunWith(Parameterized.class)
public class ParseSparqlTest {
	private File f;

	public ParseSparqlTest(File f) {
		this.f = f;
	}

	@Parameters
	public static Collection<File[]> data() throws Exception {
		File d = new File(System.getProperty("sparql.test.dir"));
		assert d.isDirectory();

		File[] queries = d.listFiles(new FileFilter() {
			public boolean accept(File pathname) {
				return pathname.getName().endsWith(".sparql") || pathname.getName().endsWith(".rq");
			}
		});

		List<File[]> files = new LinkedList<File[]>();

		for (File fi : queries) {
			files.add(new File[] { fi });
		}
		return files;

	}

	@Test
	public void testQuery() throws Exception {
		System.out.println("Testing:" + f.getAbsolutePath());
		Query q = SparqlParserUtilities.parseSparqlFile(f.getAbsolutePath(), Collections.<String,String>emptyMap());
		assert q !=null;
		System.out.println(q);
	}
}
