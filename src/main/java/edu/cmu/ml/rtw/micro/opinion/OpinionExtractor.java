package edu.cmu.ml.rtw.micro.opinion;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.cmu.ml.rtw.generic.data.annotation.AnnotationType;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.AnnotationTypeNLP;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.ConstituencyParse;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.DependencyParse;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.AnnotationTypeNLP.Target;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.micro.Annotation;
import edu.cmu.ml.rtw.generic.model.annotator.nlp.AnnotatorSentence;
import edu.cmu.ml.rtw.generic.model.annotator.nlp.AnnotatorTokenSpan;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.DocumentNLP;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.PoSTag;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.PoSTagClass;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.TokenSpan;
import edu.cmu.ml.rtw.generic.util.FileUtil;
import edu.cmu.ml.rtw.generic.util.Pair;
import edu.cmu.ml.rtw.generic.util.Triple;
import edu.cmu.ml.rtw.micro.opinion.NarSystem;
import edu.stanford.nlp.process.Morphology;


public class OpinionExtractor implements AnnotatorTokenSpan<String> {
	private static final AnnotationType<?>[] REQUIRED_ANNOTATIONS = new AnnotationType<?>[] {
		AnnotationTypeNLP.TOKEN,
		AnnotationTypeNLP.SENTENCE,
		AnnotationTypeNLP.POS,
		AnnotationTypeNLP.LEMMA,
		AnnotationTypeNLP.DEPENDENCY_PARSE,
		AnnotationTypeNLP.CONSTITUENCY_PARSE
	};

	public static final AnnotationTypeNLP<String> OPINION_FRAME = new AnnotationTypeNLP<String>("nell-opinion", String.class, Target.TOKEN_SPAN);

	private static class Singleton {
		private static final OpinionExtractor INSTANCE = new OpinionExtractor();
	}

	public static OpinionExtractor getInstance() {
		return Singleton.INSTANCE;
	}

	private OpinionExtractor() {
		NarSystem.loadLibrary();
	}

	private native boolean initialize(String configFile);
	private native String  annotate(String inputData);

	@Override
	public String getName() {
		return "cmunell_opinion-0.0.1";
	}

	@Override
	public AnnotationType<String> produces() {
		return OPINION_FRAME;
	}

	@Override
	public AnnotationType<?>[] requires() {
		return REQUIRED_ANNOTATIONS;
	}

	@Override
	public boolean measuresConfidence() {
		return true;
	}

	@Override
	public List<Triple<TokenSpan, String, Double>> annotate(DocumentNLP document) {
		try {
			InputStream resource = OpinionExtractor.class.getResourceAsStream("/opinion.config");
			BufferedReader bfr = new BufferedReader(new InputStreamReader(resource));
			String line;
			String configStr = "";
			while ((line = bfr.readLine()) != null) {
				configStr += line + "\n";
			}
			bfr.close();

			if (!initialize(configStr))
				throw new IllegalStateException("Unable to initialize opinion tagger.");
			
		} catch (IOException ioe) {
		    throw new RuntimeException(ioe);
		}
		
		StringBuilder inputData = new StringBuilder();
		
		for (int i = 0; i < document.getSentenceCount(); i++) {
			inputData.append("#begin sentence\n");
			
			List<PoSTag> tags = document.getSentencePoSTags(i);
		    List<String> words = document.getSentenceTokenStrs(i);
		   
		    String parseTree = document.getConstituencyParse(i).toString();
		    String depGraph = document.getDependencyParse(i).toString();
		    
		    StringBuilder sentStr = new StringBuilder();
		    for (String word : words) {
		    	sentStr.append(word).append(" ");
		    }
		    sentStr.setLength(sentStr.length() - 1);
		    inputData.append(sentStr.toString()).append("\n");
		    
		    StringBuilder posTagStr = new StringBuilder();
			for (PoSTag posTag : tags) {
				posTagStr.append(posTag).append(" ");
			}
			posTagStr.setLength(posTagStr.length() - 1);
			inputData.append(posTagStr.toString()).append("\n");
			
			StringBuilder sentLemmaStr = new StringBuilder();
			for (int j = 0; j < words.size(); ++j) {
				String lemmaWord = document.getTokenAnnotation(AnnotationTypeNLP.LEMMA, i, j);
		    	sentLemmaStr.append(lemmaWord).append(" ");
		    }
			sentLemmaStr.setLength(sentLemmaStr.length() - 1);
			inputData.append(sentLemmaStr.toString()).append("\n");
			
			inputData.append(parseTree).append("\n");
			inputData.append(depGraph);
			
			inputData.append("#end sentence\n");
		}
		
		String outputData = annotate(inputData.toString());
		
		List<Triple<TokenSpan, String, Double>> annotations = new ArrayList<Triple<TokenSpan, String, Double>>();
		
		String[] opinionAnnos = outputData.split("\n");
		for (int i = 0; i < opinionAnnos.length; ++i) {
			String[] fields = opinionAnnos[i].split(",");
			if (fields.length != 5) continue;
			
			int sentIndex = Integer.valueOf(fields[0]);
			int tokenStart = Integer.valueOf(fields[1]);
			int tokenEnd = Integer.valueOf(fields[2]);
			String opinionLabel = fields[3];
			double score = Double.valueOf(fields[4]);
			
			annotations.add(new Triple<TokenSpan, String, Double>(new TokenSpan(document, sentIndex, tokenStart, tokenEnd), 
					opinionLabel, score));
		}
		
		return annotations;
	}
}