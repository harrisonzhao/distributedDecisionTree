package decisiontree;

import tree.Attribute;
import java.util.ArrayList;
import java.util.TreeMap;

public class Utilities {

  private static final double LOG2 = Math.log(2);
  public static final int NUMERIC_SPLITS = 10000;
  public static final int SPLIT_FLOOR = 100;
  private static final String DELIM = ",";
  private static final String DELIM1 = "@";
  private static final String DELIM2 = ";";
  
  public static TreeMap<String, Long> getCategoryCounts(
          String categoryCountString) {
    TreeMap<String, Long> countMap = new TreeMap<>();
    String[] categoryCountTokens = categoryCountString.split(DELIM2);

    for (String categoryCountToken : categoryCountTokens) {
      String[] tokens = categoryCountToken.split(DELIM1);
      String category = tokens[0];
      Long count = Long.valueOf(tokens[1]);
      countMap.put(category, count);
    }

    return countMap;
  }
  /**
   * @param instanceLine
   * @param attributes
   * @return
   * @throws Exception 
   * takes a training instance, tokenizes it, and loads into arraylist
   */
  public static ArrayList<Object> instanceLineToArrayList(
          String instanceLine, 
          ArrayList<Attribute> attributes) 
          throws Exception {
    ArrayList<Object> values = new ArrayList<>();
    String[] tokens = instanceLine.split(DELIM);
    if (tokens.length != attributes.size()) {
      throw new Exception("instance has " 
              + tokens.length 
              + " tokens, expected " 
              + attributes.size() 
              + "- instance: " 
              + instanceLine);
    }

    for (int i = 0; i < tokens.length; i++) {
      String token = tokens[i];
      token = token.trim();
      if (attributes.get(i).isCategorical()) {
        values.add(token);
      } else {
        values.add(Double.valueOf(token));
      }
    }

    return values;
  }

  /**
   * @param categories
   * @param counts
   * @return
   * 
   * DELIM1 = "@"
   * DELIM2 = ";"
   */
  public static String printCounts(
          ArrayList<String> categories, 
          Long[] counts) {
    StringBuilder builder = new StringBuilder();
    for (int i = 0; i < counts.length; i++) {
      builder.append(categories.get(i));
      builder.append(DELIM1);
      builder.append(counts[i]);
      builder.append(DELIM2);
    }

    String retString = builder.toString();
    //get rid of final ;
    retString = retString.substring(0, retString.length() - 1);
    return retString;
  }

  /**
   * @param counts
   * @return 
   */
  public static long sumCounts(Long[] counts) {
    long sumCounts = 0;
    for (Long count : counts) {
      sumCounts += count;
    }
    return sumCounts;
  }

  public static Long[] addCounts(
          Long[] targetCounts, 
          Long[] sourceCounts) {
    for (int i = 0; i < targetCounts.length; i++) {
      targetCounts[i] += sourceCounts[i];
    }
    return targetCounts;
  }

  /**
   * @param targetCounts
   * @param sourceCounts
   * @return 
   */
  public static Long[] subtractCounts(
          Long[] targetCounts, 
          Long[] sourceCounts) {
    for (int i = 0; i < targetCounts.length; i++) {
      targetCounts[i] -= sourceCounts[i];
    }

    return targetCounts;
  }

  //calculate entropy, defined as -sum p*log(p)
  //where p is probability = count / total instance count
  public static double calculateEntropy(
          Long[] counts, 
          Long instanceCount) {
    if (instanceCount == 0) {
      return 0.0;
    }
    double entropy = 0.0;
    double invDataSize = 1.0 / instanceCount;
    for (Long count : counts) {
      if (count == 0) {
        continue; // otherwise returns NaN
      }
      double p = count * invDataSize;
      entropy += -p * Math.log(p) / LOG2;
    }
    return entropy;
  }

  //calculates information gain by a training dataset for a given attribute
  //attribute S
  //T_s is the counts for the subset of training set induced by S
  //T_sv is counts for the subset of training set 
  //in which attribute S has value v
  //InfoGain = Entropy(S)-sum((T_sv/T_s)*Entropy(S_c))
  public static double findInformationGain(
          Long[] originalCounts, 
          Long[] trueCounts, 
          Long[] falseCounts) {
    long originalTotalCount = sumCounts(originalCounts);
    double invDataSize = 1.0 / originalTotalCount;
    double originalEntropy = 
            calculateEntropy(originalCounts, originalTotalCount);
    long trueInstanceCount = sumCounts(trueCounts);
    long falseInstanceCount = originalTotalCount - trueInstanceCount;
    double informationGain = originalEntropy;
    informationGain 
            -= trueInstanceCount 
            * invDataSize 
            * calculateEntropy(trueCounts, trueInstanceCount);
    informationGain 
            -= falseInstanceCount 
            * invDataSize 
            * calculateEntropy(falseCounts, falseInstanceCount);
    return informationGain;
  }
}
