package treetester;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.Reader;
import java.util.ArrayList;
import org.jdom2.Element;
import org.jdom2.input.SAXBuilder;
import tree.Node;
import tree.Tree;

public class EvaluateTree {

  static String DELIM = ",";
  public static void main(String[] args) throws Exception {

    if (args.length != 2) {
      System.out.println("Expected arguments: <treeXML> <testCSV>");
      return;
    }
    SAXBuilder saxBuilder = new SAXBuilder();
    Reader xmlIn = new FileReader(args[0]);
    Element treeElement = saxBuilder.build(xmlIn).getRootElement();
    Tree tree = Tree.fromElement(treeElement);
    BufferedReader reader = new BufferedReader(new FileReader(args[1]));
    long errors = 0;
    long total = 0;
    while (reader.ready()) {
      String line = reader.readLine();
      line = line.trim();
      if (line.isEmpty()) {
        continue;
      }
      String[] tokens = line.split(DELIM);
      ArrayList<Object> instance = new ArrayList<Object>();
      int index = 0;
      for (String token : tokens) {
        Object value;
        if (index == tree.getOutputClassIndex()) {
          value = token;
        } else {
          try {
            value = Double.valueOf(token);
          } catch (NumberFormatException e) {
            value = token;
          }
        }
        instance.add(value);
        ++index;
      }

      Node node = tree.evaluateToNode(instance);
      String predictedClass = node.getPredictedClass();
      String actualClass = (String) instance.get(tree.getOutputClassIndex());

      if (!predictedClass.equals(actualClass))
        errors++;
      total++;
    }
    long correct = total - errors;
    double accuracy = (double) correct / (double) total;
    accuracy *= 100;
    System.out.println("Result: correct: " + correct + "/ total: " + 
            total + " - accuracy: " + accuracy);

  }
}
