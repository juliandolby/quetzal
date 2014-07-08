package com.ibm.rdf.store.hashing;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Vector;

import com.ibm.research.helix.hash.IHashFunction;
import com.ibm.research.helix.rabinfingerprints.RabinFingerprintsFamily;
import com.ibm.research.helix.rabinfingerprints.StringToBitConverter;

public class RabinHashFamily implements IHashingFamily {
	int targetRange;
	int fSize;
	RabinFingerprintsFamily<String> setOfHash;
	Vector<IHashFunction<String>> hashes;
	ArrayList<Integer> hashValues;

	public RabinHashFamily(int domain, int familySize) {
		targetRange = domain;
		fSize = familySize;
		hashes = new Vector<IHashFunction<String>>();
		hashValues = new ArrayList<Integer>();

		setOfHash = new RabinFingerprintsFamily<String>(
				new StringToBitConverter());

		Iterator<IHashFunction<String>> iter = setOfHash
				.getRabinFingerprintHashFunctions(fSize, 1000);

		while (iter != null && iter.hasNext()) {
			hashes.add(iter.next());
		}
		if (hashes.size() < fSize) {
			System.out
					.println("Cannot compute hashfunctions for family size of:"
							+ fSize);
			fSize = hashes.size();
		}
	}

	public int getFamilySize(String predicate) {
		return fSize;
	}

	public void computeHash(String value) {
		hashValues.clear();
		for (int hashID = 0; hashID < fSize; hashID++) {
			IHashFunction<String> h = hashes.elementAt(hashID);

			int hashValue = Math.abs((int) (h.hash(value)) % targetRange);
			hashValues.add(hashValue);
		}
	}

	public int hash(String value, int hashID) {
		IHashFunction<String> h = hashes.elementAt(hashID);

		int hashValue = Math.abs((int) (h.hash(value)) % targetRange);
		return hashValue;
	}

	// Required for Single triple operations.
	public int[] getHash(String value) {
		int[] hashVals = new int[getFamilySize(value)];
		for (int i = 0; i < hashVals.length; i++) {
			hashVals[i] = hash(value, i);
		}
		return hashVals;
	}

	public static void main(String args[]) {
		for (int i = 0; i < 4; i++) {
			new RabinHashFamily(4, i);
		}
	}
}
