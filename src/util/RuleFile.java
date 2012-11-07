package util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import parser.SmoothLexicon;

import base.ProbRule;
import base.TerminalRule;




import edu.stanford.nlp.parser.lexparser.IntTaggedWord;
import edu.stanford.nlp.stats.ClassicCounter;
import edu.stanford.nlp.stats.Counter;
import edu.stanford.nlp.stats.Counters;
import edu.stanford.nlp.util.HashIndex;
import edu.stanford.nlp.util.Index;

public class RuleFile {
  public static int verbose = 0;
  private static Pattern p = Pattern.compile("(.+?)->\\[(.+?)\\] : ([0-9.\\+\\-Ee]+)");
  //private static String UNK = "UNK";
    
  public static void parseRuleFile(BufferedReader br, Collection<ProbRule> rules, 
      Collection<ProbRule> extendedRules, // e.g. adaptor grammar rules, i.e. approximating pcfg with sequence of terminals on the rhs
      Map<Integer, Counter<Integer>> tag2wordsMap,
      Map<Integer, Set<IntTaggedWord>> word2tagsMap,
      Map<Integer, Integer> nonterminalMap,
      Index<String> wordIndex,
      Index<String> tagIndex
      ) throws IOException{
    String inputLine;
    
    if (verbose>=1){
      System.err.print("# Parsing rule data ...");
    }
    
    int count = 0;
    
    while ((inputLine = br.readLine()) != null){
      count++;

      inputLine = inputLine.replaceAll("(^\\s+|\\s+$)", ""); // remove leading and trailing spaces
      Matcher m = p.matcher(inputLine);
      
      if(m.matches()){
        // sanity check
        if(m.groupCount() != 3){
          System.err.println("! Num of matched groups != 3 for line \"" + inputLine + "\"");
          System.exit(1);
        }
        
        // retrieve info
        String tag = m.group(1);
        int iT = tagIndex.indexOf(tag, true);
        String rhs = m.group(2);
        double prob = Double.parseDouble(m.group(3));
        if(prob < 0){
          System.err.println("value < 0: " + inputLine);
          System.exit(1);
        }
        
        String[] children = rhs.split(" ");
        int numChilds = children.length;
        
        // create a rule node or a tagged word
        if (numChilds == 1 && children[0].startsWith("_")){ // terminal symbol, update distribution
          int iW = wordIndex.indexOf(children[0].substring(1), true);
          addWord(iW, iT, prob, tag2wordsMap, word2tagsMap); //, nonterminals, tagHash, seenEnd);
        } else { // rule
          if(!nonterminalMap.containsKey(iT)){
            nonterminalMap.put(iT, nonterminalMap.size());
          }
          
          // child indices
          List<Integer> childIndices = new ArrayList<Integer>();
          int numTerminals = 0;
          for (int i=0; i<numChilds; ++i){
            String child = children[i];
            if(!child.startsWith("_")){ // tag 
              childIndices.add(tagIndex.indexOf(child, true)); // tag index
            } else { // terminal
              numTerminals++;
              childIndices.add(wordIndex.indexOf(child.substring(1), true)); // word index
            }
          }
          assert(numTerminals==0 || numTerminals==childIndices.size()); // either all terminals or all tags
          
          if (numTerminals==childIndices.size()){ // process extended rule X -> _a _b _c
            ProbRule rule = new TerminalRule(iT, childIndices, prob);
            extendedRules.add(rule);
          } else {
            ProbRule rule = new ProbRule(iT, childIndices, prob);
            rules.add(rule);
          }
          
        }
      } else {
        System.err.println("! Fail to match line \"" + inputLine + "\"");
        System.exit(1);
      }
      
      if (verbose>=1){
        if(count % 10000 == 0){
          System.err.print(" (" + count + ") ");
        }
      }
    }

    if (verbose>=1){
      System.err.println(" Done! Total lines = " + count);
    }
    br.close();
  }
  
