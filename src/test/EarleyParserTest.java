package test;


import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;
import org.junit.Before;
import org.junit.Test;

import edu.stanford.nlp.trees.Tree;

import base.ClosureMatrix;
import base.RelationMatrix;

import parser.Completion;
import parser.EarleyParser;
import parser.EarleyParserDense;
import parser.EarleyParserSparse;
import parser.EdgeSpace;
import parser.Grammar;
import parser.Prediction;
import util.RuleFile;
import util.Util;

public class EarleyParserTest extends TestCase {
  private EarleyParser parser;
  private String rootSymbol = "ROOT";
  private int parserOpt = 1; // 0: dense, 1: sparse, 2: sparse IO
  private boolean isScaling = false; // 
  private boolean isLogProb = true; 
  private int insideOutsideOpt = 2; // false; //          
  private String objString = "surprisal,stringprob,viterbi";
  
  @Before
  public void setUp(){    
    // set output verbose modes
    RelationMatrix.verbose = 0;
    ClosureMatrix.verbose = 0;
    EdgeSpace.verbose = 0;
    Grammar.verbose = 0;
    Prediction.verbose = 0;
    Completion.verbose = 0;
    RuleFile.verbose = 0;
    EarleyParser.verbose = -1;
  }
  
  String basicGrammarString = "ROOT->[A B] : 0.9\n" + 
  "ROOT->[_a _b] : 0.1\n" +
  "A->[_a] : 1.0\n" +
  "B->[_b] : 1.0\n";
  
  String basicUnaryGrammarString = "ROOT->[X] : 1.0\n" +
  "X->[A B] : 0.9\n" + 
  "X->[_a _b] : 0.1\n" +
  "A->[_a] : 1.0\n" +
  "B->[_b] : 1.0\n";
  
  String grammarString = "ROOT->[A] : 1.0\n" + 
  "A->[B C] : 0.5\n" +
  "A->[D B] : 0.5\n" +
  "B->[_b] : 0.9\n" +
  "B->[_UNK] : 0.1\n" +
  "C->[_c] : 0.9\n" +
  "C->[_UNK] : 0.1\n" +
  "D->[_d] : 0.8\n" +
  "D->[_UNK] : 0.1\n" +
  "D->[_UNK-1] : 0.1\n";

  String recursiveGrammarString = "ROOT->[A] : 1.0\n" + 
  "A->[B C] : 0.5\n" +
  "A->[D B] : 0.5\n" +
  "B->[A] : 0.1\n" +
  "B->[_b] : 0.8\n" +
  "B->[_UNK] : 0.1\n" +
  "C->[_c] : 0.9\n" +
  "C->[_UNK] : 0.1\n" +
  "D->[_d] : 0.8\n" +
  "D->[_UNK] : 0.1\n" +
  "D->[_UNK-1] : 0.1\n";

  String extendedGrammarString = "ROOT->[A] : 1.0\n" + 
  "A->[_b _e] : 0.1\n" +
  "A->[_b _c] : 0.1\n" +
  "A->[_d _c] : 0.1\n" +
  "A->[B C] : 0.6\n" +
  "A->[D C] : 0.1\n" +
  "B->[A] : 0.1\n" +
  "B->[_b] : 0.8\n" +
  "B->[_UNK] : 0.1\n" +
  "C->[_c] : 0.9\n" +
  "C->[_UNK] : 0.1\n" + 
  "D->[_d] : 0.9\n" +
  "D->[_UNK] : 0.1\n";
  
  String wsj500AG = "grammars/WSJ.500/WSJ.500.AG-PCFG.extendedRules";
  String wsj500 = "grammars/wsj500unk.grammar";
  String wsj5 = "grammars/wsj5unk.grammar";
  String social = "grammars/social.iogrammar";
  String socialDiscourse = "grammars/socialDiscourse.grammar";
  String markGrammarFile = "grammars/testengger.grammar";
  String markGrammarVBFile = "grammars/testengger-bayes.grammar";
  
  private void initParserFromFile(String ruleFile){
    int inGrammarType = 1; // read from grammar
    if(parserOpt==0){
      parser = new EarleyParserDense(ruleFile, inGrammarType, rootSymbol, isScaling, 
          isLogProb, insideOutsideOpt, objString);
    } else if(parserOpt==1){
      parser = new EarleyParserSparse(ruleFile, inGrammarType, rootSymbol, isScaling, 
          isLogProb, insideOutsideOpt, objString);
    } else {
      assert(false);
    }
  }
  
  private void initParserFromString(String grammarString){
    try {
      if(parserOpt==0){
        parser= new EarleyParserDense(Util.getBufferedReaderFromString(grammarString), 
            rootSymbol, isScaling, isLogProb, insideOutsideOpt, objString);
      } else if(parserOpt==1){    
        parser= new EarleyParserSparse(Util.getBufferedReaderFromString(grammarString), 
            rootSymbol, isScaling, isLogProb, insideOutsideOpt, objString);
      }
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    }
  }

  @Test
  public void testBasic(){
    initParserFromString(basicGrammarString);
    
    String inputSentence = "a b";

    parser.parseSentence(inputSentence);
    
    List<Double> surprisalList = parser.getSurprisalList();
    List<Double> stringProbList = parser.getStringProbList();
    
    assertEquals(surprisalList.size(), 2);
    assertEquals(0.0, surprisalList.get(0), 1e-5);
    assertEquals(0.0, surprisalList.get(1), 1e-5);
    
    
    assertEquals(stringProbList.size(), 2);
    assertEquals(0.0, stringProbList.get(0), 1e-5);
    assertEquals(1.0, stringProbList.get(1), 1e-5);
    
    if(parser.getDecodeOpt()==1){
      Tree tree = parser.viterbiParse();
      System.err.println(tree.toString());
      assertEquals(tree.toString(), "( (ROOT (A a) (B b)))");
    }
  }
  
  public void testExtendedGrammarIO(){
    initParserFromString(extendedGrammarString);
    
    List<String> inputSentences = new ArrayList<String>();
    inputSentences.add("b c");
    inputSentences.add("d c");
    
    if(insideOutsideOpt>0){
      List<Double> sumNegLogProbList = parser.insideOutside(inputSentences);
      System.err.println(sumNegLogProbList);
      assertEquals(sumNegLogProbList.size()==3, true);
      
      if(insideOutsideOpt==1){
        assertEquals(2.340370037356804, sumNegLogProbList.get(0), 1e-5);
        assertEquals(1.3862943611198908, sumNegLogProbList.get(1), 1e-5);
        assertEquals(1.3862943611198908, sumNegLogProbList.get(2), 1e-5);  
      } else {
        assertEquals(2.340370037356804, sumNegLogProbList.get(0), 1e-5);
        assertEquals(1.413647688917206, sumNegLogProbList.get(1), 1e-5);
        assertEquals(1.4799787183333548, sumNegLogProbList.get(2), 1e-5);
      }
    }
  }

  public void testMarkGrammar(){
    rootSymbol = "S";
    initParserFromFile(markGrammarFile);
    
    String inputSentence = "the dog bites a cat";

    parser.parseSentence(inputSentence);
    
    List<Double> surprisalList = parser.getSurprisalList();
    List<Double> stringProbList = parser.getStringProbList();
    
    assertEquals(surprisalList.size(), 5);
    assertEquals(1.9459104490553583, surprisalList.get(0), 1e-5);
    assertEquals(1.9459104490553583, surprisalList.get(1), 1e-5);
    assertEquals(1.9459104490553583, surprisalList.get(2), 1e-5);
    assertEquals(2.1690540003695684, surprisalList.get(3), 1e-5);
    assertEquals(1.9459104490553583, surprisalList.get(4), 1e-5);

    assertEquals(stringProbList.size(), 5);
    assertEquals(0.0, stringProbList.get(0), 1e-5);
    assertEquals(0.0, stringProbList.get(1), 1e-5);
    assertEquals(5.83089854227563E-4, stringProbList.get(2), 1e-5);
    assertEquals(0.0, stringProbList.get(3), 1e-5);
    assertEquals(2.3799571607089895E-5, stringProbList.get(4), 1e-5);
    

    assertEquals(parser.dumpInsideChart(), "# Inside chart snapshot\ncell 0-1\n Det: 0.14285709999999996\n N: 0.14285709999999996\n V: 0.14285709999999996\ncell 1-2\n Det: 0.14285709999999996\n N: 0.14285709999999996\n V: 0.14285709999999996\ncell 2-3\n Det: 0.14285709999999996\n N: 0.14285709999999996\n V: 0.14285709999999996\ncell 3-4\n Det: 0.14285709999999996\n N: 0.14285709999999996\n V: 0.14285709999999996\ncell 4-5\n Det: 0.14285709999999996\n N: 0.14285709999999996\n V: 0.14285709999999996\ncell 0-2\n NP: 0.020408151020410003\ncell 2-4\n NP: 0.020408151020410003\ncell 3-5\n NP: 0.020408151020410003\ncell 0-3\n : 5.83089854227563E-4\n S: 5.83089854227563E-4\ncell 2-5\n VP: 0.001166179708455125\ncell 0-5\n : 2.3799571607089895E-5\n S: 2.3799571607089895E-5\n");
    
    if(parser.getDecodeOpt()==1){
      Tree tree = parser.viterbiParse();
      boolean matched = tree.toString().equals("( (S (NP (N the) (Det dog)) (VP (NP (N bites) (Det a)) (V cat))))")
          || tree.toString().equals("( (S (NP (Det the) (N dog)) (VP (V bites) (NP (Det a) (N cat)))))");
     
      assertEquals(matched, true);
    }
    
    if(insideOutsideOpt>0){
      assertEquals(parser.dumpOutsideChart(), "# Outside chart snapshot\ncell 0-1\n Det: 8.329852561437227E-5\n N: 8.329852561437227E-5\ncell 1-2\n Det: 8.329852561437227E-5\n N: 8.329852561437227E-5\ncell 2-3\n Det: 4.1649262807186136E-5\n N: 4.1649262807186136E-5\n V: 8.329852561437242E-5\ncell 3-4\n Det: 8.329852561437227E-5\n N: 8.329852561437227E-5\ncell 4-5\n Det: 4.1649262807186204E-5\n N: 4.1649262807186204E-5\n V: 8.329852561437227E-5\ncell 0-2\n NP: 0.001166179708455125\ncell 2-4\n NP: 5.830898542275625E-4\ncell 3-5\n NP: 5.83089854227563E-4\ncell 2-5\n VP: 0.020408151020410003\ncell 0-5\n : 1.0\n S: 1.0\n");
      
      List<String> inputSentences = new ArrayList<String>();
      inputSentences.add("the cat bites a dog");
      inputSentences.add("a cat gives the dog a bone");
      inputSentences.add("the dog gives a cat the bone");
      inputSentences.add("a dog bites a bone");
      inputSentences.add("the dog bites");
      parser.parseSentences(inputSentences);
      assertEquals(parser.sprintExpectedCounts(), "# Expected counts\n6.000000 S->[NP VP]\n6.500000 NP->[Det N]\n6.500000 NP->[N Det]\n1.000000 VP->[V]\n1.500000 VP->[V NP]\n1.500000 VP->[NP V]\n1.000000 VP->[V NP NP]\n1.000000 VP->[NP NP V]\n3.000000 Det->[_the]\n3.000000 N->[_the]\n3.500000 Det->[_a]\n3.500000 N->[_a]\n2.750000 Det->[_dog]\n2.750000 N->[_dog]\n0.500000 V->[_dog]\n1.750000 Det->[_cat]\n1.750000 N->[_cat]\n0.500000 V->[_cat]\n0.750000 Det->[_bone]\n0.750000 N->[_bone]\n1.500000 V->[_bone]\n0.750000 Det->[_bites]\n0.750000 N->[_bites]\n2.500000 V->[_bites]\n0.500000 Det->[_gives]\n0.500000 N->[_gives]\n1.000000 V->[_gives]\n");
    }    
  }
  
  public void testMarkGrammarIO(){
    rootSymbol = "S";
    if(insideOutsideOpt==2){ // VB
      initParserFromFile(markGrammarVBFile);
    } else {
      initParserFromFile(markGrammarFile);
    }
    
    List<String> inputSentences = new ArrayList<String>();
    inputSentences.add("the dog bites a cat");
    inputSentences.add("the cat bites a dog");
    inputSentences.add("a cat gives the dog a bone");
    inputSentences.add("the dog gives a cat the bone");
    inputSentences.add("a dog bites a bone");
    inputSentences.add("the dog bites");

    if(insideOutsideOpt>0){
      List<Double> sumNegLogProbList = parser.insideOutside(inputSentences);
      
      if(insideOutsideOpt==1){
        assertEquals(parser.sprintExpectedCounts(), "# Expected counts\n6.000000 S->[NP VP]\n6.500000 NP->[Det N]\n6.500000 NP->[N Det]\n1.000000 VP->[V]\n3.000000 VP->[V NP]\n2.000000 VP->[V NP NP]\n3.000000 Det->[_the]\n3.000000 N->[_the]\n3.500000 Det->[_a]\n3.500000 N->[_a]\n3.000000 Det->[_dog]\n3.000000 N->[_dog]\n2.000000 Det->[_cat]\n2.000000 N->[_cat]\n1.500000 Det->[_bone]\n1.500000 N->[_bone]\n4.000000 V->[_bites]\n2.000000 V->[_gives]\n");
        
        assertEquals(sumNegLogProbList.size()==9, true);
        assertEquals(68.46002594157635, sumNegLogProbList.get(0), 1e-5);
        assertEquals(58.5105558430655, sumNegLogProbList.get(1), 1e-5);
        assertEquals(55.22092447712423, sumNegLogProbList.get(2), 1e-5);
        assertEquals(53.7010153144059, sumNegLogProbList.get(3), 1e-5);
        assertEquals(52.26369575985821, sumNegLogProbList.get(4), 1e-5);
        assertEquals(50.759354005056494, sumNegLogProbList.get(5), 1e-5);
        assertEquals(50.634558651715075, sumNegLogProbList.get(6), 1e-5);
        assertEquals(50.63452160196378, sumNegLogProbList.get(7), 1e-5);
        assertEquals(50.634521601963776, sumNegLogProbList.get(8), 1e-5);
      } else {
        assertEquals(parser.sprintExpectedCounts(), "# Expected counts\n6.000000 S->[NP VP]\n13.000000 NP->[N Det]\n1.000000 VP->[V]\n3.000000 VP->[V NP]\n2.000000 VP->[V NP NP]\n6.000000 N->[_the]\n7.000000 N->[_a]\n6.010000 Det->[_dog]\n4.010000 Det->[_cat]\n3.010000 Det->[_bone]\n4.000000 V->[_bites]\n2.000000 V->[_gives]\n");  
      
        assertEquals(sumNegLogProbList.size()==10, true);
        assertEquals(68.46002594157635, sumNegLogProbList.get(0), 1e-5);
        assertEquals(58.16078910958829, sumNegLogProbList.get(1), 1e-5);
        assertEquals(54.2859492179949, sumNegLogProbList.get(2), 1e-5);
        assertEquals(50.989929766024545, sumNegLogProbList.get(3), 1e-5);
        assertEquals(50.64880443801494, sumNegLogProbList.get(4), 1e-5);
        assertEquals(49.41582153040268, sumNegLogProbList.get(5), 1e-5);
        assertEquals(41.10788394988927, sumNegLogProbList.get(6), 1e-5);
        assertEquals(32.69267818582213, sumNegLogProbList.get(7), 1e-5);
        assertEquals(32.690184343666786, sumNegLogProbList.get(8), 1e-5);
        assertEquals(32.690184343666786, sumNegLogProbList.get(9), 1e-5);
      }
      
      
    }
  }

  public void testBasicUnary(){
    initParserFromString(basicUnaryGrammarString);
    
    String inputSentence = "a b";

    parser.parseSentence(inputSentence);
    
    List<Double> surprisalList = parser.getSurprisalList();
    List<Double> stringProbList = parser.getStringProbList();
    
    for (int i = 0; i < surprisalList.size(); i++) {
      System.err.println(i + "\t" + surprisalList.get(i));
    }
    
    assertEquals(surprisalList.size(), 2);
    assertEquals(0.0, surprisalList.get(0), 1e-5);
    assertEquals(0.0, surprisalList.get(1), 1e-5);
    
    assertEquals(stringProbList.size(), 2);
    assertEquals(0.0, stringProbList.get(0), 1e-5);
    assertEquals(1.0, stringProbList.get(1), 1e-5);
    

    if(parser.getDecodeOpt()==1){
      Tree tree = parser.viterbiParse();
      System.err.println(tree.toString());
      assertEquals(tree.toString(), "( (ROOT (X (A a) (B b))))");
    }
    
  }
  
  public String getLeftInfiniteGrammar(double p){
    double q = 1-p;
    return "ROOT->[X] : " + p + "\n" +
    "ROOT->[ROOT X] : " + q + "\n" +
    "X->[_x] : 1.0\n";
  }
  
  public void testLeftInfiniteGrammar(){
    double p = 0.1;
    double q = 1-p;
    initParserFromString(getLeftInfiniteGrammar(p));
    
    int numSymbols = 100;
    StringBuffer sb = new StringBuffer("x");
    for (int i = 0; i < (numSymbols-1); i++) {
      sb.append(" x");
    }
    String inputSentence = sb.toString();
    

    parser.parseSentence(inputSentence);
    
    List<Double> surprisalList = parser.getSurprisalList();
    List<Double> stringProbList = parser.getStringProbList();
    
    assert(surprisalList.size() == numSymbols);
    
    // string x: string prob = p, prefix prob = 1.0, surprisal = -log(1)=0
    assertEquals(0, surprisalList.get(0), 1e-10);
    
    
    assert(stringProbList.size()==numSymbols);
    assertEquals(p, stringProbList.get(0), 1e-10);
    
    for (int i = 1; i < numSymbols; i++) {
      // strings of (i+1) x: string prob = p*q^i, prefix prob = q^i, surprisal = -log(q)
      assertEquals(-Math.log(q), surprisalList.get(i), 1e-10);
      if(!isScaling){
        assertEquals(p*Math.pow(q, i), stringProbList.get(i), 1e-10);
      }
    }
  }
  
  public void testLeftInfiniteIO(){
    double p=0.1;
    initParserFromString(getLeftInfiniteGrammar(p));
    
    List<String> inputSentences = new ArrayList<String>();
    inputSentences.add("x x x x x x x x x x");
    inputSentences.add("x x x x x");

    String inputSentence = inputSentences.get(0);

    parser.parseSentence(inputSentence);

//    System.err.println(parser.dumpInsideChart());
//    System.err.println(parser.dumpOutsideChart());
    assertEquals(parser.dumpInsideChart(), "# Inside chart snapshot\ncell 0-1\n : 0.10000000000000002\n X: 1.0\ncell 1-2\n X: 1.0\ncell 2-3\n X: 1.0\ncell 3-4\n X: 1.0\ncell 4-5\n X: 1.0\ncell 5-6\n X: 1.0\ncell 6-7\n X: 1.0\ncell 7-8\n X: 1.0\ncell 8-9\n X: 1.0\ncell 9-10\n X: 1.0\ncell 0-2\n : 0.09000000000000002\n ROOT: 0.09000000000000002\ncell 0-3\n : 0.08100000000000002\n ROOT: 0.08100000000000002\ncell 0-4\n : 0.0729\n ROOT: 0.0729\ncell 0-5\n : 0.06561\n ROOT: 0.06561\ncell 0-6\n : 0.059049000000000004\n ROOT: 0.059049000000000004\ncell 0-7\n : 0.0531441\n ROOT: 0.0531441\ncell 0-8\n : 0.047829689999999994\n ROOT: 0.047829689999999994\ncell 0-9\n : 0.043046720999999996\n ROOT: 0.043046720999999996\ncell 0-10\n : 0.03874204889999999\n ROOT: 0.03874204889999999\n");
    if(insideOutsideOpt>0){
      assertEquals(parser.dumpOutsideChart(), "# Outside chart snapshot\ncell 0-1\n X: 0.03874204889999999\ncell 1-2\n X: 0.03874204889999999\ncell 2-3\n X: 0.03874204889999999\ncell 3-4\n X: 0.03874204889999999\ncell 4-5\n X: 0.03874204889999999\ncell 5-6\n X: 0.03874204889999999\ncell 6-7\n X: 0.03874204889999999\ncell 7-8\n X: 0.03874204889999999\ncell 8-9\n X: 0.03874204889999999\ncell 9-10\n X: 0.03874204889999999\ncell 0-2\n ROOT: 0.43046721\ncell 0-3\n ROOT: 0.47829689999999997\ncell 0-4\n ROOT: 0.531441\ncell 0-5\n ROOT: 0.5904900000000001\ncell 0-6\n ROOT: 0.6561\ncell 0-7\n ROOT: 0.7290000000000001\ncell 0-8\n ROOT: 0.81\ncell 0-9\n ROOT: 0.9\ncell 0-10\n : 1.0\n ROOT: 1.0\n");
    
      parser.parseSentences(inputSentences.subList(1, inputSentences.size()));
//      System.err.println(parser.sprintExpectedCounts());
      assertEquals(parser.sprintExpectedCounts(), "# Expected counts\n2.000000 ROOT->[X]\n13.000000 ROOT->[ROOT X]\n15.000000 X->[_x]\n");
    }
  }
  
  public String getCatalanGrammar(double p){
    double q = 1-p;
    return "ROOT->[X] : " + p + "\n" +
      "ROOT->[ROOT ROOT] : " + q + "\n" +
      "X->[_x] : 1.0\n";
  }
  
  public void testCatalanGrammar(){
    double p=0.1;
    double q = 1-p;
    initParserFromString(getCatalanGrammar(p));
    
    int numSymbols = 100;
    StringBuffer sb = new StringBuffer("x");
    for (int i = 0; i < (numSymbols-1); i++) {
      sb.append(" x");
    }
    String inputSentence = sb.toString();
    

    parser.parseSentence(inputSentence);
    
    List<Double> surprisalList = parser.getSurprisalList();
    List<Double> stringProbList = parser.getStringProbList();
    assert(surprisalList.size() == numSymbols);
    
    // string x: string prob[1] = p, prefix prob[1] = 1.0, surprisal = -log(1)=0
    assertEquals(0, surprisalList.get(0), 1e-10);
    if(!isScaling){
      assert(stringProbList.size()==numSymbols);
      assertEquals(p, stringProbList.get(0), 1e-10);
    }
    
    /// TO THINK: by right if p<q, the total prob string < 1, should prefix prob[1] < 1.0 ?
    //double totalStringProb = Math.min(1, p/q); // see "The Linguist's Guide to Statistics", section 4.6
    // ANSWER: Stolcke's approach only handles left-corner recursion and will be incorrect for the Catalan grammar
    int[][] c = Util.permutationMatrix(2*numSymbols);
    
    double prevPrefixProb = 1.0;
    double prevStringProb = p;
    double totalStringProb = p;
    for (int i = 1; i < numSymbols; i++) {
      // strings of (i+1) x: string prob[i+1] = (c[2i][i]/(i+1))*p^(i+1)*q^i
      // prefix prob[i+1] = prefix prob[i]-string prob[i]
      double currentStringProb = (c[2*i][i]/(i+1.0))*Math.pow(p,i+1)*Math.pow(q, i);
      assertEquals(currentStringProb, stringProbList.get(i), 1e-10);
      
      double currentPrefixProb = prevPrefixProb - prevStringProb;
      assertEquals(-Math.log(currentPrefixProb/prevPrefixProb), surprisalList.get(i), 1e-10);
      
      // update
      prevPrefixProb = currentPrefixProb;
      prevStringProb = currentStringProb;
      
      totalStringProb += currentStringProb;
      System.err.println(totalStringProb);
    }
  }
  
