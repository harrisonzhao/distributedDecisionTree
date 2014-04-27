package jobs;

import tree.*;
import decisiontree.Utilities;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.TreeMap;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;


//finds best split for each tree node-attribute pair
//utilizes distributed caching to share tree file among all processes
public class NodeAttributePairSplits {
  
  private static final String DELIM = ",";
  
  public static class Map extends Mapper<LongWritable, Text, Text, Text> {

    private Tree tree;

    @Override
    protected void setup(Mapper.Context context) 
            throws IOException, InterruptedException {
      super.setup(context);
      try {
        this.tree = Tree.loadTree(context);
      } catch (Exception e) {
        System.out.println("Couldn't load tree");
        throw new IOException(e);
      }
    }

    /**
     * @param key
     * does not matter
     * @param value
     * training line instance
     * @param context
     * @throws IOException
     * @throws InterruptedException
     * output key is (leafnodeId,attributeId) if categorical
     * or output key is (leafnodeId,attributeId,minval,maxval) if numerical
     * output value is (attributeValue,outputClass)
     */
    @Override
    protected void map(LongWritable key, Text value, Context context)
            throws IOException, InterruptedException {

      String instanceString = value.toString();
      instanceString = instanceString.trim();
      if (instanceString.isEmpty()) {
        return;
      }

      ArrayList<Object> instance;
      try {
        instance = Utilities.instanceLineToArrayList(
                instanceString, tree.getAttributes());
      } catch (Exception e) {
        throw new IOException(e);
      }


      //evaluates training instance to a leaf node to grow
      //only evaluates as leaf node if already output correct output class
      Node node = tree.evaluateToNode(instance);
      if (node.isLeaf()) {
        return;
      }
      
      int leafId = node.getId();
      //get the outputClass Category
      String outputClassString = 
              instance.get(tree.getOutputClassIndex()).toString();

      //foreach attribute != outputClass Attribute
      for (int attributeId = 0
              ; attributeId < tree.getAttributes().size()
              ; attributeId++) {
        if (attributeId == tree.getOutputClassIndex()) {
          continue;
        }

        String keyString = String.valueOf(leafId) 
                + DELIM + String.valueOf(attributeId);

        Attribute attribute = tree.getAttributes().get(attributeId);
        if (!attribute.isCategorical()) {
          double[] range = node.getRange(node, attribute);
          keyString += DELIM + range[0] + DELIM + range[1];
        }

        String valueString = instance.get(attributeId).toString() 
                + DELIM + outputClassString;

        context.write(new Text(keyString), new Text(valueString));
      }

    }
  }

