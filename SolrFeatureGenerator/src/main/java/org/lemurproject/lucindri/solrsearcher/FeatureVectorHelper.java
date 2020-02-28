package org.lemurproject.lucindri.solrsearcher;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Scanner;
import java.util.SortedSet;
import java.util.StringJoiner;
import java.util.TreeSet;

public class FeatureVectorHelper {

	public Map<String, Map<String, Double>> initializeFeatureValuesForDocIds(
			Map<String, Map<String, Double>> docFeaureValueMap, List<String> docIds, List<Feature> features) {

		for (String docId : docIds) {
			docFeaureValueMap.putIfAbsent(docId, new HashMap<>());
			for (Feature feature : features) {
				docFeaureValueMap.get(docId).putIfAbsent(feature.getName(), Double.valueOf(0.0));
			}
		}
		return docFeaureValueMap;
	}

	public Map<String, Map<String, Double>> setFeaturesToZero(Map<String, Map<String, Double>> docFeatureValueMap,
			List<Feature> features) {
		for (String docId : docFeatureValueMap.keySet()) {
			initializeFeaturesForDoc(docFeatureValueMap, docId, features);
		}
		return docFeatureValueMap;
	}

	private Map<String, Map<String, Double>> initializeFeaturesForDoc(
			Map<String, Map<String, Double>> docFeatureValueMap, String docId, List<Feature> features) {
		docFeatureValueMap.putIfAbsent(docId, new HashMap<>());
		for (Feature feature : features) {
			docFeatureValueMap.get(docId).putIfAbsent(feature.getName(), Double.valueOf(0.0));
		}
		return docFeatureValueMap;
	}

	/**
	 * Creates a map for each feature name and the set of values that feature has
	 * 
	 * @param docFeatureValueMap
	 * @return
	 */
	public Map<String, SortedSet<Double>> getAllValuesForFeatureNames(
			Map<String, Map<String, Double>> docFeatureValueMap) {
		// Put all feature scores in a Map to find max and min
		Map<String, SortedSet<Double>> featureName2ValuesMap = new HashMap<>();
		for (Map<String, Double> docFeatureValues : docFeatureValueMap.values()) {
			for (Entry<String, Double> featureValue : docFeatureValues.entrySet()) {
				featureName2ValuesMap.putIfAbsent(featureValue.getKey(), new TreeSet<>());
				featureName2ValuesMap.get(featureValue.getKey()).add(featureValue.getValue());
			}
		}
		return featureName2ValuesMap;
	}

	public Integer getQrelScoreForQueryAndDoc(String queryId, String docId,
			Map<Integer, Map<String, Integer>> qrelsMap) {
		Integer qrelScore = Integer.valueOf(0);
		Map<String, Integer> queryQrels = qrelsMap.get(Integer.valueOf(queryId));
		if (queryQrels != null) {
			if (queryQrels.get(docId) != null) {
				qrelScore = queryQrels.get(docId);
				if (qrelScore.intValue() <= 0) {
					qrelScore = Integer.valueOf(0);
				}
//				} else {
//					qrelScore = Integer.valueOf(1);
//				}
			}
		}
		return qrelScore;
	}

	public double normalizeScore(Entry<String, Double> featureScoreEntry, Map<String, Double> minScores,
			Map<String, Double> maxScores) {
		double minValue = minScores.get(featureScoreEntry.getKey()).doubleValue();
		double maxValue = maxScores.get(featureScoreEntry.getKey()).doubleValue();
		double normalizedScore = 0.0d;
		if (maxValue - minValue != 0.0) {
			normalizedScore = (featureScoreEntry.getValue() - minValue) / (maxValue - minValue);
		}
		return normalizedScore;
	}

	public void writeFeatureVectors(String queryId, String qrelFileName,
			Map<String, Map<String, Double>> docFeatureValueMap, BufferedWriter writer) throws IOException {
		// Put all feature scores in a Map to find max and min
		Map<String, SortedSet<Double>> featureValues = getAllValuesForFeatureNames(docFeatureValueMap);
		// Get min and max scores for each feature
//		Map<String, Double> minScores = new HashMap<>();
//		Map<String, Double> maxScores = new HashMap<>();
//		for (String featureName : featureValues.keySet()) {
//			minScores.put(featureName, Double.valueOf(featureValues.get(featureName).first()));
//			maxScores.put(featureName, Double.valueOf(featureValues.get(featureName).last()));
//		}

		Map<Integer, Map<String, Integer>> qrelsMap = loadQrels(qrelFileName);
		boolean wroteFeatureDetails = false;
		// Write feature vectors file with normalized values
		// for (HitObject hit : responseObject2.getHits().getHits()) {
		for (Entry<String, Map<String, Double>> docFeatureValueMapEntry : docFeatureValueMap.entrySet()) {
			String docId = docFeatureValueMapEntry.getKey();
			Integer qrelScore = getQrelScoreForQueryAndDoc(queryId, docId, qrelsMap);
			StringJoiner docFeaturesStringBuffer = new StringJoiner("");
			docFeaturesStringBuffer.add(String.join("", qrelScore.toString(), " qid:", queryId));

			if (!wroteFeatureDetails) {
				System.out.print("#Feature Names - ");
			}
			int featureNum = 1;
			for (Entry<String, Double> featureScoreEntry : docFeatureValueMapEntry.getValue().entrySet()) {
				if (!wroteFeatureDetails) {
					System.out.println(
							String.join("", String.valueOf(featureNum), ":", featureScoreEntry.getKey() + " "));
				}
				// double normalizedScore = normalizeScore(featureScoreEntry, minScores,
				// maxScores);
				docFeaturesStringBuffer.add(String.join("", " ", String.valueOf(featureNum), ":",
						String.valueOf(featureScoreEntry.getValue())));
				featureNum++;
			}
			if (!wroteFeatureDetails) {
				wroteFeatureDetails = true;
			}
			writer.write(String.join("", docFeaturesStringBuffer.toString(), " #", docId, "\n"));
		}

	}

	private Map<Integer, Map<String, Integer>> loadQrels(String qrelsFileName) throws FileNotFoundException {
		Scanner scanner = new Scanner(new File(qrelsFileName));
		Map<Integer, Map<String, Integer>> qrelsMap = new HashMap<>();
		while (scanner.hasNextLine()) {
			String line = scanner.nextLine();
			String[] lineParts = line.split(" ");
			Integer queryId = Integer.valueOf(lineParts[0]);
			String docId = lineParts[2];
			Integer qrelScore = Integer.valueOf(lineParts[3]);
			qrelsMap.putIfAbsent(queryId, new HashMap<>());
			qrelsMap.get(queryId).put(docId, qrelScore);
		}
		scanner.close();
		return qrelsMap;
	}

}
