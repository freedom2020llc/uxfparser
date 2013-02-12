/**
 * 
 */
package com.freedom2020llc.utils.uxfparser;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

/**
 * UxfJavaClass. Simple representation of a java class that is parsed from UXF raw text 
 * (read from a UXF xml fragment) and output as a java source file (using the toString() method).
 * 
 */
public class UxfJavaClass {

    /**
     * The _package prefix.
     */
    private String _packagePrefix = "";
    
    /**
     * The _destination dir.
     */
    private String _destinationDir = "";
    
    /**
     * The _class name.
     */
    private String _className = "";
    
    /**
     * The _class definition.
     */
    private String _classDefinition= "";
    
    /**
     * The _package name.
     */
    private String _packageName = "";
    
    /**
     * The _fields.
     */
    private ArrayList<String> _fields = new ArrayList<String>();
    
    /**
     * The _methods.
     */
    private ArrayList<String> _methods = new ArrayList<String>();
    
    /**
     * The _comments.
     */
    private String _comments = "";
    
    /**
     * The Constant SPACER.
     */
    private static final String SPACER = "    ";
    
    /**
     * UxfJavaClass constructor.
     * 
     * @param packagePrefix_
     *            the package prefix_
     * @param destinationDir_
     *            the destination dir_
     */
    public UxfJavaClass(String packagePrefix_, String destinationDir_) {
        _packagePrefix = packagePrefix_;
        _destinationDir = destinationDir_;
    }


