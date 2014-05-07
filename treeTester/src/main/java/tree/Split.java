package tree;

import java.util.ArrayList;
import org.jdom2.Element;

public class Split {

  private final int attributeId;
  private final boolean isCategorical;
  private final String category;
  private final Double number;

  public Split(int attributeId, String category) {
    this.attributeId = attributeId;
    this.category = category;
    isCategorical = true;
    number = null;
  }

  public Split(int attributeId, Double number) {
    this.attributeId = attributeId;
    this.number = number;
    isCategorical = false;
    category = null;
  }

  public int getAttributeId() {
    return attributeId;
  }

  public Double getNumber() {
    return number;
  }
  
  public boolean evaluate(ArrayList<Object> instance) {
    boolean result;
    Object attributeValue = instance.get(attributeId);
    if (isCategorical) {
      result = ((String) attributeValue).equals(category);
    } else {
      result = ((Double) attributeValue) <= number;
    }
    return result;
  }

  /*
   * converts split to xml element
   * for categorical, format is
   * <split attributeId="attributeId" isCategorical="true/false" equalTo="category">
   * for numerical, format is
   * <split attributeId="attributeId" isCategorical="true/false" lessOrEqualTo="number">
   */
  public Element toElement() {
    Element element = new Element("split");
    element.setAttribute("attributeId", String.valueOf(attributeId));
    element.setAttribute("isCategorical", String.valueOf(isCategorical));
    if (isCategorical) {
      element.setAttribute("equalTo", category);
    } else {
      element.setAttribute("lessOrEqualTo", String.valueOf(number));
    }

    return element;
  }

  public static Split fromElement(Element element) {
    int attributeId = Integer.valueOf(element.getAttributeValue("attributeId"));
    boolean isCategorical = 
            Boolean.valueOf(element.getAttributeValue("isCategorical"));

    Split split;
    if (isCategorical) {
      String category = element.getAttributeValue("equalTo");
      split = new Split(attributeId, category);
    } else {
      String numberString = element.getAttributeValue("lessOrEqualTo");
      double splitNumber = Double.valueOf(numberString);
      split = new Split(attributeId, splitNumber);
    }

    return split;
  }
}
