package test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import cern.colt.matrix.DoubleMatrix2D;

import parser.Prediction;
import parser.Rule;
import parser.RuleFile;
import parser.EdgeSpace;
import recursion.ClosureMatrix;
import recursion.RelationMatrix;
import utility.Utility;
import edu.stanford.nlp.parser.lexparser.IntTaggedWord;
import edu.stanford.nlp.stats.Counter;
import edu.stanford.nlp.util.HashIndex;
import edu.stanford.nlp.util.Index;
import junit.framework.TestCase;


public class PredictionTest extends TestCase {
  String ruleString = 
    "ROOT->[A] : 1.0\n" + 
    "A->[A B] : 0.1\n" +
    "A->[B C] : 0.2\n" +
    "A->[A1] : 0.3\n" +
    "A->[_d _e] : 0.4\n" +
    "A1->[A2] : 1.0\n" +
    "B->[C] : 0.2\n" +
    "B->[D E] : 0.8\n" +
    "C->[B] : 0.3\n" +
    "C->[D] : 0.7\n" +
    "A2->[_a] : 1.0\n" +
    "D->[_d] : 1.0\n" +
    "E->[_e] : 1.0\n";
  
  public void testBasic(){
    System.err.println(ruleString);
    Index<String> wordIndex = new HashIndex<String>();
    Index<String> tagIndex = new HashIndex<String>();
    
    Collection<Rule> rules = new ArrayList<Rule>();
    Collection<Rule> extendedRules = new ArrayList<Rule>();
    
    Map<Integer, Counter<Integer>> tag2wordsMap = new HashMap<Integer, Counter<Integer>>();
    Map<Integer, Set<IntTaggedWord>> word2tagsMap = new HashMap<Integer, Set<IntTaggedWord>>();
    Set<Integer> nonterminals = new HashSet<Integer>();
    
    
    
    try {
      RuleFile.parseRuleFile(Utility.getBufferedReaderFromString(ruleString), 
          rules, extendedRules, tag2wordsMap, word2tagsMap, 
          nonterminals, wordIndex, tagIndex);
    } catch (IOException e){
      System.err.println("Error reading rules: " + ruleString);
      e.printStackTrace();
    }

    // statespace
    EdgeSpace stateSpace = new EdgeSpace(tagIndex);
    stateSpace.addRules(rules);
    
    // closure matrix
    RelationMatrix relationMatrix = new RelationMatrix(tagIndex);
    DoubleMatrix2D pl = relationMatrix.getPL(rules, nonterminals);
    ClosureMatrix leftCornerClosures = new ClosureMatrix(pl);
    
    Prediction[][] predictions = Prediction.constructPredictions(rules, leftCornerClosures, stateSpace, tagIndex);
    StringBuffer sb = new StringBuffer();
    for (int i = 0; i < predictions.length; i++) {
      sb.append(stateSpace.get(i).toString(tagIndex, tagIndex) + ", " + Utility.sprint(predictions[i], stateSpace, tagIndex) + "\n");
    }
    assertEquals(sb.toString(), "ROOT -> ., ()\nROOT -> . A, ((A -> . A B,f=0.1111,i=0.1000), (A -> . B C,f=0.2222,i=0.2000), (B -> . D E,f=0.1891,i=0.8000))\nA -> ., ()\nA -> . A B, ((A -> . A B,f=0.1111,i=0.1000), (A -> . B C,f=0.2222,i=0.2000), (B -> . D E,f=0.1891,i=0.8000))\nA -> A . B, ((B -> . D E,f=0.8511,i=0.8000))\nB -> ., ()\nA -> . B C, ((B -> . D E,f=0.8511,i=0.8000))\nA -> B . C, ((B -> . D E,f=0.2553,i=0.8000))\nC -> ., ()\nA -> . A1, ()\nA1 -> ., ()\nA1 -> . A2, ()\nA2 -> ., ()\nB -> . C, ((B -> . D E,f=0.2553,i=0.8000))\nB -> . D E, ()\nD -> ., ()\nB -> D . E, ()\nE -> ., ()\nC -> . B, ((B -> . D E,f=0.8511,i=0.8000))\nC -> . D, ()\n");    
  }
}