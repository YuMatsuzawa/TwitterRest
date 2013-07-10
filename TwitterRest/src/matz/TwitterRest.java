package matz;

import java.io.*;
import java.util.ArrayList;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;

import twitter4j.*;
import twitter4j.conf.ConfigurationBuilder;

public class TwitterRest {
	private static boolean forceCursorReset = false;
	private static boolean forceFileRefresh = false;
	private static boolean forceInitialize = false;
	private final static String cursorResetOption = "-cr";
	private final static String fileRefreshOption = "-fr";
	private final static String initializeOption = "-init";
	private final static String queriesListOption = "-l";
	
	private static ArrayList<String> queryKeyWordsList = new ArrayList<String>();
	private static File currentIdFile = new File("currentId.txt");
	private static File searchLog = new File("searchLog.txt");
	public static File userListFile = new File("userList.txt");
	public static File userDir = new File("userDir");
	public static ArrayList<String> userList = new ArrayList<String>();

	public static String[][] OAuthList;
	public final static int authIdIndex = 0;
	public final static int authCnsKeyIndex = 1;
	public final static int authCnsSecIndex = 2;
	public final static int authAccTknIndex = 3;
	public final static int authAccSecIndex = 4;
	public final static int authLastCallIndex = 5;
	public final static int authCallCountIndex = 6;
	public final static int OAuthListLength = 7;
	public static int currentAuthId;
	public static Twitter twitter;
	public final static long authLimitWindow = 15*60*1000; //milliseconds
	public final static long authRetryMargin = 30*1000;
	public final static int authRateLimit = 180;
	public static File authInfoFile = new File("authInfo.txt");
	public static File authInfoEmergency = new File("authInfo.emergency");
	public static String authInfoFileDelim = ",";
	private static int keywordSearchAuthIndex = 0;

	public final static int STATUS_UNAUTHORIZED = 401;
	public final static int STATUS_NOT_FOUND = 404;
	public final static int STATUS_ENHANCE_YOUR_CALM = 420;
	public final static int STATUS_TOO_MANY_REQUESTS = 429;
	public final static int STATUS_BAD_GATEWAY = 502;
	public final static int STATUS_SERVICE_UNAVAILABLE = 503;
	