  public void testCatalanIO(){
    double p=0.1;
    initParserFromString(getCatalanGrammar(p));
    
    List<String> inputSentences = new ArrayList<String>();
    inputSentences.add("x x x x x x x x x x");
    inputSentences.add("x x x x x");

    String inputSentence = inputSentences.get(0);

    parser.parseSentence(inputSentence);

    assertEquals(parser.dumpInsideChart(), "# Inside chart snapshot\ncell 0-1\n : 0.10000000000000002\n X: 1.0\ncell 1-2\n X: 1.0\ncell 2-3\n X: 1.0\ncell 3-4\n X: 1.0\ncell 4-5\n X: 1.0\ncell 5-6\n X: 1.0\ncell 6-7\n X: 1.0\ncell 7-8\n X: 1.0\ncell 8-9\n X: 1.0\ncell 9-10\n X: 1.0\ncell 0-2\n : 0.009000000000000008\n ROOT: 0.009000000000000008\ncell 1-3\n ROOT: 0.009000000000000008\ncell 2-4\n ROOT: 0.009000000000000008\ncell 3-5\n ROOT: 0.009000000000000008\ncell 4-6\n ROOT: 0.009000000000000008\ncell 5-7\n ROOT: 0.009000000000000008\ncell 6-8\n ROOT: 0.009000000000000008\ncell 7-9\n ROOT: 0.009000000000000008\ncell 8-10\n ROOT: 0.009000000000000008\ncell 0-3\n : 0.0016200000000000019\n ROOT: 0.0016200000000000019\ncell 1-4\n ROOT: 0.0016200000000000019\ncell 2-5\n ROOT: 0.0016200000000000019\ncell 3-6\n ROOT: 0.0016200000000000019\ncell 4-7\n ROOT: 0.0016200000000000019\ncell 5-8\n ROOT: 0.0016200000000000019\ncell 6-9\n ROOT: 0.0016200000000000019\ncell 7-10\n ROOT: 0.0016200000000000019\ncell 0-4\n : 3.6450000000000084E-4\n ROOT: 3.6450000000000084E-4\ncell 1-5\n ROOT: 3.6450000000000084E-4\ncell 2-6\n ROOT: 3.6450000000000084E-4\ncell 3-7\n ROOT: 3.6450000000000084E-4\ncell 4-8\n ROOT: 3.6450000000000084E-4\ncell 5-9\n ROOT: 3.6450000000000084E-4\ncell 6-10\n ROOT: 3.6450000000000084E-4\ncell 0-5\n : 9.185400000000015E-5\n ROOT: 9.185400000000015E-5\ncell 1-6\n ROOT: 9.185400000000015E-5\ncell 2-7\n ROOT: 9.185400000000015E-5\ncell 3-8\n ROOT: 9.185400000000015E-5\ncell 4-9\n ROOT: 9.185400000000015E-5\ncell 5-10\n ROOT: 9.185400000000015E-5\ncell 0-6\n : 2.4800580000000034E-5\n ROOT: 2.4800580000000034E-5\ncell 1-7\n ROOT: 2.4800580000000034E-5\ncell 2-8\n ROOT: 2.4800580000000034E-5\ncell 3-9\n ROOT: 2.4800580000000034E-5\ncell 4-10\n ROOT: 2.4800580000000034E-5\ncell 0-7\n : 7.015021200000011E-6\n ROOT: 7.015021200000011E-6\ncell 1-8\n ROOT: 7.015021200000011E-6\ncell 2-9\n ROOT: 7.015021200000011E-6\ncell 3-10\n ROOT: 7.015021200000011E-6\ncell 0-8\n : 2.051893701000006E-6\n ROOT: 2.051893701000006E-6\ncell 1-9\n ROOT: 2.051893701000006E-6\ncell 2-10\n ROOT: 2.051893701000006E-6\ncell 0-9\n : 6.155681103000018E-7\n ROOT: 6.155681103000018E-7\ncell 1-10\n ROOT: 6.155681103000018E-7\ncell 0-10\n : 1.8836384175180025E-7\n ROOT: 1.8836384175180025E-7\n");
    if(insideOutsideOpt>0){
      assertEquals(parser.dumpOutsideChart(), "# Outside chart snapshot\ncell 0-1\n X: 1.8836384175180025E-7\ncell 1-2\n X: 1.883638417518006E-7\ncell 2-3\n X: 1.883638417518006E-7\ncell 3-4\n X: 1.883638417518006E-7\ncell 4-5\n X: 1.883638417518006E-7\ncell 5-6\n X: 1.883638417518006E-7\ncell 6-7\n X: 1.8836384175180025E-7\ncell 7-8\n X: 1.8836384175180025E-7\ncell 8-9\n X: 1.8836384175180025E-7\ncell 9-10\n X: 1.8836384175180025E-7\ncell 0-2\n ROOT: 6.155681103000014E-6\ncell 1-3\n ROOT: 6.155681103000014E-6\ncell 2-4\n ROOT: 6.155681103000014E-6\ncell 3-5\n ROOT: 6.155681103000014E-6\ncell 4-6\n ROOT: 6.155681103000014E-6\ncell 5-7\n ROOT: 6.155681103000014E-6\ncell 6-8\n ROOT: 6.155681103000014E-6\ncell 7-9\n ROOT: 6.155681103000003E-6\ncell 8-10\n ROOT: 6.155681103000014E-6\ncell 0-3\n ROOT: 2.0518937010000046E-5\ncell 1-4\n ROOT: 2.0518937010000046E-5\ncell 2-5\n ROOT: 2.0518937010000046E-5\ncell 3-6\n ROOT: 2.0518937010000046E-5\ncell 4-7\n ROOT: 2.0518937010000046E-5\ncell 5-8\n ROOT: 2.0518937010000046E-5\ncell 6-9\n ROOT: 2.051893701000001E-5\ncell 7-10\n ROOT: 2.051893701000001E-5\ncell 0-4\n ROOT: 7.015021200000018E-5\ncell 1-5\n ROOT: 7.015021200000018E-5\ncell 2-6\n ROOT: 7.015021200000018E-5\ncell 3-7\n ROOT: 7.015021200000018E-5\ncell 4-8\n ROOT: 7.015021200000018E-5\ncell 5-9\n ROOT: 7.015021200000006E-5\ncell 6-10\n ROOT: 7.015021200000006E-5\ncell 0-5\n ROOT: 2.480058000000006E-4\ncell 1-6\n ROOT: 2.480058000000006E-4\ncell 2-7\n ROOT: 2.480058000000006E-4\ncell 3-8\n ROOT: 2.480058000000006E-4\ncell 4-9\n ROOT: 2.4800580000000015E-4\ncell 5-10\n ROOT: 2.4800580000000015E-4\ncell 0-6\n ROOT: 9.185400000000025E-4\ncell 1-7\n ROOT: 9.185400000000018E-4\ncell 2-8\n ROOT: 9.185400000000009E-4\ncell 3-9\n ROOT: 9.185400000000018E-4\ncell 4-10\n ROOT: 9.185400000000018E-4\ncell 0-7\n ROOT: 0.003645000000000006\ncell 1-8\n ROOT: 0.003645000000000006\ncell 2-9\n ROOT: 0.0036450000000000024\ncell 3-10\n ROOT: 0.003645000000000006\ncell 0-8\n ROOT: 0.016200000000000023\ncell 1-9\n ROOT: 0.016200000000000006\ncell 2-10\n ROOT: 0.016200000000000006\ncell 0-9\n ROOT: 0.09000000000000002\ncell 1-10\n ROOT: 0.09000000000000002\ncell 0-10\n : 1.0\n ROOT: 1.0\n");
    
      parser.parseSentences(inputSentences.subList(1, inputSentences.size()));
      assertEquals(parser.sprintExpectedCounts(), "# Expected counts\n15.000000 ROOT->[X]\n13.000000 ROOT->[ROOT ROOT]\n15.000000 X->[_x]\n");
    }
  }
  
  public void testParsing1(){
    initParserFromString(grammarString);
    
    String inputSentence = "b c";

    parser.parseSentence(inputSentence);
    
    List<Double> surprisalList = parser.getSurprisalList();
    List<Double> synSurprisalList = parser.getSynSurprisalList();
    List<Double> lexSurprisalList = parser.getLexSurprisalList();
    
    List<Double> stringProbList = parser.getStringProbList();
  
    assertEquals(surprisalList.size(), 2);
    assertEquals(0.7985076959756138, surprisalList.get(0), 1e-5);
    assertEquals(0.10536051541566838, surprisalList.get(1), 1e-5);
    
    if(!isScaling){
      assertEquals(synSurprisalList.size(), 2);
      assertEquals(0.6931471805599453, synSurprisalList.get(0), 1e-5);
      assertEquals(0.0, synSurprisalList.get(1), 1e-5);
    
      assertEquals(lexSurprisalList.size(), 2);
      assertEquals(0.10536051565782628, lexSurprisalList.get(0), 1e-5);
      assertEquals(0.10536051565782628, lexSurprisalList.get(1), 1e-5);
    }
    
    assertEquals(stringProbList.toString(), "[0.0, 0.405]");
    
    if(parser.getDecodeOpt()==1){
      Tree tree = parser.viterbiParse();
      System.err.println(tree.pennString());
      assertEquals(tree.toString(), "( (ROOT (A (B b) (C c))))");
    }
  }
  
  public void testParsing2(){
    initParserFromString(grammarString);
    
    String inputSentence = "a";

    parser.parseSentence(inputSentence);
    
    List<Double> surprisalList = parser.getSurprisalList();
    
    assertEquals(surprisalList.size(), 1);
    assertEquals(2.3025851249694824, surprisalList.get(0), 1e-5);
    
    List<Double> synSurprisalList = parser.getSynSurprisalList();
    List<Double> lexSurprisalList = parser.getLexSurprisalList();
    List<Double> stringProbList = parser.getStringProbList();
    
    if(!isScaling){
      assertEquals(synSurprisalList.size(), 1);
      assertEquals(1.1102230246251565E-16, synSurprisalList.get(0), 1e-5);
      assertEquals(lexSurprisalList.toString(), "[2.3025850929940455]");
    }
    
    assertEquals(stringProbList.toString(), "[0.0]");
  }
  
  public void testParsing3(){
    initParserFromString(grammarString);
    
    String inputSentence = "d e";

    parser.parseSentence(inputSentence);
    
    List<Double> surprisalList = parser.getSurprisalList();
    
    for (int i = 0; i < surprisalList.size(); i++) {
      System.err.println(i + "\t" + surprisalList.get(i));
    }
  
    assertEquals(surprisalList.size(), 2);
    assertEquals(0.9162907283333066, surprisalList.get(0), 1e-5);
    assertEquals(2.3025851249694824, surprisalList.get(1), 1e-5);
    
    
    List<Double> synSurprisalList = parser.getSynSurprisalList();
    List<Double> lexSurprisalList = parser.getLexSurprisalList();
    List<Double> stringProbList = parser.getStringProbList();

    if(!isScaling){
      assertEquals(synSurprisalList.size(), 2);
      assertEquals(0.6931471805599453, synSurprisalList.get(0), 1e-5);
      assertEquals(1.1102230246251565E-16, synSurprisalList.get(1), 1e-5);
      assertEquals(lexSurprisalList.toString(), "[0.2231435513142097, 2.3025850929940455]");
    }
    
    assertEquals(stringProbList.toString(), "[0.0, 0.04000000000000001]");
    

    if(parser.getDecodeOpt()==1){
      Tree tree = parser.viterbiParse();
      System.err.println(tree.pennString());
      assertEquals(tree.toString(), "( (ROOT (A (D d) (B e))))");
    }
  }
  
  public void testRecursiveGrammarIO(){
    initParserFromString(recursiveGrammarString);
    
    String inputSentence = "d d b c";

    parser.parseSentence(inputSentence);
    
    List<Double> surprisalList = parser.getSurprisalList();
  
    assertEquals(surprisalList.size(), 4);
    assertEquals(0.8649974339457559, surprisalList.get(0), 1e-5);
    assertEquals(3.167582526939802, surprisalList.get(1), 1e-5);
    assertEquals(0.17185025338581103, surprisalList.get(2), 1e-5);
    assertEquals(2.052896986215565, surprisalList.get(3), 1e-5);
    
    List<Double> synSurprisalList = parser.getSynSurprisalList();
    List<Double> lexSurprisalList = parser.getLexSurprisalList();
    List<Double> stringProbList = parser.getStringProbList();

    if(!isScaling){
      assertEquals(synSurprisalList.toString(), "[0.6418538861723948, 2.9444389791664407, -0.05129329438755048, 1.9475364707998972]");
      assertEquals(lexSurprisalList.size(), 4);
      assertEquals(0.2231435513142097, lexSurprisalList.get(0), 1e-5);
      assertEquals(0.2231435513142097, lexSurprisalList.get(1), 1e-5);
      assertEquals(0.2231435513142097, lexSurprisalList.get(2), 1e-5);
      assertEquals(0.10536051565782628, lexSurprisalList.get(3), 1e-5);
    }
    
    assertEquals(stringProbList.size(), 4);
    assertEquals(0.0, stringProbList.get(0), 1e-5);
    assertEquals(0.0, stringProbList.get(1), 1e-5);
    assertEquals(0.012800000000000008, stringProbList.get(2), 1e-5);
    assertEquals(0.0017280000000000019, stringProbList.get(3), 1e-5);
    
    assertEquals(parser.dumpInsideChart(), "# Inside chart snapshot\ncell 0-1\n D: 0.8\ncell 1-2\n D: 0.8\ncell 2-3\n B: 0.8\ncell 3-4\n C: 0.9\ncell 1-3\n A: 0.32\ncell 2-4\n A: 0.36000000000000004\ncell 0-3\n : 0.012800000000000008\n A: 0.012800000000000008\ncell 1-4\n A: 0.02880000000000002\ncell 0-4\n : 0.0017280000000000019\n A: 0.0017280000000000019\n");
    if(insideOutsideOpt>0){
      assertEquals(parser.dumpOutsideChart(), "# Outside chart snapshot\ncell 0-1\n D: 0.002160000000000002\ncell 1-2\n D: 0.0021600000000000005\ncell 2-3\n B: 0.0021600000000000005\ncell 3-4\n C: 0.0019200000000000011\ncell 1-3\n A: 0.0036\ncell 2-4\n A: 0.001600000000000002\ncell 0-3\n A: 0.045000000000000005\ncell 1-4\n A: 0.04000000000000001\ncell 0-4\n : 1.0\n A: 1.0\n");
      assertEquals(parser.sprintExpectedCounts(), "# Expected counts\n1.000000 ROOT->[A]\n1.000000 A->[B C]\n2.000000 A->[D B]\n2.000000 B->[A]\n1.000000 B->[_b]\n1.000000 C->[_c]\n2.000000 D->[_d]\n");
    }
    
    if(parser.getDecodeOpt()==1){
      Tree tree = parser.viterbiParse();
      System.err.println(tree.pennString());
      boolean matched = tree.toString().equals("( (ROOT (A (D d) (B (A (D d) (B (A (B b) (C c))))))))")
          || tree.toString().equals("( (ROOT (A (D d) (B (A (B (A (D d) (B b))) (C c))))))");
      assertEquals(matched, true);
    }
  }
  
  
  public void testWSJ500AG(){
    initParserFromFile(wsj500AG);
    
    String inputSentence = "The two young sea-lions took not the slightest interest in our arrival .";

    parser.parseSentence(inputSentence);
    
    List<Double> surprisalList = parser.getSurprisalList();
    List<Double> stringProbList = parser.getStringProbList();
    
    // Note: scores here are slightly different from those of previous version
    // that is because RoarkBaseLexicon.score returns float instead of double
    assertEquals(surprisalList.size(), 13);
    assertEquals(2.5902917249093926, surprisalList.get(0), 1e-5);
    assertEquals(11.17802383709047, surprisalList.get(1), 1e-5);
    assertEquals(6.679507135955136, surprisalList.get(2), 1e-5);
    assertEquals(4.948450710073175, surprisalList.get(3), 1e-5);
    assertEquals(9.385103677293866, surprisalList.get(4), 1e-5);
    assertEquals(6.44860564335427, surprisalList.get(5), 1e-5);
    assertEquals(4.250959913607808, surprisalList.get(6), 1e-5);
    assertEquals(2.033052140740189, surprisalList.get(7), 1e-5);
    assertEquals(7.950249255267927, surprisalList.get(8), 1e-5);
    assertEquals(3.9584749706452556, surprisalList.get(9), 1e-5);
    assertEquals(7.72672337348633, surprisalList.get(10), 1e-5);
    assertEquals(1.2492400898444487, surprisalList.get(11), 1e-5);
    assertEquals(2.072968468479857, surprisalList.get(12), 1e-5);


    assertEquals(stringProbList.size(), 13);
    assertEquals(2.7631257999498153E-6, stringProbList.get(0), 1e-5);
    assertEquals(3.7643574066525755E-8, stringProbList.get(1), 1e-5);
    assertEquals(1.7159626394143225E-12, stringProbList.get(2), 1e-5);
    assertEquals(1.8778083802959357E-12, stringProbList.get(3), 1e-5);
    assertEquals(5.8136325173038904E-18, stringProbList.get(4), 1e-5);
    assertEquals(1.1484971623511003E-20, stringProbList.get(5), 1e-5);
    assertEquals(9.315915216122732E-23, stringProbList.get(6), 1e-5);
    assertEquals(1.718918071281025E-22, stringProbList.get(7), 1e-5);
    assertEquals(8.212552526820724E-26, stringProbList.get(8), 1e-5);
    assertEquals(1.0082110788122197E-29, stringProbList.get(9), 1e-5);
    assertEquals(0.0, stringProbList.get(10), 1e-5);
    assertEquals(2.4430738209264177E-31, stringProbList.get(11), 1e-5);
    assertEquals(2.267542490039142E-31, stringProbList.get(12), 1e-5);
    
    if(parser.getDecodeOpt()==1){
      Tree tree = parser.viterbiParse();
      System.err.println(tree.pennString());
      assertEquals(tree.toString(), "( (ROOT (S (NP (DT The) (CD two)) (ADJP (JJ young) (PP sea-lions)) (VP (VBD took) (PRT (RB not)) (NP (DT the) (NNP slightest) (NN interest)) (PP (IN in) (NP (PRP$ our) (NNS arrival)))) (. .))))");
    }
  }
  
