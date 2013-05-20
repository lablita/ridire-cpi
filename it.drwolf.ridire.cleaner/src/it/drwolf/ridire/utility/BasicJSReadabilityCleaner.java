package it.drwolf.ridire.utility;

import java.io.FileReader;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

public class BasicJSReadabilityCleaner {
	public static void main(String[] args) {
		new BasicJSReadabilityCleaner(args);
	}

	public BasicJSReadabilityCleaner(String[] args) {
		ScriptEngineManager manager = new ScriptEngineManager();
		ScriptEngine engine = manager.getEngineByName("js");
		try {
			FileReader reader = new FileReader("yourFile.js");
			engine.eval(reader);
			reader.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
