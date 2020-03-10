package org.lemurproject.lucindri.solrsearcher;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Scanner;
import java.util.StringJoiner;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrQuery.ORDER;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.CloudSolrClient;
import org.apache.solr.client.solrj.impl.CloudSolrClient.Builder;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocumentList;

public class SolrFeatureGenerator {

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

		Map<String, String> queryTextMap = new HashMap<String, String>();

		int rank = 1;
		long totaltime = 0;
		Map<String, Map<String, Map<String, Double>>> queryDocFeatureValueMap = new HashMap<String, Map<String, Map<String, Double>>>();
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
			queryTextMap.put(queryNumber, queryLine);
			String SDMquery = createSDMQueries(queryString).trim();
			System.out.println(SDMquery);
			query.set("fl", String.join("", "id,url,score,[features store=", searchProps.getFeatureStore(),
					" efi.text='", queryString, "' efi.text_sdm='", SDMquery, "']"));
			query.setSort("score", ORDER.desc);
			query.setRows(Integer.valueOf(100));
			System.out.println(query.toString());

			QueryResponse response = solrClient.query(query);
			SolrDocumentList results = response.getResults();
			Map<String, Map<String, Double>> docFeatureValueMap = new HashMap<String, Map<String, Double>>();
			for (int i = 0; i < results.size(); ++i) {
				String docId = (String) results.get(i).get("id");
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

				// resultsWriter.write(String.join(" ", queryNumber, "Q0", docId,
				// String.valueOf(i+1), results.get(i).get("score").toString(), "solr"));
				// resultsWriter.newLine();
			}
			fvHelper.writeFeatureVectors(queryNumber, searchProps.getQrelsFileName(), docFeatureValueMap,
					resultsFVWriter);
			queryDocFeatureValueMap.put(queryNumber, docFeatureValueMap);
			long startTime = System.currentTimeMillis();

			long endTime = System.currentTimeMillis();
			long elapsedTime = endTime - startTime;
			resultsTimeWriter.write(String.join(" ", queryNumber, String.valueOf(elapsedTime)));
			resultsTimeWriter.newLine();
			totaltime += elapsedTime;
			rank++;
		}
		double averageTime = totaltime / ((double) rank);
		resultsTimeWriter.write(String.join("", "Average query time: ", String.valueOf(averageTime)));

		// Divide into train and test sets
		int numQueriesPerFold = 1;
		if (queryDocFeatureValueMap.size() % searchProps.getNumFolds() == 0) {
			numQueriesPerFold = queryDocFeatureValueMap.size() / searchProps.getNumFolds();
		} else {
			numQueriesPerFold = queryDocFeatureValueMap.size() / searchProps.getNumFolds() + 1;
		}

		for (int i = 0; i < searchProps.getNumFolds(); i++) {
			int startTestNum = i * numQueriesPerFold;
			int stopTestNum = startTestNum + numQueriesPerFold;
			String trainFV = String.join("", searchProps.getResultsFileName(), "_trainfv", String.valueOf(i), ".txt");
			BufferedWriter trainFVWriter = new BufferedWriter(new FileWriter(trainFV));
			String testFV = String.join("", searchProps.getResultsFileName(), "_testfv", String.valueOf(i), ".txt");
			BufferedWriter testFVWriter = new BufferedWriter(new FileWriter(testFV));
			String testQueries = String.join("", searchProps.getResultsFileName(), "_testqueries", String.valueOf(i),
					".txt");
			BufferedWriter testQueryWriter = new BufferedWriter(new FileWriter(testQueries));
			int numQuery = 0;
			for (Entry<String, Map<String, Map<String, Double>>> queryValue : queryDocFeatureValueMap.entrySet()) {
				if (numQuery >= startTestNum && numQuery < stopTestNum) {
					String queryLine = queryTextMap.get(queryValue.getKey());
					testQueryWriter.write(String.join("", queryLine, "\n"));
					fvHelper.writeFeatureVectors(queryValue.getKey(), searchProps.getQrelsFileName(),
							queryValue.getValue(), testFVWriter);
				} else {
					fvHelper.writeFeatureVectors(queryValue.getKey(), searchProps.getQrelsFileName(),
							queryValue.getValue(), trainFVWriter);
				}
				numQuery++;
			}
			trainFVWriter.close();
			testFVWriter.close();
			testQueryWriter.close();
		}

		resultsFVWriter.close();
		resultsWriter.close();
		resultsTimeWriter.close();
		scanner.close();
		solrClient.close();
	}

	private static String createSDMQueries(String queryString) throws IOException {

		queryString = queryString.trim();
		String[] queryWordArray = queryString.split(" ");
		String SDMquery = null;
		if (queryWordArray.length < 2) {
			SDMquery = String.join("", "~and(", queryString, ")");
		} else {

			// Create SDM query
			StringJoiner queryBuffer = new StringJoiner("");
			queryBuffer.add("~wsum("); // begin query

			queryBuffer.add(String.join("", "0.7 ~and(", queryString, ")"));

			// nears
			queryBuffer.add(" 0.2 ~and(");
			for (int i = 0; i < queryWordArray.length - 1; i++) {
				String bigram = String.join(" ", queryWordArray[i], queryWordArray[i + 1]);
				queryBuffer.add(String.join("", " 1.0 ~1(", bigram, ")"));

			}
			queryBuffer.add(")");

			// windows
			queryBuffer.add(" 0.1 ~and(");
			for (int i = 0; i < queryWordArray.length - 1; i++) {
				String bigram = String.join(" ", queryWordArray[i], queryWordArray[i + 1]);
				queryBuffer.add(String.join("", " ~uw8(", bigram, ")"));

			}
			queryBuffer.add(")");

			queryBuffer.add(")"); // end query

			SDMquery = queryBuffer.toString();
		}
		return SDMquery;
	}

}