  public void testWSJ500IO(){
    initParserFromFile(wsj500);
    
    String inputSentence = "The two young sea-lions took not the slightest interest in our arrival .";

    parser.parseSentence(inputSentence);
    
    List<Double> surprisalList = parser.getSurprisalList();
    List<Double> stringProbList = parser.getStringProbList();
    
    // Note: scores here are slightly different from those of previous version
    // that is because RoarkBaseLexicon.score returns float instead of double
    assertEquals(surprisalList.size(), 13);
    assertEquals(4.297369612568129, surprisalList.get(0), 1e-5);
    assertEquals(7.295271761862825, surprisalList.get(1), 1e-5);
    assertEquals(7.235382153987473, surprisalList.get(2), 1e-5);
    assertEquals(6.579374855858557, surprisalList.get(3), 1e-5);
    assertEquals(8.550845844480936, surprisalList.get(4), 1e-5);
    assertEquals(5.652534988855393, surprisalList.get(5), 1e-5);
    assertEquals(4.375939791924388, surprisalList.get(6), 1e-5);
    assertEquals(4.054906889124851, surprisalList.get(7), 1e-5);
    assertEquals(7.567168879152675, surprisalList.get(8), 1e-5);
    assertEquals(3.7381719867359706, surprisalList.get(9), 1e-5);
    assertEquals(8.101975429529261, surprisalList.get(10), 1e-5);
    assertEquals(4.346770625309475, surprisalList.get(11), 1e-5);
    assertEquals(3.413711206475597, surprisalList.get(12), 1e-5);

    assertEquals(stringProbList.size(), 13);
    assertEquals(2.4538477180526925E-5, stringProbList.get(0), 1e-5);
    assertEquals(1.6970900624754254E-7, stringProbList.get(1), 1e-5);
    assertEquals(2.8755568614102028E-11, stringProbList.get(2), 1e-5);
    assertEquals(3.658265478750189E-13, stringProbList.get(3), 1e-5);
    assertEquals(6.186894611508017E-17, stringProbList.get(4), 1e-5);
    assertEquals(1.1894000273453555E-19, stringProbList.get(5), 1e-5);
    assertEquals(1.184450111083609E-21, stringProbList.get(6), 1e-5);
    assertEquals(1.8687442504202126E-22, stringProbList.get(7), 1e-5);
    assertEquals(1.289941394650639E-25, stringProbList.get(8), 1e-5);
    assertEquals(2.316801041738345E-29, stringProbList.get(9), 1e-5);
    assertEquals(0.0, stringProbList.get(10), 1e-5);
    assertEquals(3.405533303307524E-33, stringProbList.get(11), 1e-5);
    assertEquals(1.8818812758090438E-33, stringProbList.get(12), 1e-5);
    
    if(parser.getDecodeOpt()==1){
      Tree tree = parser.viterbiParse();
      System.err.println(tree.toString());
      assertEquals(tree.toString(), "( (ROOT (S (NP (DT The) (CD two) (JJ young) (NNS sea-lions)) (VP (VBD took) (ADVP (RB not)) (NP (NP (DT the) (NN slightest) (NN interest)) (PP (IN in) (NP (PRP$ our) (NN arrival))))) (. .))))");
    }
    
    if(insideOutsideOpt>0){
      assertEquals(parser.sprintExpectedCounts(), "# Expected counts\n0.000047 ROOT->[ADJP]\n0.000290 ROOT->[SINV]\n0.000488 ROOT->[X]\n0.998773 ROOT->[S]\n0.000000 ROOT->[SQ]\n0.000096 ROOT->[NP]\n0.000302 ROOT->[FRAG]\n0.000004 ROOT->[SBARQ]\n0.000000 WHPP->[IN WHNP]\n0.000488 X->[NP PP .]\n0.001412 NX->[NN NN]\n0.003223 NX->[NN]\n0.000073 NX->[VBG NN]\n0.000030 NX->[NNP NN]\n0.000111 NX->[NNP]\n0.000335 NX->[NNS]\n0.000000 S->[NP , ADVP , VP .]\n0.000000 S->[S VP]\n0.001259 S->[NP ADVP VP .]\n0.000000 S->[S : S : S .]\n0.000000 S->[ADVP , NP VP .]\n0.000000 S->[PP ADVP NP VP]\n0.000023 S->[VP .]\n0.000000 S->[VP ,]\n0.000000 S->[S , S]\n0.000015 S->[NP ADVP VP]\n0.000000 S->[NP , NP VP .]\n0.001566 S->[NP ADJP]\n0.000205 S->[NP ADVP]\n0.000031 S->[NP VP S]\n0.000390 S->[NP NP]\n0.012594 S->[NP VP]\n0.002583 S->[NP PP]\n0.000000 S->[SBAR PRN NP VP .]\n0.003361 S->[NP ADJP VP .]\n0.000008 S->[NP NP VP]\n0.968799 S->[NP VP .]\n0.001547 S->[NP]\n0.002271 S->[VP]\n0.000026 S->[VP PP]\n0.000126 S->[ADJP]\n0.000000 S->[NP VP :]\n0.004123 S->[ADVP NP VP .]\n0.000000 S->[ADVP , NP VP]\n0.000015 S->[S CC S .]\n0.000039 S->[ADVP VP]\n0.000004 S->[: VP]\n0.000000 S->[`` NP '' VP]\n0.000434 S->[S : S .]\n0.000000 S->[NP VP ,]\n0.000000 S->[PP VP]\n0.000000 S->[CC ADVP NP ADVP VP .]\n0.000000 S->[SBAR , NP ADVP VP .]\n0.000001 S->[S : S]\n0.000000 S->[-LRB- VP -RRB-]\n0.000000 S->[SBAR , NP VP]\n0.000597 S->[ADVP NP VP]\n0.000000 S->[NP VBZ ADVP ADVP VP]\n0.000000 S->[CC NP VP .]\n0.000005 S->[PP NP VP .]\n0.000002 S->[`` VP]\n0.001152 S->[NP NP VP .]\n0.000121 S->[S NP VP .]\n0.000000 S->[CC NP VP]\n0.000000 S->[S , NP PP VP .]\n0.000167 S->[: VP .]\n0.000000 S->[CC NP ADVP VP]\n0.000000 S->[`` NP VP .]\n0.000000 S->[S , NP VP .]\n0.000085 S->[S VP .]\n0.000000 S->[S NP VP]\n0.000004 S->[NP `` VP]\n0.000011 S->[NP PP VP .]\n0.000001 S->[NP : VP]\n0.019368 S->[NP VP PP .]\n0.000000 S->[NP PP VP]\n0.000001 S->[S CC S]\n0.000000 S->[SBAR , NP VP .]\n0.000024 NP->[DT NNP]\n0.000000 NP->[`` S '']\n0.000046 NP->[NNP]\n0.000415 NP->[PRP]\n0.000011 NP->[DT VBG]\n0.000000 NP->[NP CC NP POS]\n0.000021 NP->[DT RB]\n0.000325 NP->[DT NNS]\n0.000017 NP->[NNPS]\n0.002377 NP->[DT]\n0.000001 NP->[NP CC NP .]\n0.008986 NP->[NN]\n0.000581 NP->[CD]\n0.022386 NP->[DT CD]\n0.000108 NP->[JJ]\n0.000064 NP->[DT NNPS]\n0.000133 NP->[DT JJ]\n0.031463 NP->[DT NN]\n0.000000 NP->[DT ADJP QP]\n0.000002 NP->[NP : NP .]\n0.000003 NP->[VBZ]\n0.000132 NP->[NNS]\n0.000093 NP->[DT JJR]\n0.000005 NP->[VBG]\n0.007624 NP->[RB]\n0.000070 NP->[PDT]\n0.000090 NP->[JJS]\n0.000700 NP->[DT JJS]\n0.000390 NP->[EX]\n0.000026 NP->[RBR]\n0.000036 NP->[JJR]\n0.000068 NP->[S]\n0.000025 NP->[ADJP]\n0.058329 NP->[DT JJS NN]\n0.001257 NP->[QP]\n0.000030 NP->[DT ADJP NNS]\n0.000001 NP->[ADVP NNP NN]\n0.012646 NP->[DT ADJP NN]\n0.000001 NP->[DT NNS S]\n0.000011 NP->[JJR NN]\n0.000005 NP->[NP PRP$ NNS]\n0.000656 NP->[JJ NN]\n0.000028 NP->[JJ NNP]\n0.001610 NP->[JJ NNS]\n0.000000 NP->[JJ NNS SBAR]\n0.000260 NP->[NN NN]\n0.000022 NP->[NP NNP NN]\n0.000588 NP->[DT NP]\n0.000164 NP->[DT ADJP]\n0.010578 NP->[ADVP DT NN]\n0.000000 NP->[ADJP VBN NN]\n0.000000 NP->[NP `` NP '']\n0.001460 NP->[DT NX]\n0.000011 NP->[NP : NP]\n0.000000 NP->[NP : S]\n0.000000 NP->[NP : PP]\n0.000000 NP->[NN S]\n0.000001 NP->[DT ADJP JJ NN]\n0.000000 NP->[NP : SBARQ]\n0.004610 NP->[DT VBG NN]\n0.000007 NP->[NP NNS]\n0.000002 NP->[DT ADJP NN NN]\n0.000042 NP->[NP , NP]\n0.000001 NP->[NP , VP]\n0.000000 NP->[NP , SBAR]\n0.000000 NP->[NP , PP]\n0.002961 NP->[NP ADJP]\n0.000001 NP->[NNS NN]\n0.003616 NP->[NP NP]\n0.000016 NP->[NP S]\n0.000363 NP->[NP SBAR]\n0.001289 NP->[NP VP]\n0.000000 NP->[NP QP]\n0.442874 NP->[NP PP]\n0.000000 NP->[NP , ADVP]\n0.000000 NP->[NP PRN]\n0.000308 NP->[NP NX]\n0.000003 NP->[JJ NX]\n0.000029 NP->[CD NN]\n0.000203 NP->[DT NNS NN]\n0.346680 NP->[RB DT JJ NN]\n0.000000 NP->[NP , NP ,]\n0.000101 NP->[NP CC NP]\n0.000052 NP->[QP NN]\n0.000000 NP->[NP , VP ,]\n0.008702 NP->[RB DT]\n0.000000 NP->[`` S]\n0.003775 NP->[NP NN NN]\n0.000077 NP->[DT NN S]\n0.000020 NP->[DT NN SBAR]\n0.000000 NP->[NP , SBAR ,]\n0.000000 NP->[`` ADJP]\n0.000001 NP->[NP NN S]\n0.000003 NP->[NP JJ ADJP NN]\n0.018218 NP->[QP JJ NNS]\n0.000000 NP->[NP NP , ADVP]\n0.000000 NP->[NP : NP : NP .]\n0.186913 NP->[DT JJ NN]\n0.002243 NP->[DT CD NN]\n0.006928 NP->[NP JJ NN]\n0.000000 NP->[NNS S]\n0.005771 NP->[NP JJ NNS]\n0.000001 NP->[NP NNP]\n0.000000 NP->[NP PP , PP]\n0.000000 NP->[NP PP , SBAR]\n0.000000 NP->[NP PP , VP]\n0.041631 NP->[NP NN]\n0.000000 NP->[NP PP POS]\n0.000000 NP->[NP PP '']\n0.000024 NP->[NP CD NN]\n0.000446 NP->[CD JJ NNS]\n0.000019 NP->[NP VP PP]\n0.000001 NP->[JJ NN SBAR .]\n0.000001 NP->[NP VP :]\n0.000000 NP->[NP , NP , SBAR]\n0.000306 NP->[NP NP PP]\n0.003378 NP->[DT NNP NN]\n0.000011 NP->[ADVP QP]\n0.000004 NP->[VBG NN]\n0.000001 NP->[QP NX]\n0.000000 NP->[NP NNS S]\n0.286342 NP->[DT NN NN]\n0.000243 NP->[ADJP JJ NN]\n0.000000 NP->[NP CC ADVP NP]\n0.000005 NP->[PDT NP]\n0.000000 NP->[DT ADJP , ADJP NN]\n0.000000 NP->[DT ADJP VBN NN]\n0.017499 NP->[DT JJR NN]\n0.000030 NP->[ADJP NN]\n0.000062 NP->[ADJP NNS]\n0.000000 NP->[NP : NP : NP]\n0.000000 NP->[NP , NP CC NP]\n0.014341 NP->[RB DT NN]\n0.000000 NP->[NP '' PP]\n0.000013 NP->[NP '' NX]\n0.050577 NP->[PRP$ NNS]\n0.842617 NP->[PRP$ NN]\n0.005377 NP->[RB DT ADJP]\n0.103520 NP->[PRP$ JJ]\n0.000000 NP->[JJ NN SBAR]\n0.000015 NP->[VBN NN]\n0.000000 NP->[NP PP PP PP]\n0.000302 NP->[NP ADJP NNS]\n0.000000 NP->[NP PRN PP]\n0.000000 NP->[NP PRN SBAR]\n0.000000 NP->[NP PRN :]\n0.628504 NP->[DT CD JJ NNS]\n0.328455 NP->[DT CD JJ NN]\n0.000391 NP->[NP ADJP NN]\n0.005518 NP->[DT VBN NN]\n0.000000 NP->[NP PP ADVP]\n0.003400 NP->[PRP$ NX]\n0.000000 NP->[NP PP SBAR]\n0.000000 NP->[NP PP VP]\n0.000039 NP->[NP PP PP]\n0.000001 NP->[NNP NN]\n0.000004 NP->[NP PP NP]\n0.000000 NP->[NP PP S]\n0.019795 QP->[DT CD]\n0.000000 PP->[TO NP NP]\n0.000006 PP->[TO NP PP]\n0.000000 PP->[PP CC PP]\n0.000013 PP->[VBN NP]\n0.000000 PP->[IN PP]\n0.978595 PP->[IN NP]\n0.000188 PP->[IN S]\n0.000009 PP->[IN SBAR]\n0.000008 PP->[VBG NP]\n0.016505 PP->[NP IN NP]\n0.000000 PP->[PP CC ADJP NP]\n0.000001 PP->[IN ADJP]\n0.000000 PP->[PP PP NP]\n0.002140 PP->[ADVP IN NP]\n0.000000 PP->[IN ADVP]\n0.000000 PP->[IN NP '' PP]\n0.000263 PP->[JJ NP]\n0.000000 PP->[NP RB PP]\n0.000000 PP->[JJ IN NP]\n0.000057 PP->[IN]\n0.000000 PP->[IN NP CC NP]\n0.000000 PP->[VBG PP]\n0.000001 PP->[ADVP IN S]\n0.000156 PP->[RB PP]\n0.000484 PP->[RB]\n0.000137 PP->[TO NP]\n0.000014 PP->[PP PP]\n0.000000 PP->[JJ TO NP]\n0.000156 PP->[FW NP]\n0.000011 PP->[JJ IN S]\n0.000000 PP->[PP ADVP]\n0.000000 SBAR->[WHPP S]\n0.000000 SBAR->[WHNP S ,]\n0.005407 SBAR->[S]\n0.000123 SBAR->[WHNP S]\n0.000025 SBAR->[ADVP IN S]\n0.004388 SBAR->[RB S]\n0.000085 SBAR->[DT S]\n0.000322 SBAR->[IN S]\n0.000029 SBAR->[NP IN S]\n0.000044 SBAR->[WHADVP S]\n0.000011 SBAR->[SINV]\n0.000000 SBAR->[SBAR CC SBAR]\n0.000000 SBAR->[PP IN S]\n0.000000 VP->[VBG NP SBAR]\n0.000063 VP->[VBG NP PP]\n0.000000 VP->[VBG NP NP]\n0.000000 VP->[ADVP VBG]\n0.000000 VP->[VBG NP VP]\n0.000000 VP->[VBZ ADJP S]\n0.000000 VP->[VP CC VP , SBAR]\n0.000000 VP->[VBG NP S]\n0.000001 VP->[VBG NP ADVP]\n0.000000 VP->[VBZ ADJP ADVP]\n0.000803 VP->[VB NP]\n0.000015 VP->[VB NP ADVP]\n0.000055 VP->[VB S]\n0.000000 VP->[VB NP ADJP]\n0.000003 VP->[ADVP VBD]\n0.000002 VP->[VB PP]\n0.000000 VP->[VB NP PRT PP]\n0.000005 VP->[VB SBAR]\n0.000292 VP->[VB VP]\n0.000126 VP->[VBD NP PP PP]\n0.001366 VP->[VB NP PP]\n0.000000 VP->[VB NP SBAR]\n0.000000 VP->[VB NP VP]\n0.000000 VP->[VB NP NP]\n0.000000 VP->[VB NP S]\n0.000000 VP->[VBD NP PP S]\n0.000000 VP->[NNP]\n0.000079 VP->[VBD]\n0.000000 VP->[VBG PP]\n0.000001 VP->[VBG ADJP]\n0.000014 VP->[VBZ]\n0.000000 VP->[VBG ADVP]\n0.000004 VP->[VBG]\n0.002014 VP->[TO VP]\n0.000025 VP->[ADVP VB NP PP]\n0.000027 VP->[VB]\n0.000027 VP->[VBN]\n0.000000 VP->[VBZ NP , SBAR]\n0.000002 VP->[MD]\n0.000013 VP->[VBP]\n0.000000 VP->[VBZ NP , S]\n0.000005 VP->[VBG SBAR]\n0.000173 VP->[ADJP]\n0.000010 VP->[VBG VP]\n0.000000 VP->[VBP PP NP]\n0.000033 VP->[VBG NP]\n0.000045 VP->[VBG S]\n0.000000 VP->[VBG NP , ADVP]\n0.000004 VP->[VP CC VP PP]\n0.000000 VP->[ADVP VBZ NP]\n0.000000 VP->[VP CC VP NP]\n0.000000 VP->[VB NP ADVP ADVP]\n0.000261 VP->[ADVP VBD NP PP]\n0.000000 VP->[VBG PP PP]\n0.078066 VP->[VBD NP]\n0.000000 VP->[VBD NP , PP]\n0.000000 VP->[VBD NP , SBAR]\n0.000458 VP->[VBD VP]\n0.006939 VP->[VBD S]\n0.000162 VP->[VBD S NP PP]\n0.000000 VP->[VB PP ADVP]\n0.002485 VP->[VBD ADVP]\n0.000000 VP->[VBD S NP SBAR]\n0.000001 VP->[ADVP VBN]\n0.000000 VP->[VB PP SBAR]\n0.002598 VP->[VBD PP]\n0.008775 VP->[VBD SBAR]\n0.000000 VP->[VB S , S]\n0.000000 VP->[VB PP PP]\n0.001854 VP->[VBD ADJP]\n0.000000 VP->[VBD NP ,]\n0.042466 VP->[VBD ADJP NP PP]\n0.104009 VP->[VBD PRT NP PP]\n0.000000 VP->[VBD NP PP PP PP]\n0.000000 VP->[ADVP VBG NP]\n0.000001 VP->[VBD S S]\n0.000000 VP->[MD ADVP ADVP VP]\n0.000179 VP->[VBD S PP]\n0.000004 VP->[PP PP]\n0.000000 VP->[VBZ PP PP SBAR]\n0.000000 VP->[VP , VP , VP]\n0.296122 VP->[VBD NP PP]\n0.000598 VP->[VBD NP SBAR]\n0.000000 VP->[VBD S : SBAR]\n0.006962 VP->[VBD NP NP]\n0.000000 VP->[VBP VP , SBAR]\n0.001482 VP->[VBD NP S]\n0.002406 VP->[VBD NP ADVP]\n0.000000 VP->[VB ADVP]\n0.000042 VP->[VBZ NP PP]\n0.000000 VP->[VBZ NP S]\n0.000080 VP->[VB ADJP]\n0.000012 VP->[NN NP]\n0.000000 VP->[VB ADJP S]\n0.011931 VP->[NN PP]\n0.000000 VP->[VB ADJP SBAR]\n0.000000 VP->[VB ADJP PP]\n0.000000 VP->[VBN NP PP PP]\n0.000000 VP->[NN NP PP PP]\n0.000011 VP->[VBZ S PP]\n0.000000 VP->[VBP ADVP SBAR]\n0.000000 VP->[VBP ADVP VP]\n0.000000 VP->[VBP ADVP S]\n0.000000 VP->[PP VBD VP]\n0.000000 VP->[VBP ADVP ADJP]\n0.000013 VP->[VBD PP SBAR]\n0.000004 VP->[VBD PP PP]\n0.000000 VP->[NNP NP PRT]\n0.000000 VP->[VBN ADVP PP]\n0.000000 VP->[VBD NP NP , SBAR]\n0.000000 VP->[VBN ADVP VP]\n0.000000 VP->[ADVP VP CC VP]\n0.000000 VP->[VBN NP , S]\n0.000000 VP->[VBN NP , SBAR]\n0.000000 VP->[VBP NP S , PP]\n0.000002 VP->[VP : NP]\n0.000025 VP->[VBD NP ADVP PP]\n0.000000 VP->[VB NP NP PP]\n0.000000 VP->[VBN ADVP NP PRN]\n0.000102 VP->[VBD SBAR PP]\n0.000450 VP->[VBD PP NP]\n0.000001 VP->[ADVP VBG NP PP]\n0.000000 VP->[VBD RB VP]\n0.000000 VP->[VBN S SBAR]\n0.004694 VP->[VBD RB PP]\n0.000032 VP->[VBD RB ADJP]\n0.000002 VP->[VBN ADJP]\n0.000000 VP->[VBN ADVP]\n0.000000 VP->[VB NP PP S]\n0.000001 VP->[VBN PP]\n0.000000 VP->[MD ADVP VP]\n0.000000 VP->[VP , VP CC VP]\n0.000038 VP->[VBN NP]\n0.000000 VP->[VB NP PP SBAR]\n0.000043 VP->[VBN S]\n0.000005 VP->[VBN SBAR]\n0.000095 VP->[VBN VP]\n0.000000 VP->[VB NP PP NP]\n0.000000 VP->[ADVP VB PP]\n0.000003 VP->[ADVP VB NP]\n0.000029 VP->[JJ PP]\n0.000000 VP->[VBG ADVP S]\n0.000000 VP->[VBG ADVP PP]\n0.000000 VP->[VB ADVP S]\n0.000001 VP->[VB ADVP NP]\n0.000000 VP->[VB ADVP VP]\n0.000000 VP->[VP CC ADVP VP]\n0.000000 VP->[VBP NP : S]\n0.000000 VP->[VBD S PP SBAR]\n0.000381 VP->[VBP NP PP]\n0.000000 VP->[VBN ADVP SBAR , S]\n0.000002 VP->[POS NP]\n0.000007 VP->[ADVP VBN S]\n0.000352 VP->[VBD PRT PP]\n0.000000 VP->[ADVP VBN PP]\n0.012303 VP->[VBD PRT NP]\n0.000000 VP->[VBZ NP PP S]\n0.002244 VP->[MD VP]\n0.000000 VP->[VB NP S , SBAR]\n0.000000 VP->[VBP NP SBAR]\n0.000000 VP->[VBN PP SBAR]\n0.000000 VP->[VBN PP PP]\n0.000073 VP->[VBD ADJP SBAR]\n0.000048 VP->[VBD ADJP PP]\n0.000000 VP->[VB ADVP NP PP]\n0.000000 VP->[VBN PP NP]\n0.000000 VP->[VBN PP S]\n0.000000 VP->[VBN ADJP SBAR]\n0.000000 VP->[VB NP S S]\n0.000001 VP->[VBD PRT ADVP SBAR]\n0.000000 VP->[VBD PRT ADVP PP]\n0.000033 VP->[VBZ NP]\n0.000033 VP->[VBZ S]\n0.000000 VP->[VBZ ADVP NP]\n0.000000 VP->[VBZ ADVP VP]\n0.363529 VP->[VBD ADVP NP]\n0.000000 VP->[VBZ PP]\n0.000105 VP->[VBN NP PP]\n0.000007 VP->[VBZ SBAR]\n0.000000 VP->[VBN NP SBAR]\n0.000000 VP->[VBZ ADVP S]\n0.000527 VP->[VBZ VP]\n0.062578 VP->[VBD ADVP PP]\n0.000005 VP->[VBZ ADJP]\n0.000431 VP->[VBD ADVP VP]\n0.000000 VP->[VBZ ADVP ADJP]\n0.000014 VP->[VBP SBAR]\n0.000001 VP->[VBP PP]\n0.000309 VP->[VBP NP]\n0.000000 VP->[VB S SBAR]\n0.000423 VP->[VBP VP]\n0.000053 VP->[VBP S]\n0.000021 VP->[VP CC VP]\n0.000000 VP->[VBP ADVP]\n0.000001 VP->[VBN NP ADVP]\n0.000092 VP->[VBP ADJP]\n0.000000 VP->[NNS SBAR]\n0.116666 PRT->[RB]\n0.000000 PRT->[IN]\n0.000000 PRT->[RP]\n0.000000 PRT->[NNP]\n0.000000 ADJP->[RB VBD]\n0.000000 ADJP->[ADVP JJ SBAR]\n0.000025 ADJP->[NP JJR]\n0.000000 ADJP->[JJR PP]\n0.000158 ADJP->[NP JJ SBAR]\n0.016611 ADJP->[JJ]\n0.000076 ADJP->[VBG]\n0.042978 ADJP->[RB]\n0.000054 ADJP->[JJ '' S]\n0.000947 ADJP->[VBN]\n0.000000 ADJP->[ADJP PRN]\n0.000001 ADJP->[VBN PP]\n0.002679 ADJP->[JJR]\n0.001959 ADJP->[CD NN]\n0.000006 ADJP->[VBN S]\n0.000027 ADJP->[ADJP PP]\n0.000014 ADJP->[JJ PP]\n0.000000 ADJP->[ADJP CC ADJP]\n0.000248 ADJP->[QP]\n0.000976 ADJP->[JJ S]\n0.000591 ADJP->[JJ NP]\n0.000641 ADJP->[NN PP]\n0.003726 ADJP->[ADVP NN PP]\n0.000009 ADJP->[QP NN]\n0.000002 ADJP->[ADVP VBN]\n0.000578 ADJP->[NP JJ]\n0.000000 PRN->[-LRB- S -RRB-]\n0.000000 PRN->[: NP :]\n0.000000 PRN->[: S :]\n0.000000 PRN->[, S]\n0.000000 PRN->[, S ,]\n0.000000 PRN->[-LRB- NP -RRB-]\n0.000116 SINV->[ADJP VP NP .]\n0.000045 SINV->[S , VP NP .]\n0.000129 SINV->[S VP NP .]\n0.000000 SINV->[`` S , VP NP .]\n0.000000 SINV->[VP VP NP , PP .]\n0.000001 SINV->[ADVP VP NP .]\n0.000010 SINV->[VBD RB NP VP]\n0.000044 WHADVP->[WRB]\n0.000170 ADVP->[ADVP SBAR]\n0.000088 ADVP->[ADVP PP]\n0.000034 ADVP->[IN]\n0.000272 ADVP->[RBR]\n0.000038 ADVP->[NP RB]\n0.000000 ADVP->[IN PP]\n0.000946 ADVP->[JJ]\n0.058961 ADVP->[RB NP]\n0.002623 ADVP->[IN NP]\n0.000114 ADVP->[RB SBAR]\n0.000106 ADVP->[ADVP JJR]\n0.390880 ADVP->[RB]\n0.000015 ADVP->[JJ RB S]\n0.000254 ADVP->[NP IN]\n0.000335 ADVP->[RBS]\n0.000024 ADVP->[RBR NP]\n0.000001 ADVP->[NNP]\n0.000002 WHNP->[WRB]\n0.000009 WHNP->[IN]\n0.000055 WHNP->[WDT]\n0.000057 WHNP->[WP]\n0.000004 WHNP->[NP PP]\n0.000000 FRAG->[NP : NP : NP .]\n0.000006 FRAG->[NP]\n0.000296 FRAG->[NP .]\n0.000001 FRAG->[NP : NP .]\n0.000000 SQ->[MD VP]\n0.000000 SQ->[VBZ NP S]\n0.000000 SQ->[VP]\n0.000004 SQ->[VBD RB NP VP]\n0.000000 SBARQ->[SBAR , SBARQ .]\n0.000004 SBARQ->[WHNP SQ .]\n0.000000 SBARQ->[WHNP SQ]\n1.000000 VBD->[_took]\n1.000000 DT->[_the]\n1.000000 DT->[_The]\n1.000000 NN->[_interest]\n1.000000 IN->[_in]\n1.000000 JJ->[_young]\n1.000000 CD->[_two]\n1.000000 .->[_.]\n1.000000 RB->[_not]\n0.000000 RP->[_in]\n1.000000 PRP$->[_our]\n0.000435 VBP->[_sea-lions]\n0.000404 PRP->[_sea-lions]\n0.001983 TO->[_sea-lions]\n0.000069 VBG->[_sea-lions]\n0.000133 RBS->[_sea-lions]\n0.000068 PDT->[_sea-lions]\n0.000004 .->[_sea-lions]\n0.000278 VB->[_sea-lions]\n0.000020 JJ->[_sea-lions]\n0.000510 VBD->[_sea-lions]\n0.000000 POS->[_sea-lions]\n0.000074 NNP->[_sea-lions]\n0.000088 JJS->[_sea-lions]\n0.000030 WRB->[_sea-lions]\n0.000156 VBN->[_sea-lions]\n0.000362 RB->[_sea-lions]\n0.000597 :->[_sea-lions]\n0.002211 MD->[_sea-lions]\n0.000032 CD->[_sea-lions]\n0.000036 WDT->[_sea-lions]\n0.000038 WP->[_sea-lions]\n0.000065 IN->[_sea-lions]\n0.000054 ''->[_sea-lions]\n0.000134 RBR->[_sea-lions]\n0.655082 NNS->[_sea-lions]\n0.000584 VBZ->[_sea-lions]\n0.000016 NNPS->[_sea-lions]\n0.000000 FW->[_sea-lions]\n0.000003 ``->[_sea-lions]\n0.000160 JJR->[_sea-lions]\n0.000033 CC->[_sea-lions]\n0.000046 ,->[_sea-lions]\n0.000000 -LRB-->[_sea-lions]\n0.000379 EX->[_sea-lions]\n0.335902 NN->[_sea-lions]\n0.000014 DT->[_sea-lions]\n0.000011 PRP->[_slightest]\n0.000851 VBP->[_slightest]\n0.000174 TO->[_slightest]\n0.004881 VBG->[_slightest]\n0.000007 PDT->[_slightest]\n0.000202 RBS->[_slightest]\n0.000001 .->[_slightest]\n0.000000 -RRB-->[_slightest]\n0.002394 VB->[_slightest]\n0.544928 JJ->[_slightest]\n0.000172 VBD->[_slightest]\n0.000002 POS->[_slightest]\n0.003457 NNP->[_slightest]\n0.000015 WRB->[_slightest]\n0.059031 JJS->[_slightest]\n0.006672 VBN->[_slightest]\n0.001803 RB->[_slightest]\n0.000027 :->[_slightest]\n0.000119 PRP$->[_slightest]\n0.004392 CD->[_slightest]\n0.000035 MD->[_slightest]\n0.000018 WDT->[_slightest]\n0.000739 IN->[_slightest]\n0.000019 WP->[_slightest]\n0.000013 ''->[_slightest]\n0.000189 RBR->[_slightest]\n0.000532 NNS->[_slightest]\n0.000091 VBZ->[_slightest]\n0.000000 RP->[_slightest]\n0.000064 NNPS->[_slightest]\n0.000156 FW->[_slightest]\n0.000003 ``->[_slightest]\n0.020288 JJR->[_slightest]\n0.000109 CC->[_slightest]\n0.000043 ,->[_slightest]\n0.000011 EX->[_slightest]\n0.348430 NN->[_slightest]\n0.000122 DT->[_slightest]\n0.845454 NN->[_arrival]\n0.050914 NNS->[_arrival]\n0.000111 NNP->[_arrival]\n0.103520 JJ->[_arrival]\n");
    }
  }
  
