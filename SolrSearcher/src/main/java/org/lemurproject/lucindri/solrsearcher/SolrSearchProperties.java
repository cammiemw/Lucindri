package org.lemurproject.lucindri.solrsearcher;

public class SolrSearchProperties {

	private String hostName;
	private String hostPort;
	private String collectionName;
	private String query;
	private String resultsFileName;
	private String qrelsFileName;
	private String featureStore;
	private int numFolds;

	public String getResultsFileName() {
		return resultsFileName;
	}

	public void setResultsFileName(String resultsFileName) {
		this.resultsFileName = resultsFileName;
	}

	public String getHostName() {
		return hostName;
	}

	public void setHostName(String hostName) {
		this.hostName = hostName;
	}

	public String getHostPort() {
		return hostPort;
	}

	public void setHostPort(String hostPort) {
		this.hostPort = hostPort;
	}

	public String getCollectionName() {
		return collectionName;
	}

	public void setCollectionName(String collectionName) {
		this.collectionName = collectionName;
	}

	public String getQuery() {
		return query;
	}

	public void setQuery(String query) {
		this.query = query;
	}

	public String getQrelsFileName() {
		return qrelsFileName;
	}

	public void setQrelsFileName(String qrelsFileName) {
		this.qrelsFileName = qrelsFileName;
	}

	public int getNumFolds() {
		return numFolds;
	}

	public void setNumFolds(int numFolds) {
		this.numFolds = numFolds;
	}

	public String getFeatureStore() {
		return featureStore;
	}

	public void setFeatureStore(String featureStore) {
		this.featureStore = featureStore;
	}

}
