 package DemoClassify;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import edu.stanford.nlp.classify.RVFDataset;
import edu.stanford.nlp.ling.CoreAnnotations.PrevChildAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.RVFDatum;
import edu.stanford.nlp.ling.CoreAnnotations.LemmaAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.PartOfSpeechAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TextAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;

import edu.stanford.nlp.stats.ClassicCounter;
import edu.stanford.nlp.util.CoreMap;
import Corpus.Document;
import Corpus.Corpus;
import Qwordnet.QWordNetDB;



public class CorpusTestGenerator {
	
	private final static String[] NEGATION_TOKENS = {"not", "nt", "neither", "nor"};
	static int TestSplit = 10; //%
	int Num_Test = 0;
	int Num_Train = 0;
	TaxonomyClass myTC = new TaxonomyClass();
	List<String> RoomSentences = new ArrayList();
	List<String> ServiceSentences = new ArrayList();
	List<String> StaffSentences = new ArrayList();
	List<String> FacilitiesSentences = new ArrayList();
	
	
	public String Generate(Corpus corpus) throws IOException
	{
		int max = 0;
		int featC = 0;
		String generatorData = "";
		
		int CorpusMax= (corpus.size())*TestSplit/100;
		QWordNetDB qwordnet = QWordNetDB.createInstance();
		int Num_Test = 0;
		int Num_Train = 0;
		Properties props = new Properties();
		props.put("annotators", "tokenize, ssplit, pos, lemma");
		String  tFichero = "data/THOFUDemo.test";
		String  trFichero = "data/THOFUDemo.train";
		File  TrainFile = new File (trFichero);
		File  TestFile = new File (tFichero);
		BufferedWriter  trainf = new BufferedWriter (new FileWriter (TrainFile));
		BufferedWriter  testf = new BufferedWriter (new FileWriter (TestFile));
		
		StanfordCoreNLP pipeline = new StanfordCoreNLP(props);
		RVFDataset<String, String> dataset = new RVFDataset<String, String>();
		int nDoc = 1;
		for (Document doc : corpus) {
			
			final Annotation document = new Annotation(doc.getText());
			pipeline.annotate(document);

			final List<CoreMap> sentences = document.get(SentencesAnnotation.class);

			ClassicCounter<String> featureCounter = new ClassicCounter<String>();
			for (int h = 0; h < sentences.size(); h++) {
				final CoreMap sentence = sentences.get(h);
				final List<CoreLabel> tokens = sentence.get(TokensAnnotation.class);
				final Iterator<CoreLabel> it = tokens.iterator();
				
				while(it.hasNext()) {
					final String pos = it.next().get(PartOfSpeechAnnotation.class);
					if(pos.equals(".") || pos.equals(",") || pos.equals("``")) {
						it.remove();
					}
				}
				
				boolean negate = false;
				
				for (int i = 0; i < tokens.size(); i++) {		
										
					final CoreLabel token = tokens.get(i);
					
					String word = token.get(TextAnnotation.class).toLowerCase();
					String wordPos = token.get(PartOfSpeechAnnotation.class);
					String lemma = word;
					boolean located = false;
					
					if(wordPos.startsWith("NN") || wordPos.startsWith("JJ") || wordPos.startsWith("RB")) {
						featureCounter.incrementCount("lemma_" + lemma);
			
						
						if(negate) {
							featureCounter.incrementCount("not_lemma_" + lemma);
						}
					}

					
					if(wordPos.startsWith("JJ") | wordPos.startsWith("JJR")| wordPos.startsWith("JJS")) {
						String RelWord = "";
						for(int j = 1; j < 3; j++) {
							if(i+j<tokens.size()){
								if(tokens.get(i+j).get(PartOfSpeechAnnotation.class).startsWith("NN")|tokens.get(i+j).get(PartOfSpeechAnnotation.class).startsWith("NNS")| tokens.get(i+j).get(PartOfSpeechAnnotation.class).startsWith("NNP")| tokens.get(i+j).get(PartOfSpeechAnnotation.class).startsWith("NNPS")) {
									RelWord = tokens.get(i+j).get(TextAnnotation.class).toLowerCase();
									String prevLemma = tokens.get(i+j).get(LemmaAnnotation.class).toLowerCase();
									String prevLemmaPos = tokens.get(i+j).get(PartOfSpeechAnnotation.class);
									final String polarityFeature = "lemma_" + prevLemma + "_" + lemma ;
								final boolean negative = negate != (( qwordnet.getPolarity(lemma, wordPos)) < 0);
									if(negative) {
										featureCounter.decrementCount(polarityFeature);
									} else {
										featureCounter.incrementCount(polarityFeature);
									}
									located = true;
								}
							}
							if ((i-j)>0){
								if(tokens.get(i-j).get(PartOfSpeechAnnotation.class).startsWith("NN") |tokens.get(i-j).get(PartOfSpeechAnnotation.class).startsWith("NNP") |tokens.get(i-j).get(PartOfSpeechAnnotation.class).startsWith("NNS") |tokens.get(i-j).get(PartOfSpeechAnnotation.class).startsWith("NNPS")) {
									RelWord = tokens.get(i-j).get(TextAnnotation.class).toLowerCase();
									String prevLemma = tokens.get(i-j).get(LemmaAnnotation.class).toLowerCase();
									String prevLemmaPos = tokens.get(i-j).get(PartOfSpeechAnnotation.class);
									final String polarityFeature = "lemma_" + prevLemma + "_" + lemma ;
								final boolean negative = negate != (( qwordnet.getPolarity(lemma, wordPos)) < 0);
									if(negative) {
										featureCounter.decrementCount(polarityFeature);
									} else {
										featureCounter.incrementCount(polarityFeature);
									}
									located = true;
								}
							}
							if(located)
							{
								located = false;
								break;
							}
						}
					} 
					
					if(!negate) {
						for(String negationToken: NEGATION_TOKENS) {
							if(negationToken.equals(word)) {
								negate = true;
								break;
							}
						}
					}
				}
			}
			dataset.add(new RVFDatum<String, String>(featureCounter, doc.getClassification()));
			String temp = featureCounter.toString();
			temp = temp.replace(" ", "");
			temp = temp.replace("{", "");
			temp = temp.replace("}", "");
			temp = temp.replace("'", "");
			
			generatorData = generatorData.replace(" ", "");
			generatorData = generatorData.replace("{", "");
			generatorData = generatorData.replace("}", "");
			generatorData = generatorData.replace("'", "");
			double rnd = Math.random()*100;
			
			
			
			
			
			if ((rnd > TestSplit) &&( Num_Test <=CorpusMax)){
			trainf.write(doc.getClassification() +"	" +temp+"\n");
			Num_Train++;}
			else{
				testf.write(doc.getClassification() +"	" +temp+"\n");
			Num_Test++;}

			
		}
		
		String resp = "Total items: " + (Num_Test+Num_Train) + "Total test items: " + Num_Test + "Total train items: " + Num_Train ;
		System.out.println(CorpusMax);
		System.out.println("Data files created");
		System.out.println("Total items: " + (Num_Test+Num_Train));
		System.out.println("Total test items: " + Num_Test);
		System.out.println("Total train items: " + Num_Train);
	

		return resp;
		}
	
