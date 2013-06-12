package matz;

import java.io.*;

import twitter4j.*;
import twitter4j.conf.ConfigurationBuilder;
import twitter4j.json.DataObjectFactory;

public class TwitterRest {
	private static boolean forceCursorReset = false;
	private static boolean forceFileRefresh = false;
	private static boolean forceInitialize = false;
	private final static String cursorResetOption = "-cr";
	private final static String fileRefreshOption = "-fr";
	private final static String initializeOption = "-init";
	
	public static String queryKeyWords = "Ž©–¯“}";
	
	public static void main (String[] args) {
		for (int i=0; i<args.length; i++) {
			if(args[i].matches(cursorResetOption)) {
				forceCursorReset = true;
			} else if(args[i].matches(fileRefreshOption)) {
				forceFileRefresh = true;
			} else if(args[i].matches(initializeOption)){
				forceInitialize = true;
			} else {
				queryKeyWords = args[i];
			}
		}
		long currentId = 0;
		File currentIdFile = new File("currentId.txt");
		File searchLog = new File("searchLog.txt");
		
		if (forceInitialize) {
			currentIdFile.delete();
			File oldLogFile = new File("searchLog" + System.currentTimeMillis() + ".txt");
			searchLog.renameTo(oldLogFile);
			return;
		}
		
		ConfigurationBuilder cb = new ConfigurationBuilder();
		cb.setDebugEnabled(true)
		.setOAuthConsumerKey("KuG34YZ202uEZCHgHQhQ")
		.setOAuthConsumerSecret("5R9nqymC0Zl5pF5KSDr80gCOTHqkaPS9sehrGoMpI")
		.setOAuthAccessToken("352840258-QrB0NPIYNtGiNUt3vnetmnMumnppJETnvccdZVJa")
		.setOAuthAccessTokenSecret("PkuqzpWNCWglAGKtMfUeg1UVCRQqjqesmlOuyrVA")
	    .setJSONStoreEnabled(true);
		
		TwitterFactory tf = new TwitterFactory(cb.build());
		Twitter twitter = tf.getInstance();
		
		InputStreamReader isr;
		try {
			isr = new InputStreamReader(new FileInputStream(currentIdFile));
			BufferedReader br = new BufferedReader(isr);
			
			String line = new String();
			while((line = br.readLine()) != null) {
				currentId = Long.parseLong(line);
			}
			br.close();
			isr.close();
		} catch (Exception e) {
			// ignore
		}
		

		try {
			Query query = new Query(queryKeyWords);
			OutputStreamWriter osw = new OutputStreamWriter(new FileOutputStream(searchLog, forceFileRefresh? false : true));
			BufferedWriter bw = new BufferedWriter(osw);
			
			query.setSinceId(forceCursorReset? 0 : currentId);
			query.setCount(100);
			QueryResult result = twitter.search(query);

			long since = result.getSinceId();
			long until = result.getMaxId();
			
			System.out.println("Since " + since + " until " + until);
			//int counter = 0;
			for (Status status : result.getTweets()) {
				//counter++;
				String rawJSON = DataObjectFactory.getRawJSON(status);
				//System.out.println(counter + ": @" + status.getUser().getScreenName() + ":" + status.getText());
				bw.write(rawJSON);
				bw.newLine();
				//System.out.println(counter + ": " + rawJSON);
			}
			bw.close();
			osw.close();
			
			OutputStreamWriter osw1 = new OutputStreamWriter(new FileOutputStream(currentIdFile));
			BufferedWriter bw1 = new BufferedWriter(osw1);
			
			long nextSince = until + 1;
			bw1.write(Long.toString(nextSince));
			bw1.close();
			osw1.close();
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}

}
