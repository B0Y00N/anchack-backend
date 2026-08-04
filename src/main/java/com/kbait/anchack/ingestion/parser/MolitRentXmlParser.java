package com.kbait.anchack.ingestion.parser;

import com.kbait.anchack.ingestion.client.MolitRentApiCategory;
import com.kbait.anchack.ingestion.dto.external.MolitRentPage;
import com.kbait.anchack.ingestion.dto.external.RawRentalTransaction;
import com.kbait.anchack.ingestion.exception.MolitRentApiException;
import com.kbait.anchack.ingestion.exception.MolitRentParseException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

public class MolitRentXmlParser {

    public MolitRentPage parse(MolitRentApiCategory apiCategory, String xml) {
        try {
            Document document = parseDocument(xml);
            Element response = getResponseElement(document);
            Element body = getRequiredDirectChild(response, "body");
            Element items = getRequiredDirectChild(body, "items");

            return new MolitRentPage(
                    parseItems(items, apiCategory),
                    parseRequiredInt(body, "pageNo"),
                    parseRequiredInt(body, "numOfRows"),
                    parseRequiredInt(body, "totalCount")
            );
        } catch (MolitRentApiException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new MolitRentParseException("국토부 전월세 XML 응답을 파싱할 수 없습니다.", exception);
        }
    }

    private Document parseDocument(String xml) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        factory.setXIncludeAware(false);
        factory.setExpandEntityReferences(false);

        DocumentBuilder documentBuilder = factory.newDocumentBuilder();
        return documentBuilder.parse(new InputSource(new StringReader(xml)));
    }

    private Element getResponseElement(Document document) {
        Element response = document.getDocumentElement();

        if (response == null || !"response".equals(response.getTagName())) {
            throw new MolitRentParseException("국토부 전월세 XML 응답의 최상위 요소가 response가 아닙니다.");
        }

        return response;
    }

    private List<RawRentalTransaction> parseItems(Element items, MolitRentApiCategory apiCategory) {
        List<RawRentalTransaction> transactions = new ArrayList<>();
        NodeList childNodes = items.getChildNodes();

        for (int index = 0; index < childNodes.getLength(); index++) {
            Node childNode = childNodes.item(index);

            if (isElementNamed(childNode, "item")) {
                transactions.add(parseItem((Element) childNode, apiCategory));
            }
        }

        return transactions;
    }

    private RawRentalTransaction parseItem(Element item, MolitRentApiCategory apiCategory) {
        return RawRentalTransaction.builder()
                .apiCategory(apiCategory)
                .guCode(getOptionalDirectText(item, "sggCd"))
                .legalDongName(getOptionalDirectText(item, "umdNm"))
                .dealYear(getOptionalDirectText(item, "dealYear"))
                .dealMonth(getOptionalDirectText(item, "dealMonth"))
                .dealDay(getOptionalDirectText(item, "dealDay"))
                .houseType(getOptionalDirectText(item, "houseType"))
                .exclusiveArea(getOptionalDirectText(item, "excluUseAr"))
                .totalFloorArea(getOptionalDirectText(item, "totalFloorAr"))
                .deposit(getOptionalDirectText(item, "deposit"))
                .monthlyRent(getOptionalDirectText(item, "monthlyRent"))
                .build();
    }

    private int parseRequiredInt(Element parent, String tagName) {
        Element element = getRequiredDirectChild(parent, tagName);
        String value = element.getTextContent();

        if (value == null || value.isBlank()) {
            throw new MolitRentParseException(
                    "국토부 전월세 XML 응답의 " + tagName + " 값이 비어 있습니다."
            );
        }

        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException exception) {
            throw new MolitRentParseException(
                    "국토부 전월세 XML 응답의 " + tagName + " 값을 정수로 파싱할 수 없습니다.",
                    exception
            );
        }
    }

    private String getOptionalDirectText(Element parent, String tagName) {
        Element element = findDirectChild(parent, tagName);

        if (element == null) {
            return null;
        }

        String value = element.getTextContent();
        return value == null || value.isBlank() ? null : value;
    }

    private Element getRequiredDirectChild(Element parent, String tagName) {
        Element child = findDirectChild(parent, tagName);

        if (child == null) {
            throw new MolitRentParseException(
                    "국토부 전월세 XML 응답에 " + tagName + " 요소가 없습니다."
            );
        }

        return child;
    }

    private Element findDirectChild(Element parent, String tagName) {
        NodeList childNodes = parent.getChildNodes();

        for (int index = 0; index < childNodes.getLength(); index++) {
            Node childNode = childNodes.item(index);

            if (isElementNamed(childNode, tagName)) {
                return (Element) childNode;
            }
        }

        return null;
    }

    private boolean isElementNamed(Node node, String tagName) {
        return node.getNodeType() == Node.ELEMENT_NODE && tagName.equals(node.getNodeName());
    }
}