  public void testWSJ5IO(){
    initParserFromFile(wsj5);
    
    String inputSentence = "The two young sea-lions .";

    parser.parseSentence(inputSentence);
    
    List<Double> surprisalList = parser.getSurprisalList();
    List<Double> stringProbList = parser.getStringProbList();
    
    assertEquals(surprisalList.size(), 5);
    assertEquals(3.3767245251434295, surprisalList.get(0), 1e-5);
    assertEquals(2.450539006283793, surprisalList.get(1), 1e-5);
    assertEquals(2.5505675598325848, surprisalList.get(2), 1e-5);
    assertEquals(2.292861303099343, surprisalList.get(3), 1e-5);
    assertEquals(4.894604763694462, surprisalList.get(4), 1e-5);
    
    assertEquals(stringProbList.size(), 5);
    assertEquals(0.0, stringProbList.get(0), 1e-5);
    assertEquals(0.0, stringProbList.get(1), 1e-5);
    assertEquals(2.5315814789499013E-6, stringProbList.get(2), 1e-5);
    assertEquals(5.442039250199556E-7, stringProbList.get(3), 1e-5);
    assertEquals(1.2514394214109883E-7, stringProbList.get(4), 1e-5);
    
    if(parser.getDecodeOpt()==1){
      Tree tree = parser.viterbiParse();
      System.err.println(tree.pennString());
      assertEquals(tree.toString(), "( (ROOT (S (NP (DT The) (VBG two) (NN young)) (VP (VBD sea-lions)) (. .))))");
    }
    
    assertEquals(parser.dumpInsideChart(), "# Inside chart snapshot\ncell 0-1\n DT: 0.12500000000000003\ncell 1-2\n ,: 0.07692307692307693\n .: 0.16666666666666669\n CC: 0.11111111111111109\n VBD: 0.07142857142857141\n VBZ: 0.2\n ``: 0.6666666666666666\n TO: 0.3333333333333333\n VB: 0.3333333333333333\n NN: 0.030303030303030304\n DT: 0.0625\n JJ: 0.0909090909090909\n NNPS: 0.16666666666666669\n NNP: 0.02777777777777778\n NNS: 0.11111111111111109\n PRP: 0.4\n CD: 0.0909090909090909\n POS: 0.3333333333333333\n VBG: 0.3333333333333333\n IN: 0.11764705882352941\n $: 0.3333333333333333\n RB: 0.3333333333333333\n '': 0.6666666666666666\ncell 2-3\n ,: 0.07692307692307693\n .: 0.16666666666666669\n CC: 0.11111111111111109\n VBD: 0.07142857142857141\n VBZ: 0.2\n ``: 0.6666666666666666\n TO: 0.3333333333333333\n VB: 0.3333333333333333\n NN: 0.030303030303030304\n DT: 0.0625\n JJ: 0.0909090909090909\n NNPS: 0.16666666666666669\n NNP: 0.02777777777777778\n NNS: 0.11111111111111109\n PRP: 0.2\n CD: 0.0909090909090909\n POS: 0.3333333333333333\n VBG: 0.3333333333333333\n IN: 0.0588235294117647\n $: 0.3333333333333333\n RB: 0.3333333333333333\n '': 0.6666666666666666\ncell 3-4\n ,: 0.07692307692307693\n .: 0.16666666666666669\n CC: 0.11111111111111109\n VBD: 0.07142857142857141\n VBZ: 0.2\n ``: 0.6666666666666666\n TO: 0.3333333333333333\n VB: 0.3333333333333333\n NN: 0.030303030303030304\n DT: 0.0625\n JJ: 0.0909090909090909\n NNPS: 0.16666666666666669\n NNP: 0.02777777777777778\n NNS: 0.11111111111111109\n PRP: 0.2\n CD: 0.0909090909090909\n POS: 0.3333333333333333\n VBG: 0.3333333333333333\n IN: 0.0588235294117647\n $: 0.3333333333333333\n RB: 0.3333333333333333\n '': 0.6666666666666666\ncell 4-5\n .: 0.8333333333333333\ncell 0-2\n NP: 5.316321105794787E-4\ncell 2-4\n VP: 0.0011986500844061413\n PP: 9.965496033607483E-4\ncell 0-3\n : 2.5315814789499013E-6\n S: 3.164476848687374E-6\n NP: 2.5312450114487086E-5\ncell 1-4\n QP: 0.002754820936639117\ncell 0-4\n : 5.442039250199556E-7\n S: 6.802549062749451E-7\n NP: 6.269843578954069E-6\ncell 0-5\n : 1.2514394214109883E-7\n S: 1.5642992767637368E-7\n");
    if(insideOutsideOpt>0){
      assertEquals(parser.dumpOutsideChart(), "# Outside chart snapshot\ncell 0-1\n DT: 1.0011515371287891E-6\ncell 1-2\n NN: 2.9694419128042183E-6\n VBG: 1.054825616229126E-7\ncell 2-3\n VBD: 1.1660468180112154E-7\n VBZ: 1.0007291190934357E-7\n TO: 7.032170774860844E-8\n VB: 1.0007291190934357E-7\n NN: 1.2657907394749483E-6\n NNS: 1.4804570052338546E-8\ncell 3-4\n VBD: 9.047312024762741E-7\n NN: 6.267267988823337E-8\n NNPS: 6.267267988823337E-8\n NNP: 3.760360793294006E-7\n NNS: 6.267267988823337E-8\n PRP: 1.2534535977646652E-7\n CD: 6.267267988823337E-8\ncell 4-5\n .: 1.5017273056931864E-7\ncell 0-2\n NP: 1.6324568301733438E-4\ncell 2-4\n VP: 7.088428141059727E-5\ncell 0-3\n NP: 0.0015873015873015858\ncell 0-5\n : 1.0\n S: 0.8\n");
    
      assertEquals(parser.sprintExpectedCounts(), "# Expected counts\n1.000000 ROOT->[S]\n0.008028 S->[VP]\n1.000000 S->[NP VP .]\n0.004014 VP->[VBD SBAR]\n0.159932 VP->[VBZ NP]\n0.517803 VP->[VBD]\n0.057119 VP->[VBD NP]\n0.187309 VP->[TO VP]\n0.266554 VP->[VB NP]\n0.004014 VP->[VBD S]\n0.013144 NP->[NP NNS]\n0.025542 NP->[DT NN NN]\n0.083467 NP->[NNP]\n0.200322 NP->[PRP]\n0.001408 NP->[NP VP]\n0.083467 NP->[NNPS]\n0.015176 NP->[NN]\n0.045528 NP->[CD]\n0.693495 NP->[DT NN]\n0.055645 NP->[NNS]\n0.280963 NP->[DT VBG NN]\n0.004014 SBAR->[S]\n1.000000 DT->[_The]\n1.000000 .->[_.]\n0.280963 VBG->[_two]\n0.719037 NN->[_two]\n0.013144 NNS->[_young]\n0.266554 VB->[_young]\n0.159932 VBZ->[_young]\n0.066555 VBD->[_young]\n0.187309 TO->[_young]\n0.306505 NN->[_young]\n0.045528 CD->[_sea-lions]\n0.055645 NNS->[_sea-lions]\n0.083467 NNPS->[_sea-lions]\n0.200322 PRP->[_sea-lions]\n0.516395 VBD->[_sea-lions]\n0.083467 NNP->[_sea-lions]\n0.015176 NN->[_sea-lions]\n");
    }
  }

  @Test
  public void testSocialViterbi(){
    rootSymbol = "Sentence";
    initParserFromFile(social);
    
    String inputSentence = ".dog kid.eyes mom.eyes # .pig kid.hands # ## and whats that is this a puppy dog";

    parser.parseSentence(inputSentence);    
    
    if(parser.getDecodeOpt()==1){
      Tree tree = parser.viterbiParse();
      assertEquals(tree.toString(), "( (Sentence (Topic.dog (T.dog (PSEUDO.DOG .dog) (Socials.Topical.kid.eyes (PSEUDOKID.EYES kid.eyes) (Socials.Topical.kid.hands (Socials.Topical.mom.eyes (PSEUDOMOM.EYES mom.eyes) (Socials.Topical.mom.hands (Socials.Topical.mom.point #)))))) (Topic.None (T.None (PSEUDO.PIG .pig) (Socials.NotTopical.kid.eyes (Socials.NotTopical.kid.hands (PSEUDOKID.HANDS kid.hands) (Socials.NotTopical.mom.eyes (Socials.NotTopical.mom.hands (Socials.NotTopical.mom.point #)))))) (Topic.None ##))) (Words.dog (Word.dog (Word and)) (Words.dog (Word.dog (Word whats)) (Words.dog (Word.dog (Word that)) (Words.dog (Word.dog (Word is)) (Words.dog (Word.dog (Word this)) (Words.dog (Word.dog (Word a)) (Words.dog (Word.dog (Word puppy)) (Words.dog (Word.dog (Word dog))))))))))))");
      
    }
    
    assertEquals(parser.dumpInsideChart(), "# Inside chart snapshot\ncell 0-1\n PSEUDO.DOG: 1.0\ncell 1-2\n PSEUDOKID.EYES: 1.0\ncell 2-3\n PSEUDOMOM.EYES: 1.0\ncell 3-4\n Socials.NotTopical.mom.point: 0.9954919\n Socials.Topical.mom.point: 0.9240551\n PSEUDO#: 1.0\ncell 4-5\n PSEUDO.PIG: 1.0\ncell 5-6\n PSEUDOKID.HANDS: 1.0\ncell 6-7\n Socials.NotTopical.mom.point: 0.9954919\n Socials.Topical.mom.point: 0.9240551\n PSEUDO#: 1.0\ncell 7-8\n Topic.None: 0.6392033\ncell 8-9\n Word: 0.008512715000000004\ncell 9-10\n Word: 0.007482514000000003\ncell 10-11\n Word: 0.023098189999999998\ncell 11-12\n Word: 0.017567640000000002\ncell 12-13\n Word: 0.014585479999999994\ncell 13-14\n Word: 0.023477740000000004\ncell 14-15\n Word: 0.0012470859999999997\ncell 15-16\n Word: 0.003741257000000001\ncell 2-4\n Socials.Topical.mom.eyes: 0.16067402449033166\n Socials.NotTopical.mom.eyes: 0.02254170972232741\ncell 5-7\n Socials.Topical.kid.hands: 0.1270535981750901\n Socials.NotTopical.kid.hands: 0.14238098917060493\ncell 8-10\n Words.None: 1.1625689229372852E-5\n Words.pig: 1.2403822643193782E-5\n Words.dog: 1.2462627832778722E-5\ncell 9-11\n Words.None: 3.154485715791116E-5\n Words.pig: 3.3656225086684064E-5\n Words.dog: 3.381578563135388E-5\ncell 10-12\n Words.None: 7.406183194600182E-5\n Words.pig: 7.901895620667529E-5\n Words.dog: 7.939357658252253E-5\ncell 11-13\n Words.None: 4.676675395828715E-5\n Words.pig: 4.9896957526686654E-5\n Words.dog: 5.013351363777209E-5\ncell 12-14\n Words.None: 6.25000108197025E-5\n Words.pig: 6.66832765017151E-5\n Words.dog: 6.699941474632149E-5\ncell 13-15\n Words.None: 5.3438685934984235E-6\n Words.pig: 5.701545685120958E-6\n Words.dog: 5.728576100226463E-6\ncell 14-16\n Words.None: 8.515634717185806E-7\n Words.pig: 9.08560521808256E-7\n Words.dog: 9.128679095605021E-7\ncell 1-4\n Socials.Topical.kid.eyes: 0.06265375028444266\n Socials.NotTopical.kid.eyes: 8.892864705595748E-4\ncell 4-7\n T.None: 0.021514356657352324\n T.pig: 0.037113613857565744\ncell 8-11\n Words.None: 2.0402421488079603E-7\n Words.pig: 2.106073003428523E-7\n Words.dog: 2.1103815638859621E-7\ncell 9-12\n Words.None: 4.2104357520584956E-7\n Words.pig: 4.3462905004984997E-7\n Words.dog: 4.355182051435483E-7\ncell 10-13\n Words.None: 8.207298570097437E-7\n Words.pig: 8.472116880664823E-7\n Words.dog: 8.489448961615189E-7\ncell 11-14\n Words.None: 8.342161092757464E-7\n Words.pig: 8.611330904017133E-7\n Words.dog: 8.628947786128328E-7\ncell 12-15\n Words.None: 5.9219065899133355E-8\n Words.pig: 6.112983993164185E-8\n Words.dog: 6.125489808996296E-8\ncell 13-16\n Words.None: 1.5190020817182228E-8\n Words.pig: 1.5680145017725447E-8\n Words.dog: 1.571222313309961E-8\ncell 0-4\n T.dog: 0.06265375028444266\n T.None: 1.777611622444473E-4\ncell 4-8\n Topic.None: 0.004961693454652923\n Topic.pig: 0.012275550586297956\ncell 8-12\n Words.None: 2.7232041163462092E-9\n Words.pig: 2.719736115557204E-9\n Words.dog: 2.7179897604373575E-9\ncell 9-13\n Words.None: 4.6658720719400624E-9\n Words.pig: 4.6599300832621815E-9\n Words.dog: 4.656937920635637E-9\ncell 10-14\n Words.None: 1.4640016895159881E-8\n Words.pig: 1.462137283177928E-8\n Words.dog: 1.4611984380760798E-8\ncell 11-15\n Words.None: 7.904238431546914E-10\n Words.pig: 7.89417238289765E-10\n Words.dog: 7.889103498354386E-10\ncell 12-16\n Words.None: 1.6833101863252005E-10\n Words.pig: 1.6811664905885158E-10\n Words.dog: 1.6800870058210992E-10\ncell 8-13\n Words.None: 3.017768891602236E-11\n Words.pig: 2.9159993198719727E-11\n Words.dog: 2.9063100999665872E-11\ncell 9-14\n Words.None: 8.322890337233836E-11\n Words.pig: 8.04221377928561E-11\n Words.dog: 8.01549128408379E-11\ncell 10-15\n Words.None: 1.3871487603096472E-11\n Words.pig: 1.3403693214812774E-11\n Words.dog: 1.3359155710905322E-11\ncell 11-16\n Words.None: 2.246790769990981E-12\n Words.pig: 2.171021238710509E-12\n Words.dog: 2.163807415971328E-12\ncell 8-14\n Words.None: 5.38303647435359E-13\n Words.pig: 5.032498233159115E-13\n Words.dog: 5.002322056280966E-13\ncell 9-15\n Words.None: 7.885979296447372E-14\n Words.pig: 7.372451787235266E-14\n Words.dog: 7.328244636263734E-14\ncell 10-16\n Words.None: 3.9429896482237066E-14\n Words.pig: 3.6862258936176266E-14\n Words.dog: 3.664122318131887E-14\ncell 8-15\n Words.None: 5.100453384428727E-16\n Words.pig: 4.613387757594292E-16\n Words.dog: 4.573423946027671E-16\ncell 9-16\n Words.None: 2.241600585438104E-16\n Words.pig: 2.0275398908355192E-16\n Words.dog: 2.0099761770532466E-16\ncell 0-8\n Topic.None: 3.182213887999734E-7\n Topic.dog: 3.024839831335377E-4\n Topic.pig: 1.0529792702262415E-6\ncell 8-16\n Words.None: 1.4498109699179447E-18\n Words.pig: 1.2687540021095776E-18\n Words.dog: 1.254389507357823E-18\ncell 0-9\n : 9.834103812626127E-8\n Sentence: 9.834103812626127E-8\ncell 0-10\n : 5.394757284428202E-10\n Sentence: 5.394757284428202E-10\ncell 0-11\n : 9.135658174526192E-12\n Sentence: 9.135658174526192E-12\ncell 0-12\n : 1.1766401369615847E-13\n Sentence: 1.1766401369615847E-13\ncell 0-13\n : 1.25821600643398E-15\n Sentence: 1.25821600643398E-15\ncell 0-14\n : 2.1657222445987203E-17\n Sentence: 2.1657222445987203E-17\ncell 0-15\n : 1.9801171309599656E-20\n Sentence: 1.9801171309599656E-20\ncell 0-16\n : 5.431260293503627E-23\n Sentence: 5.431260293503627E-23\n");
    if(insideOutsideOpt>0){
      assertEquals(parser.dumpOutsideChart(), "# Outside chart snapshot\ncell 0-1\n PSEUDO.DOG: 5.431260293503588E-23\ncell 1-2\n PSEUDOKID.EYES: 5.431260293503627E-23\ncell 2-3\n PSEUDOMOM.EYES: 5.431260293503627E-23\ncell 3-4\n Socials.NotTopical.mom.point: 3.141007280125314E-25\n Socials.Topical.mom.point: 5.843798514235332E-23\ncell 4-5\n PSEUDO.PIG: 5.431260293503588E-23\ncell 5-6\n PSEUDOKID.HANDS: 5.431260293503627E-23\ncell 6-7\n Socials.NotTopical.mom.point: 5.42912797335866E-23\n Socials.Topical.mom.point: 2.8794140048192755E-25\ncell 7-8\n Topic.None: 8.496921548283038E-23\ncell 8-9\n Word: 6.380174002657925E-21\ncell 9-10\n Word: 7.25860358363992E-21\ncell 10-11\n Word: 2.3513791745169584E-21\ncell 11-12\n Word: 3.091627727744628E-21\ncell 12-13\n Word: 3.7237446374775365E-21\ncell 13-14\n Word: 2.313365891905955E-21\ncell 14-15\n Word: 4.3551609860936576E-20\ncell 15-16\n Word: 1.4517207167279866E-20\ncell 2-4\n Socials.Topical.mom.eyes: 3.360836848134411E-22\n Socials.NotTopical.mom.eyes: 1.3871384840470467E-23\ncell 5-7\n Socials.Topical.kid.hands: 2.094184843547665E-24\n Socials.NotTopical.kid.hands: 3.79590909785431E-22\ncell 14-16\n Words.None: 5.473580355677241E-20\n Words.pig: 2.9285194902252697E-19\n Words.dog: 5.915414227948253E-17\ncell 1-4\n Socials.Topical.kid.eyes: 8.618784663226156E-22\n Socials.NotTopical.kid.eyes: 3.516130525676682E-22\ncell 4-7\n T.None: 2.51211458823472E-21\n T.pig: 7.169167643916456E-24\ncell 13-16\n Words.None: 3.0685284414743642E-18\n Words.pig: 1.696883028286342E-17\n Words.dog: 3.4368095302031684E-15\ncell 0-4\n T.dog: 8.618784663226156E-22\n T.None: 1.7590160109922853E-21\ncell 4-8\n Topic.None: 1.0892758633594455E-20\n Topic.pig: 2.167509455041977E-23\ncell 12-16\n Words.None: 2.76900902060522E-16\n Words.pig: 1.5826732278212714E-15\n Words.dog: 3.2141143891606966E-13\ncell 11-16\n Words.None: 2.0745594795325902E-14\n Words.pig: 1.2255693996549922E-13\n Words.dog: 2.495597242431791E-11\ncell 10-16\n Words.None: 1.1821235930738322E-12\n Words.pig: 7.218052482273284E-12\n Words.dog: 1.4737476949745042E-9\ncell 9-16\n Words.None: 2.0793628984086637E-10\n Words.pig: 1.3122983218190767E-9\n Words.dog: 2.6865949368456127E-7\ncell 0-8\n Topic.None: 1.4647353240422799E-19\n Topic.dog: 1.7852157871339675E-19\n Topic.pig: 2.526865695649458E-19\ncell 8-16\n Words.None: 3.214971597762803E-8\n Words.pig: 2.097126150333827E-7\n Words.dog: 4.304876426960728E-5\ncell 0-16\n : 1.0\n Sentence: 1.0\n");
    
      assertEquals(parser.sprintExpectedCounts(), "# Expected counts\n1.000000 PSEUDO.PIG->[_.pig]\n0.994243 T.dog->[PSEUDO.DOG Socials.Topical.kid.eyes]\n0.004899 T.pig->[PSEUDO.PIG Socials.Topical.kid.eyes]\n0.019596 Word.pig->[Word]\n1.000000 Topic.None->[_##]\n0.995959 Topic.None->[T.None Topic.None]\n3.976971 Word.dog->[Word]\n1.000000 PSEUDOKID.HANDS->[_kid.hands]\n0.000858 Words.None->[Word.None]\n0.006007 Words.None->[Word.None Words.None]\n1.000000 PSEUDOKID.EYES->[_kid.eyes]\n1.000858 Socials.NotTopical.mom.hands->[Socials.NotTopical.mom.point]\n0.994243 Socials.Topical.kid.hands->[Socials.Topical.mom.eyes]\n0.004899 Socials.Topical.kid.hands->[PSEUDOKID.HANDS Socials.Topical.mom.eyes]\n4.003433 Word.None->[Word]\n1.000000 Word->[_that]\n1.000000 Word->[_a]\n1.000000 Word->[_puppy]\n1.000000 Word->[_is]\n1.000000 Word->[_and]\n1.000000 Word->[_dog]\n1.000000 Word->[_this]\n1.000000 Word->[_whats]\n0.999142 Socials.Topical.mom.point->[_#]\n0.995101 T.None->[PSEUDO.PIG Socials.NotTopical.kid.eyes]\n0.005757 T.None->[PSEUDO.DOG Socials.NotTopical.kid.eyes]\n1.000000 PSEUDO.DOG->[_.dog]\n0.002449 Words.pig->[Word.None]\n0.002449 Words.pig->[Word.pig]\n0.017146 Words.pig->[Word.pig Words.pig]\n0.017146 Words.pig->[Word.None Words.pig]\n0.994243 Topic.dog->[T.dog Topic.None]\n0.004899 Socials.Topical.mom.eyes->[Socials.Topical.mom.hands]\n0.994243 Socials.Topical.mom.eyes->[PSEUDOMOM.EYES Socials.Topical.mom.hands]\n0.497121 Words.dog->[Word.dog]\n0.497121 Words.dog->[Word.None]\n3.479850 Words.dog->[Word.None Words.dog]\n3.479850 Words.dog->[Word.dog Words.dog]\n0.999142 Socials.Topical.mom.hands->[Socials.Topical.mom.point]\n0.000858 Sentence->[Topic.None Words.None]\n0.994243 Sentence->[Topic.dog Words.dog]\n0.004899 Sentence->[Topic.pig Words.pig]\n0.004899 Socials.Topical.kid.eyes->[Socials.Topical.kid.hands]\n0.994243 Socials.Topical.kid.eyes->[PSEUDOKID.EYES Socials.Topical.kid.hands]\n0.005757 Socials.NotTopical.mom.eyes->[PSEUDOMOM.EYES Socials.NotTopical.mom.hands]\n0.995101 Socials.NotTopical.mom.eyes->[Socials.NotTopical.mom.hands]\n0.004899 Topic.pig->[T.pig Topic.None]\n0.004899 Topic.pig->[T.None Topic.pig]\n1.000000 PSEUDOMOM.EYES->[_mom.eyes]\n0.995101 Socials.NotTopical.kid.eyes->[Socials.NotTopical.kid.hands]\n0.005757 Socials.NotTopical.kid.eyes->[PSEUDOKID.EYES Socials.NotTopical.kid.hands]\n0.995101 Socials.NotTopical.kid.hands->[PSEUDOKID.HANDS Socials.NotTopical.mom.eyes]\n0.005757 Socials.NotTopical.kid.hands->[Socials.NotTopical.mom.eyes]\n1.000858 Socials.NotTopical.mom.point->[_#]\n");
    }
  }
  
