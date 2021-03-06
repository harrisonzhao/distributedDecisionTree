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
import org.apache.hadoop.filecache.DistributedCache;
import org.jdom2.Element;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;

public class Tree {

  private final int outputClassAtIndex;
  private final ArrayList<Attribute> attributes;
  private final Node root;

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

  //recursively evaluates a line to leaf node
  public Node evaluateToNode(ArrayList<Object> instance) {
    return root.evaluateToNode(instance);
  }

  //maps each output class to an id
  public HashMap<String, Integer> createOutputClassIdMap() {
    ArrayList<String> outputClasses
            = new ArrayList<>(this.getOutputClass().getCategorySet());
    HashMap<String, Integer> outputClassIdMap = new HashMap<>();
    for (int i = 0; i < outputClasses.size(); ++i) {
      outputClassIdMap.put(outputClasses.get(i), i);
    }
    return outputClassIdMap;
  }

  // writes tree to XML element
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
  
  //used to load tree into memory from xml file using distributedCache
  public static Tree loadTree(Configuration conf) throws Exception {
    URI[] files = DistributedCache.getCacheFiles(conf);
    Path path = new Path(files[0].toString());
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
    outputter.setFormat(Format.getPrettyFormat());
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

}
