package org.lemurproject.lucindri.indexer.documentparser;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.StringJoiner;

import org.apache.lucene.analysis.Analyzer;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.safety.Whitelist;
import org.jsoup.select.Elements;
import org.lemurproject.lucindri.indexer.domain.IndexingConfiguration;
import org.lemurproject.lucindri.indexer.domain.ParsedDocument;
import org.lemurproject.lucindri.indexer.domain.ParsedDocumentField;
import org.lemurproject.lucindri.indexer.factory.ConfigurableAnalyzerFactory;
import org.xml.sax.SAXException;

public class IndriGov2DocumentParser extends DocumentParser {

	private final static String EXTERNALID_FIELD = "id";
	private final static String ID_FIELD = "internalId";
	private final static String BODY_FIELD = "body";
	private final static String TITLE_FIELD = "title";
	private final static String HEADING_FIELD = "heading";
	private final static String URL_FIELD = "url";

	private int docNum;
	private Iterator<File> fileIterator;
	private BufferedReader br;
	private String nextLine;
	private Analyzer analyzer;
	private List<String> fieldsToIndex;
	private boolean indexFullText;

	public IndriGov2DocumentParser(IndexingConfiguration options) throws IOException {
		// File folder = Paths.get(options.getDataDirectory()).toFile();
		List<File> files = new ArrayList<>();
		listFiles(options.getDataDirectory(), files);
		fileIterator = files.iterator();
		getNextScanner();
		nextLine = "";
		ConfigurableAnalyzerFactory analyzerFactory = new ConfigurableAnalyzerFactory();
		analyzer = analyzerFactory.getConfigurableAnalyzer(options);
		docNum = 0;
		fieldsToIndex = options.getIndexFields();
		indexFullText = options.isIndexFullText();
	}

	public static void listFiles(String directoryName, List<File> files) {
		File directory = new File(directoryName);

		// get all the files from a directory
		File[] fList = directory.listFiles();
		for (File file : fList) {
			if (file.isFile()) {
				files.add(file);
			} else if (file.isDirectory()) {
				listFiles(file.getAbsolutePath(), files);
			}
		}
	}

	private void getNextScanner() throws IOException {
		if (fileIterator.hasNext()) {
			File nextFile = fileIterator.next();
			InputStream fileStream = new FileInputStream(nextFile);
			Reader decoder = new InputStreamReader(fileStream, "UTF-8");
			br = new BufferedReader(decoder);
		} else {
			br = null;
		}
	}

	@Override
	public boolean hasNextDocument() {
		return fileIterator.hasNext() || nextLine != null;
	}

	@Override
	public ParsedDocument getNextDocument() throws IOException, SAXException {
		String docno = "";
		String url = "";
		while (br != null) {
			if ((nextLine = br.readLine()) == null) {
				br.close();
				getNextScanner();
				if (br != null) {
					nextLine = br.readLine();
				}
			}
			if (nextLine != null) {
				docNum++;

				StringJoiner docBuffer = null;

				// Get values from header
				if (nextLine.startsWith("<DOCNO>")) {
					docno = nextLine.substring(7, nextLine.length() - 8);
				}
//				if (nextLine.startsWith("WARC-Target-URI:")) {
//					url = nextLine.split(" ")[1];
//				}
//
				if (nextLine.equals("<DOCHDR>")) {
					docBuffer = new StringJoiner("");
					nextLine = br.readLine();
					docBuffer.add(nextLine);
					if (nextLine.startsWith("HTTP") || nextLine.startsWith("http")) {
						url = nextLine;
					}
				}

				while (docBuffer != null && ((nextLine = br.readLine()) != null) /* && !(nextLine.length() == 6) */
						&& !nextLine.startsWith("</DOC>")) {
					// nextLine = nextLine.replaceAll("\\&\\#[0-9]+\\;", "");
					docBuffer.add(nextLine);
					docBuffer.add("\n");
				}

				if (docBuffer != null) {
					try {
						Document htmlDoc = Jsoup.parse(docBuffer.toString());

//						if (docno == null || docno.length() == 0) {
//							docno = String.valueOf(docNum);
//						}

						ParsedDocument doc = new ParsedDocument();
						doc.setDocumentFields(new ArrayList<ParsedDocumentField>());

						ParsedDocumentField externalIdField = new ParsedDocumentField(EXTERNALID_FIELD, docno, false);
						doc.getDocumentFields().add(externalIdField);

						ParsedDocumentField internalIdField = new ParsedDocumentField(ID_FIELD, String.valueOf(docNum),
								false);
						doc.getDocumentFields().add(internalIdField);

						if (fieldsToIndex.contains(BODY_FIELD)) {
							String body = "";
							Elements bodyElements = htmlDoc.getElementsByTag("body");
							if (bodyElements != null && bodyElements.size() > 0) {
								Element element = bodyElements.get(0);
								body = Jsoup.clean(element.toString(), Whitelist.none());
							}
							ParsedDocumentField bodyField = new ParsedDocumentField(BODY_FIELD, body, false);
							doc.getDocumentFields().add(bodyField);
						}

						if (fieldsToIndex.contains(TITLE_FIELD)) {
							String title = "";
							Elements titleElements = htmlDoc.getElementsByTag("title");
							if (titleElements != null && titleElements.size() > 0) {
								Element element = titleElements.get(0);
								title = Jsoup.clean(element.toString(), Whitelist.none());
							}
							ParsedDocumentField titleField = new ParsedDocumentField(TITLE_FIELD, title, false);
							doc.getDocumentFields().add(titleField);
						}

						if (fieldsToIndex.contains(HEADING_FIELD)) {
							Elements headerElements = htmlDoc.getElementsByTag("h1");
							StringJoiner headersBuffer = new StringJoiner(" ");
							if (headerElements != null && headerElements.size() > 0) {
								for (Element headerElement : headerElements) {
									headersBuffer.add(headerElement.toString());
								}
							}
							ParsedDocumentField headingField = new ParsedDocumentField(HEADING_FIELD,
									headersBuffer.toString(), false);
							doc.getDocumentFields().add(headingField);
						}

						if (fieldsToIndex.contains(URL_FIELD)) {
							ParsedDocumentField urlField = new ParsedDocumentField(URL_FIELD, url, false);
							doc.getDocumentFields().add(urlField);
						}

						// Index fullText (catch-all) field
						if (indexFullText) {
							ParsedDocumentField fullTextField = new ParsedDocumentField(FULLTEXT_FIELD,
									Jsoup.clean(docBuffer.toString(), Whitelist.none()), false);
							doc.getDocumentFields().add(fullTextField);
						}

						return doc;
					} catch (Exception e) {
						System.out.println("Could not parse document: " + docno);
					}
				}
			}
			// }
		}
		return null;
	}

}
