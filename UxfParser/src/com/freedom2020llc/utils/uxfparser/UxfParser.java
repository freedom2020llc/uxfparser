/**
 * 
 */
package com.freedom2020llc.utils.uxfparser;

import java.io.IOException;
import java.util.ArrayList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * UxfParser. Quick and dirty UXF parser, that converts XML file to 
 * java source files in specified directory with specified imports.
 * See usage() method for how to run.
 */
public class UxfParser {
    
	/**
	 * Given a delimited single string list of package names, output a string list that can be used to print java package imports
	 * Example input: parseImportsList("com.mycom.myapp", ".myutils;.mycode;java.text;java.util", ";", ".")
	 * Example output: { "com.mycom.myapp.myutils", "com.mycom.myapp.mycode", "java.text", "java.util" }
	 * @param packageRoot_ package prefix to add with entry is prefixed with importListAppPackagePrefix_ 
	 * @param importList_ delimited single string list of package names
	 * @param importListSeperator_ seperator for each incoming package name in importList_ (usually ";") 
	 * @param importListAppPackagePrefix_ prefix that will automatically add packageRoot_ name to the output package name (usually ".")
	 * @return string list of java package imports
	 */
	public static ArrayList<String> parseImportsList(String packageRoot_, String importList_, String importListSeperator_, String importListAppPackagePrefix_) {
		if (importList_ == null) return null;
		String []list = importList_.split(importListSeperator_);
		if (list == null) return null;
		if (list.length == 0) return null;
		ArrayList<String> outputArrayList = new ArrayList<String>();
		for (String s: list) {
			if (s.startsWith(importListAppPackagePrefix_)) {
				outputArrayList.add(packageRoot_ + s);
			} else {
				outputArrayList.add(s);
			}
		}
		return outputArrayList;
	}
	
	/**
	 * Print usage
	 */
	protected static void usage() {
        System.err.println("Usage:    UxfParser [uxfFilename] [outputJavaSrcDir] [outputJavaPackagePrefix] [importsList]");		
        System.err.println("[uxfFilename]             input UXF filename");		
        System.err.println("[outputJavaSrcDir]        target output src directory. Check here for java src files after execution is complete.");		
        System.err.println("[outputJavaPackagePrefix] prefix package for output java packages e.g. \"com.mycom.myapp\" ");		
        System.err.println("[importsList]             delimited (;) list of required java imports in output src files Example: \".myutils;.mycode;java.text;java.util\". ");
        System.err.println("                          A list entry with \".\" prefix means use [outputJavaPackagePrefix] as output package name prefix.");		
        System.err.println("Example:  UxfParser \"C:\\uxf\\collar.uxf\" \"C:\\users\\me\\workspace\\myapp\\src\\\" \"com.mycom.myapp\" \".myutils;.mycode;java.text;java.util\"");		
	}
	
    /**
     * Main entry point - requires uxf filename.
     * 
     * @param args_
     *            the arguments
     */
    public static void main(String[] args_) {
        
        try {
          
          // Check args
          if (args_.length != 4) {
        	  usage();
              return;
          }
          String uxfFilename = args_[0];
          String outputJavaSrcDir = args_[1];
          String outputJavaPackagePrefix = args_[2];
          String importsList = args_[3];
          
          // Verify imports list
          ArrayList<String> outputImportsList = parseImportsList(
        		  outputJavaPackagePrefix, 
        		  importsList, 
        		  ";", 
        		  "."
          );
          if (outputImportsList == null) {
        	  usage();
              System.err.println("[importsList] could not be parsed - "+importsList);
              return;        	  
          }
          
          // first of all we request out 
          // DOM-implementation:
          DocumentBuilderFactory factory = 
            DocumentBuilderFactory.newInstance();
          // then we have to create document-loader:
          DocumentBuilder loader = factory.newDocumentBuilder();

          // loading a DOM-tree...
          Document document = loader.parse(uxfFilename);
          // at last, we get a root element:
          Element tree = document.getDocumentElement();

          // ... do something with document element ...
          NodeList classList = tree.getElementsByTagName("panel_attributes");
          for (int s = 0; s < classList.getLength(); s++) {

              Node fstNode = classList.item(s);
              
              if (fstNode.getNodeType() == Node.ELEMENT_NODE) {

                // Read text node
                Element fstElmnt = (Element) fstNode;
                NodeList fstNm = fstElmnt.getChildNodes();
                String rawtext = ((Node) fstNm.item(0)).getNodeValue();
                
                // Pr
                UxfJavaClass uxf = UxfJavaClass.parse(
                        outputJavaPackagePrefix, 
                        rawtext,
                        outputJavaSrcDir,
                        outputImportsList
                );                
                System.out.println(uxf);
                System.out.println("-----------------------");
                uxf.writeclassfile();
              }

         }          
          
        } catch (IOException ex) {
          // any IO errors occur:
          System.err.println(ex);
        } catch (SAXException ex) {
          // parse errors occur:
          System.err.println(ex);
        } catch (ParserConfigurationException ex) {
          // document-loader cannot be created which,
          // satisfies the configuration requested
          System.err.println(ex);
        } catch (FactoryConfigurationError ex) {
          // DOM-implementation is not available 
          // or cannot be instantiated:
          System.err.println(ex);
        }
       
        return;
    }
}
