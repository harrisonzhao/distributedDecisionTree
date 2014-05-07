package tree;

import java.util.List;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import org.jdom2.Element;
import org.jdom2.output.XMLOutputter;

/**
 * Attribute for dataset
 * either numerical (continuous) or categorical (discrete)
 * sorted by index when outputting to xml file
 */
public class Attribute implements Comparable<Attribute> {

  private int index;
  private Boolean isCategorical;
  private double minValue;
  private double maxValue;
  private long count;
  private TreeMap<String, Long> categoryMap;

  public Attribute(int index) {
    this.index = index;
    minValue = Double.MAX_VALUE;
    maxValue = -Double.MAX_VALUE;
    categoryMap = new TreeMap<>();
    isCategorical = null;
    count = 0;
  }

  public int getIndex() {
    return index;
  }

  public boolean isCategorical() {
    return isCategorical;
  }

  public double getMaxValue() {
    return maxValue;
  }

  public double getMinValue() {
    return minValue;
  }

  public Set<String> getCategorySet() {
    return categoryMap.keySet();
  }

  public TreeMap<String, Long> getCategoryMap() {
    return categoryMap;
  }

  public String getMostCommonCategory() {
    String mostCommonCategory = null;
    long maxCategoryCount = -Long.MAX_VALUE;
    for (Entry<String, Long> entry : categoryMap.entrySet()) {
      String category = entry.getKey();
      Long categoryCount = entry.getValue();

      if (categoryCount > maxCategoryCount) {
        mostCommonCategory = category;
        maxCategoryCount = categoryCount;
      }
    }
    return mostCommonCategory;
  }

  public void addCategoricalValue(String category) throws Exception {
    if (isCategorical != null && !isCategorical) {
      throw new Exception("Attribute " + index + 
              " is numeric.  Can't accept: " + category);
    } else {
      if (isCategorical == null) {
        isCategorical = true;
      }
      incrementCategory(category);
      this.count++;
    }
  }

  /**
   * @param number
   * @throws Exception 
   * 
   * adds a number and updates the min/max range for attribute
   * sum used for debugging purposes
   */
  public void addNumericValue(double number) throws Exception {
    if (isCategorical != null && isCategorical) {
      throw new Exception("Attribute " + index + 
              " is categorical.  Can't accept: " + number);
    } else {
      if (isCategorical == null) {
        isCategorical = false;
      }
      minValue = Math.min(number, minValue);
      maxValue = Math.max(number, maxValue);
      this.count++;
    }
  }

  /*
   * converts attribute to an XML element
   * for numerical:
   * <attribute index="index" isCategorical="false" min="min" max="max"/>
   * where min = minValue and max = maxValue
   * for categorical:
   * <attribute index="index" isCategorical="true">
   *  <category value="category1" count="countforcategory1"/>
   * </attribute>
   */
  public Element toElement() {
    Element element = new Element("attribute");
    element.setAttribute("index", String.valueOf(index));
    element.setAttribute("isCategorical", String.valueOf(isCategorical));
    if (isCategorical) {
      for (Entry<String, Long> entry : categoryMap.entrySet()) {
        String category = entry.getKey();
        Long categoryCount = entry.getValue();
        Element categoryElement = new Element("category");
        categoryElement.setAttribute("value", category);
        categoryElement.setAttribute("count", String.valueOf(categoryCount));
        element.addContent(categoryElement);
      }
    } else {
      element.setAttribute("minValue", String.valueOf(minValue));
      element.setAttribute("maxValue", String.valueOf(maxValue));
    }
    element.setAttribute("count", String.valueOf(count));

    return element;
  }

  private void incrementCategory(String category) {
    Long categoryCount = categoryMap.get(category);
    if (categoryCount == null) {
      categoryCount = 1l;
    } else {
      categoryCount++;
    }
    categoryMap.put(category, categoryCount);
  }

  private void setCategoryEntry(String category, Long categoryCount) {
    categoryMap.put(category, categoryCount);
  }

  //called when loading from element
  private void setNumericInfo(double minValue, double maxValue) {
    this.minValue = minValue;
    this.maxValue = maxValue;
  }

  private void setCount(long count) {
    this.count = count;
  }

  private void setCategorical(boolean isCategorical) {
    this.isCategorical = isCategorical;
  }

  public static Attribute fromElement(Element element) throws Exception {
    int index = Integer.valueOf(element.getAttributeValue("index"));
    Attribute attr = new Attribute(index);

    long count = Long.valueOf(element.getAttributeValue("count"));
    attr.setCount(count);

    boolean isCategorical = 
            Boolean.valueOf(element.getAttributeValue("isCategorical"));
    attr.setCategorical(isCategorical);

    if (isCategorical) {
      List<Element> children = (List<Element>) element.getChildren("category");
      for (Element child : children) {
        String categoryValue = child.getAttributeValue("value");
        Long categoryCount = Long.valueOf(child.getAttributeValue("count"));
        attr.setCategoryEntry(categoryValue, categoryCount);
      }
    } else {
      double minValue = Double.valueOf(element.getAttributeValue("minValue"));
      double maxValue = Double.valueOf(element.getAttributeValue("maxValue"));

      attr.setNumericInfo(minValue, maxValue);
    }

    return attr;
  }

  @Override
  public String toString() {
    XMLOutputter outputter = new XMLOutputter();
    String output = outputter.outputString(this.toElement());
    return output;
  }

  @Override
  public int compareTo(Attribute attr) {
    return Integer.valueOf(
            this.getIndex()).compareTo(attr.getIndex());
  }
}