  @Test
  public void testSocialDiscourse(){
    rootSymbol = "Discourse";
    initParserFromFile(socialDiscourse);
    
    String inputSentence = ".dog kid.eyes mom.eyes # .pig kid.hands # ## and whats that is this a puppy dog .dog kid.eyes mom.eyes mom.hands # .pig kid.hands # ## woof woof woof .dog mom.hands # ## woof woof woof woof woof .dog kid.eyes mom.hands # ## thats nice ## come here you come here you";

    parser.parseSentence(inputSentence);    
    
    if(parser.getDecodeOpt()==1){
      Tree tree = parser.viterbiParse();
      System.err.println(tree.pennString());
      assertEquals(tree.toString(), "( (Discourse (Discourse.pig (Sentence.pig (Topic.pig (T.None (PSEUDO.DOG .dog) (Socials.NotTopical.kid.eyes (PSEUDOKID.EYES kid.eyes) (Socials.NotTopical.kid.hands (Socials.NotTopical.mom.eyes (PSEUDOMOM.EYES mom.eyes) (Socials.NotTopical.mom.hands (Socials.NotTopical.mom.point #)))))) (Topic.pig (T.pig (PSEUDO.PIG .pig) (Socials.Topical.kid.eyes (Socials.Topical.kid.hands (PSEUDOKID.HANDS kid.hands) (Socials.Topical.mom.eyes (Socials.Topical.mom.hands (Socials.Topical.mom.point #)))))) (Topic.None ##))) (Words.pig (Word.pig and) (Words.pig (Word.pig whats) (Words.pig (Word.pig that) (Words.pig (Word.pig is) (Words.pig (Word.pig this) (Words.pig (Word.pig a) (Words.pig (Word.pig puppy) (Words.pig (Word.pig dog)))))))))) (Discourse.pig (Sentence.pig (Topic.pig (T.None (PSEUDO.DOG .dog) (Socials.NotTopical.kid.eyes (PSEUDOKID.EYES kid.eyes) (Socials.NotTopical.kid.hands (Socials.NotTopical.mom.eyes (PSEUDOMOM.EYES mom.eyes) (Socials.NotTopical.mom.hands (PSEUDOMOM.HANDS mom.hands) (Socials.NotTopical.mom.point #)))))) (Topic.pig (T.pig (PSEUDO.PIG .pig) (Socials.Topical.kid.eyes (Socials.Topical.kid.hands (PSEUDOKID.HANDS kid.hands) (Socials.Topical.mom.eyes (Socials.Topical.mom.hands (Socials.Topical.mom.point #)))))) (Topic.None ##))) (Words.pig (Word.pig woof) (Words.pig (Word.pig woof) (Words.pig (Word.None woof))))) (Discourse.dog (Sentence.dog (Topic.dog (T.dog (PSEUDO.DOG .dog) (Socials.Topical.kid.eyes (Socials.Topical.kid.hands (Socials.Topical.mom.eyes (Socials.Topical.mom.hands (PSEUDOMOM.HANDS mom.hands) (Socials.Topical.mom.point #)))))) (Topic.None ##)) (Words.dog (Word.dog woof) (Words.dog (Word.dog woof) (Words.dog (Word.dog woof) (Words.dog (Word.dog woof) (Words.dog (Word.None woof))))))) (Discourse.dog (Sentence.dog (Topic.dog (T.dog (PSEUDO.DOG .dog) (Socials.Topical.kid.eyes (PSEUDOKID.EYES kid.eyes) (Socials.Topical.kid.hands (Socials.Topical.mom.eyes (Socials.Topical.mom.hands (PSEUDOMOM.HANDS mom.hands) (Socials.Topical.mom.point #)))))) (Topic.None ##)) (Words.dog (Word.dog thats) (Words.dog (Word.None nice)))) (Discourse.None (Sentence.None (Topic.None ##) (Words.None (Word.None come) (Words.None (Word.None here) (Words.None (Word.None you) (Words.None (Word.None come) (Words.None (Word.None here) (Words.None (Word.None you)))))))))))))))");
      
    }
    
    assertEquals(parser.dumpInsideChart(), "# Inside chart snapshot\ncell 0-1\n PSEUDO.DOG: 1.0\ncell 1-2\n PSEUDOKID.EYES: 1.0\ncell 2-3\n PSEUDOMOM.EYES: 1.0\ncell 3-4\n Socials.NotTopical.mom.point: 0.5\n PSEUDO#: 1.0\n Socials.Topical.mom.point: 0.5\ncell 4-5\n PSEUDO.PIG: 1.0\ncell 5-6\n PSEUDOKID.HANDS: 1.0\ncell 6-7\n Socials.NotTopical.mom.point: 0.5\n PSEUDO#: 1.0\n Socials.Topical.mom.point: 0.5\ncell 7-8\n Topic.None: 0.5\ncell 8-9\n Word.None: 9.009010000000001E-4\n Word.alphabet: 9.009010000000001E-4\n Word.baby: 9.009010000000001E-4\n Word.ball: 9.009010000000001E-4\n Word.bear: 9.009010000000001E-4\n Word.block: 9.009010000000001E-4\n Word.blocks: 9.009010000000001E-4\n Word.book: 9.009010000000001E-4\n Word.box: 9.009010000000001E-4\n Word.brush: 9.009010000000001E-4\n Word.bus: 9.009010000000001E-4\n Word.car: 9.009010000000001E-4\n Word.cheese: 9.009010000000001E-4\n Word.dog: 9.009010000000001E-4\n Word.doll: 9.009010000000001E-4\n Word.dough: 9.009010000000001E-4\n Word.ernie: 9.009010000000001E-4\n Word.firetruck: 9.009010000000001E-4\n Word.flashlight: 9.009010000000001E-4\n Word.hotdog: 9.009010000000001E-4\n Word.mickey: 9.009010000000001E-4\n Word.oscar: 9.009010000000001E-4\n Word.pepperoni: 9.009010000000001E-4\n Word.phone: 9.009010000000001E-4\n Word.pig: 9.009010000000001E-4\n Word.popnpals: 9.009010000000001E-4\n Word.rabbit: 9.009010000000001E-4\n Word.rings: 9.009010000000001E-4\n Word.train: 9.009010000000001E-4\n Word.truck: 9.009010000000001E-4\n Word.waffles: 9.009010000000001E-4\ncell 9-10\n Word.None: 9.009010000000001E-4\n Word.alphabet: 9.009010000000001E-4\n Word.baby: 9.009010000000001E-4\n Word.ball: 9.009010000000001E-4\n Word.bear: 9.009010000000001E-4\n Word.block: 9.009010000000001E-4\n Word.blocks: 9.009010000000001E-4\n Word.book: 9.009010000000001E-4\n Word.box: 9.009010000000001E-4\n Word.brush: 9.009010000000001E-4\n Word.bus: 9.009010000000001E-4\n Word.car: 9.009010000000001E-4\n Word.cheese: 9.009010000000001E-4\n Word.dog: 9.009010000000001E-4\n Word.doll: 9.009010000000001E-4\n Word.dough: 9.009010000000001E-4\n Word.ernie: 9.009010000000001E-4\n Word.firetruck: 9.009010000000001E-4\n Word.flashlight: 9.009010000000001E-4\n Word.hotdog: 9.009010000000001E-4\n Word.mickey: 9.009010000000001E-4\n Word.oscar: 9.009010000000001E-4\n Word.pepperoni: 9.009010000000001E-4\n Word.phone: 9.009010000000001E-4\n Word.pig: 9.009010000000001E-4\n Word.popnpals: 9.009010000000001E-4\n Word.rabbit: 9.009010000000001E-4\n Word.rings: 9.009010000000001E-4\n Word.train: 9.009010000000001E-4\n Word.truck: 9.009010000000001E-4\n Word.waffles: 9.009010000000001E-4\ncell 10-11\n Word.None: 9.009010000000001E-4\n Word.alphabet: 9.009010000000001E-4\n Word.baby: 9.009010000000001E-4\n Word.ball: 9.009010000000001E-4\n Word.bear: 9.009010000000001E-4\n Word.block: 9.009010000000001E-4\n Word.blocks: 9.009010000000001E-4\n Word.book: 9.009010000000001E-4\n Word.box: 9.009010000000001E-4\n Word.brush: 9.009010000000001E-4\n Word.bus: 9.009010000000001E-4\n Word.car: 9.009010000000001E-4\n Word.cheese: 9.009010000000001E-4\n Word.dog: 9.009010000000001E-4\n Word.doll: 9.009010000000001E-4\n Word.dough: 9.009010000000001E-4\n Word.ernie: 9.009010000000001E-4\n Word.firetruck: 9.009010000000001E-4\n Word.flashlight: 9.009010000000001E-4\n Word.hotdog: 9.009010000000001E-4\n Word.mickey: 9.009010000000001E-4\n Word.oscar: 9.009010000000001E-4\n Word.pepperoni: 9.009010000000001E-4\n Word.phone: 9.009010000000001E-4\n Word.pig: 9.009010000000001E-4\n Word.popnpals: 9.009010000000001E-4\n Word.rabbit: 9.009010000000001E-4\n Word.rings: 9.009010000000001E-4\n Word.train: 9.009010000000001E-4\n Word.truck: 9.009010000000001E-4\n Word.waffles: 9.009010000000001E-4\ncell 11-12\n Word.None: 9.009010000000001E-4\n Word.alphabet: 9.009010000000001E-4\n Word.baby: 9.009010000000001E-4\n Word.ball: 9.009010000000001E-4\n Word.bear: 9.009010000000001E-4\n Word.block: 9.009010000000001E-4\n Word.blocks: 9.009010000000001E-4\n Word.book: 9.009010000000001E-4\n Word.box: 9.009010000000001E-4\n Word.brush: 9.009010000000001E-4\n Word.bus: 9.009010000000001E-4\n Word.car: 9.009010000000001E-4\n Word.cheese: 9.009010000000001E-4\n Word.dog: 9.009010000000001E-4\n Word.doll: 9.009010000000001E-4\n Word.dough: 9.009010000000001E-4\n Word.ernie: 9.009010000000001E-4\n Word.firetruck: 9.009010000000001E-4\n Word.flashlight: 9.009010000000001E-4\n Word.hotdog: 9.009010000000001E-4\n Word.mickey: 9.009010000000001E-4\n Word.oscar: 9.009010000000001E-4\n Word.pepperoni: 9.009010000000001E-4\n Word.phone: 9.009010000000001E-4\n Word.pig: 9.009010000000001E-4\n Word.popnpals: 9.009010000000001E-4\n Word.rabbit: 9.009010000000001E-4\n Word.rings: 9.009010000000001E-4\n Word.train: 9.009010000000001E-4\n Word.truck: 9.009010000000001E-4\n Word.waffles: 9.009010000000001E-4\ncell 12-13\n Word.None: 9.009010000000001E-4\n Word.alphabet: 9.009010000000001E-4\n Word.baby: 9.009010000000001E-4\n Word.ball: 9.009010000000001E-4\n Word.bear: 9.009010000000001E-4\n Word.block: 9.009010000000001E-4\n Word.blocks: 9.009010000000001E-4\n Word.book: 9.009010000000001E-4\n Word.box: 9.009010000000001E-4\n Word.brush: 9.009010000000001E-4\n Word.bus: 9.009010000000001E-4\n Word.car: 9.009010000000001E-4\n Word.cheese: 9.009010000000001E-4\n Word.dog: 9.009010000000001E-4\n Word.doll: 9.009010000000001E-4\n Word.dough: 9.009010000000001E-4\n Word.ernie: 9.009010000000001E-4\n Word.firetruck: 9.009010000000001E-4\n Word.flashlight: 9.009010000000001E-4\n Word.hotdog: 9.009010000000001E-4\n Word.mickey: 9.009010000000001E-4\n Word.oscar: 9.009010000000001E-4\n Word.pepperoni: 9.009010000000001E-4\n Word.phone: 9.009010000000001E-4\n Word.pig: 9.009010000000001E-4\n Word.popnpals: 9.009010000000001E-4\n Word.rabbit: 9.009010000000001E-4\n Word.rings: 9.009010000000001E-4\n Word.train: 9.009010000000001E-4\n Word.truck: 9.009010000000001E-4\n Word.waffles: 9.009010000000001E-4\ncell 13-14\n Word.None: 9.009010000000001E-4\n Word.alphabet: 9.009010000000001E-4\n Word.baby: 9.009010000000001E-4\n Word.ball: 9.009010000000001E-4\n Word.bear: 9.009010000000001E-4\n Word.block: 9.009010000000001E-4\n Word.blocks: 9.009010000000001E-4\n Word.book: 9.009010000000001E-4\n Word.box: 9.009010000000001E-4\n Word.brush: 9.009010000000001E-4\n Word.bus: 9.009010000000001E-4\n Word.car: 9.009010000000001E-4\n Word.cheese: 9.009010000000001E-4\n Word.dog: 9.009010000000001E-4\n Word.doll: 9.009010000000001E-4\n Word.dough: 9.009010000000001E-4\n Word.ernie: 9.009010000000001E-4\n Word.firetruck: 9.009010000000001E-4\n Word.flashlight: 9.009010000000001E-4\n Word.hotdog: 9.009010000000001E-4\n Word.mickey: 9.009010000000001E-4\n Word.oscar: 9.009010000000001E-4\n Word.pepperoni: 9.009010000000001E-4\n Word.phone: 9.009010000000001E-4\n Word.pig: 9.009010000000001E-4\n Word.popnpals: 9.009010000000001E-4\n Word.rabbit: 9.009010000000001E-4\n Word.rings: 9.009010000000001E-4\n Word.train: 9.009010000000001E-4\n Word.truck: 9.009010000000001E-4\n Word.waffles: 9.009010000000001E-4\ncell 14-15\n Word.None: 9.009010000000001E-4\n Word.alphabet: 9.009010000000001E-4\n Word.baby: 9.009010000000001E-4\n Word.ball: 9.009010000000001E-4\n Word.bear: 9.009010000000001E-4\n Word.block: 9.009010000000001E-4\n Word.blocks: 9.009010000000001E-4\n Word.book: 9.009010000000001E-4\n Word.box: 9.009010000000001E-4\n Word.brush: 9.009010000000001E-4\n Word.bus: 9.009010000000001E-4\n Word.car: 9.009010000000001E-4\n Word.cheese: 9.009010000000001E-4\n Word.dog: 9.009010000000001E-4\n Word.doll: 9.009010000000001E-4\n Word.dough: 9.009010000000001E-4\n Word.ernie: 9.009010000000001E-4\n Word.firetruck: 9.009010000000001E-4\n Word.flashlight: 9.009010000000001E-4\n Word.hotdog: 9.009010000000001E-4\n Word.mickey: 9.009010000000001E-4\n Word.oscar: 9.009010000000001E-4\n Word.pepperoni: 9.009010000000001E-4\n Word.phone: 9.009010000000001E-4\n Word.pig: 9.009010000000001E-4\n Word.popnpals: 9.009010000000001E-4\n Word.rabbit: 9.009010000000001E-4\n Word.rings: 9.009010000000001E-4\n Word.train: 9.009010000000001E-4\n Word.truck: 9.009010000000001E-4\n Word.waffles: 9.009010000000001E-4\ncell 15-16\n Word.None: 9.009010000000001E-4\n Word.alphabet: 9.009010000000001E-4\n Word.baby: 9.009010000000001E-4\n Word.ball: 9.009010000000001E-4\n Word.bear: 9.009010000000001E-4\n Word.block: 9.009010000000001E-4\n Word.blocks: 9.009010000000001E-4\n Word.book: 9.009010000000001E-4\n Word.box: 9.009010000000001E-4\n Word.brush: 9.009010000000001E-4\n Word.bus: 9.009010000000001E-4\n Word.car: 9.009010000000001E-4\n Word.cheese: 9.009010000000001E-4\n Word.dog: 9.009010000000001E-4\n Word.doll: 9.009010000000001E-4\n Word.dough: 9.009010000000001E-4\n Word.ernie: 9.009010000000001E-4\n Word.firetruck: 9.009010000000001E-4\n Word.flashlight: 9.009010000000001E-4\n Word.hotdog: 9.009010000000001E-4\n Word.mickey: 9.009010000000001E-4\n Word.oscar: 9.009010000000001E-4\n Word.pepperoni: 9.009010000000001E-4\n Word.phone: 9.009010000000001E-4\n Word.pig: 9.009010000000001E-4\n Word.popnpals: 9.009010000000001E-4\n Word.rabbit: 9.009010000000001E-4\n Word.rings: 9.009010000000001E-4\n Word.train: 9.009010000000001E-4\n Word.truck: 9.009010000000001E-4\n Word.waffles: 9.009010000000001E-4\ncell 16-17\n PSEUDO.DOG: 1.0\ncell 17-18\n PSEUDOKID.EYES: 1.0\ncell 18-19\n PSEUDOMOM.EYES: 1.0\ncell 19-20\n PSEUDOMOM.HANDS: 1.0\ncell 20-21\n Socials.NotTopical.mom.point: 0.5\n PSEUDO#: 1.0\n Socials.Topical.mom.point: 0.5\ncell 21-22\n PSEUDO.PIG: 1.0\ncell 22-23\n PSEUDOKID.HANDS: 1.0\ncell 23-24\n Socials.NotTopical.mom.point: 0.5\n PSEUDO#: 1.0\n Socials.Topical.mom.point: 0.5\ncell 24-25\n Topic.None: 0.5\ncell 25-26\n Word.None: 9.009010000000001E-4\n Word.alphabet: 9.009010000000001E-4\n Word.baby: 9.009010000000001E-4\n Word.ball: 9.009010000000001E-4\n Word.bear: 9.009010000000001E-4\n Word.block: 9.009010000000001E-4\n Word.blocks: 9.009010000000001E-4\n Word.book: 9.009010000000001E-4\n Word.box: 9.009010000000001E-4\n Word.brush: 9.009010000000001E-4\n Word.bus: 9.009010000000001E-4\n Word.car: 9.009010000000001E-4\n Word.cheese: 9.009010000000001E-4\n Word.dog: 9.009010000000001E-4\n Word.doll: 9.009010000000001E-4\n Word.dough: 9.009010000000001E-4\n Word.ernie: 9.009010000000001E-4\n Word.firetruck: 9.009010000000001E-4\n Word.flashlight: 9.009010000000001E-4\n Word.hotdog: 9.009010000000001E-4\n Word.mickey: 9.009010000000001E-4\n Word.oscar: 9.009010000000001E-4\n Word.pepperoni: 9.009010000000001E-4\n Word.phone: 9.009010000000001E-4\n Word.pig: 9.009010000000001E-4\n Word.popnpals: 9.009010000000001E-4\n Word.rabbit: 9.009010000000001E-4\n Word.rings: 9.009010000000001E-4\n Word.train: 9.009010000000001E-4\n Word.truck: 9.009010000000001E-4\n Word.waffles: 9.009010000000001E-4\ncell 26-27\n Word.None: 9.009010000000001E-4\n Word.alphabet: 9.009010000000001E-4\n Word.baby: 9.009010000000001E-4\n Word.ball: 9.009010000000001E-4\n Word.bear: 9.009010000000001E-4\n Word.block: 9.009010000000001E-4\n Word.blocks: 9.009010000000001E-4\n Word.book: 9.009010000000001E-4\n Word.box: 9.009010000000001E-4\n Word.brush: 9.009010000000001E-4\n Word.bus: 9.009010000000001E-4\n Word.car: 9.009010000000001E-4\n Word.cheese: 9.009010000000001E-4\n Word.dog: 9.009010000000001E-4\n Word.doll: 9.009010000000001E-4\n Word.dough: 9.009010000000001E-4\n Word.ernie: 9.009010000000001E-4\n Word.firetruck: 9.009010000000001E-4\n Word.flashlight: 9.009010000000001E-4\n Word.hotdog: 9.009010000000001E-4\n Word.mickey: 9.009010000000001E-4\n Word.oscar: 9.009010000000001E-4\n Word.pepperoni: 9.009010000000001E-4\n Word.phone: 9.009010000000001E-4\n Word.pig: 9.009010000000001E-4\n Word.popnpals: 9.009010000000001E-4\n Word.rabbit: 9.009010000000001E-4\n Word.rings: 9.009010000000001E-4\n Word.train: 9.009010000000001E-4\n Word.truck: 9.009010000000001E-4\n Word.waffles: 9.009010000000001E-4\ncell 27-28\n Word.None: 9.009010000000001E-4\n Word.alphabet: 9.009010000000001E-4\n Word.baby: 9.009010000000001E-4\n Word.ball: 9.009010000000001E-4\n Word.bear: 9.009010000000001E-4\n Word.block: 9.009010000000001E-4\n Word.blocks: 9.009010000000001E-4\n Word.book: 9.009010000000001E-4\n Word.box: 9.009010000000001E-4\n Word.brush: 9.009010000000001E-4\n Word.bus: 9.009010000000001E-4\n Word.car: 9.009010000000001E-4\n Word.cheese: 9.009010000000001E-4\n Word.dog: 9.009010000000001E-4\n Word.doll: 9.009010000000001E-4\n Word.dough: 9.009010000000001E-4\n Word.ernie: 9.009010000000001E-4\n Word.firetruck: 9.009010000000001E-4\n Word.flashlight: 9.009010000000001E-4\n Word.hotdog: 9.009010000000001E-4\n Word.mickey: 9.009010000000001E-4\n Word.oscar: 9.009010000000001E-4\n Word.pepperoni: 9.009010000000001E-4\n Word.phone: 9.009010000000001E-4\n Word.pig: 9.009010000000001E-4\n Word.popnpals: 9.009010000000001E-4\n Word.rabbit: 9.009010000000001E-4\n Word.rings: 9.009010000000001E-4\n Word.train: 9.009010000000001E-4\n Word.truck: 9.009010000000001E-4\n Word.waffles: 9.009010000000001E-4\ncell 28-29\n PSEUDO.DOG: 1.0\ncell 29-30\n PSEUDOMOM.HANDS: 1.0\ncell 30-31\n Socials.NotTopical.mom.point: 0.5\n PSEUDO#: 1.0\n Socials.Topical.mom.point: 0.5\ncell 31-32\n Topic.None: 0.5\ncell 32-33\n Word.None: 9.009010000000001E-4\n Word.alphabet: 9.009010000000001E-4\n Word.baby: 9.009010000000001E-4\n Word.ball: 9.009010000000001E-4\n Word.bear: 9.009010000000001E-4\n Word.block: 9.009010000000001E-4\n Word.blocks: 9.009010000000001E-4\n Word.book: 9.009010000000001E-4\n Word.box: 9.009010000000001E-4\n Word.brush: 9.009010000000001E-4\n Word.bus: 9.009010000000001E-4\n Word.car: 9.009010000000001E-4\n Word.cheese: 9.009010000000001E-4\n Word.dog: 9.009010000000001E-4\n Word.doll: 9.009010000000001E-4\n Word.dough: 9.009010000000001E-4\n Word.ernie: 9.009010000000001E-4\n Word.firetruck: 9.009010000000001E-4\n Word.flashlight: 9.009010000000001E-4\n Word.hotdog: 9.009010000000001E-4\n Word.mickey: 9.009010000000001E-4\n Word.oscar: 9.009010000000001E-4\n Word.pepperoni: 9.009010000000001E-4\n Word.phone: 9.009010000000001E-4\n Word.pig: 9.009010000000001E-4\n Word.popnpals: 9.009010000000001E-4\n Word.rabbit: 9.009010000000001E-4\n Word.rings: 9.009010000000001E-4\n Word.train: 9.009010000000001E-4\n Word.truck: 9.009010000000001E-4\n Word.waffles: 9.009010000000001E-4\ncell 33-34\n Word.None: 9.009010000000001E-4\n Word.alphabet: 9.009010000000001E-4\n Word.baby: 9.009010000000001E-4\n Word.ball: 9.009010000000001E-4\n Word.bear: 9.009010000000001E-4\n Word.block: 9.009010000000001E-4\n Word.blocks: 9.009010000000001E-4\n Word.book: 9.009010000000001E-4\n Word.box: 9.009010000000001E-4\n Word.brush: 9.009010000000001E-4\n Word.bus: 9.009010000000001E-4\n Word.car: 9.009010000000001E-4\n Word.cheese: 9.009010000000001E-4\n Word.dog: 9.009010000000001E-4\n Word.doll: 9.009010000000001E-4\n Word.dough: 9.009010000000001E-4\n Word.ernie: 9.009010000000001E-4\n Word.firetruck: 9.009010000000001E-4\n Word.flashlight: 9.009010000000001E-4\n Word.hotdog: 9.009010000000001E-4\n Word.mickey: 9.009010000000001E-4\n Word.oscar: 9.009010000000001E-4\n Word.pepperoni: 9.009010000000001E-4\n Word.phone: 9.009010000000001E-4\n Word.pig: 9.009010000000001E-4\n Word.popnpals: 9.009010000000001E-4\n Word.rabbit: 9.009010000000001E-4\n Word.rings: 9.009010000000001E-4\n Word.train: 9.009010000000001E-4\n Word.truck: 9.009010000000001E-4\n Word.waffles: 9.009010000000001E-4\ncell 34-35\n Word.None: 9.009010000000001E-4\n Word.alphabet: 9.009010000000001E-4\n Word.baby: 9.009010000000001E-4\n Word.ball: 9.009010000000001E-4\n Word.bear: 9.009010000000001E-4\n Word.block: 9.009010000000001E-4\n Word.blocks: 9.009010000000001E-4\n Word.book: 9.009010000000001E-4\n Word.box: 9.009010000000001E-4\n Word.brush: 9.009010000000001E-4\n Word.bus: 9.009010000000001E-4\n Word.car: 9.009010000000001E-4\n Word.cheese: 9.009010000000001E-4\n Word.dog: 9.009010000000001E-4\n Word.doll: 9.009010000000001E-4\n Word.dough: 9.009010000000001E-4\n Word.ernie: 9.009010000000001E-4\n Word.firetruck: 9.009010000000001E-4\n Word.flashlight: 9.009010000000001E-4\n Word.hotdog: 9.009010000000001E-4\n Word.mickey: 9.009010000000001E-4\n Word.oscar: 9.009010000000001E-4\n Word.pepperoni: 9.009010000000001E-4\n Word.phone: 9.009010000000001E-4\n Word.pig: 9.009010000000001E-4\n Word.popnpals: 9.009010000000001E-4\n Word.rabbit: 9.009010000000001E-4\n Word.rings: 9.009010000000001E-4\n Word.train: 9.009010000000001E-4\n Word.truck: 9.009010000000001E-4\n Word.waffles: 9.009010000000001E-4\ncell 35-36\n Word.None: 9.009010000000001E-4\n Word.alphabet: 9.009010000000001E-4\n Word.baby: 9.009010000000001E-4\n Word.ball: 9.009010000000001E-4\n Word.bear: 9.009010000000001E-4\n Word.block: 9.009010000000001E-4\n Word.blocks: 9.009010000000001E-4\n Word.book: 9.009010000000001E-4\n Word.box: 9.009010000000001E-4\n Word.brush: 9.009010000000001E-4\n Word.bus: 9.009010000000001E-4\n Word.car: 9.009010000000001E-4\n Word.cheese: 9.009010000000001E-4\n Word.dog: 9.009010000000001E-4\n Word.doll: 9.009010000000001E-4\n Word.dough: 9.009010000000001E-4\n Word.ernie: 9.009010000000001E-4\n Word.firetruck: 9.009010000000001E-4\n Word.flashlight: 9.009010000000001E-4\n Word.hotdog: 9.009010000000001E-4\n Word.mickey: 9.009010000000001E-4\n Word.oscar: 9.009010000000001E-4\n Word.pepperoni: 9.009010000000001E-4\n Word.phone: 9.009010000000001E-4\n Word.pig: 9.009010000000001E-4\n Word.popnpals: 9.009010000000001E-4\n Word.rabbit: 9.009010000000001E-4\n Word.rings: 9.009010000000001E-4\n Word.train: 9.009010000000001E-4\n Word.truck: 9.009010000000001E-4\n Word.waffles: 9.009010000000001E-4\ncell 36-37\n Word.None: 9.009010000000001E-4\n Word.alphabet: 9.009010000000001E-4\n Word.baby: 9.009010000000001E-4\n Word.ball: 9.009010000000001E-4\n Word.bear: 9.009010000000001E-4\n Word.block: 9.009010000000001E-4\n Word.blocks: 9.009010000000001E-4\n Word.book: 9.009010000000001E-4\n Word.box: 9.009010000000001E-4\n Word.brush: 9.009010000000001E-4\n Word.bus: 9.009010000000001E-4\n Word.car: 9.009010000000001E-4\n Word.cheese: 9.009010000000001E-4\n Word.dog: 9.009010000000001E-4\n Word.doll: 9.009010000000001E-4\n Word.dough: 9.009010000000001E-4\n Word.ernie: 9.009010000000001E-4\n Word.firetruck: 9.009010000000001E-4\n Word.flashlight: 9.009010000000001E-4\n Word.hotdog: 9.009010000000001E-4\n Word.mickey: 9.009010000000001E-4\n Word.oscar: 9.009010000000001E-4\n Word.pepperoni: 9.009010000000001E-4\n Word.phone: 9.009010000000001E-4\n Word.pig: 9.009010000000001E-4\n Word.popnpals: 9.009010000000001E-4\n Word.rabbit: 9.009010000000001E-4\n Word.rings: 9.009010000000001E-4\n Word.train: 9.009010000000001E-4\n Word.truck: 9.009010000000001E-4\n Word.waffles: 9.009010000000001E-4\ncell 37-38\n PSEUDO.DOG: 1.0\ncell 38-39\n PSEUDOKID.EYES: 1.0\ncell 39-40\n PSEUDOMOM.HANDS: 1.0\ncell 40-41\n Socials.NotTopical.mom.point: 0.5\n PSEUDO#: 1.0\n Socials.Topical.mom.point: 0.5\ncell 41-42\n Topic.None: 0.5\ncell 42-43\n Word.None: 9.009010000000001E-4\n Word.alphabet: 9.009010000000001E-4\n Word.baby: 9.009010000000001E-4\n Word.ball: 9.009010000000001E-4\n Word.bear: 9.009010000000001E-4\n Word.block: 9.009010000000001E-4\n Word.blocks: 9.009010000000001E-4\n Word.book: 9.009010000000001E-4\n Word.box: 9.009010000000001E-4\n Word.brush: 9.009010000000001E-4\n Word.bus: 9.009010000000001E-4\n Word.car: 9.009010000000001E-4\n Word.cheese: 9.009010000000001E-4\n Word.dog: 9.009010000000001E-4\n Word.doll: 9.009010000000001E-4\n Word.dough: 9.009010000000001E-4\n Word.ernie: 9.009010000000001E-4\n Word.firetruck: 9.009010000000001E-4\n Word.flashlight: 9.009010000000001E-4\n Word.hotdog: 9.009010000000001E-4\n Word.mickey: 9.009010000000001E-4\n Word.oscar: 9.009010000000001E-4\n Word.pepperoni: 9.009010000000001E-4\n Word.phone: 9.009010000000001E-4\n Word.pig: 9.009010000000001E-4\n Word.popnpals: 9.009010000000001E-4\n Word.rabbit: 9.009010000000001E-4\n Word.rings: 9.009010000000001E-4\n Word.train: 9.009010000000001E-4\n Word.truck: 9.009010000000001E-4\n Word.waffles: 9.009010000000001E-4\ncell 43-44\n Word.None: 9.009010000000001E-4\n Word.alphabet: 9.009010000000001E-4\n Word.baby: 9.009010000000001E-4\n Word.ball: 9.009010000000001E-4\n Word.bear: 9.009010000000001E-4\n Word.block: 9.009010000000001E-4\n Word.blocks: 9.009010000000001E-4\n Word.book: 9.009010000000001E-4\n Word.box: 9.009010000000001E-4\n Word.brush: 9.009010000000001E-4\n Word.bus: 9.009010000000001E-4\n Word.car: 9.009010000000001E-4\n Word.cheese: 9.009010000000001E-4\n Word.dog: 9.009010000000001E-4\n Word.doll: 9.009010000000001E-4\n Word.dough: 9.009010000000001E-4\n Word.ernie: 9.009010000000001E-4\n Word.firetruck: 9.009010000000001E-4\n Word.flashlight: 9.009010000000001E-4\n Word.hotdog: 9.009010000000001E-4\n Word.mickey: 9.009010000000001E-4\n Word.oscar: 9.009010000000001E-4\n Word.pepperoni: 9.009010000000001E-4\n Word.phone: 9.009010000000001E-4\n Word.pig: 9.009010000000001E-4\n Word.popnpals: 9.009010000000001E-4\n Word.rabbit: 9.009010000000001E-4\n Word.rings: 9.009010000000001E-4\n Word.train: 9.009010000000001E-4\n Word.truck: 9.009010000000001E-4\n Word.waffles: 9.009010000000001E-4\ncell 44-45\n Topic.None: 0.5\ncell 45-46\n Word.None: 9.009010000000001E-4\n Word.alphabet: 9.009010000000001E-4\n Word.baby: 9.009010000000001E-4\n Word.ball: 9.009010000000001E-4\n Word.bear: 9.009010000000001E-4\n Word.block: 9.009010000000001E-4\n Word.blocks: 9.009010000000001E-4\n Word.book: 9.009010000000001E-4\n Word.box: 9.009010000000001E-4\n Word.brush: 9.009010000000001E-4\n Word.bus: 9.009010000000001E-4\n Word.car: 9.009010000000001E-4\n Word.cheese: 9.009010000000001E-4\n Word.dog: 9.009010000000001E-4\n Word.doll: 9.009010000000001E-4\n Word.dough: 9.009010000000001E-4\n Word.ernie: 9.009010000000001E-4\n Word.firetruck: 9.009010000000001E-4\n Word.flashlight: 9.009010000000001E-4\n Word.hotdog: 9.009010000000001E-4\n Word.mickey: 9.009010000000001E-4\n Word.oscar: 9.009010000000001E-4\n Word.pepperoni: 9.009010000000001E-4\n Word.phone: 9.009010000000001E-4\n Word.pig: 9.009010000000001E-4\n Word.popnpals: 9.009010000000001E-4\n Word.rabbit: 9.009010000000001E-4\n Word.rings: 9.009010000000001E-4\n Word.train: 9.009010000000001E-4\n Word.truck: 9.009010000000001E-4\n Word.waffles: 9.009010000000001E-4\ncell 46-47\n Word.None: 9.009010000000001E-4\n Word.alphabet: 9.009010000000001E-4\n Word.baby: 9.009010000000001E-4\n Word.ball: 9.009010000000001E-4\n Word.bear: 9.009010000000001E-4\n Word.block: 9.009010000000001E-4\n Word.blocks: 9.009010000000001E-4\n Word.book: 9.009010000000001E-4\n Word.box: 9.009010000000001E-4\n Word.brush: 9.009010000000001E-4\n Word.bus: 9.009010000000001E-4\n Word.car: 9.009010000000001E-4\n Word.cheese: 9.009010000000001E-4\n Word.dog: 9.009010000000001E-4\n Word.doll: 9.009010000000001E-4\n Word.dough: 9.009010000000001E-4\n Word.ernie: 9.009010000000001E-4\n Word.firetruck: 9.009010000000001E-4\n Word.flashlight: 9.009010000000001E-4\n Word.hotdog: 9.009010000000001E-4\n Word.mickey: 9.009010000000001E-4\n Word.oscar: 9.009010000000001E-4\n Word.pepperoni: 9.009010000000001E-4\n Word.phone: 9.009010000000001E-4\n Word.pig: 9.009010000000001E-4\n Word.popnpals: 9.009010000000001E-4\n Word.rabbit: 9.009010000000001E-4\n Word.rings: 9.009010000000001E-4\n Word.train: 9.009010000000001E-4\n Word.truck: 9.009010000000001E-4\n Word.waffles: 9.009010000000001E-4\ncell 47-48\n Word.None: 9.009010000000001E-4\n Word.alphabet: 9.009010000000001E-4\n Word.baby: 9.009010000000001E-4\n Word.ball: 9.009010000000001E-4\n Word.bear: 9.009010000000001E-4\n Word.block: 9.009010000000001E-4\n Word.blocks: 9.009010000000001E-4\n Word.book: 9.009010000000001E-4\n Word.box: 9.009010000000001E-4\n Word.brush: 9.009010000000001E-4\n Word.bus: 9.009010000000001E-4\n Word.car: 9.009010000000001E-4\n Word.cheese: 9.009010000000001E-4\n Word.dog: 9.009010000000001E-4\n Word.doll: 9.009010000000001E-4\n Word.dough: 9.009010000000001E-4\n Word.ernie: 9.009010000000001E-4\n Word.firetruck: 9.009010000000001E-4\n Word.flashlight: 9.009010000000001E-4\n Word.hotdog: 9.009010000000001E-4\n Word.mickey: 9.009010000000001E-4\n Word.oscar: 9.009010000000001E-4\n Word.pepperoni: 9.009010000000001E-4\n Word.phone: 9.009010000000001E-4\n Word.pig: 9.009010000000001E-4\n Word.popnpals: 9.009010000000001E-4\n Word.rabbit: 9.009010000000001E-4\n Word.rings: 9.009010000000001E-4\n Word.train: 9.009010000000001E-4\n Word.truck: 9.009010000000001E-4\n Word.waffles: 9.009010000000001E-4\ncell 48-49\n Word.None: 9.009010000000001E-4\n Word.alphabet: 9.009010000000001E-4\n Word.baby: 9.009010000000001E-4\n Word.ball: 9.009010000000001E-4\n Word.bear: 9.009010000000001E-4\n Word.block: 9.009010000000001E-4\n Word.blocks: 9.009010000000001E-4\n Word.book: 9.009010000000001E-4\n Word.box: 9.009010000000001E-4\n Word.brush: 9.009010000000001E-4\n Word.bus: 9.009010000000001E-4\n Word.car: 9.009010000000001E-4\n Word.cheese: 9.009010000000001E-4\n Word.dog: 9.009010000000001E-4\n Word.doll: 9.009010000000001E-4\n Word.dough: 9.009010000000001E-4\n Word.ernie: 9.009010000000001E-4\n Word.firetruck: 9.009010000000001E-4\n Word.flashlight: 9.009010000000001E-4\n Word.hotdog: 9.009010000000001E-4\n Word.mickey: 9.009010000000001E-4\n Word.oscar: 9.009010000000001E-4\n Word.pepperoni: 9.009010000000001E-4\n Word.phone: 9.009010000000001E-4\n Word.pig: 9.009010000000001E-4\n Word.popnpals: 9.009010000000001E-4\n Word.rabbit: 9.009010000000001E-4\n Word.rings: 9.009010000000001E-4\n Word.train: 9.009010000000001E-4\n Word.truck: 9.009010000000001E-4\n Word.waffles: 9.009010000000001E-4\ncell 49-50\n Word.None: 9.009010000000001E-4\n Word.alphabet: 9.009010000000001E-4\n Word.baby: 9.009010000000001E-4\n Word.ball: 9.009010000000001E-4\n Word.bear: 9.009010000000001E-4\n Word.block: 9.009010000000001E-4\n Word.blocks: 9.009010000000001E-4\n Word.book: 9.009010000000001E-4\n Word.box: 9.009010000000001E-4\n Word.brush: 9.009010000000001E-4\n Word.bus: 9.009010000000001E-4\n Word.car: 9.009010000000001E-4\n Word.cheese: 9.009010000000001E-4\n Word.dog: 9.009010000000001E-4\n Word.doll: 9.009010000000001E-4\n Word.dough: 9.009010000000001E-4\n Word.ernie: 9.009010000000001E-4\n Word.firetruck: 9.009010000000001E-4\n Word.flashlight: 9.009010000000001E-4\n Word.hotdog: 9.009010000000001E-4\n Word.mickey: 9.009010000000001E-4\n Word.oscar: 9.009010000000001E-4\n Word.pepperoni: 9.009010000000001E-4\n Word.phone: 9.009010000000001E-4\n Word.pig: 9.009010000000001E-4\n Word.popnpals: 9.009010000000001E-4\n Word.rabbit: 9.009010000000001E-4\n Word.rings: 9.009010000000001E-4\n Word.train: 9.009010000000001E-4\n Word.truck: 9.009010000000001E-4\n Word.waffles: 9.009010000000001E-4\ncell 50-51\n Word.None: 9.009010000000001E-4\n Word.alphabet: 9.009010000000001E-4\n Word.baby: 9.009010000000001E-4\n Word.ball: 9.009010000000001E-4\n Word.bear: 9.009010000000001E-4\n Word.block: 9.009010000000001E-4\n Word.blocks: 9.009010000000001E-4\n Word.book: 9.009010000000001E-4\n Word.box: 9.009010000000001E-4\n Word.brush: 9.009010000000001E-4\n Word.bus: 9.009010000000001E-4\n Word.car: 9.009010000000001E-4\n Word.cheese: 9.009010000000001E-4\n Word.dog: 9.009010000000001E-4\n Word.doll: 9.009010000000001E-4\n Word.dough: 9.009010000000001E-4\n Word.ernie: 9.009010000000001E-4\n Word.firetruck: 9.009010000000001E-4\n Word.flashlight: 9.009010000000001E-4\n Word.hotdog: 9.009010000000001E-4\n Word.mickey: 9.009010000000001E-4\n Word.oscar: 9.009010000000001E-4\n Word.pepperoni: 9.009010000000001E-4\n Word.phone: 9.009010000000001E-4\n Word.pig: 9.009010000000001E-4\n Word.popnpals: 9.009010000000001E-4\n Word.rabbit: 9.009010000000001E-4\n Word.rings: 9.009010000000001E-4\n Word.train: 9.009010000000001E-4\n Word.truck: 9.009010000000001E-4\n Word.waffles: 9.009010000000001E-4\ncell 2-4\n Socials.NotTopical.mom.eyes: 0.12500000000000003\n Socials.Topical.mom.eyes: 0.12500000000000003\ncell 5-7\n Socials.NotTopical.kid.hands: 0.0625\n Socials.Topical.kid.hands: 0.0625\ncell 8-10\n Words.dog: 2.0290565295024999E-7\n Words.pig: 2.0290565295024999E-7\n Words.None: 2.0290565295024999E-7\ncell 9-11\n Words.dog: 2.0290565295024999E-7\n Words.pig: 2.0290565295024999E-7\n Words.None: 2.0290565295024999E-7\ncell 10-12\n Words.dog: 2.0290565295024999E-7\n Words.pig: 2.0290565295024999E-7\n Words.None: 2.0290565295024999E-7\ncell 11-13\n Words.dog: 2.0290565295024999E-7\n Words.pig: 2.0290565295024999E-7\n Words.None: 2.0290565295024999E-7\ncell 12-14\n Words.dog: 2.0290565295024999E-7\n Words.pig: 2.0290565295024999E-7\n Words.None: 2.0290565295024999E-7\ncell 13-15\n Words.dog: 2.0290565295024999E-7\n Words.pig: 2.0290565295024999E-7\n Words.None: 2.0290565295024999E-7\ncell 14-16\n Words.dog: 2.0290565295024999E-7\n Words.pig: 2.0290565295024999E-7\n Words.None: 2.0290565295024999E-7\ncell 19-21\n Socials.NotTopical.mom.hands: 0.25\n Socials.Topical.mom.hands: 0.25\ncell 22-24\n Socials.NotTopical.kid.hands: 0.0625\n Socials.Topical.kid.hands: 0.0625\ncell 25-27\n Words.dog: 2.0290565295024999E-7\n Words.pig: 2.0290565295024999E-7\n Words.None: 2.0290565295024999E-7\ncell 26-28\n Words.dog: 2.0290565295024999E-7\n Words.pig: 2.0290565295024999E-7\n Words.None: 2.0290565295024999E-7\ncell 29-31\n Socials.NotTopical.mom.hands: 0.25\n Socials.Topical.mom.hands: 0.25\ncell 32-34\n Words.dog: 2.0290565295024999E-7\n Words.None: 2.0290565295024999E-7\ncell 33-35\n Words.dog: 2.0290565295024999E-7\n Words.None: 2.0290565295024999E-7\ncell 34-36\n Words.dog: 2.0290565295024999E-7\n Words.None: 2.0290565295024999E-7\ncell 35-37\n Words.dog: 2.0290565295024999E-7\n Words.None: 2.0290565295024999E-7\ncell 39-41\n Socials.NotTopical.mom.hands: 0.25\n Socials.Topical.mom.hands: 0.25\ncell 42-44\n Words.dog: 2.0290565295024999E-7\n Words.None: 2.0290565295024999E-7\ncell 44-46\n Sentence.None: 2.2522525000000018E-4\ncell 45-47\n Words.None: 2.0290565295024999E-7\ncell 46-48\n Words.None: 2.0290565295024999E-7\ncell 47-49\n Words.None: 2.0290565295024999E-7\ncell 48-50\n Words.None: 2.0290565295024999E-7\ncell 49-51\n Words.None: 2.0290565295024999E-7\ncell 1-4\n Socials.NotTopical.kid.eyes: 0.03125\n Socials.Topical.kid.eyes: 0.03125\ncell 4-7\n T.None: 0.0010416656250000008\n T.pig: 0.03125\ncell 8-11\n Words.dog: 9.139895282426633E-11\n Words.pig: 9.139895282426633E-11\n Words.None: 9.139895282426667E-11\ncell 9-12\n Words.dog: 9.139895282426633E-11\n Words.pig: 9.139895282426633E-11\n Words.None: 9.139895282426667E-11\ncell 10-13\n Words.dog: 9.139895282426633E-11\n Words.pig: 9.139895282426633E-11\n Words.None: 9.139895282426667E-11\ncell 11-14\n Words.dog: 9.139895282426633E-11\n Words.pig: 9.139895282426633E-11\n Words.None: 9.139895282426667E-11\ncell 12-15\n Words.dog: 9.139895282426633E-11\n Words.pig: 9.139895282426633E-11\n Words.None: 9.139895282426667E-11\ncell 13-16\n Words.dog: 9.139895282426633E-11\n Words.pig: 9.139895282426633E-11\n Words.None: 9.139895282426667E-11\ncell 18-21\n Socials.NotTopical.mom.eyes: 0.12500000000000003\n Socials.Topical.mom.eyes: 0.12500000000000003\ncell 21-24\n T.None: 0.0010416656250000008\n T.pig: 0.03125\ncell 25-28\n Words.dog: 9.139895282426633E-11\n Words.pig: 9.139895282426633E-11\n Words.None: 9.139895282426667E-11\ncell 28-31\n T.None: 0.0010416656250000008\n T.dog: 0.031250000000000014\ncell 32-35\n Words.dog: 9.139895282426633E-11\n Words.None: 9.139895282426667E-11\ncell 33-36\n Words.dog: 9.139895282426633E-11\n Words.None: 9.139895282426667E-11\ncell 34-37\n Words.dog: 9.139895282426633E-11\n Words.None: 9.139895282426667E-11\ncell 38-41\n Socials.NotTopical.kid.eyes: 0.03125\n Socials.Topical.kid.eyes: 0.03125\ncell 44-47\n Sentence.None: 1.0145282647512498E-7\ncell 45-48\n Words.None: 9.139895282426667E-11\ncell 46-49\n Words.None: 9.139895282426667E-11\ncell 47-50\n Words.None: 9.139895282426667E-11\ncell 48-51\n Words.None: 9.139895282426667E-11\ncell 0-4\n T.None: 0.0010416656250000008\n T.dog: 0.03125\ncell 4-8\n Topic.pig: 0.007812500000000002\n Topic.None: 2.6041640625000013E-4\ncell 8-12\n Words.dog: 4.117070399916714E-14\n Words.pig: 4.117070399916714E-14\n Words.None: 4.1170703999167293E-14\ncell 9-13\n Words.dog: 4.117070399916714E-14\n Words.pig: 4.117070399916714E-14\n Words.None: 4.1170703999167293E-14\ncell 10-14\n Words.dog: 4.117070399916714E-14\n Words.pig: 4.117070399916714E-14\n Words.None: 4.1170703999167293E-14\ncell 11-15\n Words.dog: 4.117070399916714E-14\n Words.pig: 4.117070399916714E-14\n Words.None: 4.1170703999167293E-14\ncell 12-16\n Words.dog: 4.117070399916714E-14\n Words.pig: 4.117070399916714E-14\n Words.None: 4.1170703999167293E-14\ncell 17-21\n Socials.NotTopical.kid.eyes: 0.03125\n Socials.Topical.kid.eyes: 0.03125\ncell 21-25\n Topic.pig: 0.007812500000000002\n Topic.None: 2.6041640625000013E-4\ncell 28-32\n Topic.dog: 0.007812500000000002\n Topic.None: 2.6041640625000013E-4\ncell 32-36\n Words.dog: 4.117070399916714E-14\n Words.None: 4.1170703999167293E-14\ncell 33-37\n Words.dog: 4.117070399916714E-14\n Words.None: 4.1170703999167293E-14\ncell 37-41\n T.None: 0.0010416656250000008\n T.dog: 0.03125\ncell 44-48\n Sentence.None: 4.569947641213341E-11\ncell 45-49\n Words.None: 4.1170703999167293E-14\ncell 46-50\n Words.None: 4.1170703999167293E-14\ncell 47-51\n Words.None: 4.1170703999167293E-14\ncell 8-13\n Words.dog: 1.854536420177689E-17\n Words.pig: 1.854536420177689E-17\n Words.None: 1.854536420177689E-17\ncell 9-14\n Words.dog: 1.854536420177689E-17\n Words.pig: 1.854536420177689E-17\n Words.None: 1.854536420177689E-17\ncell 10-15\n Words.dog: 1.854536420177689E-17\n Words.pig: 1.854536420177689E-17\n Words.None: 1.854536420177689E-17\ncell 11-16\n Words.dog: 1.854536420177689E-17\n Words.pig: 1.854536420177689E-17\n Words.None: 1.854536420177689E-17\ncell 16-21\n T.None: 0.0010416656250000008\n T.dog: 0.03125\ncell 28-33\n Sentence.dog: 3.519144531250007E-6\n Sentence.None: 1.1730470040351556E-7\ncell 32-37\n Words.dog: 1.854536420177689E-17\n Words.None: 1.854536420177689E-17\ncell 37-42\n Topic.dog: 0.007812500000000002\n Topic.None: 2.6041640625000013E-4\ncell 44-49\n Sentence.None: 2.058535199958368E-14\ncell 45-50\n Words.None: 1.854536420177689E-17\ncell 46-51\n Words.None: 1.854536420177689E-17\ncell 8-14\n Words.dog: 8.353768577372522E-21\n Words.pig: 8.353768577372522E-21\n Words.None: 8.353768577372522E-21\ncell 9-15\n Words.dog: 8.353768577372522E-21\n Words.pig: 8.353768577372522E-21\n Words.None: 8.353768577372522E-21\ncell 10-16\n Words.dog: 8.353768577372522E-21\n Words.pig: 8.353768577372522E-21\n Words.None: 8.353768577372522E-21\ncell 28-34\n Sentence.dog: 1.585200413673827E-9\n Sentence.None: 5.283996094911393E-11\ncell 37-43\n Sentence.dog: 3.519144531250007E-6\n Sentence.None: 1.1730470040351556E-7\ncell 44-50\n Sentence.None: 9.272682100888427E-18\ncell 45-51\n Words.None: 8.353768577372522E-21\ncell 8-15\n Words.dog: 3.762959232561752E-24\n Words.pig: 3.762959232561752E-24\n Words.None: 3.762959232561752E-24\ncell 9-16\n Words.dog: 3.762959232561752E-24\n Words.pig: 3.762959232561752E-24\n Words.None: 3.762959232561752E-24\ncell 28-35\n Sentence.dog: 7.140543189395816E-13\n Sentence.None: 2.3801786829508822E-14\ncell 37-44\n Sentence.dog: 1.585200413673827E-9\n Sentence.None: 5.283996094911393E-11\ncell 44-51\n Sentence.None: 4.176884288686254E-21\ncell 0-8\n Topic.dog: 4.0690063476562495E-6\n Topic.pig: 4.0690063476562495E-6\n Topic.None: 1.356334092883302E-7\ncell 8-16\n Words.dog: 1.6950268677870618E-27\n Words.pig: 1.6950268677870618E-27\n Words.None: 1.6950268677870618E-27\ncell 28-36\n Sentence.dog: 3.216461249934937E-16\n Sentence.None: 1.0721526778245655E-17\ncell 0-9\n : 3.756927544551728E-12\n Sentence.dog: 1.8328859438049299E-9\n Sentence.pig: 1.8328859438049299E-9\n Sentence.None: 6.109613703063304E-11\ncell 16-25\n Topic.dog: 4.0690063476562495E-6\n Topic.pig: 4.0690063476562495E-6\n Topic.None: 1.356334092883302E-7\ncell 28-37\n Sentence.dog: 1.4488565782638213E-19\n Sentence.None: 4.829517098024157E-21\ncell 37-46\n Discourse.dog: 3.486593354197191E-16\n Discourse.None: 1.1621966225346156E-17\ncell 0-10\n : 1.6923098909071026E-15\n Sentence.dog: 8.256243898299018E-13\n Sentence.pig: 8.256243898299018E-13\n Sentence.None: 2.7520785473517144E-14\ncell 16-26\n Sentence.dog: 1.8328859438049299E-9\n Sentence.pig: 1.8328859438049299E-9\n Sentence.None: 6.109613703063304E-11\ncell 37-47\n Discourse.dog: 1.570537719694806E-19\n Discourse.None: 5.2351204971903024E-21\ncell 0-11\n : 7.623018365140518E-19\n Sentence.dog: 3.7190291921107515E-16\n Sentence.pig: 3.7190291921107515E-16\n Sentence.None: 1.2396751576938567E-17\ncell 16-27\n Sentence.dog: 8.256243898299018E-13\n Sentence.pig: 8.256243898299018E-13\n Sentence.None: 2.7520785473517144E-14\ncell 37-48\n Discourse.dog: 7.07449501105387E-23\n Discourse.None: 2.3581626455196265E-24\ncell 0-12\n : 3.433792434086738E-22\n Sentence.dog: 1.6752385591008886E-19\n Sentence.pig: 1.6752385591008886E-19\n Sentence.None: 5.584122946207741E-21\ncell 16-28\n Sentence.dog: 3.7190291921107515E-16\n Sentence.pig: 3.7190291921107515E-16\n Sentence.None: 1.2396751576938567E-17\ncell 37-49\n Discourse.dog: 3.1867098149766847E-26\n Discourse.None: 1.0622355427556263E-27\ncell 0-13\n : 1.5467535188305812E-25\n Sentence.dog: 7.546120465662714E-23\n Sentence.pig: 7.546120465662714E-23\n Sentence.None: 2.5153709731807565E-24\ncell 37-50\n Discourse.dog: 1.435455029511159E-29\n Discourse.None: 4.784845313520445E-31\ncell 0-14\n : 6.967358959339916E-29\n Sentence.dog: 3.399153736818011E-26\n Sentence.pig: 3.399153736818011E-26\n Sentence.None: 1.1330501125547613E-27\ncell 37-51\n Discourse.dog: 6.46601435770818E-33\n Discourse.None: 2.1553359638979466E-34\ncell 0-15\n : 3.138450326914198E-32\n Sentence.dog: 1.5311505003265566E-29\n Sentence.pig: 1.5311505003265566E-29\n Sentence.None: 5.103829897253463E-31\ncell 28-43\n Discourse.dog: 5.145208389456655E-28\n Discourse.None: 1.715067748082748E-29\ncell 0-16\n : 1.4137165189836475E-35\n Sentence.dog: 6.897075084473396E-33\n Sentence.pig: 6.897075084473396E-33\n Sentence.None: 2.299022729132777E-34\ncell 28-44\n Discourse.dog: 2.3176616916349347E-31\n Discourse.None: 7.725531246577499E-33\ncell 16-33\n Discourse.dog: 1.3207090672019667E-24\n Discourse.pig: 1.3207090672019667E-24\n Discourse.None: 4.402359154976344E-26\ncell 16-34\n Discourse.dog: 5.949140596756611E-28\n Discourse.pig: 5.949140596756611E-28\n Discourse.None: 1.983044882538663E-29\ncell 28-46\n Discourse.dog: 1.631237293480927E-36\n Discourse.None: 5.437452207478794E-38\ncell 16-35\n Discourse.dog: 2.679793356379264E-31\n Discourse.pig: 2.679793356379264E-31\n Discourse.None: 8.932635588619716E-33\ncell 28-47\n Discourse.dog: 7.347916544671426E-40\n Discourse.None: 2.449303065584968E-41\ncell 16-36\n Discourse.dog: 1.2071142572777207E-34\n Discourse.pig: 1.2071142572777207E-34\n Discourse.None: 4.023710167211556E-36\ncell 28-48\n Discourse.dog: 3.309872681505478E-43\n Discourse.None: 1.1032897905442687E-44\ncell 16-37\n Discourse.dog: 5.437452207478794E-38\n Discourse.pig: 5.437452207478794E-38\n Discourse.None: 1.8124822566755338E-39\ncell 28-49\n Discourse.dog: 1.4909338043204661E-46\n Discourse.None: 4.9697743779555534E-48\ncell 28-50\n Discourse.dog: 6.715918776230674E-50\n Discourse.None: 2.238637353437306E-51\ncell 28-51\n Discourse.dog: 3.0251889707124605E-53\n Discourse.None: 1.0083953151744993E-54\ncell 0-26\n : 1.646479653283196E-45\n Discourse.dog: 2.5102039644403017E-44\n Discourse.pig: 2.5102039644403017E-44\n Discourse.None: 8.367338180787814E-46\ncell 0-27\n : 7.416575830612444E-49\n Discourse.dog: 1.130722630884119E-47\n Discourse.pig: 1.130722630884119E-47\n Discourse.None: 3.769071667204971E-49\ncell 16-43\n Discourse.dog: 6.179068406887105E-45\n Discourse.pig: 6.179068406887105E-45\n Discourse.None: 2.0596874092729052E-46\ncell 0-28\n : 3.340800291187299E-52\n Discourse.dog: 5.093345744430682E-51\n Discourse.pig: 5.093345744430682E-51\n Discourse.None: 1.6977802170283172E-52\ncell 16-44\n Discourse.dog: 2.783364453416547E-48\n Discourse.pig: 2.783364453416547E-48\n Discourse.None: 9.277872233507005E-50\ncell 16-46\n Discourse.dog: 1.9590123589432526E-53\n Discourse.pig: 1.9590123589432526E-53\n Discourse.None: 6.530034666436332E-55\ncell 16-47\n Discourse.dog: 8.824380965921825E-57\n Discourse.pig: 8.824380965921825E-57\n Discourse.None: 2.941457380513628E-58\ncell 16-48\n Discourse.dog: 3.974946818289923E-60\n Discourse.pig: 3.974946818289923E-60\n Discourse.None: 1.3249809477810387E-61\ncell 0-33\n : 3.796453328748726E-59\n Discourse.dog: 5.788029130900136E-58\n Discourse.pig: 5.788029130900136E-58\n Discourse.None: 1.9293411142903406E-59\ncell 16-49\n Discourse.dog: 1.7905167817720841E-63\n Discourse.pig: 1.7905167817720841E-63\n Discourse.None: 5.968383304184359E-65\ncell 0-34\n : 1.7101143001615569E-62\n Discourse.dog: 2.607220616028576E-61\n Discourse.pig: 2.607220616028576E-61\n Discourse.None: 8.690726696026557E-63\ncell 16-50\n Discourse.dog: 8.065391796076398E-67\n Discourse.pig: 8.065391796076398E-67\n Discourse.None: 2.688461243561542E-68\ncell 0-35\n : 7.703218415648926E-66\n Discourse.dog: 1.174423830100333E-64\n Discourse.pig: 1.174423830100333E-64\n Discourse.None: 3.9147421855883544E-66\ncell 16-51\n Discourse.dog: 3.6330597672384694E-70\n Discourse.pig: 3.6330597672384694E-70\n Discourse.None: 1.2110187113929042E-71\ncell 0-36\n : 3.469918586938325E-69\n Discourse.dog: 5.2901980148061895E-68\n Discourse.pig: 5.2901980148061895E-68\n Discourse.None: 1.7633975748693967E-69\ncell 0-37\n : 1.5630265624456884E-72\n Discourse.dog: 2.3829723408684958E-71\n Discourse.pig: 2.3829723408684958E-71\n Discourse.None: 7.943233192987206E-73\ncell 0-43\n : 1.7762083568935717E-79\n Discourse.dog: 2.7079868556257725E-78\n Discourse.pig: 2.7079868556257725E-78\n Discourse.None: 9.026613825463081E-80\ncell 0-44\n : 8.000939424669014E-83\n Discourse.dog: 1.2198140331100776E-81\n Discourse.pig: 1.2198140331100776E-81\n Discourse.None: 4.0660427109868266E-83\ncell 0-46\n : 5.631292444237328E-88\n Discourse.dog: 8.58540376752272E-87\n Discourse.pig: 8.58540376752272E-87\n Discourse.None: 2.861798394039659E-88\ncell 0-47\n : 2.536618497152969E-91\n Discourse.dog: 3.8672994197825583E-90\n Discourse.pig: 3.8672994197825583E-90\n Discourse.None: 1.289098517494383E-91\ncell 0-48\n : 1.1426210703517579E-94\n Discourse.dog: 1.7420269572906938E-93\n Discourse.pig: 1.7420269572906938E-93\n Discourse.None: 5.8067507175458044E-95\ncell 0-49\n : 5.146942324504932E-98\n Discourse.dog: 7.846969139250848E-97\n Discourse.pig: 7.846969139250848E-97\n Discourse.None: 2.6156537640939105E-98\ncell 0-50\n : 2.3184427435444478E-101\n Discourse.dog: 3.5346711722601737E-100\n Discourse.pig: 3.5346711722601737E-100\n Discourse.None: 1.1782225458630039E-101\ncell 0-51\n : 1.0443436930509266E-104\n Discourse.dog: 1.5921943968801178E-103\n Discourse.pig: 1.5921943968801178E-103\n Discourse.None: 5.307309348952418E-105\n");
    if(insideOutsideOpt>0){
      assertEquals(parser.dumpOutsideChart(), "# Outside chart snapshot\ncell 0-1\n PSEUDO.DOG: 1.0443436930508969E-104\ncell 1-2\n PSEUDOKID.EYES: 1.0443436930508969E-104\ncell 2-3\n PSEUDOMOM.EYES: 1.0443436930508969E-104\ncell 3-4\n Socials.NotTopical.mom.point: 1.0614640646218358E-104\n Socials.Topical.mom.point: 1.0272233214799547E-104\ncell 4-5\n PSEUDO.PIG: 1.0443436930508969E-104\ncell 5-6\n PSEUDOKID.HANDS: 1.0443436930508969E-104\ncell 6-7\n Socials.NotTopical.mom.point: 1.0614640646218358E-104\n Socials.Topical.mom.point: 1.0272233214799547E-104\ncell 7-8\n Topic.None: 2.0886873861017975E-104\ncell 8-9\n Word.None: 5.891124910627662E-102\n Word.dog: 2.850544403547088E-102\n Word.pig: 2.850544403547088E-102\ncell 9-10\n Word.None: 5.891124910627662E-102\n Word.dog: 2.850544403547088E-102\n Word.pig: 2.850544403547088E-102\ncell 10-11\n Word.None: 5.89112491062783E-102\n Word.dog: 2.850544403547169E-102\n Word.pig: 2.850544403547169E-102\ncell 11-12\n Word.None: 5.891124910627997E-102\n Word.dog: 2.8505444035472503E-102\n Word.pig: 2.8505444035472503E-102\ncell 12-13\n Word.None: 5.891124910627997E-102\n Word.dog: 2.8505444035472503E-102\n Word.pig: 2.8505444035472503E-102\ncell 13-14\n Word.None: 5.8911249106281646E-102\n Word.dog: 2.850544403547331E-102\n Word.pig: 2.850544403547331E-102\ncell 14-15\n Word.None: 5.8911249106281646E-102\n Word.dog: 2.850544403547331E-102\n Word.pig: 2.850544403547331E-102\ncell 15-16\n Word.None: 5.8911249106281646E-102\n Word.dog: 2.850544403547412E-102\n Word.pig: 2.850544403547412E-102\ncell 16-17\n PSEUDO.DOG: 1.0443436930508969E-104\ncell 17-18\n PSEUDOKID.EYES: 1.0443436930508969E-104\ncell 18-19\n PSEUDOMOM.EYES: 1.0443436930508969E-104\ncell 19-20\n PSEUDOMOM.HANDS: 1.0443436930508969E-104\ncell 20-21\n Socials.NotTopical.mom.point: 1.0614640646218358E-104\n Socials.Topical.mom.point: 1.0272233214799547E-104\ncell 21-22\n PSEUDO.PIG: 1.0443436930508969E-104\ncell 22-23\n PSEUDOKID.HANDS: 1.0443436930508969E-104\ncell 23-24\n Socials.NotTopical.mom.point: 1.0614640646218358E-104\n Socials.Topical.mom.point: 1.0272233214799547E-104\ncell 24-25\n Topic.None: 2.0886873861017975E-104\ncell 25-26\n Word.None: 5.891124910627662E-102\n Word.dog: 2.850544403547088E-102\n Word.pig: 2.850544403547088E-102\ncell 26-27\n Word.None: 5.891124910627495E-102\n Word.dog: 2.850544403547007E-102\n Word.pig: 2.850544403547007E-102\ncell 27-28\n Word.None: 5.891124910627662E-102\n Word.dog: 2.850544403547088E-102\n Word.pig: 2.850544403547088E-102\ncell 28-29\n PSEUDO.DOG: 1.0443436930508969E-104\ncell 29-30\n PSEUDOMOM.HANDS: 1.0443436930508969E-104\ncell 30-31\n Socials.NotTopical.mom.point: 6.737694725133478E-106\n Socials.Topical.mom.point: 2.0213104388504764E-104\ncell 31-32\n Topic.None: 2.0886873861017975E-104\ncell 32-33\n Word.None: 5.983077866916461E-102\n Word.dog: 5.609135850805162E-102\ncell 33-34\n Word.None: 5.983077866916291E-102\n Word.dog: 5.6091358508050024E-102\ncell 34-35\n Word.None: 5.983077866916461E-102\n Word.dog: 5.609135850805162E-102\ncell 35-36\n Word.None: 5.983077866916461E-102\n Word.dog: 5.609135850805162E-102\ncell 36-37\n Word.None: 5.983077866916631E-102\n Word.dog: 5.609135850805321E-102\ncell 37-38\n PSEUDO.DOG: 1.0443436930509266E-104\ncell 38-39\n PSEUDOKID.EYES: 1.0443436930509266E-104\ncell 39-40\n PSEUDOMOM.HANDS: 1.0443436930509266E-104\ncell 40-41\n Socials.NotTopical.mom.point: 6.737694725133669E-106\n Socials.Topical.mom.point: 2.0213104388505337E-104\ncell 41-42\n Topic.None: 2.088687386101857E-104\ncell 42-43\n Word.None: 5.983077866916461E-102\n Word.dog: 5.609135850805162E-102\ncell 43-44\n Word.None: 5.983077866916631E-102\n Word.dog: 5.609135850805321E-102\ncell 44-45\n Topic.None: 2.088687386101857E-104\ncell 45-46\n Word.None: 1.1592213717721889E-101\ncell 46-47\n Word.None: 1.1592213717721889E-101\ncell 47-48\n Word.None: 1.1592213717721559E-101\ncell 48-49\n Word.None: 1.1592213717721559E-101\ncell 49-50\n Word.None: 1.159221371772123E-101\ncell 50-51\n Word.None: 1.159221371772123E-101\ncell 2-4\n Socials.NotTopical.mom.eyes: 4.245856258487359E-104\n Socials.Topical.mom.eyes: 4.1088932859198344E-104\ncell 5-7\n Socials.NotTopical.kid.hands: 8.491712516974734E-104\n Socials.Topical.kid.hands: 8.217786571839684E-104\ncell 14-16\n Words.dog: 2.531283151908922E-98\n Words.pig: 2.531283151908922E-98\n Words.None: 8.437602068751152E-100\ncell 19-21\n Socials.NotTopical.mom.hands: 2.1229281292436757E-104\n Socials.Topical.mom.hands: 2.0544466429599132E-104\ncell 22-24\n Socials.NotTopical.kid.hands: 8.491712516974734E-104\n Socials.Topical.kid.hands: 8.217786571839684E-104\ncell 26-28\n Words.dog: 2.5312831519086342E-98\n Words.pig: 2.5312831519086342E-98\n Words.None: 8.437602068751632E-100\ncell 29-31\n Socials.NotTopical.mom.hands: 1.3475389450266979E-105\n Socials.Topical.mom.hands: 4.04262087770096E-104\ncell 35-37\n Words.dog: 4.98091208761473E-98\n Words.None: 1.6603023689007912E-99\ncell 39-41\n Socials.NotTopical.mom.hands: 1.3475389450267362E-105\n Socials.Topical.mom.hands: 4.0426208777010755E-104\ncell 42-44\n Words.dog: 4.98091208761473E-98\n Words.None: 1.6603023689008856E-99\ncell 49-51\n Words.None: 5.1469423245046394E-98\ncell 1-4\n Socials.NotTopical.kid.eyes: 1.69834250339495E-103\n Socials.Topical.kid.eyes: 1.64355731436794E-103\ncell 4-7\n T.None: 5.0950326052174406E-102\n T.pig: 1.64355731436794E-103\ncell 13-16\n Words.dog: 5.619447979098435E-95\n Words.pig: 5.619447979098435E-95\n Words.None: 1.8731474532165577E-96\ncell 18-21\n Socials.NotTopical.mom.eyes: 4.245856258487359E-104\n Socials.Topical.mom.eyes: 4.1088932859198344E-104\ncell 21-24\n T.None: 5.0950326052174406E-102\n T.pig: 1.64355731436794E-103\ncell 25-28\n Words.dog: 5.619447979097955E-95\n Words.pig: 5.619447979097955E-95\n Words.None: 1.873147453216664E-96\ncell 28-31\n T.None: 3.234096702160786E-103\n T.dog: 3.234096702160786E-103\ncell 34-37\n Words.dog: 1.1057623618165917E-94\n Words.None: 3.6858708535140053E-96\ncell 38-41\n Socials.NotTopical.kid.eyes: 1.078031156021395E-104\n Socials.Topical.kid.eyes: 3.234096702160878E-103\ncell 48-51\n Words.None: 1.1426210703517255E-94\ncell 0-4\n T.None: 5.0950326052174406E-102\n T.dog: 1.64355731436794E-103\ncell 4-8\n Topic.pig: 6.574229257471784E-103\n Topic.None: 2.038013042086984E-101\ncell 12-16\n Words.dog: 1.247517314132927E-91\n Words.pig: 1.247517314132927E-91\n Words.None: 4.1583868887182485E-93\ncell 17-21\n Socials.NotTopical.kid.eyes: 1.69834250339495E-103\n Socials.Topical.kid.eyes: 1.64355731436794E-103\ncell 21-25\n Topic.pig: 6.574229257471784E-103\n Topic.None: 2.038013042086984E-101\ncell 28-32\n Topic.dog: 1.2936386808643194E-102\n Topic.None: 1.2936386808643194E-102\ncell 33-37\n Words.dog: 2.454792173205653E-91\n Words.None: 8.182632394711623E-93\ncell 37-41\n T.None: 3.234096702160878E-103\n T.dog: 3.234096702160878E-103\ncell 47-51\n Words.None: 2.5366184971528253E-91\ncell 11-16\n Words.dog: 2.7694881327313563E-88\n Words.pig: 2.7694881327313563E-88\n Words.None: 9.231617877476652E-90\ncell 16-21\n T.None: 5.0950326052174406E-102\n T.dog: 1.64355731436794E-103\ncell 32-37\n Words.dog: 5.44963802505643E-88\n Words.None: 1.8165441918061402E-89\ncell 37-42\n Topic.dog: 1.293638680864356E-102\n Topic.None: 1.293638680864356E-102\ncell 46-51\n Words.None: 5.631292444237168E-88\ncell 10-16\n Words.dog: 6.14826297835458E-85\n Words.pig: 6.14826297835458E-85\n Words.None: 2.0494189433637565E-86\ncell 45-51\n Words.None: 1.2501467851045195E-84\ncell 9-16\n Words.dog: 1.3649142310541284E-81\n Words.pig: 1.3649142310541284E-81\n Words.None: 4.549709553799541E-83\ncell 37-44\n Sentence.dog: 6.375567472146938E-96\n Sentence.None: 6.375567472146938E-96\ncell 44-51\n Sentence.None: 2.5002935702090435E-84\ncell 0-8\n Topic.dog: 1.2622532796878728E-99\n Topic.pig: 1.2622532796878728E-99\n Topic.None: 1.2622532796878728E-99\ncell 8-16\n Words.dog: 3.030109259628095E-78\n Words.pig: 3.030109259628095E-78\n Words.None: 1.0100354098396149E-79\ncell 16-25\n Topic.dog: 1.2622532796878728E-99\n Topic.pig: 1.2622532796878728E-99\n Topic.None: 1.2622532796878728E-99\ncell 28-37\n Sentence.dog: 6.975536672072322E-86\n Sentence.None: 6.975536672072322E-86\ncell 16-28\n Sentence.dog: 1.3810369163800202E-89\n Sentence.pig: 1.3810369163800202E-89\n Sentence.None: 1.3810369163800202E-89\ncell 37-51\n Discourse.dog: 1.563026562445644E-72\n Discourse.None: 1.563026562445644E-72\ncell 0-16\n Sentence.dog: 7.446803963265918E-73\n Sentence.pig: 7.446803963265918E-73\n Sentence.None: 7.446803963265918E-73\ncell 28-51\n Discourse.dog: 3.3408002911872516E-52\n Discourse.None: 3.3408002911872516E-52\ncell 16-51\n Discourse.dog: 1.4137165189836475E-35\n Discourse.pig: 1.4137165189836475E-35\n Discourse.None: 1.4137165189836475E-35\ncell 0-51\n : 1.0\n Discourse.dog: 0.032258100000000005\n Discourse.pig: 0.032258100000000005\n Discourse.None: 0.032258100000000005\n");
    
      assertEquals(parser.sprintExpectedCounts(), "# Expected counts\n0.491803 Discourse->[Discourse.dog]\n2.919091 Sentence.dog->[Topic.dog Words.dog]\n0.491803 Discourse->[Discourse.pig]\n0.983607 Sentence.pig->[Topic.pig Words.pig]\n0.016393 Discourse->[Discourse.None]\n1.000000 Discourse.None->[Sentence.None]\n1.097303 Sentence.None->[Topic.None Words.None]\n1.654334 Discourse.dog->[Sentence.dog Discourse.dog]\n0.241870 Discourse.dog->[Sentence.dog Discourse.pig]\n1.022886 Discourse.dog->[Sentence.dog Discourse.None]\n0.717809 Discourse.pig->[Sentence.pig Discourse.dog]\n0.241870 Discourse.pig->[Sentence.pig Discourse.pig]\n0.023927 Discourse.pig->[Sentence.pig Discourse.None]\n0.055144 Discourse.None->[Sentence.None Discourse.dog]\n0.008062 Discourse.None->[Sentence.None Discourse.pig]\n0.034096 Discourse.None->[Sentence.None Discourse.None]\n1.097303 Words.None->[Word.None]\n5.308831 Words.None->[Word.None Words.None]\n1.459545 Words.dog->[Word.dog]\n1.459545 Words.dog->[Word.None]\n4.632470 Words.dog->[Word.dog Words.dog]\n4.632470 Words.dog->[Word.None Words.dog]\n0.491803 Words.pig->[Word.pig]\n0.491803 Words.pig->[Word.None]\n2.213115 Words.pig->[Word.pig Words.pig]\n2.213115 Words.pig->[Word.None Words.pig]\n0.245902 Word.dog->[_a]\n0.245902 Word.dog->[_and]\n0.245902 Word.dog->[_dog]\n0.245902 Word.dog->[_is]\n0.483871 Word.dog->[_nice]\n0.245902 Word.dog->[_puppy]\n0.245902 Word.dog->[_that]\n0.483871 Word.dog->[_thats]\n0.245902 Word.dog->[_this]\n0.245902 Word.dog->[_whats]\n3.157060 Word.dog->[_woof]\n0.245902 Word.pig->[_a]\n0.245902 Word.pig->[_and]\n0.245902 Word.pig->[_dog]\n0.245902 Word.pig->[_is]\n0.245902 Word.pig->[_puppy]\n0.245902 Word.pig->[_that]\n0.245902 Word.pig->[_this]\n0.245902 Word.pig->[_whats]\n0.737705 Word.pig->[_woof]\n0.508197 Word.None->[_a]\n0.508197 Word.None->[_and]\n2.000000 Word.None->[_come]\n0.508197 Word.None->[_dog]\n2.000000 Word.None->[_here]\n0.508197 Word.None->[_is]\n0.516129 Word.None->[_nice]\n0.508197 Word.None->[_puppy]\n0.508197 Word.None->[_that]\n0.516129 Word.None->[_thats]\n0.508197 Word.None->[_this]\n0.508197 Word.None->[_whats]\n4.105235 Word.None->[_woof]\n2.000000 Word.None->[_you]\n5.000000 Topic.None->[_##]\n1.113696 Topic.None->[T.None Topic.None]\n4.000000 PSEUDO.DOG->[_.dog]\n1.080909 T.None->[PSEUDO.DOG Socials.NotTopical.kid.eyes]\n2.000000 PSEUDO.PIG->[_.pig]\n1.016393 T.None->[PSEUDO.PIG Socials.NotTopical.kid.eyes]\n2.919091 Topic.dog->[T.dog Topic.None]\n2.919091 T.dog->[PSEUDO.DOG Socials.Topical.kid.eyes]\n0.983607 Topic.pig->[T.pig Topic.None]\n0.983607 Topic.pig->[T.None Topic.pig]\n0.983607 T.pig->[PSEUDO.PIG Socials.Topical.kid.eyes]\n3.000000 PSEUDOKID.EYES->[_kid.eyes]\n1.048651 Socials.NotTopical.kid.eyes->[PSEUDOKID.EYES Socials.NotTopical.kid.hands]\n1.048651 Socials.NotTopical.kid.eyes->[Socials.NotTopical.kid.hands]\n2.000000 PSEUDOKID.HANDS->[_kid.hands]\n1.016393 Socials.NotTopical.kid.hands->[PSEUDOKID.HANDS Socials.NotTopical.mom.eyes]\n1.080909 Socials.NotTopical.kid.hands->[Socials.NotTopical.mom.eyes]\n2.000000 PSEUDOMOM.EYES->[_mom.eyes]\n1.016393 Socials.NotTopical.mom.eyes->[PSEUDOMOM.EYES Socials.NotTopical.mom.hands]\n1.080909 Socials.NotTopical.mom.eyes->[Socials.NotTopical.mom.hands]\n3.000000 PSEUDOMOM.HANDS->[_mom.hands]\n0.572713 Socials.NotTopical.mom.hands->[PSEUDOMOM.HANDS Socials.NotTopical.mom.point]\n1.524590 Socials.NotTopical.mom.hands->[Socials.NotTopical.mom.point]\n2.097303 Socials.NotTopical.mom.point->[_#]\n1.951349 Socials.Topical.kid.eyes->[PSEUDOKID.EYES Socials.Topical.kid.hands]\n1.951349 Socials.Topical.kid.eyes->[Socials.Topical.kid.hands]\n0.983607 Socials.Topical.kid.hands->[PSEUDOKID.HANDS Socials.Topical.mom.eyes]\n2.919091 Socials.Topical.kid.hands->[Socials.Topical.mom.eyes]\n0.983607 Socials.Topical.mom.eyes->[PSEUDOMOM.EYES Socials.Topical.mom.hands]\n2.919091 Socials.Topical.mom.eyes->[Socials.Topical.mom.hands]\n2.427287 Socials.Topical.mom.hands->[PSEUDOMOM.HANDS Socials.Topical.mom.point]\n1.475410 Socials.Topical.mom.hands->[Socials.Topical.mom.point]\n3.902697 Socials.Topical.mom.point->[_#]\n");
      
      List<String> results = parser.socialMarginalDecoding();
      assertEquals(results.size(), 5);
      assertEquals(results.get(0), "Sentence.dog (Word.None and) (Word.None whats) (Word.None that) (Word.None is) (Word.None this) (Word.None a) (Word.None puppy) (Word.None dog)");
      assertEquals(results.get(1), "Sentence.dog (Word.None woof) (Word.None woof) (Word.None woof)");
      assertEquals(results.get(2), "Sentence.dog (Word.None woof) (Word.None woof) (Word.None woof) (Word.None woof) (Word.None woof)");
      assertEquals(results.get(3), "Sentence.dog (Word.None thats) (Word.None nice)");
      assertEquals(results.get(4), "Sentence.None (Word.None come) (Word.None here) (Word.None you) (Word.None come) (Word.None here) (Word.None you)");
    }
  }