    /**
     * Parse raw text from uxf panel_attributes element.
     * 
     * @param packagePrefix_
     *            the package prefix_
     * @param rawtext_
     *            the rawtext_
     * @param destinationdir_
     *            the destinationdir_
     * @return new UxfJavaClass
     */
    public static UxfJavaClass parse(String packagePrefix_, String rawtext_, String destinationdir_, ArrayList<String> importsList) {
               
        // verify args
        if (rawtext_ == null) { 
            return null;
        }

        if (_importPackages.isEmpty()) {
        	_importPackages.addAll(importsList);
        }
        
        // 
        UxfJavaClass uxf = new UxfJavaClass(packagePrefix_, destinationdir_);
        
        // Split on "--" to get all sections
        String[]sections = rawtext_.split("--");
        
        // Get class name and package
        if (sections.length > 1) {
            
            String className = "CLASSNAME_NOT_FOUND";
            String classDefinition = "CLASSNAME_DEFINITION_NOT_FOUND";
            String packageName = "PACKAGENAME_NOT_FOUND";
            
            // Get class and package names
            String[] lines = sections[0].split("\n");
            printArray(lines);
            
            // Get class name and definition
            if (lines.length > 0) {
                String []tmp = lines[0].trim().split(" ");
                classDefinition = lines[0].trim();
                System.out.println("Classdef=" + classDefinition);
                classDefinition = classDefinition.replace("{abstract}", "abstract class");
                System.out.println("Classdef=" + classDefinition);
                classDefinition = classDefinition.replace("//", "");                
                System.out.println("Classdef=" + classDefinition);
                if (classDefinition.contains("abstract")) {
                    className = tmp[1].trim();
                } else {
                    classDefinition="class " + classDefinition;
                    className = tmp[0].trim();
                }
                System.out.println("Classdef=" + classDefinition);
            }
            
            // Get package name
            String[] line = lines[1].split("::");
            printArray(line);
            if (line.length > 1) {
                packageName = line[1].trim().toLowerCase();
            }            
            
            // Set values
            uxf.setClassName(className);
            uxf.setClassDefinition(classDefinition);
            uxf.setPackageName(packageName);
        }

        // Read any fields from this class 
        // Field examples that will be translated in Java field source:
        // myvar:int:0                  -> protected int myvar = 0;
        // myvar:int                    -> protected int myvar;
        // myvar:HashMap:new HashMap() -> protected HashMap myvar = new HashMap();          
        // MY_FINALVAR:HashMap:new HashMap() -> public static final HashMap MY_FINALVAR = new HashMap();
        // Field examples that end with ";" will just be translated with no change (nothing clever):
        // protected static NewClass MY_COMPLEX_VAR = new NewClass(5);  
        if (sections.length > 2) {
            String [] fields = sections[1].split("\n");
            
            // Example: myvar:int:0
            for (int x=0; x<fields.length; x++) {
                
                // Field ends with ";" assume it should just be translated with no change
                String fieldtext = fields[x].trim(); 
                if (fieldtext.endsWith(";")) {
                    uxf.addField(fieldtext);
                } else {
                    // Field needs parsing
                    String tokens[] = fieldtext.split(":");
                    if (tokens.length >= 2) {
                        String modifier = "protected";
                        String variableName = tokens[0].trim();
                        String dataType = tokens[1].trim();
                        String initialisation = (tokens.length > 2)?tokens[2].trim():"";
                        boolean constant = false;
                        if (variableName.equals(variableName.toUpperCase())) {
                            modifier = "public static final";
                            constant = true;
                        }                    
                        String field = modifier + " " + dataType + " " + (!constant?"_":"") + variableName + (("".equals(initialisation))?";":" = " + initialisation + ";");
                        uxf.addField(field);
                    }
                }
            }
        }

        // Get methods
        if (sections.length > 3) {
            String [] methods = sections[2].split("\n");
            boolean insidemethod = false;
            String method = "METHOD_NOT_FOUND";
            String modifier = "public";
            for (int x=0; x<methods.length; x++) {
                
                // ignore blank rows
                if (methods[x].trim().length()==0) continue;
                
                // Search for key modifiers
                modifier = "public ";
                if ( methods[x].trim().contains("{private}") ) {
                    methods[x] = methods[x].replace("{private}", "");
                    modifier = "private ";
                }
                if ( methods[x].trim().contains("{protected}") ) {
                    methods[x] = methods[x].replace("{protected}", "");
                    modifier = "protected ";
                }
                if ( methods[x].trim().contains("{abstract}") ) {
                    methods[x] = methods[x].replace("{abstract}", "");
                    modifier = "public abstract ";
                }
                if ( methods[x].trim().contains("{static}") ) {
                    methods[x] = methods[x].replace("{static}", "");
                    modifier = "public static ";
                }
                
                // method
                if ( (methods[x].contains("{") && methods[x].contains("}")) &&
                     (methods[x].indexOf("{") < methods[x].indexOf("}"))) {
                    uxf.addMethod(modifier + methods[x]);
                    insidemethod = false;
                } else {
                    System.out.println("methods[x]="+methods[x] + ", insidemethod=" + insidemethod);
                    if (methods[x].contains("//JAVADOC")) {
                        System.out.println("METHOD " + methods[x]);
                        String[] line = methods[x].split("//JAVADOC");
                        
                        // Fix for javadoc comment issue
                        //method = "/**\n" + SPACER + " * " + (line.length>1?line[1]:"") + "\n" + SPACER + " */\n";
                        method = "/**\n * " + (line.length>1?line[1]:"") + "\n */\n";
                        
                        // Fix for constructor/method sig extra space at end of "{"
                        //method += SPACER + modifier + line[0].trim();
                        method += SPACER + modifier + line[0];
                        
                        //method = method.replace("//JAVADOC","");                        
                        insidemethod = true;
                        continue;
                    }
                    if (insidemethod) {
                        method += "\n" + SPACER + methods[x];
                        if (methods[x].contains("//END")) {           
                            method = method.replace("//END", "");
                            uxf.addMethod(method);
                            method = "METHOD_NOT_FOUND";
                            insidemethod = false;
                        }
                    } else {
                        uxf.addMethod(modifier + methods[x]);
                    }
                }
                
                
                /*
                // Constructor
                if (x == 0) {
                    uxf.addMethod("public " + methods[x]);
                } else {
                    
                    
                    String sig = "CANNOT_READ_SIGNATURE";
                    String ret = "void"; 
                    String body = "{ }"; 

                    // Standard methods
                    int index1 = methods[x].indexOf(")");
                    int index2 = methods[x].indexOf("{");
                    if (index2 == -1) index2=methods[x].length();
                    if (index1 > 0) {
                        int index3 = methods[x].indexOf("(");
                        sig = methods[x].substring(0, index3+1);
                        
                        // figure out java signature
                        String arglist = methods[x].substring(index3+1, index1);
                        System.out.println("Arglist = " + arglist);
                        String[] args = arglist.split(",");
                        for (int f=0; f<args.length; f++) {
                            String[] tmp = args[f].split(":");
                            if (tmp.length>=2) {
                                sig += (f>0?",":"") + tmp[1] + " " + tmp[0];
                            }
                        }
                        
                        sig += ")";
                    }
                    if (index1 > 0 && index2 > 0) {
                        System.out.println("RET" + index1 + "," + index2);
                        ret = methods[x].substring(index1, index2).trim();
                        ret = ret.replace(":","");
                        ret = ret.replace(")","");
                        ret = ret.trim();
                    }
                    if (index2 > 0) {
                        body = methods[x].substring(index2, methods[x].length()).trim();
                    }
                    //body = ""; 
                    String method = "public " + ret + " " + sig + " " + body;
                    uxf.addMethod(method);
                    
                    
                    uxf.addMethod("public " + methods[x]);
                }
                */
            }
        }
        
        // Split on "Responsibilities" to get comments section
        sections = rawtext_.split("Responsibilities");
        if (sections.length > 1) {
            String comments = sections[1];
            comments = comments.replaceAll("--", "+");
            
            // Comment lines
            String [] commentlines = comments.split("\n");
            StringBuffer formattedcomments = new StringBuffer();
            for (int x=0; x<commentlines.length; x++) {
                if (!commentlines[x].isEmpty()) {
                    formattedcomments.append(" * ");
                    formattedcomments.append(commentlines[x]);
                    formattedcomments.append("\n");
                }
            }            
            uxf.setComments(formattedcomments.toString());
        }
     
        return uxf;
    }