  private static void addWord(int iW, int iT, double prob, 
      Map<Integer, Counter<Integer>> tag2wordsMap,
      Map<Integer, Set<IntTaggedWord>> word2tagsMap
      // Set<Integer> nonterminals
      //Map<Label, Counter<String>> tagHash,
      //Set<String> seenEnd
      ){
    
    // initialize counter
    if(!tag2wordsMap.containsKey(iT)){
      tag2wordsMap.put(iT, new ClassicCounter<Integer>());
    }
    Counter<Integer> wordCounter = tag2wordsMap.get(iT);
    
    // sanity check
    assert(!wordCounter.containsKey(iW));
    
    // set prob
    wordCounter.setCount(iW, prob);
    
   
    // update list of tags per terminal
    if (!word2tagsMap.containsKey(iW)) {
      word2tagsMap.put(iW, new HashSet<IntTaggedWord>());
    }
    word2tagsMap.get(iW).add(new IntTaggedWord(iW, iT)); // NOTE: it is important to have the tag here due to BaseLexicon.score() method's requirement
  }
  
  /**
   * Thang v110901: output rules to file
   * @throws IOException 
   **/
  public static void printRules(String ruleFile, Collection<ProbRule> rules
      , Map<Integer, Counter<Integer>> origTag2wordsMap, Index<String> wordIndex, Index<String> tagIndex, boolean isExp) throws IOException{
    System.err.println("# Output rules to file " + (new File(ruleFile)).getAbsolutePath());
    BufferedWriter bw = new BufferedWriter(new FileWriter(ruleFile));
    
    Map<Integer, Counter<Integer>> tag2wordsMap;
    if (isExp){ // exp
      tag2wordsMap = new HashMap<Integer, Counter<Integer>>();
      for(Integer iT : origTag2wordsMap.keySet()){
        Counter<Integer> counter = Counters.getCopy(origTag2wordsMap.get(iT));
        Counters.expInPlace(counter);
        
        tag2wordsMap.put(iT, counter);
      }
    } else {
      tag2wordsMap = origTag2wordsMap;
    }
    
    // rules: non-terminal->[terminal] : prob
    for(Integer iT : tag2wordsMap.keySet()) { //Entry<Integer, Counter<Integer>> mapEntry : tag2wordsMap.entrySet()){
      String prefix = tagIndex.get(iT) + "->[_";
      Counter<Integer> counter = tag2wordsMap.get(iT);
      
      for(Integer iW : counter.keySet()){
        double prob = counter.getCount(iW);
        if(isExp && prob < 0){
          System.err.println("Prob < 0: " + prefix + "\t" + wordIndex.get(iW) + "\t" + prob);
          System.exit(1);
        }
        bw.write(prefix + wordIndex.get(iW) + "] : " + prob + "\n");
      }
    }
    
    // rules: non-terminal -> non-terminals
    bw.write(Util.sprint(rules, wordIndex, tagIndex));
//    for(Rule rule : rules){
//      bw.write(rule + "\n");
//    }
    
    bw.close();
  }
  
  /**
   * Thang v110901: output rules to file
   * @throws IOException 
   **/
  public static void printRulesSchemeFormat(String prefixFile, Collection<ProbRule> rules
      , Collection<ProbRule> extendedRules
      , Map<Integer, Counter<Integer>> tag2wordsMap 
      , Index<String> wordIndex, Index<String> tagIndex) throws IOException{
    String ruleFile = prefixFile + ".forms.txt";
    String countFile = prefixFile + ".counts.txt";
    System.err.println("# Output rules to files: " + (new File(ruleFile)).getAbsolutePath()
        + "\t" + (new File(countFile)).getAbsolutePath());
    BufferedWriter bw = new BufferedWriter(new FileWriter(ruleFile));
    BufferedWriter bwCount = new BufferedWriter(new FileWriter(countFile));
    
    // rules: non-terminal->[terminal] : prob
    for(int iT : tag2wordsMap.keySet()){
      String tag = tagIndex.get(iT);
      Counter<Integer> counter = tag2wordsMap.get(iT);
      for(Integer iW : counter.keySet()){
        String word = wordIndex.get(iW);
        if (word.startsWith("UNK")){
          int count = (int) counter.getCount(iW);
          System.err.println("(" + tag + " _" + word + ")\t" + count);
          bw.write("(" + tag + " _" + word + ")\n");
          bwCount.write(count + "\n");
        }
      }
    }
    
    // print rules
//    rules.addAll(extendedRules);
//    for(Rule rule : rules){
//      bw.write(rule.schemeString() + "\n");
//      bwCount.write((int) rule.score + "\n"); 
//    }
    
    bw.close();
    bwCount.close();
  }
  