	public void PrintResults(){
		
		System.out.println("Data files created");
		System.out.println("Total items: " + (Num_Test+Num_Train));
		System.out.println("Total test items: " + Num_Test);
		System.out.println("Total train items: " + Num_Train);
		}
	
	
	
	public String GenerateSentence(String INsentence) throws IOException
	{
		
		
		RoomSentences = new ArrayList();
		ServiceSentences = new ArrayList();
		StaffSentences = new ArrayList();
		FacilitiesSentences = new ArrayList();
		
		QWordNetDB qwordnet = QWordNetDB.createInstance();
		
		Properties props = new Properties();
		props.put("annotators", "tokenize, ssplit, pos, lemma");
	
		
		StanfordCoreNLP pipeline = new StanfordCoreNLP(props);
		RVFDataset<String, String> dataset = new RVFDataset<String, String>();
		
			
			final Annotation document = new Annotation(INsentence);
			pipeline.annotate(document);

			final List<CoreMap> sentences = document.get(SentencesAnnotation.class);

			ClassicCounter<String> featureCounter = new ClassicCounter<String>();
			for (int h = 0; h < sentences.size(); h++) {
				final CoreMap sentence = sentences.get(h);
				final List<CoreLabel> tokens = sentence.get(TokensAnnotation.class);
			
				final Iterator<CoreLabel> it = tokens.iterator();
				
				while(it.hasNext()) {
					final String pos = it.next().get(PartOfSpeechAnnotation.class);
					if(pos.equals(".") || pos.equals(",") || pos.equals("``")) {
						it.remove();
					}
				}
				
				boolean negate = false;
				boolean roomFeat = false;
				boolean serviceFeat = false;
				boolean staffFeat = false;
				boolean facilityFeat = false;
				
				for (int i = 0; i < tokens.size(); i++) {
							
					final CoreLabel token = tokens.get(i);
					String lemma = token.get(LemmaAnnotation.class).toLowerCase();
					
					String word = token.get(TextAnnotation.class).toLowerCase();
					String wordPos = token.get(PartOfSpeechAnnotation.class);
					
					if(myTC.SearchFeature(word,0)){ roomFeat = true;}
					if(myTC.SearchFeature(word,1)){ serviceFeat = true;}
					if(myTC.SearchFeature(word,2)){ staffFeat = true;}
					if(myTC.SearchFeature(word,3)){ facilityFeat = true;}
	
					boolean located = false;
					if(wordPos.startsWith("NN") || wordPos.startsWith("JJ") || wordPos.startsWith("RB")) {
						featureCounter.incrementCount("lemma_" + lemma);
						
						
						if(negate) {
							featureCounter.incrementCount("not_lemma_" + lemma);
						}
					}

					
					if(wordPos.startsWith("JJ") | wordPos.startsWith("JJR")| wordPos.startsWith("JJS")) {
						String RelWord = "";
						for(int j = 1; j < 3; j++) {
							if(i+j<tokens.size()){
								if(tokens.get(i+j).get(PartOfSpeechAnnotation.class).startsWith("NN")|tokens.get(i+j).get(PartOfSpeechAnnotation.class).startsWith("NNS")| tokens.get(i+j).get(PartOfSpeechAnnotation.class).startsWith("NNP")| tokens.get(i+j).get(PartOfSpeechAnnotation.class).startsWith("NNPS")) {
									RelWord = tokens.get(i+j).get(TextAnnotation.class).toLowerCase();
									String prevLemma = tokens.get(i+j).get(LemmaAnnotation.class).toLowerCase();
									String prevLemmaPos = tokens.get(i+j).get(PartOfSpeechAnnotation.class);
									final String polarityFeature = "lemma_" + prevLemma + "_" + lemma ;
								final boolean negative = negate != (( qwordnet.getPolarity(lemma, wordPos)) < 0);
									if(negative) {
										featureCounter.decrementCount(polarityFeature);
									} else {
										featureCounter.incrementCount(polarityFeature);
									}
									located = true;
								}
							}
							if ((i-j)>0){
								if(tokens.get(i-j).get(PartOfSpeechAnnotation.class).startsWith("NN") |tokens.get(i-j).get(PartOfSpeechAnnotation.class).startsWith("NNP") |tokens.get(i-j).get(PartOfSpeechAnnotation.class).startsWith("NNS") |tokens.get(i-j).get(PartOfSpeechAnnotation.class).startsWith("NNPS")) {
									RelWord = tokens.get(i-j).get(TextAnnotation.class).toLowerCase();
									String prevLemma = tokens.get(i-j).get(LemmaAnnotation.class).toLowerCase();
									String prevLemmaPos = tokens.get(i-j).get(PartOfSpeechAnnotation.class);
									final String polarityFeature = "lemma_" + prevLemma + "_" + lemma ;
								final boolean negative = negate != (( qwordnet.getPolarity(lemma, wordPos)) < 0);
									if(negative) {
										featureCounter.decrementCount(polarityFeature);
									} else {
										featureCounter.incrementCount(polarityFeature);
									}
									located = true;
								}
							}
							if(located)
							{
								located = false;
								break;
							}
						}
					} 
					
			
					/////////////////////////////////////////////////
					
					if(!negate) {
						for(String negationToken: NEGATION_TOKENS) {
							if(negationToken.equals(word)) {
								negate = true;
								break;
							}
						}
					}
					
				}
				if(roomFeat){
					RoomSentences.add(sentence.toString());
					}
				if(serviceFeat){
					ServiceSentences.add(sentence.toString());
					}
				if(staffFeat){
					StaffSentences.add(sentence.toString());
					}
				if(facilityFeat){
					FacilitiesSentences.add(sentence.toString());
					}
			}

			String temp = featureCounter.toString();
		
			
			String resp = "AA" + "	" +temp;
	
			
			
			
				
		return resp;
		}
	}
	