	public static void loadAuthInfo() {
		try {
			ArrayList<String[]> tmpList = new ArrayList<String[]>();
			BufferedReader br;
			if (authInfoEmergency.exists() && authInfoEmergency.lastModified() > authInfoFile.lastModified()) { //退避ファイルチェック
				br = new BufferedReader(new InputStreamReader(new FileInputStream(authInfoEmergency)));
			} else {
				br = new BufferedReader(new InputStreamReader(new FileInputStream(authInfoFile)));
			}
			String authInfoLine = new String();
			while ((authInfoLine = br.readLine()) != null) {
				String[] tmpArray = new String[OAuthListLength];
				StringTokenizer st = new StringTokenizer(authInfoLine,authInfoFileDelim);
				for (int i=0;i<OAuthListLength;i++){
					try {
						tmpArray[i] = st.nextToken();
					} catch (NoSuchElementException e) {
						tmpArray[i] = "0";
					}
				}
				tmpList.add(tmpArray);
			}
			br.close();
			authInfoEmergency.delete();

			OAuthList = tmpList.toArray(new String[tmpList.size()][OAuthListLength]);
		} catch (IOException e) {
			OAuthList = OAuthListDefault;
		}
	}
	public static void saveAuthInfo() {
		try {
			BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(authInfoFile)));
			for (String[] authInfo : OAuthList) {
				for (String value : authInfo) {
					bw.write(value + authInfoFileDelim);
				}
				bw.newLine();
			}
			bw.close();
		} catch (FileNotFoundException e) {
			saveAuthInfoEmergency();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private static void saveAuthInfoEmergency() {
		try {
			BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(authInfoEmergency)));
			for (String[] authInfo : OAuthList) {
				for (String value : authInfo) {
					bw.write(value + authInfoFileDelim);
				}
				bw.newLine();
			}
			bw.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	public static void setCB(int index, ConfigurationBuilder cb) {
		System.out.println("Auth with id: " + OAuthList[index][authIdIndex]);
		currentAuthId = index;
		cb.setDebugEnabled(true)
		.setOAuthConsumerKey(OAuthList[index][authCnsKeyIndex])
		.setOAuthConsumerSecret(OAuthList[index][authCnsSecIndex])
		.setOAuthAccessToken(OAuthList[index][authAccTknIndex])
		.setOAuthAccessTokenSecret(OAuthList[index][authAccSecIndex])
	    .setJSONStoreEnabled(true);
		//return cb;
	}
	
	private static boolean lastCall(int index) {
		long time = System.currentTimeMillis();
		boolean isRefreshed = false;
		if (OAuthList[index][authLastCallIndex].isEmpty()
				|| time - Long.parseLong(OAuthList[index][authLastCallIndex]) > authLimitWindow) {
			OAuthList[index][authLastCallIndex] = Long.toString(time);
			isRefreshed = true;
		}
		return isRefreshed;
	}
	public static void callCount(int index) {
		int count = Integer.parseInt(OAuthList[index][authCallCountIndex].isEmpty()? "0" : OAuthList[index][authCallCountIndex]);
		boolean isRefreshed = lastCall(index);
		if (isRefreshed) {
			OAuthList[index][authCallCountIndex] = "1";
		} else {
			count++;
			OAuthList[index][authCallCountIndex] = Integer.toString(count);
		}
	}
	
	public static Twitter buildTwitterIns(int index) {
		ConfigurationBuilder cb = new ConfigurationBuilder();
		setCB(index, cb);
		TwitterFactory tf = new TwitterFactory(cb.build());
		return tf.getInstance();
	}
	
	public static boolean authAvailabilityCheck() {
		// check if current auth profile still within the rate limit
		int count = Integer.parseInt(OAuthList[currentAuthId][authCallCountIndex]);
		long lastCall = Long.parseLong(OAuthList[currentAuthId][authLastCallIndex]);
		long curr = System.currentTimeMillis();
		if (curr - lastCall > authLimitWindow) { // 15min passed from previous call
			return true;
		} else if(count >= authRateLimit) { // rate limit reached
			return false;
		}
		return true; // neither
	}
	
	public static boolean authAvailabilityCheck(int index) {
		// check if current auth profile still within the rate limit
		int count = Integer.parseInt(OAuthList[index][authCallCountIndex]);
		long lastCall = Long.parseLong(OAuthList[index][authLastCallIndex]);
		long curr = System.currentTimeMillis();
		if (curr - lastCall > authLimitWindow) { // 15min passed from previous call
			return true;
		} else if(count >= authRateLimit) { // rate limit reached
			return false;
		}
		return true; // neither
		
	}

	public static void sleepUntilReset() throws InterruptedException {
		long curr = System.currentTimeMillis();
		long lastCall = Long.parseLong(OAuthList[currentAuthId][authLastCallIndex]);
		long authResetInMilliSec = lastCall + authLimitWindow + authRetryMargin - curr;
		System.out.println("Waiting for call limit reset: " + (authResetInMilliSec / 1000) + "sec");
		Thread.sleep(authResetInMilliSec);
	}
	
	public static void sleepUntilReset(long retryAfter) throws InterruptedException {
		System.out.println("Waiting for call limit reset: " + (retryAfter / 1000) + "sec");
		Thread.sleep(retryAfter);
	}
	
	public static void setQueryKeyWordsList(File file) {
		try {
			BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8"));
			String line = new String();
			while((line = br.readLine()) != null) {
				queryKeyWordsList.add(line);
			}
			br.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static long extractUsers(String queryKeyWords, long currentId, Twitter twitter)
			throws FileNotFoundException, TwitterException, IOException {
		Query query = new Query(queryKeyWords);
		System.out.println(queryKeyWords);
		
		OutputStreamWriter osw = new OutputStreamWriter(new FileOutputStream(userListFile, forceFileRefresh? false : true));
		BufferedWriter bw = new BufferedWriter(osw);
		
		query.setSinceId(forceCursorReset? 0 : currentId);
		query.setCount(100);
		QueryResult result = twitter.search(query);
		callCount(currentAuthId);

		//long since = result.getSinceId();
		long until = result.getMaxId();
		
		for (Status status : result.getTweets()) { //pickup relevant user
			User thisone = status.getUser();
			String thisoneId = String.format("%d", thisone.getId());
			if (!userList.contains(thisoneId)) {
				userList.add(thisoneId); //new relevant user addition
				bw.write(thisoneId);
				bw.newLine();
			}
		}
		
		bw.close();
		osw.close();
		
		long nextSince = until;
		return nextSince;
	}

	public static void main (String[] args) {
		loadAuthInfo();
		
		String queryKeyWord = "自民党";
		for (int i=0; i<args.length; i++) {
			if(args[i].matches(cursorResetOption)) {
				forceCursorReset = true;
			} else if(args[i].matches(fileRefreshOption)) {
				forceFileRefresh = true;
			} else if(args[i].matches(initializeOption)){
				forceInitialize = true;
			} else if(args[i].matches(queriesListOption)) {
				File queriesFile = new File("queries.txt");
				setQueryKeyWordsList(queriesFile);
			} else {
				queryKeyWord = args[i];
			}
		}
		long currentId = 0;
		try { //userList loading
			BufferedReader ulbr = new BufferedReader(new InputStreamReader(new FileInputStream(userListFile)));
			String userid = new String();
			while((userid = ulbr.readLine()) != null) {
				if (!userList.contains(userid)) userList.add(userid);
			}
			ulbr.close();
		} catch (FileNotFoundException e) {
			//ignore
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		if (forceInitialize) {
			currentIdFile.delete();
			userListFile.delete();
			File oldUserDir = new File("userDir" + System.currentTimeMillis());
			userDir.renameTo(oldUserDir);
			File oldLogFile = new File("searchLog" + System.currentTimeMillis() + ".txt");
			searchLog.renameTo(oldLogFile);
			return;
		}
		
		try {
			InputStreamReader isr = new InputStreamReader(new FileInputStream(currentIdFile));
			BufferedReader br = new BufferedReader(isr);
			
			String line = br.readLine();
			currentId = Long.parseLong(line);
			
			br.close();
			isr.close();
		} catch (Exception e) {
			// ignore
		}
		

		try {
			System.out.println("Since " + currentId);
			
			//Twitter twitter = buildTwitterIns(keywordSearchAuthIndex); //init
			twitter = buildTwitterIns(keywordSearchAuthIndex); //init
			
			//search for relevant users
			long tmp = Long.MAX_VALUE;
			if (!queryKeyWordsList.isEmpty()){
				for (String query : queryKeyWordsList) {
					while(!authAvailabilityCheck()) {
						sleepUntilReset();
					}
					
					long thisUntil = extractUsers(query, currentId, twitter);
					if (tmp > thisUntil) tmp = thisUntil;
				}
			} else {
				while(!authAvailabilityCheck()) {
					sleepUntilReset();
				}
				tmp = extractUsers(queryKeyWord, currentId, twitter);
			}
			currentId = tmp;

			BufferedWriter bw1 = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(currentIdFile)));
			bw1.write(Long.toString(currentId));
			bw1.close();
			
		} catch (Exception e) {
			saveAuthInfo();
			e.printStackTrace();
		}
		
		saveAuthInfo();
		System.out.println("Done.");
	}
	
	public static String[][] OAuthListDefault = {
		{
			"gada_twt",												//screan name
			"KuG34YZ202uEZCHgHQhQ",									//consumer key
			"5R9nqymC0Zl5pF5KSDr80gCOTHqkaPS9sehrGoMpI",			//consumer secret
			"352840258-QrB0NPIYNtGiNUt3vnetmnMumnppJETnvccdZVJa",	//access token
			"PkuqzpWNCWglAGKtMfUeg1UVCRQqjqesmlOuyrVA",				//access token secret
			"",														//last call time
			"0"														//call count within 15min window
		},{
			"matz_0001",
			"3Szgzy5KOLrpY5KgXPj5Og",
			"sfrMISapC9I9RB1xK1nXLuBwLxuOshRTjJOpO4Ddfo",
			"1562516232-4BOWKF2kplQ9rgJIuhSKaby7GOFVF8Cjy9afvC8",
			"VGxbc7Pgsieylc3Cw3sgnwMjwO0LHkTVbljZg3Lr8",
			"",
			"0"
		},
	};
}