  @Test
  public void testSocialMarginalDecoding(){
    rootSymbol = "Discourse";
    initParserFromFile(socialDiscourse);
    
    String inputSent = ".dog kid.eyes mom.eyes # .pig kid.hands # ## and whats that is this a puppy dog .dog kid.eyes mom.eyes mom.hands # .pig kid.hands # ## woof dog woof .dog mom.hands # ## woof dog woof woof woof .dog kid.eyes mom.hands # ## thats nice ## come here you come here you"; 

    parser.parseSentence(inputSent);
    parser.computeOutsideProbs();

//    EarleyParser.verbose = 3;
    List<String> results = parser.socialMarginalDecoding();
    for(String result : results){
      System.err.println(result);
    }
    assertEquals(results.size(), 5);
    assertEquals(results.get(0), "Sentence.dog (Word.None and) (Word.None whats) (Word.None that) (Word.None is) (Word.None this) (Word.None a) (Word.None puppy) (Word.None dog)");
    assertEquals(results.get(1), "Sentence.dog (Word.None woof) (Word.None dog) (Word.None woof)");
    assertEquals(results.get(2), "Sentence.dog (Word.None woof) (Word.None dog) (Word.None woof) (Word.None woof) (Word.None woof)");
    assertEquals(results.get(3), "Sentence.dog (Word.None thats) (Word.None nice)");
    assertEquals(results.get(4), "Sentence.None (Word.None come) (Word.None here) (Word.None you) (Word.None come) (Word.None here) (Word.None you)");

  }

