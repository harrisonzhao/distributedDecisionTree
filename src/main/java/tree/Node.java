package tree;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.TreeMap;
import org.jdom.Element;

/**
 * trueChild is yes direction child node
 * falseChild is no direction child node
 */
public class Node {

  private Node parent;
  private int id;
  private Node trueChild;
  private Node falseChild;
  private boolean isLeaf;
  private Split split;
  private TreeMap<String, Long> outputClassCountMap;

  public Node(int id, Node parent) {
    this.id = id;
    this.parent = parent;
    this.isLeaf = false;
  }

  public Node(
          int id, 
          Node parent, 
          TreeMap<String, Long> outputClassCountMap) {
    this(id, parent);
    this.outputClassCountMap = outputClassCountMap;
  }

  public double[] getRange(Node node, Attribute attribute) {
    if (attribute.isCategorical()) {
      return null;
    }

    double[] range;

    if (parent == null) {
      range = new double[2];
      range[0] = attribute.getMinValue();
      range[1] = attribute.getMaxValue();
    } else {
      range = parent.getRange(this, attribute);
      if (split != null && split.getAttributeId() == attribute.getIndex()) {
        if (node == trueChild) {
          range[1] = Math.min(range[1], split.getNumber());
        } else if (node == falseChild) {
          range[0] = Math.max(range[0], split.getNumber());
        }
      }
    }
    return range;
  }

  public long getTotalCount() {
    long count = 0;
    for (Long outputClassCount : outputClassCountMap.values()) {
      count += outputClassCount;
    }
    return count;
  }

  public String getPredictedClass() {
    String predictedClass = null;
    long maxCount = 0;
    for (Entry<String, Long> entry : outputClassCountMap.entrySet()) {
      long count = entry.getValue();
      if (maxCount < count) {
        maxCount = count;
        predictedClass = entry.getKey();
      }
    }
    return predictedClass;
  }

  /**
   * @param instance
   * a training line
   * @return
   * returns self if leaf node
   * else uses split to recursively call a child node evaluation
   */
  public Node evaluateToNode(ArrayList<Object> instance) {
    if (split == null) {
      return this;
    } else {
      if (split.evaluate(instance)) {
        return trueChild.evaluateToNode(instance);
      } else {
        return falseChild.evaluateToNode(instance);
      }
    }
  }

  public void setIsLeaf(boolean isLeaf) {
    this.isLeaf = isLeaf;
  }

  public boolean isLeaf() {
    return isLeaf;
  }

  public int getId() {
    return id;
  }

  public void addSplit(Split split, Node trueChild, Node falseChild) {
    this.split = split;
    this.trueChild = trueChild;
    this.falseChild = falseChild;
  }

  /*
   * format is
   * <[nodename1] id="id" isLeaf="true/false">
   *  <classCounts>
   *   <classCount classCategory="categoryname1" count="countforcategory1"/>
   *   <classCount classCategory="categoryname2" count="countforcategory2"/>
   *  ...
   *  </classCounts>
   *  [split element]
   *  [true child node]
   *  [false child node]
   * </[nodename1]>
   */
  public Element toElement(String nodeName) {
    Element element = new Element(nodeName);
    element.setAttribute("id", String.valueOf(id));
    element.setAttribute("isLeaf", String.valueOf(isLeaf));

    if (outputClassCountMap != null) {
      Element classCounts = new Element("classCounts");
      for (Entry<String, Long> entry : outputClassCountMap.entrySet()) {
        String outputClass = entry.getKey();
        Long count = entry.getValue();
        Element classCount = new Element("classCount");
        classCount.setAttribute("classCategory", outputClass);
        classCount.setAttribute("count", String.valueOf(count));
        classCounts.addContent(classCount);
      }
      element.addContent(classCounts);
    }

    if (split != null) {
      element.addContent(split.toElement());
      element.addContent(trueChild.toElement("trueChild"));
      element.addContent(falseChild.toElement("falseChild"));
    }

    return element;
  }

  public static Node fromElement(
          Element element, 
          int outputClassAtIndex, 
          Node parent) throws Exception {
    int id = Integer.valueOf(element.getAttributeValue("id"));

    Element classCountsElement = element.getChild("classCounts");

    Node node;
    if (classCountsElement != null) {
      TreeMap<String, Long> classCountsMap = new TreeMap<>();
      List<Element> children = 
              (List<Element>) classCountsElement.getChildren("classCount");
      for (Element classCountElement : children) {
        String outputClass = classCountElement.getAttributeValue("classCategory");
        Long count = Long.valueOf(classCountElement.getAttributeValue("count"));
        classCountsMap.put(outputClass, count);
      }
      node = new Node(id, parent, classCountsMap);
    } else {
      node = new Node(id, parent);
    }

    boolean isLeaf = Boolean.valueOf(element.getAttributeValue("isLeaf"));
    node.setIsLeaf(isLeaf);

    Element splitElement = element.getChild("split");
    if (splitElement != null) {
      Split split = Split.fromElement(splitElement);
      Node trueChild = Node.fromElement(
              element.getChild("trueChild"), 
              outputClassAtIndex, 
              node);
      Node falseChild = Node.fromElement(
              element.getChild("falseChild"), 
              outputClassAtIndex, 
              node);
      node.addSplit(split, trueChild, falseChild);
    }

    return node;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    final Node other = (Node) obj;
    if (this.id != other.id) {
      return false;
    }
    if (this.trueChild != other.trueChild 
            && (this.trueChild == null 
            || !this.trueChild.equals(other.trueChild))) {
      return false;
    }
    if (this.falseChild != other.falseChild 
            && (this.falseChild == null 
            || !this.falseChild.equals(other.falseChild))) {
      return false;
    }
    if (this.isLeaf != other.isLeaf) {
      return false;
    }
    if (this.split != other.split 
            && (this.split == null 
            || !this.split.equals(other.split))) {
      return false;
    }
    return true;
  }

  @Override
  public int hashCode() {
    int hash = 7;
    hash = 29 * hash + this.id;
    hash = 29 * hash + (this.trueChild != null ? this.trueChild.hashCode() : 0);
    hash = 29 * hash + (this.falseChild != null ? this.falseChild.hashCode() : 0);
    hash = 29 * hash + (this.split != null ? this.split.hashCode() : 0);
    return hash;
  }
}