  public static class Reduce
          extends Reducer<Text, Text, NullWritable, Text> {

    private Tree tree;

    @Override
    protected void setup(Reducer.Context context) 
            throws IOException, InterruptedException {
      super.setup(context);
      try {
        this.tree = Tree.loadTree(context);
      } catch (Exception e) {
        throw new IOException(e);
      }
    }

    /**
     * @param key
     * output of map
     * will have 4 values if numerical, 2 if categorical
     * @param values
     * @param context
     * @throws IOException
     * @throws InterruptedException
     * outputs 
     * value of (leafnodeid, attributeId, attributeSplitRule, informationGain
     * [category1:category1counts, category2:category2counts ... ], [cate ...])
     */
    @Override
    public void reduce(Text key, Iterable<Text> values, Context context)
            throws IOException, InterruptedException {

      String[] keyTokens = key.toString().split(DELIM);
      int attributeId = Integer.valueOf(keyTokens[1]);
      Attribute attribute = tree.getAttributes().get(attributeId);
      boolean isCategorical = attribute.isCategorical();

      String result;
      Text newText;
      if (isCategorical) {
        result = reduceForCategorical(attribute, values);
        newText = key;
      } else {
        double[] rangeValues = new double[2];
        int leafId = Integer.valueOf(keyTokens[0]);
        rangeValues[0] = Double.valueOf(keyTokens[2]);
        rangeValues[1] = Double.valueOf(keyTokens[3]);
        result = reduceForNumeric(rangeValues, values);
        newText = new Text(
                String.valueOf(leafId) + DELIM + String.valueOf(attributeId));
      }

      context.write(NullWritable.get(), 
              new Text(newText.toString() + DELIM + result));

    }

    
    private String reduceForCategorical(
            Attribute attribute, 
            Iterable<Text> values) {

      int outputClassCount = tree.getOutputClass().getCategorySet().size();

      ArrayList<String> outputClasses = 
              new ArrayList<>(tree.getOutputClass().getCategorySet());
      HashMap<String, Integer> outputClassIdMap = tree.createOutputClassIdMap();

      Long[] originalCounts = new Long[outputClassCount];
      Arrays.fill(originalCounts, 0l);

      HashMap<String, Long[]> splitCounts = new HashMap<>();
      //iterate through all categories for given attribute
      for (String split : attribute.getCategorySet()) {
        Long[] countArray = new Long[outputClassCount];
        Arrays.fill(countArray, 0l);
        splitCounts.put(split, countArray);
      }

      Iterator<Text> iter = values.iterator();
      while (iter.hasNext()) {
        Text textValue = iter.next();
        String[] tokens = textValue.toString().split(DELIM);
        String attributeValue = tokens[0];
        String targetCategory = tokens[1];

        int categoryId = outputClassIdMap.get(targetCategory);
        Long[] counts = splitCounts.get(attributeValue);
        counts[categoryId]++;
        originalCounts[categoryId]++;
      }

      double maxInformationGain = -Double.MAX_VALUE;
      String bestSplitValue = null;
      Long[] bestEqualToCounts = null;
      Long[] bestNotEqualToCounts = null;

      for (Entry<String, Long[]> possibleSplitEntry : splitCounts.entrySet()) {
        String possibleSplit = possibleSplitEntry.getKey();

        Long[] equalToCounts = possibleSplitEntry.getValue();
        Long[] notEqualToCounts = Utilities.subtractCounts(
                originalCounts.clone(), equalToCounts);

        double informationGain = Utilities.findInformationGain(
                originalCounts, equalToCounts, notEqualToCounts);
        long equalToInstanceCount = Utilities.sumCounts(equalToCounts);
        long notEqualToInstanceCount = Utilities.sumCounts(notEqualToCounts);

        if (informationGain > maxInformationGain
                && equalToInstanceCount > Utilities.SPLIT_FLOOR
                && notEqualToInstanceCount > Utilities.SPLIT_FLOOR) {
          maxInformationGain = informationGain;
          bestSplitValue = possibleSplit;
          bestEqualToCounts = equalToCounts.clone();
          bestNotEqualToCounts = notEqualToCounts.clone();
        }
      }

      String result = getDefaultReduceResult(outputClasses);

      if (maxInformationGain != -Double.MAX_VALUE) {
        StringBuilder builder = new StringBuilder();

        builder.append(String.valueOf(bestSplitValue));
        builder.append(DELIM);
        builder.append(String.valueOf(maxInformationGain));
        builder.append(DELIM);
        builder.append(Utilities.printCounts(outputClasses, bestEqualToCounts));
        builder.append(DELIM);
        builder.append(Utilities.printCounts(
                outputClasses, bestNotEqualToCounts));
        result = builder.toString();
      }
      return result;
    }

    private String reduceForNumeric(
            double[] rangeValues, Iterable<Text> values) {

      double range = rangeValues[1] - rangeValues[0];
      int splitCount = Utilities.NUMERIC_SPLITS;
      int bucketCount = splitCount + 1;
      double bucketSize = range / (double) bucketCount;

      int outputClassCount = tree.getOutputClass().getCategorySet().size();

      ArrayList<String> outputClasses = new ArrayList<>(
              tree.getOutputClass().getCategorySet());
      HashMap<String, Integer> outputClassIdMap = tree.createOutputClassIdMap();

      Long[] originalCounts = new Long[outputClassCount];
      Arrays.fill(originalCounts, 0l);

      TreeMap<Double, Long[]> bucketCounts = new TreeMap<>();
      for (int i = 1; i <= bucketCount; i++) {
        double bucketCeiling = rangeValues[0] + (i * bucketSize);
        Long[] countArray = new Long[outputClassCount];
        Arrays.fill(countArray, 0l);
        bucketCounts.put(bucketCeiling, countArray);
      }

      Iterator<Text> iter = values.iterator();
      while (iter.hasNext()) {
        Text textValue = iter.next();
        String[] tokens = textValue.toString().split(DELIM);
        double attributeValue = Double.valueOf(tokens[0]);
        String targetCategory = tokens[1];

        int categoryId = outputClassIdMap.get(targetCategory);
        Entry<Double, Long[]> entry = bucketCounts.ceilingEntry(attributeValue);
        if (entry != null) {
          Long[] counts = entry.getValue();
          counts[categoryId]++;
          originalCounts[categoryId]++;
        }

      }

      long totalInstanceCount = Utilities.sumCounts(originalCounts);

      long lessThanInstanceCount = 0;
      Long[] lessThanCounts = new Long[outputClassCount];
      Arrays.fill(lessThanCounts, 0l);

      long greaterThanInstanceCount = totalInstanceCount;
      Long[] greaterThanCounts = new Long[outputClassCount];
      Arrays.fill(greaterThanCounts, 0l);
      Utilities.addCounts(greaterThanCounts, originalCounts);

      double maxInformationGain = -Double.MAX_VALUE;
      double bestSplitValue = -Double.MAX_VALUE;
      Long[] bestLessThanCounts = null;
      Long[] bestGreaterThanCounts = null;

      for (Entry<Double, Long[]> possibleSplitEntry : bucketCounts.entrySet()){
        double possibleSplit = possibleSplitEntry.getKey();
        Long[] counts = possibleSplitEntry.getValue();

        lessThanCounts = Utilities.addCounts(lessThanCounts, counts);
        greaterThanCounts = Utilities.subtractCounts(greaterThanCounts, counts);

        double informationGain = Utilities.findInformationGain(
                originalCounts, lessThanCounts, greaterThanCounts);

        long instanceCount = Utilities.sumCounts(counts);
        greaterThanInstanceCount -= instanceCount;
        lessThanInstanceCount += instanceCount;

        if (informationGain > maxInformationGain
                && lessThanInstanceCount > Utilities.SPLIT_FLOOR
                && greaterThanInstanceCount > Utilities.SPLIT_FLOOR) {
          maxInformationGain = informationGain;
          bestSplitValue = possibleSplit;
          bestLessThanCounts = lessThanCounts.clone();
          bestGreaterThanCounts = greaterThanCounts.clone();
        }
      }

      String result = getDefaultReduceResult(outputClasses);

      if (maxInformationGain != -Double.MAX_VALUE) {
        StringBuilder builder = new StringBuilder();

        builder.append(String.valueOf(bestSplitValue));
        builder.append(DELIM);
        builder.append(String.valueOf(maxInformationGain));
        builder.append(DELIM);
        builder.append(Utilities.printCounts(
                outputClasses, bestLessThanCounts));
        builder.append(DELIM);
        builder.append(Utilities.printCounts(
                outputClasses, bestGreaterThanCounts));
        result = builder.toString();
      }
      return result;
    }

    private static String getDefaultReduceResult(ArrayList<String> categories) {
      Long[] counts = new Long[categories.size()];
      Arrays.fill(counts, 0l);

      StringBuilder builder = new StringBuilder();
      builder.append(String.valueOf(-Double.MAX_VALUE));
      builder.append(DELIM);
      builder.append(String.valueOf(-Double.MAX_VALUE));
      builder.append(DELIM);
      builder.append(Utilities.printCounts(categories, counts));
      builder.append(DELIM);
      builder.append(Utilities.printCounts(categories, counts));
      return builder.toString();
    }
  }
}
