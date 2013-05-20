/*******************************************************************************
 * Copyright 2013 Università degli Studi di Firenze
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package it.drwolf.ridire.index.sketch;

import it.drwolf.ridire.index.results.Sketch;

import java.util.ArrayList;
import java.util.List;

public class SketchList {
	private final static List<Sketch> sketches = new ArrayList<Sketch>();
	private final static List<Sketch> sketchesToUpdate = new ArrayList<Sketch>();

	// c'è stato un errore nel nominare gli sketch. E' stato fatto un primo
	// indice con i nomi sbagliati.
	// Per questo c'è un hack sui nomi; i risultati vanno bene.
	static {
		Sketch s = new Sketch();
		s.setName("postV_N");
		s.addGramrel(
				"A1=[lemma=\"%1$s\" & pos=\"VER.*\"] [pos=\"ADV.*\"]{0,2} [pos=\"DET:.*|ART|NUM\"]{0,3}  [pos=\"ADV.*|NEG\"]? [pos=\"ADJ|DETnum|NUM\"]* @[pos=\"NOUN\"]",
				false, null, null);
		s.setGoodFor(Sketch.VERB);
		SketchList.sketches.add(s);
		s = new Sketch();
		s.setName("preN_V");
		s.addGramrel(
				"A1=[lemma=\"%1$s\" & pos=\"NOUN\"] [pos=\"ADJ|DETnum|NUM\"]* [pos=\"ADV.*|NEG\"]? [pos=\"DET:.*|ART|NUM\"]{0,3} [pos=\"ADV.*\"]{0,2} @[pos=\"VER.*\"]",
				true,
				null,
				"A1=@[pos=\"VER.*\"] [pos=\"ADV.*\"]{0,2} [pos=\"DET:.*|ART|NUM\"]{0,3} [pos=\"ADV.*|NEG\"]? [pos=\"ADJ|DETnum|NUM\"]* [lemma=\"%1$s\" & pos=\"NOUN\"]");
		s.setGoodFor(Sketch.NOUN);
		SketchList.sketches.add(s);
		s = new Sketch();
		s.setName("preV_N");
		s.addGramrel(
				"A1=[lemma=\"%1$s\" & pos=\"VER.*\"] ([pos=\"AUX.*|VER2.*\" & lemma !=\"essere\" & lemma!=\"venire\" & lemma!=\"fare\"] [pos=\"ADV.*|NEG\"]?){0,2} [pos=\"CLI\"]{0,2} [pos=\"ADV|NEG\"]{0,3} [pos=\"ADJ\"]* @[pos=\"NOUN\"]",
				true,
				null,
				"A1= @[pos=\"NOUN\"] [pos=\"ADJ\"]* [pos=\"ADV|NEG\"]{0,3} [pos=\"CLI\"]{0,2} ([pos=\"AUX.*|VER2.*\" & lemma !=\"essere\" & lemma!=\"venire\" & lemma!=\"fare\"] [pos=\"ADV.*|NEG\"]?){0,2} [lemma=\"%1$s\" & pos=\"VER.*\"]");
		s.setGoodFor(Sketch.VERB);
		SketchList.sketches.add(s);
		s = new Sketch();
		s.setName("postN_V");
		s.addGramrel(
				"A1=[lemma=\"%1$s\" & pos=\"NOUN\"] [pos=\"ADJ\"]* [pos=\"ADV|NEG\"]{0,3} [pos=\"CLI\"]{0,2} ([pos=\"AUX.*|VER2.*\" & lemma !=\"essere\" & lemma!=\"venire\" & lemma!=\"fare\"] [pos=\"ADV.*|NEG\"]?){0,2} @[pos=\"VER.*\"]",
				false, null, null);
		s.setGoodFor(Sketch.NOUN);
		SketchList.sketches.add(s);
		s = new Sketch();
		s.setName("AofN");
		s.addGramrel(
				"A1=[lemma=\"%1$s\" & pos=\"NOUN\"] [pos=\"ADV.*|NEG\"]? @[pos=\"ADJ\"]",
				false, null, null);
		s.addGramrel("A1=[lemma=\"%1$s\" & pos=\"NOUN\"] @[pos=\"ADJ\"]", true,
				null, "A1=@[pos=\"ADJ\"] [lemma=\"%1$s\" & pos=\"NOUN\"]");
		s.setGoodFor(Sketch.NOUN);
		SketchList.sketches.add(s);
		s = new Sketch();
		s.setName("NofA");
		s.addGramrel(
				"A1=[lemma=\"%1$s\" & pos=\"ADJ\"] [pos=\"ADV.*|NEG\"]? @[pos=\"NOUN\"]",
				true, null,
				"A1=@[pos=\"NOUN\"] [pos=\"ADV.*|NEG\"]? [lemma=\"%1$s\" & pos=\"ADJ\"]");
		s.addGramrel("A1=[lemma=\"%1$s\" & pos=\"ADJ\"] @[pos=\"NOUN\"]",
				false, null, null);
		s.setGoodFor(Sketch.ADJECTIVE);
		SketchList.sketches.add(s);
		s = new Sketch();
		// HACK: era preADV_V
		s.setName("postV_ADV");
		s.addGramrel(
				"A1=[lemma=\"%1$s\" & pos=\"VER.*\"] [pos=\"DET:.*|ART\"]{0,1} [pos=\"ADJ|DETnum\"]{0,1} [pos=\"NOUN\"]{0,1} [pos=\"ADV.*\"]{0,2} @[pos=\"ADV.*\"|lemma=\"addosso\"|lemma=\"appresso\"|lemma=\"su\"|lemma=\"lontano\"|lemma=\"vicino\"]",
				false, null, null);
		s.setGoodFor(Sketch.VERB);
		SketchList.sketches.add(s);
		s = new Sketch();
		// HACK: era postV_ADV
		s.setName("preADV_V");
		s.addGramrel(
				"A1=[(lemma=\"%1$s\" & pos=\"ADV.*\") | lemma=\"addosso\"|lemma=\"appresso\"|lemma=\"su\"|lemma=\"lontano\"|lemma=\"vicino\"] [pos=\"ADV.*\"]{0,2} [pos=\"NOUN\"]{0,1} [pos=\"ADJ|DETnum\"]{0,1} [pos=\"DET:.*|ART\"]{0,1} @[pos=\"VER.*\"]",
				true,
				null,
				"A1=@[pos=\"VER.*\"] [pos=\"DET:.*|ART\"]{0,1} [pos=\"ADJ|DETnum\"]{0,1} [pos=\"NOUN\"]{0,1} [pos=\"ADV.*\"]{0,2} [(lemma=\"%1$s\" & pos=\"ADV.*\") | lemma=\"addosso\"|lemma=\"appresso\"|lemma=\"su\"|lemma=\"lontano\"|lemma=\"vicino\"]");
		s.setGoodFor(Sketch.ADVERB);
		SketchList.sketches.add(s);
		s = new Sketch();
		s.setName("n_modifier");
		String subquery = "A1=[pos=\"DET.*|ART|ADJ|PRE|ARTPRE\"] [lemma=\"%1$s\" & pos=\"NOUN\"] [pos=\"ADJ|NOUN\"]{0,2} @[pos=\"NOUN\"]";
		s.addGramrel(
				"ASUB= [lemma=\"%1$s\" & pos=\"NOUN\"] [pos=\"ADJ|NOUN\"]{0,2} @[pos=\"NOUN\"] expand to text",
				false, subquery, null);
		s.setGoodFor(Sketch.NOUN);
		SketchList.sketches.add(s);
		s = new Sketch();
		s.setName("n_modifies");
		s.addGramrel(
				"A1=[lemma=\"%1$s\" & pos=\"NOUN\"] [pos=\"ADJ|NOUN.*\"]{0,2} @[pos=\"NOUN\"] [pos=\"DET.*|ART|ADJ|PRE|ARTPRE\"]",
				true,
				null,
				"A1=[pos=\"DET.*|ART|ADJ|PRE|ARTPRE\"] @[pos=\"NOUN\"] [pos=\"ADJ|NOUN.*\"]{0,2} [lemma=\"%1$s\" & pos=\"NOUN\"]");
		s.setGoodFor(Sketch.NOUN);
		SketchList.sketches.add(s);
		s = new Sketch();
		s.setName("e_o");
		s.addGramrel(
				"A1=[lemma=\"%1$s\" & pos=\"NOUN|NPR\"] [pos=\"ADJ|ADV.*\"]{0,2} [lemma=\",\"]{0,1} [lemma=\"e|o|od|ed|oppure|,\"] [pos=\"DET.*|PROposs|ART.*\"]{0,1} [pos=\"NUM\"]{0,2} [pos=\"ADJ\"|pos=\"ADV.*\"|lemma=\",\"]{0,3} [pos=\"NOUN|NPR\"]{0,2} [lemma=\",\"]{0,2} @[pos=\"NOUN|NPR\"] [pos!=\"NOUN|NPR\"]",
				false, null, null);
		subquery = "A1=[pos!=\"NOUN|NPR\"] [lemma=\"%1$s\" & pos=\"NOUN|NPR\"] [lemma=\",\"]{0,1} [pos=\"NOUN|NPR\"]{0,2} [pos=\"ADJ\"|pos=\"ADV.*\"|lemma=\",\"]{0,3} [pos=\"NUM\"]{0,2} [pos=\"DET.*|PROposs|ART.*\"]{0,1} [lemma=\"e|o|od|ed|oppure|,\"] [lemma=\",\"]{0,1} [pos=\"ADJ|ADV.*\"]{0,2} @[pos=\"NOUN|NPR\"]";
		s.addGramrel(
				"ASUB=[lemma=\"%1$s\" & pos=\"NOUN|NPR\"] [lemma=\",\"]{0,1} [pos=\"NOUN|NPR\"]{0,2} [pos=\"ADJ\"|pos=\"ADV.*\"|lemma=\",\"]{0,3} [pos=\"NUM\"]{0,2} [pos=\"DET.*|PROposs|ART.*\"]{0,1} [lemma=\"e|o|od|ed|oppure|,\"] [lemma=\",\"]{0,1} [pos=\"ADJ|ADV.*\"]{0,2} @[pos=\"NOUN|NPR\"] expand to text",
				true,
				subquery,
				"A1=@[pos=\"NOUN|NPR\"] [pos=\"ADJ|ADV.*\"]{0,2} [lemma=\",\"]{0,1} [lemma=\"e|o|od|ed|oppure|,\"] [pos=\"DET.*|PROposs|ART.*\"]{0,1} [pos=\"NUM\"]{0,2} [pos=\"ADJ\"|pos=\"ADV.*\"|lemma=\",\"]{0,3} [pos=\"NOUN|NPR\"]{0,2} [lemma=\",\"]{0,1} [lemma=\"%1$s\" & pos=\"NOUN|NPR\"] [pos!=\"NOUN|NPR\"]");
		s.setGoodFor(Sketch.NOUN);
		SketchList.sketches.add(s);
		SketchList.sketchesToUpdate.add(s);
		s = new Sketch();
		s.setName("e_o");
		s.addGramrel(
				"A1=lab1:[lemma=\"%1$s\" & pos=\"VER.*\"] [pos=\"ADV.*\"]{0,2} [pos=\"DET.*|ART.*\"]{0,1} [pos=\"NUM\"]{0,2} [pos=\"ADJ\"|pos=\"ADV.*\"|lemma=\",\"]{0,3} [pos=\"NOUN|NPR\"]{0,2} [lemma=\",\"]{0,1} [lemma=\"e|o|od|ed|oppure|,\"] [pos=\"ADV.*\"]{0,2} [pos=\"AUX.*\"]{0,1} [pos=\"VER2.*\"]{0,1} @[pos=\"VER.*\"] :: lab1.pos = target.pos",
				false, null, null);
		s.addGramrel(
				"A1=lab1:[lemma=\"%1$s\" & pos=\"VER.*\"] [pos=\"VER2.*\"]{0,1} [pos=\"AUX.*\"]{0,1} [pos=\"ADV.*\"]{0,2} [lemma=\"e|o|od|ed|oppure|,\"] [lemma=\",\"]{0,1} [pos=\"NOUN|NPR\"]{0,2} [pos=\"ADJ\"|pos=\"ADV.*\"|lemma=\",\"]{0,3} [pos=\"NUM\"]{0,2} [pos=\"DET.*|ART.*\"]{0,1} [pos=\"ADV.*\"]{0,2} @[pos=\"VER.*\"] :: lab1.pos = target.pos",
				true,
				null,
				"A1=@[pos=\"VER.*\"] [pos=\"ADV.*\"]{0,2} [pos=\"DET.*|ART.*\"]{0,1} [pos=\"NUM\"]{0,2} [pos=\"ADJ\"|pos=\"ADV.*\"|lemma=\",\"]{0,3} [pos=\"NOUN|NPR\"]{0,2} [lemma=\",\"]{0,1} [lemma=\"e|o|od|ed|oppure|,\"] [pos=\"ADV.*\"]{0,2} [pos=\"AUX.*\"]{0,1} [pos=\"VER2.*\"]{0,1} lab1:[lemma=\"%1$s\" & pos=\"VER.*\"] :: lab1.pos = target.pos");
		s.setGoodFor(Sketch.VERB);
		SketchList.sketches.add(s);
		SketchList.sketchesToUpdate.add(s);
		s = new Sketch();
		s.setName("e_o");
		s.addGramrel(
				"A1=[lemma=\"%1$s\" & pos=\"ADJ\"] [lemma=\",\"]{0,1} [lemma=\"e|o|od|ed|oppure|,\"]{0,1} [pos=\"ADV.*\"]{0,2} @[pos=\"ADJ\"]",
				false, null, null);
		s.addGramrel(
				"A1=[lemma=\"%1$s\" & pos=\"ADJ\"] [pos=\"ADV.*\"]{0,2} [lemma=\"e|o|od|ed|oppure|,\"]{0,1} [lemma=\",\"]{0,1} @[pos=\"ADJ\"]",
				true,
				null,
				"A1=@[pos=\"ADJ\"] [lemma=\",\"]{0,1} [lemma=\"e|o|od|ed|oppure|,\"]{0,1} [pos=\"ADV.*\"]{0,2} [lemma=\"%1$s\" & pos=\"ADJ\"]");
		s.setGoodFor(Sketch.ADJECTIVE);
		SketchList.sketches.add(s);
		SketchList.sketchesToUpdate.add(s);
		s = new Sketch();
		s.setName("e_o");
		s.addGramrel(
				"A1=lab1:[lemma=\"%1$s\" & pos=\"ADV.*\"] [lemma=\",\"]{0,1} [lemma=\"e|o|od|ed|oppure|,\"] \"ADV.*\"{0,2} @[pos=\"ADV.*\"] :: lab1.pos = target.pos",
				false, null, null);
		s.addGramrel(
				"A1=lab1:[lemma=\"%1$s\" & pos=\"ADV.*\"] [pos=\"ADV.*\"]{0,2} [lemma=\"e|o|od|ed|oppure|,\"] [lemma=\",\"]{0,1} @[pos=\"ADV.*\"] :: lab1.pos = target.pos",
				true,
				null,
				"A1=@[pos=\"ADV.*\"] [lemma=\",\"]{0,1} [lemma=\"e|o|od|ed|oppure|,\"] [pos=\"ADV.*\"]{0,2} lab1:[lemma=\"%1$s\" & pos=\"ADV.*\"] :: lab1.pos = target.pos");
		s.setGoodFor(Sketch.ADVERB);
		SketchList.sketches.add(s);
		SketchList.sketchesToUpdate.add(s);
		s = new Sketch();
		s.setName("pp_%s");
		s.setTrinary(true);
		s.addGramrel(
				"A1=[lemma=\"%1$s\" & pos=\"NOUN\"] [pos=\"ADV.*\"]? [pos=\"PRE|ARTPRE\"] [pos=\"ADV.*\"]{0,2} [pos=\"DET:.*|ART|NUM\"]{0,3} [pos=\"ADV.*|NEG\"]? [pos=\"ADJ|DET:num|NUM\"]* @[pos=\"NOUN\"]",
				false, null, null);
		s.setGoodFor(Sketch.NOUN);
		SketchList.sketches.add(s);
		SketchList.sketchesToUpdate.add(s);
		s = new Sketch();
		s.setName("pp_%s");
		s.setTrinary(true);
		s.addGramrel(
				"A1=[lemma=\"%1$s\" & pos=\"ADJ\"] [pos=\"ADV.*\"]? [pos=\"PRE|ARTPRE\"] [pos=\"ADV.*\"]{0,2} [pos=\"DET:.*|ART|NUM\"]{0,3} [pos=\"ADV.*|NEG\"]? [pos=\"ADJ|DET:num|NUM\"]* @[pos=\"NOUN\"]",
				false, null, null);
		s.setGoodFor(Sketch.ADJECTIVE);
		SketchList.sketches.add(s);
		SketchList.sketchesToUpdate.add(s);
		s = new Sketch();
		s.setName("pp_%s");
		s.setTrinary(true);
		s.addGramrel(
				"A1=[lemma=\"%1$s\" & pos=\"VER.*\"] [pos=\"ADV.*\"]? [pos=\"PRE|ARTPRE\"] [pos=\"ADV.*\"]{0,2} [pos=\"DET:.*|ART|NUM\"]{0,3} [pos=\"ADV.*|NEG\"]? [pos=\"ADJ|DET:num|NUM\"]* @[pos=\"NOUN\"]",
				false, null, null);
		s.setGoodFor(Sketch.VERB);
		SketchList.sketches.add(s);
		SketchList.sketchesToUpdate.add(s);
	}

	public static Sketch getSketchByName(String sketchName, String goodFor) {
		if (sketchName.startsWith("pp_")) {
			sketchName = "pp_%s";
		}
		for (Sketch s : SketchList.sketches) {
			if (s.getName().equals(sketchName)
					&& s.getGoodFor().equals(goodFor)) {
				return s;
			}
		}
		return null;
	}

	public final static List<Sketch> getSketches() {
		return SketchList.sketches;
	}

	public static List<Sketch> getSketchesByGoodFor(String goodFor) {
		List<Sketch> sketchesGoodFor = new ArrayList<Sketch>();
		for (Sketch s : SketchList.sketches) {
			if (s.getGoodFor().equals(goodFor)) {
				sketchesGoodFor.add(s);
			}
		}
		return sketchesGoodFor;
	}

	public final static List<Sketch> getSketchesToUpdate() {
		return SketchList.sketchesToUpdate;
	}

	public static boolean isSketchNameGoodFor(String sketchName, String goodFor) {
		boolean ret = false;
		for (Sketch s : SketchList.sketches) {
			if (s.getName().equals(sketchName) || s.getName().startsWith("pp_")
					&& sketchName.startsWith("pp_")) {
				if (s.getGoodFor().equals(goodFor)) {
					ret = true;
				}
			}
		}
		return ret;
	}
}
