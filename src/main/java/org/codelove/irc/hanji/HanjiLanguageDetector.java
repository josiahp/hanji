package org.codelove.irc.hanji;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import org.pircbotx.hooks.events.MessageEvent;
import org.pircbotx.hooks.events.PrivateMessageEvent;
import org.pircbotx.hooks.types.GenericMessageEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cybozu.labs.langdetect.Detector;
import com.cybozu.labs.langdetect.DetectorFactory;
import com.cybozu.labs.langdetect.LangDetectException;

public class HanjiLanguageDetector extends HashMap<String, HashMap<String, Integer>> {
	static Logger logger = LoggerFactory.getLogger(HanjiLanguageDetector.class);

	final static long serialVersionUID = 1L;

	final static List<String> commands = Arrays.asList(".score", ".scoreboard", ".cool", ".coolboard");

	public void handleMessageEvent(MessageEvent event) {
		if (isCommand(event.getMessage()))
			handleCommand(event);
		else
			try {
				Detector detector = DetectorFactory.create();
				detector.append(event.getMessage());

				String lang = detector.detect();
				logger.info(lang + ": " + event.getMessage());

				HashMap<String, Integer> languageScores = get(event.getUser().getNick());
				if (languageScores == null) {
					languageScores = new HashMap<String, Integer>();
					languageScores.put("japanese", 0);
					languageScores.put("other", 0);
				}

				if (lang.equals("ja"))
					languageScores.put("japanese", languageScores.get("japanese") + 1);
				else
					languageScores.put("other", languageScores.get("other") + 1);

				put(event.getUser().getNick(), languageScores);
				save(System.getProperty("hanji.scoresFile", "scores.bin"));
			} catch (LangDetectException e) {
				e.printStackTrace();
			}
	}

	public void handlePrivateMessageEvent(PrivateMessageEvent event) {
		if (isCommand(event.getMessage()))
			handleCommand(event);
	}

	public boolean isCommand(String message) {
		return commands.stream().anyMatch(c -> message.startsWith(c));
	}

	public void handleCommand(GenericMessageEvent event) {
		if (event.getMessage().startsWith(".scoreboard")) {
			logger.debug(".scoreboard -> getScoreBoard(" + event.getUser().getNick() + ")");
			event.respondWith(getScoreBoard());
		} else if (event.getMessage().startsWith(".score")) {
			logger.debug(".score -> getScore(" + event.getUser().getNick() + ")");
			event.respond(getScore(event.getUser().getNick()));
		} else if (event.getMessage().startsWith(".coolboard")) {
			logger.debug(".coolboard -> getPercentages(" + event.getUser().getNick() + ")");
			event.respondWith(getPercentages());
		} else if (event.getMessage().startsWith(".cool")) {
			logger.debug(".cool -> getPercentage(" + event.getUser().getNick() + ")");
			event.respond(getPercentage(event.getUser().getNick()));
		}
	}

	public void save(String filename) {
		try {
			FileOutputStream fos = new FileOutputStream(filename);
			ObjectOutputStream oos = new ObjectOutputStream(fos);
			oos.writeObject(this);
			oos.close();
			fos.close();
		} catch (IOException e) {
			logger.error("unable to save scores: " + e.getMessage());
		}
	}

	public String getScore(String nickname) {
		return this.get(nickname).get("japanese").toString();
	}

	public String getScoreBoard() {
		return this.keySet().stream().map(k -> k + ": " + getScore(k)).collect(Collectors.joining("\n"));
	}

	public String getPercentage(String nickname) {
		Integer totalLines = this.get(nickname).get("japanese") + this.get(nickname).get("other");
		logger.info(this.get(nickname).get("japanese") + " / " + totalLines + " = "
				+ ((float) this.get(nickname).get("japanese") / (float) totalLines));
		return (int) (((float) this.get(nickname).get("japanese") / totalLines) * 100.0f) + "% cool\n";
	}

	public String getPercentages() {
		return this.keySet().stream().map(k -> k + ": " + getPercentage(k)).collect(Collectors.joining("\n"));
	}

	public static HanjiLanguageDetector load(String filename) {
		HanjiLanguageDetector loadedScores = null;

		try {
			FileInputStream fis = new FileInputStream(filename);
			ObjectInputStream ois = new ObjectInputStream(fis);
			loadedScores = (HanjiLanguageDetector) ois.readObject();
			ois.close();
			fis.close();
		} catch (Exception e) {
			logger.error("unable to load scores: " + e.getMessage());
		}

		return loadedScores;
	}
}
