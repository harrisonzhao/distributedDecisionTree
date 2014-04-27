package decisiontree;

import jobs.*;
import tree.*;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IOUtils;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.util.GenericOptionsParser;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;

public class DecisionTreeBuilder {

  public static void main(String[] args) throws Exception {
    Configuration conf = new Configuration();

    Map<String, String> env = System.getenv();
    Path coreSiteXml = new Path(env.get("HADOOP_CONF_DIR")+"/core-site.xml");
    Path hdfsSiteXml = new Path(env.get("HADOOP_CONF_DIR")+"/hdfs-site.xml");
    Path yarnSiteXml = new Path(env.get("HADOOP_CONF_DIR")+"/yarn-site.xml");
    Path mapredSiteXml = new Path(env.get("HADOOP_CONF_DIR")+"/mapred-site.xml");
    conf.addResource(coreSiteXml);
    conf.addResource(hdfsSiteXml);
    conf.addResource(yarnSiteXml);
    conf.addResource(mapredSiteXml);
    
    String[] otherArgs = 
            new GenericOptionsParser(conf, args).getRemainingArgs();
    if (otherArgs.length != 3) {
      System.err.println("Usage: decisiontree <in> <out> outputClassAtIndex");
      System.exit(2);
    }

    Path inputPath = new Path(otherArgs[0]);
    Path outputPath = new Path(otherArgs[1]);
    int outputClassAtIndex = Integer.valueOf(otherArgs[2]);
    Path dataPath = inputPath;
    Path attributePath = new Path(outputPath, "attributes");
    Path categorySplitsPath = new Path(outputPath, "categorySplits");
    Path attributeSplitsPath = new Path(outputPath, "attributeSplits");
    Path treePath = new Path(outputPath, "tree/tree.xml");

    Job defineAttributesJob = setupDefineAttributesJob(
            conf, inputPath, attributePath);
    boolean result = defineAttributesJob.waitForCompletion(true);
    if (!result) {
      System.out.println("Failed on defineAttributesJob");
      System.exit(1);
    }

    //get list of attributes from defineAttributesJob output file
    //get outputClass (category, count) map and initialize tree root using it
    ArrayList<Attribute> attributes = 
            readAttributeDefinitions(conf, attributePath);
    TreeMap<String, Long> outputClassCategoryCounts = 
            attributes.get(outputClassAtIndex).getCategoryMap();
    HashMap<Integer, Node> nodeMap = new HashMap<>();
    Node treeRoot = new Node(nodeMap.size(), null, outputClassCategoryCounts);
    nodeMap.put(treeRoot.getId(), treeRoot);
    Tree tree = new Tree(treeRoot, attributes, outputClassAtIndex);
    FileSystem fs = FileSystem.get(conf);
    tree.writeToFile(fs, treePath);

    boolean grewTree = true;
    while (grewTree) {

      Job categorySplitsJob = findBestCategorySplitJob(
              conf, dataPath, categorySplitsPath);
      result = categorySplitsJob.waitForCompletion(true);
      if (!result) {
        System.out.println("failed on findBestCategorySplitJob");
        System.exit(1);
      }

      Job attributeSplitsJob = findBestAttributeSplitJob(
              conf, categorySplitsPath, attributeSplitsPath, treePath);
      result = attributeSplitsJob.waitForCompletion(true);
      if (!result) {
        System.out.println("failed on findBestAttributeSplitJob");
        System.exit(1);
      }
      
      grewTree = readNewSplits(tree, nodeMap, conf, attributeSplitsPath);
      
      tree.writeToFile(fs, treePath);
      
      fs.delete(categorySplitsPath, true);
      fs.delete(attributeSplitsPath, true);
    }

    System.exit(0);
  }
  
  /*
   * maps (X, trainingline) -> (attributeIndex, attributeValue)
   * attributeIndex is index attribute is at after tokenizing line
   * reduces (attributeIndex, attributeValue) 
   * -> writes attribute + category counts or min-max range to XML file
   */
  private static Job setupDefineAttributesJob(
          Configuration conf, 
          Path inputPath, 
          Path outputPath) 
          throws IOException {
    Job defineAttributesJob = new Job(conf, "define attributes");
    defineAttributesJob.setJarByClass(DecisionTreeBuilder.class);
    defineAttributesJob.setMapperClass(DefineAttributes.Map.class);
    defineAttributesJob.setReducerClass(DefineAttributes.Reduce.class);
    defineAttributesJob.setMapOutputKeyClass(IntWritable.class);
    defineAttributesJob.setMapOutputValueClass(Text.class);

    defineAttributesJob.setOutputKeyClass(NullWritable.class);
    defineAttributesJob.setOutputValueClass(Text.class);

    System.out.println(inputPath);
    System.out.println(outputPath);
    FileInputFormat.addInputPath(defineAttributesJob, inputPath);
    FileOutputFormat.setOutputPath(defineAttributesJob, outputPath);

    return defineAttributesJob;
  }
  
