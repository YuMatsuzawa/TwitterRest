/**
 * 
 */
package matz.twitter.search.keyword;

import java.util.List;

import twitter4j.*;
import twitter4j.conf.ConfigurationBuilder;

/**testing twitter API of keyword search.
 * @author Matsuzawa
 *
 */
public class KeywordSearchTest {
	public static ConfigurationBuilder cb = new ConfigurationBuilder();
	

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		cb.setDebugEnabled(true)
		.setOAuthConsumerKey("KuG34YZ202uEZCHgHQhQ")
		.setOAuthConsumerSecret("5R9nqymC0Zl5pF5KSDr80gCOTHqkaPS9sehrGoMpI")
		.setOAuthAccessToken("352840258-QrB0NPIYNtGiNUt3vnetmnMumnppJETnvccdZVJa")
		.setOAuthAccessTokenSecret("PkuqzpWNCWglAGKtMfUeg1UVCRQqjqesmlOuyrVA")
	    .setJSONStoreEnabled(true);
		
		TwitterFactory tf = new TwitterFactory(cb.build());
		Twitter tw = tf.getInstance();
		
		Query query = new Query("���ە�");
		query.count(100);
		
		try {
			QueryResult qr = tw.search(query);
			
			List<Status> res = qr.getTweets();
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}

}