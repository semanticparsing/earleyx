package base;

import java.util.ArrayList;
import java.util.List;


import edu.stanford.nlp.util.Index;

/**
 * Represent an active edge used in Earley algorithms, e.g X -> a b . c
 * 
 * @author Minh-Thang Luong, 2012
 */
public class Edge {
  protected Rule rule; // rule being expanded
  protected int dot; // number of children found so far, right = left + dot
  
  
  public Edge(Rule rule, int dot) {
    super();
    this.rule = rule;
    this.dot = dot;
    assert(dot<=rule.numChildren());
  }

  public void setMother(int mother){
    rule.setMother(mother);
  }
  
  /* Getters */
  public Rule getRule() {
    return rule;
  }
  public int getDot() {
    return dot;
  }
  public int getMother(){
    return rule.getMother();
  }
  public List<Integer> getChildren(){
    return rule.getChildren();
  }
  public int numChildren(){
    return rule.numChildren();
  }
  public int numRemainingChildren(){
    return rule.numChildren()-dot;
  }
  public List<Integer> getChildrenAfterDot(int pos){
    return rule.getChildren(dot+pos);
  }
  public int getChildAfterDot(int pos){
    return rule.getChild(dot+pos);
  }
  public int getChild(int pos){
    return rule.getChild(pos);
  }
  
  public boolean isTerminalEdge(){
    return (rule instanceof TerminalRule) || numChildren()==0;
  }
  
  public Edge getPrevEdge(){
    if(dot>=1){
      return new Edge(rule, dot-1);
    } else {
      return null;
    }
  }
  
  /** 
  * create mother edge which doesn't have any children, mother -> []
  * @return
  */
  public Edge getMotherEdge(){
    return new Edge(new TagRule(rule.getMother(), new ArrayList<Integer>()), 0);
  }

  /** 
  * via edge: first child -> []
  * @return
  */
  public Edge getViaEdge(){ // first child after the dot
    if(rule instanceof TagRule){
      return new Edge(new TagRule(rule.getChildren().get(dot), new ArrayList<Integer>()), 0);
    } else {
      return null;
    }
  }
  
  /** 
   * to edge: mother -> [second children onwards]
   * @return
   */
   public Edge getToEdge(){
     return new Edge(new TagRule(rule.getMother(), rule.getChildren(dot+1)), 0);
   }
   
  public boolean equals(Object o) {
    if (this == o){ // compare pointer
      return true;
    }
    
    if (!(o instanceof Edge)) { // check class
      return false;
    } 

    Edge otherEdge = (Edge) o;
    
    // compare dot position & children
    if (this.dot != otherEdge.getDot() || !rule.equals(otherEdge.getRule())){
      return false;
    }
    
    // compare children
    return true;
  }

  public int hashCode() {
    return rule.hashCode()<< 8 + dot;
  }
  
  public String toString(Index<String> tagIndex, Index<String> wordIndex){
    StringBuffer sb = new StringBuffer();
    sb.append(rule.lhsString(tagIndex) + " -> ");
    List<Integer> children = rule.getChildren();
    for (int i = 0; i < dot; i++) {
      if(rule instanceof TagRule){
        sb.append(tagIndex.get(children.get(i)) + " ");
      } else {
        sb.append("_" + wordIndex.get(children.get(i)) + " ");
      }
    }
    sb.append(".");
    for (int i = dot; i < children.size(); i++) {
      if(rule instanceof TagRule){        
        sb.append(" " + tagIndex.get(children.get(i)));
      } else {
        sb.append(" " + wordIndex.get(children.get(i)));
      }
      
    }
    return sb.toString();
  }
}

/*** Unused code ***/
//// create a tag edge: tag -> []
//public static Edge createTagEdge(int tag){
//  return new Edge(new Rule(tag, new ArrayList<Integer>()), 0);
//}

//
///** 
//* to edge: mother -> [second child onwards]
//* @return
//*/
//public ActiveEdge getToEdge(){
//return new ActiveEdge(new Edge(edge.getMother(), edge.getChildren(dot+1)), 0);
//}

//protected int left; // location of the left edge
//protected int right; // location of the right edge
//, int left, int right
//this.left = left;
//this.right = right;
//public int getLeft() {
//  return left;
//}
//public int getRight() {
//  return right;
//}
//assert(left<=right);