  /**
   * finds best split of each tree node - attribute pair
   */
  private static Job findBestCategorySplitJob(
          Configuration conf, 
          Path inputPath, 
          Path outputPath) 
          throws IOException {
    Job categorySplitJob = new Job(conf, "best category splits");
    categorySplitJob.setJarByClass(DecisionTreeBuilder.class);
    categorySplitJob.setMapperClass(NodeAttributePairSplits.Map.class);
    categorySplitJob.setReducerClass(NodeAttributePairSplits.Reduce.class);

    categorySplitJob.setMapOutputKeyClass(Text.class);
    categorySplitJob.setMapOutputValueClass(Text.class);
    categorySplitJob.setOutputKeyClass(NullWritable.class);
    categorySplitJob.setOutputValueClass(Text.class);

    System.out.println(inputPath);
    System.out.println(outputPath);
    FileInputFormat.addInputPath(categorySplitJob, inputPath);
    FileOutputFormat.setOutputPath(categorySplitJob, outputPath);

    return categorySplitJob;
  }
  
  private static Job findBestAttributeSplitJob(
          Configuration conf, 
          Path inputPath, 
          Path outputPath,
          Path treePath) 
          throws IOException {
    Job attributeSplitJob = new Job(conf, "best attribute splits");
    attributeSplitJob.addCacheFile(treePath.toUri());
    attributeSplitJob.setJarByClass(DecisionTreeBuilder.class);
    attributeSplitJob.setMapperClass(NodeSplits.Map.class);
    attributeSplitJob.setReducerClass(NodeSplits.Reduce.class);

    attributeSplitJob.setMapOutputKeyClass(Text.class);
    attributeSplitJob.setMapOutputValueClass(Text.class);
    attributeSplitJob.setOutputKeyClass(NullWritable.class);
    attributeSplitJob.setOutputValueClass(Text.class);

    System.out.println(inputPath);
    System.out.println(outputPath);
    FileInputFormat.addInputPath(attributeSplitJob, inputPath);
    FileOutputFormat.setOutputPath(attributeSplitJob, outputPath);

    return attributeSplitJob;
  }

  /**
   * loads XML file for attribute definitions
   * @param conf
   * @param inputPath
   * @return ArrayList of Attributes sorted by index
   * @throws Exception
   */
  private static ArrayList<Attribute> readAttributeDefinitions(
          Configuration conf, 
          Path inputPath) 
          throws Exception {
    FileSystem fs = FileSystem.get(conf);
    FileStatus[] ls = fs.listStatus(inputPath);

    ArrayList<String> allLines = new ArrayList<>();
    for (FileStatus fileStatus : ls) {
      if (fileStatus.getPath().getName().startsWith("part")) {
        FSDataInputStream in = fs.open(fileStatus.getPath());
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        IOUtils.copyBytes(in, out, conf);
        in.close();
        out.close();

        String[] lines = out.toString().split("\n");
        allLines.addAll(Arrays.asList(lines));
      }
    }

    ArrayList<Attribute> allAttributes = new ArrayList<>();

    SAXBuilder builder = new SAXBuilder();
    for (String line : allLines) {
      if (line.isEmpty()) {
        continue;
      }

      Reader in = new StringReader(line);
      Element attributeElement = builder.build(in).getRootElement();
      Attribute attribute = Attribute.fromElement(attributeElement);
      allAttributes.add(attribute);
    }

    Collections.sort(allAttributes);
    return allAttributes;
  }

  private static Boolean readNewSplits(
          Tree tree, 
          HashMap<Integer, Node> nodeMap, 
          Configuration conf, 
          Path inputPath) throws Exception {
    boolean grewTree = false;
    FileSystem fs = FileSystem.get(conf);
    FileStatus[] ls = fs.listStatus(inputPath);

    ArrayList<String> allLines = new ArrayList<>();
    for (FileStatus fileStatus : ls) {
      if (fileStatus.getPath().getName().startsWith("part")) {
        FSDataInputStream in = fs.open(fileStatus.getPath());
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        IOUtils.copyBytes(in, out, conf);
        in.close();
        out.close();

        String[] lines = out.toString().split("\n");
        allLines.addAll(Arrays.asList(lines));
      }
    }

    long newLeafCount = 0;
    for (String line : allLines) {
      if (line.isEmpty()) {
        continue;
      }

      String[] tokens = line.split(",");
      int nodeId = Integer.valueOf(tokens[0]);
      Node node = nodeMap.get(nodeId);
      double informationGain = Double.valueOf(tokens[3]);

      if (informationGain <= 0.0) {
        if (!node.isLeaf()) {
          newLeafCount += node.getTotalCount();
          node.setIsLeaf(true);
        }
      } else {
        int attributeId = Integer.valueOf(tokens[1]);
        Attribute attribute = tree.getAttributes().get(attributeId);

        String splitValueString = tokens[2];
        Split split;
        if (attribute.isCategorical()) {
          split = new Split(attributeId, splitValueString);
        } else {
          double splitNumber = Double.valueOf(splitValueString);
          split = new Split(attributeId, splitNumber);
        }

        TreeMap<String, Long> trueChildClassCounts = 
                Utilities.getCategoryCounts(tokens[4]);
        TreeMap<String, Long> falseChildClassCounts = 
                Utilities.getCategoryCounts(tokens[5]);

        Node trueChild = new Node(nodeMap.size(), node, trueChildClassCounts);
        nodeMap.put(trueChild.getId(), trueChild);
        Node falseChild = new Node(nodeMap.size(), node, falseChildClassCounts);
        nodeMap.put(falseChild.getId(), falseChild);

        node.addSplit(split, trueChild, falseChild);
        grewTree = true;

        System.out.println("New Split: " + line);
      }

    }

    return grewTree;
  }


}
