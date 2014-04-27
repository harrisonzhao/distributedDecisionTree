package tree;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IOUtils;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;
import org.jdom.output.XMLOutputter;

public class Tree {

  private final int outputClassAtIndex;
  private final ArrayList<Attribute> attributes;
  private Node root;

  public Tree(
          Node root, 
          ArrayList<Attribute> attributes, 
          int outputClassAtIndex) 
          throws Exception {
    this.outputClassAtIndex = outputClassAtIndex;
    this.attributes = attributes;
    this.root = root;

    Attribute outputClass = attributes.get(outputClassAtIndex);
    if (!outputClass.isCategorical()) {
      throw new Exception("Numeric output class not supported yet");
    }
  }

  public ArrayList<Attribute> getAttributes() {
    return attributes;
  }

  public int getOutputClassIndex() {
    return outputClassAtIndex;
  }

  public Attribute getOutputClass() {
    return attributes.get(outputClassAtIndex);
  }

  
  public Node evaluateToNode(ArrayList<Object> instance) {
    return root.evaluateToNode(instance);
  }

  public HashMap<String, Integer> createOutputClassIdMap() {
    ArrayList<String> outputClasses
            = new ArrayList<>(this.getOutputClass().getCategorySet());
    HashMap<String, Integer> outputClassIdMap = new HashMap<>();
    for (int i = 0; i < outputClasses.size(); ++i) {
      outputClassIdMap.put(outputClasses.get(i), i);
    }
    return outputClassIdMap;
  }

  /*
   * writes tree to XML element
   */
  public Element toElement() {
    Element treeElement = new Element("tree");
    treeElement.setAttribute(
            "outputClassAtIndex", 
            String.valueOf(outputClassAtIndex));

    Element attributesElement = new Element("attributes");
    for (Attribute attribute : attributes) {
      attributesElement.addContent(attribute.toElement());
    }
    treeElement.addContent(attributesElement);

    treeElement.addContent(root.toElement("root"));

    return treeElement;
  }

  public static Tree fromElement(Element treeElement) throws Exception {
    int outputClassAtIndex = Integer.valueOf(
            treeElement.getAttributeValue("outputClassAtIndex"));

    Element rootElement = treeElement.getChild("root");
    Node root = Node.fromElement(rootElement, outputClassAtIndex, null);

    ArrayList<Attribute> attributes = null;
    Element attributesElement = treeElement.getChild("attributes");
    attributes = new ArrayList<Attribute>();
    List<Element> attributeElements = 
            (List<Element>) attributesElement.getChildren();
    for (Element attributeElement : attributeElements) {
      Attribute attribute = Attribute.fromElement(attributeElement);
      attributes.add(attribute);
    }

    Tree tree = new Tree(root, attributes, outputClassAtIndex);

    return tree;
  }
  
  //load for mapper
  public static Tree loadTree(Mapper.Context context) throws Exception {
    URI[] files = context.getCacheFiles();
    Path path = new Path(files[0].toString());
    Configuration conf = context.getConfiguration();
    FileSystem fs = FileSystem.get(conf);
    FSDataInputStream in = fs.open(path);
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    IOUtils.copyBytes(in, out, conf);
    in.close();
    out.close();
    String[] lines = out.toString().split("\n");

    StringBuilder stringBuilder = new StringBuilder();
    for (String line : lines) {
      stringBuilder.append(line);
    }
    
    SAXBuilder saxBuilder = new SAXBuilder();
    Reader xmlIn = new StringReader(stringBuilder.toString());

    Tree tree = null;
    try {
      Element treeElement = saxBuilder.build(xmlIn).getRootElement();
      tree = Tree.fromElement(treeElement);
    } catch (Exception e) {
      throw new IOException("TreeXml: " + stringBuilder.toString(), e);
    }

    return tree;
  }
  
  //load for reducer
  public static Tree loadTree(Reducer.Context context) throws Exception {
    URI[] files = context.getCacheFiles();
    Path path = new Path(files[0].toString());
    Configuration conf = context.getConfiguration();
    FileSystem fs = FileSystem.get(conf);
    FSDataInputStream in = fs.open(path);
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    IOUtils.copyBytes(in, out, conf);
    in.close();
    out.close();
    String[] lines = out.toString().split("\n");

    StringBuilder stringBuilder = new StringBuilder();
    for (String line : lines) {
      stringBuilder.append(line);
    }
    
    SAXBuilder saxBuilder = new SAXBuilder();
    Reader xmlIn = new StringReader(stringBuilder.toString());

    Tree tree = null;
    try {
      Element treeElement = saxBuilder.build(xmlIn).getRootElement();
      tree = Tree.fromElement(treeElement);
    } catch (Exception e) {
      throw new IOException("TreeXml: " + stringBuilder.toString(), e);
    }

    return tree;
  }
    
  public void writeToFile(FileSystem fs, Path treePath) throws IOException {
    XMLOutputter outputter = new XMLOutputter();
    String treeXml = outputter.outputString(this.toElement());

    File treeFile = new File("tree.xml");
    treeFile.delete();
    System.out.println("Writing tree to: " + treeFile.getAbsolutePath());
    FileWriter writer = new FileWriter(treeFile);
    writer.write(treeXml);
    writer.close();
    fs.copyFromLocalFile(false, true, new Path(treeFile.getPath()), treePath);
  }

  
  @Override
  public String toString() {
    XMLOutputter outputter = new XMLOutputter();
    String output = outputter.outputString(this.toElement());
    return output;
  }
  
  @Override
  public boolean equals(Object obj) {
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    final Tree other = (Tree) obj;
    if (this.outputClassAtIndex != other.outputClassAtIndex) {
      return false;
    }
    if (this.attributes != other.attributes 
            && (this.attributes == null 
            || !this.attributes.equals(other.attributes))) {
      return false;
    }
    if (this.root != other.root 
            && (this.root == null 
            || !this.root.equals(other.root))) {
      return false;
    }
    return true;
  }

  @Override
  public int hashCode() {
    int hash = 7;
    hash = 11 * hash + this.outputClassAtIndex;
    hash = 11 * hash 
            + (this.attributes != null ? this.attributes.hashCode() : 0);
    hash = 11 * hash + (this.root != null ? this.root.hashCode() : 0);
    return hash;
  }
}
