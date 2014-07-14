package matz;

import twitter4j.*;
import twitter4j.conf.ConfigurationBuilder;

public class TLTest {
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
		Twitter twitter = tf.getInstance();

		try {
			long id = Long.parseLong("340880618198020095");
			@SuppressWarnings("unused")
			Paging paging = new Paging(id);
			ResponseList<Status> tl = twitter.getUserTimeline("hal_twitter");
			for(Status status : tl) {
				String rawJSON = TwitterObjectFactory.getRawJSON(status);
				System.out.println(rawJSON);
			}
			
		} catch (Exception e) {
			//ignore
			e.printStackTrace();
		}
	}

}
