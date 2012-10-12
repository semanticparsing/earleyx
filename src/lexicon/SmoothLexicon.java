package lexicon;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;


import edu.stanford.nlp.parser.lexparser.IntTaggedWord;
import edu.stanford.nlp.stats.ClassicCounter;
import edu.stanford.nlp.stats.Counter;
import edu.stanford.nlp.stats.Counters;
import edu.stanford.nlp.util.Index;

public class SmoothLexicon extends BaseLexicon {
  public SmoothLexicon(Index<String> wordIndex, Index<String> tagIndex){
    super(wordIndex, tagIndex);
  }
  
  public Set<IntTaggedWord> tagsForWord(String word) {
    int iW = wordIndex.indexOf(word, true);
    if(!word2tagsMap.containsKey(iW)){ // unknown word
      word = getSignature(word);
      iW = wordIndex.indexOf(word, true);
      
      if(!word2tagsMap.containsKey(iW)){ // unknown signature
        word = UNKNOWN_WORD;
        iW = wordIndex.indexOf(word, true);
      }
    }
    assert(word2tagsMap.containsKey(iW));
    
    return word2tagsMap.get(iW);
  }

  public double score(IntTaggedWord itw) {
    if(!tag2wordsMap.containsKey(itw.tag())){ // no such tag
      return Double.NEGATIVE_INFINITY;
    }
    int iW = itw.word();
    String word = wordIndex.get(iW);
    
    if(!word2tagsMap.containsKey(iW)){ // unknown word
      word = getSignature(word);
      iW = wordIndex.indexOf(word, true);
      
      if(!word2tagsMap.containsKey(iW)){ // unknown signature
        word = UNKNOWN_WORD;
        iW = unkIndex;
      }
    }
     
//    System.err.println("score: " + word + "\t" + itw + "\t" + itw.wordString(wordIndex) +
//        "\t" + tag2wordsMap.get(itw.tag()).getCount(iW));
    
    return tag2wordsMap.get(itw.tag()).getCount(iW);
  }

  // convert to logprob
  public static void smooth(Map<Integer, Counter<Integer>> tag2wordsMap, Index<String> wordIndex
      , Map<Integer, Set<IntTaggedWord>> word2tagsMap){
    smooth(tag2wordsMap, wordIndex, word2tagsMap, false);
  }
  
  public static void smooth(Map<Integer, Counter<Integer>> tag2wordsMap, Index<String> wordIndex
      , Map<Integer, Set<IntTaggedWord>> word2tagsMap, boolean keepRawCount){
    int unkIndex = wordIndex.indexOf(UNKNOWN_WORD, true);
    word2tagsMap.put(unkIndex, new HashSet<IntTaggedWord>());
    
    for(Integer iT : tag2wordsMap.keySet()){ // tag
      Counter<Integer> wordCounter = tag2wordsMap.get(iT);
      
      // find singleton words to build each signature counter per tag
      ClassicCounter<Integer> signatureCounter = new ClassicCounter<Integer>();
      for(Integer iW : wordCounter.keySet()){ // word
        if(wordCounter.getCount(iW) == 1) { // singleton
          String wordSignature = getSignature(wordIndex.get(iW));
          int signatureIndex = wordIndex.indexOf(wordSignature, true);
          signatureCounter.incrementCount(signatureIndex);
          
          // update word2tags map
          if(!word2tagsMap.containsKey(signatureIndex)){
            word2tagsMap.put(signatureIndex, new HashSet<IntTaggedWord>());
          }
          word2tagsMap.get(signatureIndex).add(new IntTaggedWord(signatureIndex, iT));
        }
      }
      wordCounter.addAll(signatureCounter);
      
      // add unk, for a totally new word at test time, whose signature we haven't seen
      wordCounter.incrementCount(unkIndex);
      word2tagsMap.get(unkIndex).add(new IntTaggedWord(unkIndex, iT)); // add all tags to unk
      
      if(!keepRawCount){ // convert to log prob
        // normalize
        Counters.normalize(wordCounter);
        
        // convert to log space
        Counters.logInPlace(wordCounter);
      }
    }
  }
  
  public void train(Collection<IntTaggedWord> intTaggedWords) {
    // initialize
    tag2wordsMap = new HashMap<Integer, Counter<Integer>>(); 
    word2tagsMap = new HashMap<Integer, Set<IntTaggedWord>>();
    
    
    // scan data, collect counts
    for(IntTaggedWord iTW : intTaggedWords){
      int iT = iTW.tag();
      int iW = iTW.word();
      
      // tag2wordsMap
      if(!tag2wordsMap.containsKey(iT)){
        tag2wordsMap.put(iT, new ClassicCounter<Integer>());
      }
      tag2wordsMap.get(iT).incrementCount(iW);
      
      // word2tagsMap
      if(!word2tagsMap.containsKey(iW)){
        word2tagsMap.put(iW, new HashSet<IntTaggedWord>());
      }
      word2tagsMap.get(iW).add(iTW);
    }
    
    // smoothing
    smooth(tag2wordsMap, wordIndex, word2tagsMap);
  }

  /**
   * Signature for a specific word (copied from BaseUnknownWordModel)
   * 
   * @param word The word
   * @return A "signature" (which represents an equivalence class of Strings), e.g., a suffix of the string
   */
  public static String getSignature(String word) {
    boolean useFirst = false; //= true;
    boolean useEnd = true; 
    boolean useFirstCap = true;
    int endLength = 2;
  
    StringBuilder subStr = new StringBuilder("UNK-");
    int n = word.length(); // Thang fix, remove -1;
    char first = word.charAt(0);
    if (useFirstCap) {
      if (Character.isUpperCase(first) || Character.isTitleCase(first)) {
        subStr.append('C');
      } else {
        subStr.append('c');
      }
    }
    if (useFirst) {
      subStr.append(first);
    }
    if (useEnd) {
      subStr.append(word.substring(n - endLength > 0 ? n - endLength : 0, n));
    }
    return subStr.toString();
  }
}