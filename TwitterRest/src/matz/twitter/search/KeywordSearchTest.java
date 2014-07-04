package matz.twitter.search;

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
		
		
		try {
			Query query = null;
			QueryResult qr = null;
			//List<Status> res = new ArrayList<Status>();
//			long maxid = Long.MAX_VALUE;
			long maxid = Long.parseLong("359085410497986560");
			int extraTry = 3;
			int secondsUntilReset = 300;
			int total = 0;
			System.out.println("total\tOldest\t(requiredTime)");
			do {
				do {
					query = new Query("選挙");
					query.count(100);
//					query.setUntil("2014-05-30");
					query.setMaxId(maxid);
					try{
						do {
							qr = tw.search(query);
							List<Status> res = qr.getTweets();
							total += res.size();
							Status oldest = res.get(res.size()-1);
							maxid = oldest.getId();
							System.out.println(total+"\t"+oldest.getCreatedAt().toString()+"\t("+qr.getCompletedIn()+"s)");
						} while ((query = qr.nextQuery())!=null);
					} catch (TwitterException te) {
						if(te.getStatusCode() == 429) {
							secondsUntilReset = te.getRateLimitStatus().getSecondsUntilReset();
							System.err.println("API Rate Limit kicked in. "+secondsUntilReset+"s until resume...");
						}
						break;
					}
				} while(true);
				System.out.println("TEST: Resuming immediately without sleep. Remaining try: "+(extraTry--));
			} while(extraTry >= 0);
			System.out.println("TEST: Done.");
			
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}

}
