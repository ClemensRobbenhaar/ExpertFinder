package org.apache.lucene.analysis.de;

/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.IOException;
import java.util.Set;

import org.apache.lucene.analysis.Token;
import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;

/**
 * A filter that stems German words. It supports a table of words that should
 * not be stemmed at all. The stemmer used can be changed at runtime after the
 * filter object is created (as long as it is a GermanStemmer).
 * 
 * 
 * @version $Id$
 */
public final class GermanStemFilter extends TokenFilter {
	private GermanStemmer stemmer = null;
	private Set<String> exclusionSet = null;

	public GermanStemFilter(TokenStream in) {
		super(in);
		stemmer = new GermanStemmer();
	}

	/**
	 * Builds a GermanStemFilter that uses an exclusion table.
	 */
	public GermanStemFilter(TokenStream in, Set<String> exclusionSet) {
		this(in);
		this.exclusionSet = exclusionSet;
	}

	/**
	 * @return Returns the next token in the stream, or null at EOS
	 */
	public final Token next(final Token reusableToken) throws IOException {
		assert reusableToken != null;
		Token nextToken = input.next(reusableToken);

		if (nextToken == null)
			return null;

		String term = String.copyValueOf(nextToken.termBuffer(), 0, nextToken.termLength());
		// Check the exclusion table.
		if (exclusionSet == null || !exclusionSet.contains(term)) {
			String s = stemmer.stem(term);
			// If not stemmed, don't waste the time adjusting the token.
			if ((s != null) && !s.equals(term))
				nextToken.setTermText(s);
		}
		return nextToken;
	}

	/**
	 * Set a alternative/custom GermanStemmer for this filter.
	 */
	public void setStemmer(GermanStemmer stemmer) {
		if (stemmer != null) {
			this.stemmer = stemmer;
		}
	}

	/**
	 * Set an alternative exclusion list for this filter.
	 */
	public void setExclusionSet(Set<String> exclusionSet) {
		this.exclusionSet = exclusionSet;
	}
}
