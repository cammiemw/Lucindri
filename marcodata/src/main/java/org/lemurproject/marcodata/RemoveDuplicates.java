package org.lemurproject.marcodata;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
import java.util.Set;

public class RemoveDuplicates {

	public static void main(String[] args) throws IOException {
		Scanner dupsFiles = new Scanner(new File("C:\\dev\\Documents_Lucene\\marco_duplicate_list.txt"));
		Set<String> duplicates = new HashSet<String>();
		while (dupsFiles.hasNext()) {
			String dupsLine = dupsFiles.nextLine();
			String[] parts = dupsLine.split(":");
			if (parts.length > 1) {
				List<String> duplicateList = Arrays.asList(parts[1].split(","));
				duplicates.addAll(duplicateList);
			}
		}

		dupsFiles.close();

		// Scanner scanner = new Scanner(new
		// File("C:\\dev\\Documents_Lucene\\MARCO\\collection.tsv"));
		BufferedReader br = new BufferedReader(
				new InputStreamReader(new FileInputStream("C:\\dev\\Documents_Lucene\\MARCO\\collection.tsv")));
		File file = new File("C:\\dev\\Documents_Lucene\\MARCO\\marco_nodups.tsv");

		FileWriter fw = new FileWriter(file);
		BufferedWriter bw = new BufferedWriter(fw);

		String nextLine;
		while ((nextLine = br.readLine()) != null) {
			String[] docParts = nextLine.split("\t");
			String id = String.join("_", "MARCO", docParts[0]);
			if (!duplicates.contains(id)) {
				bw.write(String.join("\t", id, docParts[1]));
				bw.write("\n");
			}
		}
		bw.close();
		br.close();
		// scanner.close();
	}

}