    /**
     * Sets the class name.
     * 
     * @param className
     *            the className to set
     */
    public void setClassName(String className) {
        _className = className;
    }


    /**
     * Gets the class name.
     * 
     * @return the className
     */
    public String getClassName() {
        return _className;
    }


    /**
     * Sets the package name.
     * 
     * @param packageName
     *            the packageName to set
     */
    public void setPackageName(String packageName) {
        _packageName = _packagePrefix + "." + packageName;
    }


    /**
     * Gets the package name.
     * 
     * @return the packageName
     */
    public String getPackageName() {
        return _packageName;
    }


    /**
     * Sets the comments.
     * 
     * @param comments
     *            the comments to set
     */
    public void setComments(String comments) {
        _comments = comments;
    }


    /**
     * Gets the comments.
     * 
     * @return the comments
     */
    public String getComments() {
        return _comments;
    }


    /**
     * Adds the method.
     * 
     * @param method_
     *            method to add
     */
    public void addMethod(String method_) {
        _methods.add(method_);
    }

    /**
     * Adds the field.
     * 
     * @param field_
     *            field to add
     */
    public void addField(String field_) {
        _fields.add(field_);
    }

    /**
     * Gets the methods.
     * 
     * @return the methods
     */
    public ArrayList<String> getMethods() {
        return _methods;
    }
    
    
    /**
     * List of import package names
     */
    protected static ArrayList<String> _importPackages = new ArrayList<String>();
    
    /**
     * Write out Java class definition.
     * 
     * @return the string
     * @see java.lang.Object#toString()
     */
    public String toString() {
        String PAD = "    ";
        StringBuffer buf = new StringBuffer();
        buf.append("/**\n");
        buf.append(" *\n"); 
        buf.append(" */\n");
        buf.append("package " + _packageName + ";\n\n");
        for (String imp : _importPackages) {
            buf.append("import " + imp + ".*;\n");
        }        
        buf.append("/**\n");
        buf.append(" * " + _className + "\n");
        buf.append(" *\n");
        buf.append(_comments);
        buf.append(" */\n");
        buf.append("public " + _classDefinition + " {\n");        
        buf.append("\n");
        for (String field : _fields) {
            buf.append(PAD + field + "\n");
        }
        buf.append("\n");        
        for (String method : _methods) {
            buf.append(PAD + method + "\n");
        }
        buf.append("\n");        
        buf.append("}\n");
        return buf.toString();
    }
    
    /**
     * Prints the array.
     * 
     * @param array
     *            the array
     */
    private static void printArray(String[] array) {
        System.out.println("[");
        for (int x=0; x<array.length; x++) {        
            System.out.println(((x != 0 )?",\n":"") + array[x]);            
        }
        System.out.println("]");
    }
    
    /**
     * Writeclassfile.
     */
    public void writeclassfile() {
        String dirname = _destinationDir + File.separator + _packageName.replace(".", File.separator);
        String filename = dirname + File.separator + _className + ".java";
        
        File dir = new File(dirname);
        File toFile = new File(filename);
                
        FileOutputStream to = null;
        try {
            dir.mkdirs();
            toFile.createNewFile();
            to = new FileOutputStream(toFile);
            to.write(toString().getBytes());
            System.out.println("Wrote" + filename);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
          if (to != null)
            try {              
              to.close();
            } catch (IOException e) {
              ;
            }
        }
        
        
    }


    /**
     * Gets the class definition.
     * 
     * @return class name
     */
    public String getClassDefinition() {
        return _classDefinition;
    }


    /**
     * Sets the class definition.
     * 
     * @param classDefinition_
     *            the new class definition
     */
    public void setClassDefinition(String classDefinition_) {
        _classDefinition = classDefinition_;
    }
}
