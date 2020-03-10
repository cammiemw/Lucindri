package org.lemurproject.lucindri.solrsearcher;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.StringJoiner;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrQuery.ORDER;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.CloudSolrClient;
import org.apache.solr.client.solrj.impl.CloudSolrClient.Builder;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocumentList;

public class SolrSearch {

	public static void main(String[] args) throws SolrServerException, IOException {
		String searchPropertiesFilename = args[0];
		SearchOptionsFactory searchOptionsFactory = new SearchOptionsFactory();
		SolrSearchProperties searchProps = searchOptionsFactory.getIndexOptions(searchPropertiesFilename);

		List<String> zkHosts = new ArrayList<String>();
		String zkHost = String.join(":", searchProps.getHostName(), searchProps.getHostPort());
		zkHosts.add(zkHost);
		Builder builder = new CloudSolrClient.Builder(zkHosts, java.util.Optional.empty());
		CloudSolrClient solrClient = builder.build();
		solrClient.setDefaultCollection(searchProps.getCollectionName());

		// SolrClient solrClient = new
		// HttpSolrClient.Builder("http://localhost:8985/solr/toy").build();

		SolrQuery query = new SolrQuery();

		File queriesFile = new File(searchProps.getQuery());
		Scanner scanner = new Scanner(queriesFile);

		String resultsFile = String.join("", searchProps.getResultsFileName(), ".txt");
		BufferedWriter resultsWriter = new BufferedWriter(new FileWriter(resultsFile));
		String resultsTime = String.join("", searchProps.getResultsFileName(), "_time.txt");
		BufferedWriter resultsTimeWriter = new BufferedWriter(new FileWriter(resultsTime));
		String resultsFV = String.join("", searchProps.getResultsFileName(), "_fv.txt");
		BufferedWriter resultsFVWriter = new BufferedWriter(new FileWriter(resultsFV));
		FeatureVectorHelper fvHelper = new FeatureVectorHelper();

		int rank = 1;
		long totaltime = 0;
		while (scanner.hasNext()) {
			String queryLine = scanner.nextLine();
			String[] queryParts = queryLine.split(":");
			String queryNumber = queryParts[0];
			String queryString = queryParts[1].trim();

			String[] queryWords = queryString.split(" ");
			StringJoiner queryBuffer = new StringJoiner(" ");
			for (String word : queryWords) {
				queryBuffer.add(String.join(":", "fulltext", word));
			}

			query.setQuery(queryBuffer.toString());
			System.out.println(
					String.join("", "{!ltr model=", searchProps.getLtrModel(), " efi.text=", queryString, "}"));
			query.set("rq", String.join("", "{!ltr model=", searchProps.getLtrModel(), " reRankDocs=100 efi.text=",
					queryString, "}"));
//			query.set("fl", String.join("", "id,score"));
//			query.set("rq", String.join("", "{!ltr model=", searchProps.getLtrModel(), " efi.text=", queryString, "}"));
			query.set("fl", String.join("", "id,url,score,[features store=", searchProps.getFeatureStore(),
					" efi.text=", queryString, "]"));
			query.set("h1", "on");
			query.set("h1.fl", "fulltext");
			query.setSort("score", ORDER.desc);
			query.setRows(Integer.valueOf(100));
			// query.setFields("fulltext");

			long startTime = System.currentTimeMillis();

			QueryResponse response = solrClient.query(query);
			SolrDocumentList results = response.getResults();
			Map<String, Map<String, List<String>>> highlightsMap = response.getHighlighting();
			Map<String, Map<String, Double>> docFeatureValueMap = new HashMap<String, Map<String, Double>>();
			for (int i = 0; i < results.size(); ++i) {
				String docId = (String) results.get(i).get("id");

				resultsWriter.write(String.join(" ", queryNumber, "Q0", docId, String.valueOf(i + 1),
						results.get(i).get("score").toString(), "solr"));
				resultsWriter.newLine();

				String featuresString = results.get(i).get("[features]").toString();
				String[] featureNamesAndValues = featuresString.split(",");

				Map<String, Double> featureValues = new HashMap<String, Double>();
				for (int j = 0; j < featureNamesAndValues.length; j++) {
					String featureNameAndValue = featureNamesAndValues[j];
					String[] featureParts = featureNameAndValue.split("=");
					Double featureValue = Double.valueOf(featureParts[1]);
					featureValues.put(featureParts[0], featureValue);
				}
				docFeatureValueMap.put(docId, featureValues);
			}
			fvHelper.writeFeatureVectors(queryNumber, "qrel.all", docFeatureValueMap, resultsFVWriter);
			long endTime = System.currentTimeMillis();
			long elapsedTime = endTime - startTime;
			resultsTimeWriter.write(String.join(" ", queryNumber, String.valueOf(elapsedTime)));
			resultsTimeWriter.newLine();
			totaltime += elapsedTime;
			rank++;
		}
		double averageTime = totaltime / ((double) rank);
		resultsTimeWriter.write(String.join("", "Average query time: ", String.valueOf(averageTime)));

		resultsFVWriter.close();
		resultsWriter.close();
		resultsTimeWriter.close();
		scanner.close();
		solrClient.close();
	}

}
