package jobs;

import java.io.IOException;
import java.util.Iterator;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Mapper.Context;
import org.apache.hadoop.mapreduce.Reducer;

public class NodeSplits {

  public static class Map extends Mapper<NullWritable, Text, Text, Text> {
    
    public static String DELIM = ",";
    
    /**
     * @param key
     * does not matter
     * @param value
     * Splits line
     * format is:
     * (leaf,attribute, attributeSplitRule, information gain,
     * [category1:category1counts, category2:category2counts ... ], [cate ...])
     * @param context
     * @throws IOException
     * @throws InterruptedException
     * pops the first token (leaf) off the beginning of splits line
     * emits (leaf node id, edited splits line)
     * information gain at position 2
     */
    @Override
    public void map(NullWritable key, Text value, Context context)
            throws IOException, InterruptedException {
      String[] valueTokens = value.toString().split(DELIM);

      String poppedAttribute = valueTokens[0];

      StringBuilder builder = new StringBuilder();
      for (int i = 1; i < valueTokens.length; i++) {
        builder.append(valueTokens[i]);
        builder.append(DELIM);
      }
      String newValueString = builder.toString();
      newValueString = newValueString.substring(0, newValueString.length() - 1);

      Text newKey = new Text(poppedAttribute);
      Text newValue = new Text(newValueString);

      context.write(newKey, newValue);
    }
  }

  public static class Reduce
          extends Reducer<Text, Text, NullWritable, Text> {
    
    public static String DELIM = ",";
    public static Integer infoGainIndex = 2;
    
    /**
     * @param key
     * leaf node id
     * @param values
     * @param context
     * @throws IOException
     * @throws InterruptedException 
     */
    @Override
    public void reduce(Text key, Iterable<Text> values, Context context)
            throws IOException, InterruptedException {

      double maxInformationGain = -Double.MAX_VALUE;
      Text bestSplit = null;

      Iterator<Text> iter = values.iterator();
      while (iter.hasNext()) {
        Text text = iter.next();
        String tokens[] = text.toString().split(DELIM);
        double informationGain = Double.valueOf(tokens[infoGainIndex]);
        if (informationGain > maxInformationGain || bestSplit == null) {
          maxInformationGain = informationGain;
          bestSplit = new Text(text);
        }
      }

      Text newValue = new Text(key.toString() + DELIM + bestSplit.toString());
      context.write(NullWritable.get(), newValue);
    }
  }
}