  public static String schemeString(String mother, List<String> children) {
    StringBuffer sb = new StringBuffer();
    sb.append("(" + mother + " ");
    for (String dtr : children){
      sb.append("(X " + dtr + ") ");
    }
    
    if(children.size() > 0){
      sb.delete(sb.length()-1, sb.length());
      sb.append(")");
    }
    
    
    //sb.append("\t" + ((int) score));
    return sb.toString();
  }
  
  public static void main(String[] args) {
    String ruleFile = null;
    String outRuleFile = null;
    if (args.length == 2){
      ruleFile = args[0];
      outRuleFile = args[1];
    } else {
      System.err.println("# Run program with two arguments [inFile] [outFile]");
      System.exit(1);
    }
    //ruleFile = "../grammars/WSJ.5/WSJ.5.lexicon-rule-counts.deescaped.txt";
    //outRuleFile = ruleFile + ".out";
    
    // extract rules and taggedWords from grammar file
    Map<Integer, Counter<Integer>> tag2wordsMap = new HashMap<Integer, Counter<Integer>>();
    Map<Integer, Set<IntTaggedWord>> word2tagsMap = new HashMap<Integer, Set<IntTaggedWord>>();
    Map<Integer, Integer> nonterminalMap = new HashMap<Integer, Integer>();
    Index<String> wordIndex = new HashIndex<String>();
    Index<String> tagIndex = new HashIndex<String>();
    Collection<ProbRule> rules = new ArrayList<ProbRule>();
    Collection<ProbRule> extendedRules = new ArrayList<ProbRule>();
    
    /* Input */
    try {
      RuleFile.parseRuleFile(Util.getBufferedReaderFromFile(ruleFile), rules, extendedRules, tag2wordsMap, 
          word2tagsMap, nonterminalMap, wordIndex, tagIndex); //, tagHash, seenEnd); // we don't care much about extended rules, just treat them as rules
      //rules.addAll(extendedRules);
    } catch (IOException e){
      System.err.println("Can't read rule file: " + ruleFile);
      e.printStackTrace();
    }
    
    /* Smooth */
    SmoothLexicon.smooth(tag2wordsMap, wordIndex, tagIndex, word2tagsMap);
    
    /* Output */
    try {
      //RuleFile.printRules(outRuleFile, rules, newWordCounterTagMap);
      RuleFile.printRulesSchemeFormat(outRuleFile, rules, extendedRules, tag2wordsMap, wordIndex, tagIndex);
    } catch (IOException e){
      System.err.println("Can't write to: " + outRuleFile);
      e.printStackTrace();
    }
    
    
  }
}

