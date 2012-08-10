package com.android.contacts.numberarea;

import java.io.IOException;
import java.io.InputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.android.contacts.util.Constants;

import android.util.Log;

/**
 * A utility to query the location of number online.
 * @author yongan.qiu
 */
public class OnlineUpdateUtil {

    private static final String TAG = OnlineUpdateUtil.class.getSimpleName();

    private static final boolean DEBUG = Constants.TOTAL_DEBUG;

    private static final String UPDATE_URI = "http://www.youdao.com/smartresult-xml/search.s";

    private static final String TYPE_MOBILE = "mobile";

    private static final String NODE_PRODUCT = "product";
    private static final String NODE_TYPE = "type";
    private static final String NODE_PHONENUM = "phonenum";
    private static final String NODE_LOCATION = "location";

    /**
     * Query location of number online.
     * @param number the number whose location to query
     * @return location of the number, or null if no result
     */
    public static String getNumberArea(String number) {
        String uri = UPDATE_URI + "?type=" + TYPE_MOBILE + "&q=" + number;
        HttpGet httpRequest = new HttpGet(uri);
        HttpResponse response;
        try {
            response = new DefaultHttpClient().execute(httpRequest);
            Product product = readXML(response.getEntity().getContent());
            if (product != null) {
                if (DEBUG) {
                    Log.d(TAG, "type = " + product.type + ", number = " + product.phonenum
                            + ", location = "
                            + product.location);
                }
                return product.location;
            }
        } catch (ClientProtocolException e) {
            // TODO handle exception
            e.printStackTrace();
        } catch (IOException e) {
            // TODO handle exception
            e.printStackTrace();
        } catch (Exception e) {
            // TODO handle exception
        }
        return null;
    }

    private static Product readXML(InputStream inStream) {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        Product product = null;

        try {
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document dom = builder.parse(inStream);
            Element root = dom.getDocumentElement();

            NodeList items = root.getElementsByTagName(NODE_PRODUCT);
            // Just one product needed.
            if (items.getLength() > 0) {
                Element productNode = (Element) items.item(0);
                product = new Product();
                product.type = productNode.getAttribute(NODE_TYPE);
                NodeList childsNodes = productNode.getChildNodes();

                for (int j = 0; j < childsNodes.getLength(); j++) {
                    Node node = (Node) childsNodes.item(j);
                    if (node.getNodeType() == Node.ELEMENT_NODE) {
                        Element childNode = (Element) node;
                        if (NODE_PHONENUM.equals(childNode.getNodeName())) {
                            product.phonenum = (childNode.getFirstChild().getNodeValue());
                        } else if (NODE_LOCATION.equals(childNode.getNodeName())) {
                            product.location = (childNode.getFirstChild().getNodeValue());
                        }
                    }
                }
            }
            inStream.close();
        } catch (ParserConfigurationException e) {
            // TODO handle exception
            e.printStackTrace();
        } catch (SAXException e) {
            // TODO handle exception
            e.printStackTrace();
        } catch (IOException e) {
            // TODO handle exception
            e.printStackTrace();
        } catch (Exception e) {

        }
        return product;
    }

    static class Product {
        private String type;
        private String phonenum;
        private String location;
    }
}