  public void testExtendedRule(){
    initParserFromString(extendedGrammarString);
    
    String inputSentence = "b c";

    
    parser.parseSentence(inputSentence);
    
    List<Double> surprisalList = parser.getSurprisalList();
    
    for (int i = 0; i < surprisalList.size(); i++) {
      System.err.println(i + "\t" + surprisalList.get(i));
    }
    
    assertEquals(surprisalList.size(), 2);
    assertEquals(0.3237870745944744, surprisalList.get(0), 1e-5);
    assertEquals(0.24544930825601569, surprisalList.get(1), 1e-5);
    
    if(parser.getDecodeOpt()==1){
      Tree tree = parser.viterbiParse();
      System.err.println(tree.toString());
      assertEquals(tree.toString(), "( (ROOT (A (B b) (C c))))");
    }
  }

  public void testSimpleExtendedRule(){
    double x = 0.7;
    
    String simpleExtendedGrammarString = "ROOT->[A] : 1.0\n" + 
    "A->[_b _c] : " + x + "\n" + 
    "A->[D] : " + (1-x) + "\n";
    
    initParserFromString(simpleExtendedGrammarString);
    
    String inputSentence = "b c";

    parser.parseSentence(inputSentence);
    
    List<Double> surprisalList = parser.getSurprisalList();
    
    for (int i = 0; i < surprisalList.size(); i++) {
      System.err.println(i + "\t" + surprisalList.get(i));
    }
    
    assertEquals(surprisalList.size(), 2);
    assertEquals(-Math.log(x), surprisalList.get(0), 1e-5);
    assertEquals(0.0, surprisalList.get(1), 1e-5); // -ln(1)
    
    if(parser.getDecodeOpt()==1){
      Tree tree = parser.viterbiParse();
      System.err.println(tree.pennString());
      assertEquals(tree.toString(), "( (ROOT (A b c)))");
    }
  }
  
