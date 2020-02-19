/*
 * ===============================================================================================
 * Copyright (c) 2016 Carnegie Mellon University and University of Massachusetts. All Rights
 * Reserved.
 *
 * Use of the Lemur Toolkit for Language Modeling and Information Retrieval is subject to the terms
 * of the software license set forth in the LICENSE file included with this software, and also
 * available at http://www.lemurproject.org/license.html
 *
 * ================================================================================================
 */
package org.lemurproject.lucindri.solrsearcher;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Defines the IndexOptions object based on the user input properties file.
 * 
 * @author cmw2
 *
 *         Nov 30, 2016
 */
public class SearchOptionsFactory {

	public SolrSearchProperties getIndexOptions(String propertiesFileName) throws IOException {
		Properties properties = readPropertiesFromFile(propertiesFileName);
		SolrSearchProperties searchProps = new SolrSearchProperties();
		searchProps.setHostName(properties.getProperty("hostName"));
		searchProps.setHostPort(properties.getProperty("hostPort"));
		searchProps.setCollectionName(properties.getProperty("collectionName"));
		searchProps.setQuery(properties.getProperty("query"));
		searchProps.setResultsFileName(properties.getProperty("resultsFileName"));
		searchProps.setQrelsFileName(properties.getProperty("qrelsFileName"));
		searchProps.setFeatureStore(properties.getProperty("featureStore"));
		searchProps.setNumFolds(Integer.valueOf(properties.getProperty("numFolds")).intValue());
		return searchProps;
	}

	private Properties readPropertiesFromFile(String propertiesFileName) throws IOException {
		Properties properties = new Properties();

		File propertiesFile = new File(propertiesFileName);
		InputStream is = new FileInputStream(propertiesFile);
		properties.load(is);

		return properties;
	}

}