/** Unused code **/
//public static void processRuleFile(String grammarFile) throws IOException{
//  BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(grammarFile)));
//  String inputLine;
//  
//  System.err.print("# Reading rule file ...");
//  int count = 0;
//  while ((inputLine = br.readLine()) != null){
//    count++;
//    inputLine = inputLine.replaceAll("(^\\s+|\\s+$)", ""); // remove leading and trailing spaces
//    Matcher m = p.matcher(inputLine);
//    if(m.matches()){ // check pattern
//      // sanity check
//      if(m.groupCount() != 3){
//        System.err.println("! Num of matched groups != 3 for line \"" + inputLine + "\"");
//        System.exit(1);
//      }
//      
//      // retrieve info
//      String tag = m.group(1);
//      String rhs = m.group(2);
//      double prob = Double.parseDouble(m.group(3));
//      
//      System.err.println(tag + "\t" + rhs + "\t" + prob);
//      break;
//    } else {
//      System.err.println("! Fail to match line \"" + inputLine + "\"");
//      System.exit(1);
//    }
//    
//    if(count % 10000 == 0){
//      System.err.print(" (" + count + ") ");
//    }
//  }
//  System.err.println(" Done! Total lines = " + count);    
//}
//public static void parseRuleFile(BufferedReader br, Collection<Rule> rules,
//    Collection<Rule> extendedRules,
//    Map<Integer, Counter<Integer>> tag2wordsMap) throws IOException{
//
//  Map<Integer, Set<IntTaggedWord>> word2tagsMap = new HashMap<Integer, Set<IntTaggedWord>>();
//  Set<Integer> nonterminals = new HashSet<Integer>();
//  Index<String> wordIndex = new HashIndex<String>();
//  Index<String> tagIndex = new HashIndex<String>();
//  parseRuleFile(br, rules, extendedRules, tag2wordsMap, word2tagsMap, nonterminals, wordIndex, tagIndex); //tagHash, seenEnd);
//}
///** Read from string **/
//public static void parseRuleFile(StringReader sr, Collection<Rule> rules, 
//    Collection<Rule> extendedRules,
//    Map<Integer, Counter<Integer>> tag2wordsMap,
//    Map<Integer, Set<IntTaggedWord>> word2tagsMap,
//    Set<Integer> nonterminals,
//    Index<String> wordIndex,
//    Index<String> tagIndex
//    ) throws IOException{
//  BufferedReader br = new BufferedReader(sr);
//  
//  System.err.println("Reading from string ... ");
//  parseRuleFile(br, rules, extendedRules, tag2wordsMap, word2tagsMap, nonterminals, wordIndex, tagIndex);
//}

//public static void parseRuleFile(String grammarFile, Collection<Rule> rules, 
//    Collection<Rule> extendedRules,
//    Map<Integer, Counter<Integer>> tag2wordsMap,
//    Map<Integer, Set<IntTaggedWord>> word2tagsMap,
//    Set<Integer> nonterminals,
//    Index<String> wordIndex,
//    Index<String> tagIndex
//    //Map<Label, Counter<String>> tagHash,
//    //Set<String> seenEnd
//    ) throws IOException{
//  System.err.print("# Reading rule file " + grammarFile + " ...");
//  BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(grammarFile)));
//  
//  parseRuleFile(br, rules, extendedRules, tag2wordsMap, word2tagsMap, nonterminals, wordIndex, tagIndex); //, tagHash, seenEnd);
//}

//Set<String> unarySet = new HashSet<String>();
//if(children.length == 1){ // unary
//  String unaryPair = tag + "->" + children[0];
//  String reverseUnaryPair = children[0] + "->" + tag;
//  if (unarySet.contains(reverseUnaryPair)) {
//    System.err.println("# Recursive unary rule: " + unaryPair);
//  }
//  unarySet.add(unaryPair);
//}

//if(word.startsWith(UNK)){ // store tagHash
//  // add seenEnd
//  if(!word.equals(UNK)){
//    seenEnd.add(word);
//  }
//  
//  // initialize counter
//  Label tagLabel = iT.tagLabel();
//  if(!tagHash.containsKey(tagLabel)){
//    tagHash.put(tagLabel, new ClassicCounter<String>());
//  }
//  Counter<String> unknownWordCounter = tagHash.get(tagLabel);
//
//  // sanity check
//  if(unknownWordCounter.containsKey(word)){
//    System.err.println("! Error duplicate key: " + word + ", for tag=" + tagLabel);
//    System.exit(1);
//  }
//  
//  // set prob
//  unknownWordCounter.setCount(word, prob);
//  
//  // set tags for UNK
//  if(!nonterminals.contains(iT)){
//    nonterminals.add(iT);
//  }
//  
//} else { // store tag2wordsMap
//  // initialize counter
//  if(!tag2wordsMap.containsKey(iT)){
//    tag2wordsMap.put(iT, new ClassicCounter<Integer>());
//  }
//  Counter<Integer> wordCounter = tag2wordsMap.get(iT);
//  
//  // sanity check
//  assert(!wordCounter.containsKey(iW));
//  
//  // set prob
//  wordCounter.setCount(iW, prob);
//}