  public void testSimpleExtendedRule1(){
    double x = 0.7;
    double y = 0.6;
    String simpleExtendedGrammarString = "ROOT->[A] : 1.0\n" + 
    "A->[B] : " + x + "\n" + 
    "A->[_b _c] : " + (1-x) + "\n" + 
    "B->[B1] : " + y + "\n" + 
    "B->[_b _c] : " + (1-y) + "\n" +
    "B1->[_b] : " + 1.0 + "\n";
    
    initParserFromString(simpleExtendedGrammarString);
    
    String inputSentence = "b c";

    parser.parseSentence(inputSentence);
    
    List<Double> surprisalList = parser.getSurprisalList();
    List<Double> stringProbList = parser.getStringProbList();
    
    for (int i = 0; i < surprisalList.size(); i++) {
      System.err.println(i + "\t" + surprisalList.get(i));
    }
    
    assertEquals(surprisalList.size(), 2);
    assertEquals(0.0, surprisalList.get(0), 1e-5);
    assertEquals(-Math.log(1-x*y), surprisalList.get(1), 1e-5);
    
    assertEquals(stringProbList.size(), 2);
    assertEquals(x*y, stringProbList.get(0), 1e-5);
    assertEquals(1-x*y, stringProbList.get(1), 1e-5);
    
    if(parser.getDecodeOpt()==1){
      Tree tree = parser.viterbiParse();
      System.err.println(tree.pennString());
      assertEquals(tree.toString(), "( (ROOT (A b c)))");
    }
  }
  
  public void testComplexAG(){
    double x1 = 0.1;
    double x2 = 0.2;
    double x3 = 0.3;
    double x4 = 0.3;
    double x5 = 1 - x1 - x2 - x3 - x4;
    double y1 = 0.1;
    double y2 = 0.2;
    double y3 = 0.3;
    double y4 = 0.3;
    double y5 = 1 - y1 - y2 - y3 - y4;
    
    String simpleExtendedGrammarString = "ROOT->[A] : 1.0\n" + 
    "A->[B] : " + x1 + "\n" +
    "A->[_b _c] : " + x2 + "\n" +
    "A->[_b _e] : " + x3 + "\n" +
    "A->[_b _c _d] : " + x4 + "\n" +
    "A->[_b _c _f] : " + x5 + "\n" +
    "B->[B1] : " + y1 + "\n" +
    "B->[_b _c] : " + y2 + "\n" +
    "B->[_b _e] : " + y3 + "\n" +
    "B->[_b _c _d] : " + y4 + "\n" +
    "B->[_b _c _f] : " + y5 + "\n" +
    "B1->[_b] : " + 1.0;
    
    initParserFromString(simpleExtendedGrammarString);
    
    String inputSentence = "b c d";

    parser.parseSentence(inputSentence);
    
    List<Double> surprisalList = parser.getSurprisalList();
    List<Double> stringProbList = parser.getStringProbList();

    for (int i = 0; i < surprisalList.size(); i++) {
      System.err.println(i + "\t" + surprisalList.get(i));
    }
    
    assertEquals(surprisalList.size(), 3);
    assertEquals(0.0, surprisalList.get(0), 1e-5);
    assertEquals(-Math.log(x2 + x4 + x5 + x1*(y2 + y4 + y5)), surprisalList.get(1), 1e-5);
    assertEquals(-Math.log((x4 + x1*y4)/(x2 + x4 + x5 + x1*(y2 + y4 + y5))), surprisalList.get(2), 1e-5);
    
    assertEquals(stringProbList.size(), 3);
    assertEquals(x1*y1, stringProbList.get(0), 1e-5);
    assertEquals(x1*y2 + x2, stringProbList.get(1), 1e-5);
    assertEquals(x4 + x1*y4, stringProbList.get(2), 1e-5);
    
    if(parser.getDecodeOpt()==1){
      Tree tree = parser.viterbiParse();
      System.err.println(tree.toString());
      assertEquals(tree.toString(), "( (ROOT (A b c d)))");
    }
  }
  
  public void testComplexAG1(){
    double x1 = 0.1;
    double x2 = 1-x1;
    double y1 = 0.2;
    double y2 = 1-y1;
    double z1 = 0.3;
    double z2 = 1-z1;
    
    String complexAGGrammarString = 
      "ROOT->[S] : " + x1 + "\n" +
      "ROOT->[_a _b _c] : " + x2 + "\n" +
      "S->[A S1] : " + y1 + "\n" +
      "S->[_a _b _c] : " + y2 + "\n" +
      "S1->[B C] : " + z1 + "\n" +
      "S1->[_b _c] : " + z2 + "\n" +
      "A->[_a] : " + 1.0 + "\n" +
      "B->[_b] : " + 1.0 + "\n" + 
      "C->[_c] : " + 1.0;
    
    initParserFromString(complexAGGrammarString);
    
    String inputSentence = "a b c";

    parser.parseSentence(inputSentence);
    
    List<Double> surprisalList = parser.getSurprisalList();
    List<Double> stringProbList = parser.getStringProbList();
    
    for (int i = 0; i < surprisalList.size(); i++) {
      System.err.println(i + "\t" + surprisalList.get(i));
    }
    
    assertEquals(surprisalList.size(), 3);
    assertEquals(0.0, surprisalList.get(0), 1e-5);
    assertEquals(0.0, surprisalList.get(1), 1e-5);
    assertEquals(0.0, surprisalList.get(2), 1e-5);
    
    assertEquals(stringProbList.size(), 3);
    assertEquals(0.0, stringProbList.get(0), 1e-5);
    assertEquals(0.0, stringProbList.get(1), 1e-5);
    assertEquals(1.0, stringProbList.get(2), 1e-5);
    
    if(parser.getDecodeOpt()==1){
      Tree tree = parser.viterbiParse();
      System.err.println(tree.toString());
      assertEquals(tree.toString(), "( (ROOT a b c))");
    }
  }
  
  // @TODO: TRY THIS TEST!!!
  public void testComplexAGClosure(){
    double x1 = 0.01;
    double x2 = 1-x1;
    double y1 = 0.11;
    double y2 = 1-y1;
    
    double z1 = 0.21;
    double z2 = 0.22;
    double z3 = 0.23;
    double z4 = 1 - z1 - z2 - z3;
    
    String simpleExtendedGrammarString = 
      "ROOT->[S] : " + x1 + "\n" +
      "ROOT->[_a _b _c] : " + x2 + "\n" +
      "S->[A] : " + y1 + "\n" +
      "S->[S A] : " + y2 + "\n" +
      "A->[S] : " + z1 + "\n" +
      "A->[B C] : " + z2 + "\n" +
      "A->[_b _c] : " + z3 + "\n" +
      "A->[A1] : " + z4 + "\n" +
      "B->[B1] : " + 1.0 + "\n" +
      "C->[C1] : " + 1.0 + "\n" +
      "A1->[_a] : " + 1.0 + "\n" +
      "B1->[_b] : " + 1.0 + "\n" + 
      "C1->[_c] : " + 1.0;
    
    initParserFromString(simpleExtendedGrammarString);
    
    String inputSentence = "a b c";
    parser.parseSentence(inputSentence);
    
    List<Double> surprisalList = parser.getSurprisalList();
    List<Double> stringProbList = parser.getStringProbList();
    
    // test left-corner closure
    // a = zeros(6,6); a(1,2)=1.0; a(2,3)=0.01; a(3,3)=0.89;a(3,4)=0.11;a(4,3)=0.21;a(4,5)=0.22;
    // (eye(6)-a)^(-1)
    if(isLogProb){
      assertEquals(Util.sprint(
        parser.getGrammar().getLeftCornerClosures().getClosureMatrix()), "0.0 0.0 -2.1621729392773004 -4.3694478524670215 -5.883575585096797 -Infinity\n-Infinity 0.0 -2.1621729392773004 -4.3694478524670215 -5.883575585096797 -Infinity\n-Infinity -Infinity 2.442997246710791 0.23572233352106994 -1.278405399108706 -Infinity\n-Infinity -Infinity 0.8823494984461224 0.23572233352106994 -1.2784053991087057 -Infinity");
    } else {
      assertEquals(Util.sprint(
          parser.getGrammar().getLeftCornerClosures().getClosureMatrix()), "1.0 1.0 0.11507479861910243 0.012658227848101267 0.002784810126582277 0.0\n0.0 1.0 0.11507479861910243 0.012658227848101267 0.002784810126582277 0.0\n0.0 0.0 11.507479861910243 1.2658227848101267 0.2784810126582277 0.0\n0.0 0.0 2.416570771001151 1.2658227848101267 0.27848101265822783 0.0");
    }
    

    for (int i = 0; i < surprisalList.size(); i++) {
      System.err.println(i + "\tsurprisal=" + surprisalList.get(i));
      if(!isScaling){
        assert(stringProbList.get(i)<=1.0);
      }
    }
    
    assertEquals(surprisalList.size(), 3);
    assertEquals(0.005712487765391473, surprisalList.get(0), 1e-5);
    assertEquals(0.0020843742359873776, surprisalList.get(1), 1e-5);
    assertEquals(0.0, surprisalList.get(2), 1e-5);
    
    assertEquals(stringProbList.size(), 3);
    assertEquals(3.8284368922100536E-4, stringProbList.get(0), 1e-5);
    assertEquals(0.0, stringProbList.get(1), 1e-5);
    assertEquals(0.9901606659305786, stringProbList.get(2), 1e-5);
    
    if(parser.getDecodeOpt()==1){
      Tree tree = parser.viterbiParse();
      System.err.println(tree.toString());
      assertEquals(tree.toString(), "( (ROOT a b c))");
    }
  }
}
