package jobs;

import java.io.IOException;
import java.util.Iterator;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Mapper.Context;
import org.apache.hadoop.mapreduce.Reducer;
import org.jdom2.output.XMLOutputter;
import tree.Attribute;

/**
 * task 1 - only run once
 * defines and counts attributes
 * for numeric attributes - defines a min and max range
 * for categorical attributes - counts all categories
 */
public class DefineAttributes {

  public static class Map
          extends Mapper<LongWritable, Text, IntWritable, Text> {

    private final static String DELIM = ",";
    
    /**
     * @param key
     * does not matter
     * @param value
     * training example
     * @param context
     * @throws IOException
     * @throws InterruptedException 
     * 
     * tokenizes DELIM separated line
     * emits:
     * key - attributeId
     * value - attributeValue
     */
    @Override
    public void map(LongWritable key, Text value, Context context)
            throws IOException, InterruptedException {
      String[] attributes = value.toString().split(DELIM);

      int attributeId = 0;
      for (String attribute : attributes) {
        String token = attribute.trim();
        Text outputValue = null;
        if (!token.isEmpty()) {
          outputValue = new Text(token);
        }

        IntWritable outputKey = new IntWritable(attributeId);
        if (outputValue != null) {
          context.write(outputKey, outputValue);
        }
        ++attributeId;
      }
    }
  }

  public static class Reduce
          extends Reducer<IntWritable, Text, NullWritable, Text> {

    /**
     * @param key
     * attributeId
     * @param values
     * attributeValue
     * @param context
     * @throws IOException
     * @throws InterruptedException
     * 
     * groups by attributeIds
     * outputs attribute as XML element
     * assumes that categorical can't be parsed as numbers
     */
    @Override
    public void reduce(IntWritable key, Iterable<Text> values, Context context)
            throws IOException, InterruptedException {
      
      try {
        Attribute attributeDefinition = new Attribute(key.get());

        Iterator<Text> iter = values.iterator();
        while (iter.hasNext()) {
          Text text = iter.next();
          try {
            double number = Double.valueOf(text.toString());
            attributeDefinition.addNumericValue(number);
          } catch (NumberFormatException e) {
            attributeDefinition.addCategoricalValue(text.toString());
          }
        }

        XMLOutputter outputter = new XMLOutputter();
        String xmlString = 
                outputter.outputString(attributeDefinition.toElement());
        Text outputValue = new Text(xmlString);
        System.out.println(xmlString);
        context.write(NullWritable.get(), outputValue);
      } catch (Exception e) {
        throw new IOException(e);
      }
    }
  }
}
