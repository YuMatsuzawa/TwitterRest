package matz;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;

import twitter4j.Status;
import twitter4j.TwitterObjectFactory;

public class Sandbox {

	public static void main(String[] args) {

		BufferedReader br = null;
		try {
			br = new BufferedReader(new InputStreamReader(new FileInputStream(new File("sampleTweetLog.txt"))));
			String line = null;
			while ((line = br.readLine()) != null) {
				if (line.isEmpty()) continue;
				Status tweet = TwitterObjectFactory.createStatus(line);
				if (tweet.isRetweet()) {
					Status retweet = tweet.getRetweetedStatus();
					
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
