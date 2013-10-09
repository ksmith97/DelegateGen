/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.bufferflush.delegategenerator;

import japa.parser.ast.body.MethodDeclaration;
import japa.parser.ast.expr.NameExpr;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.CharBuffer;
import java.util.Comparator;
import java.util.SortedSet;
import java.util.TreeSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Kevin Smith
 */
public class DelegateBuilder {
    private static final Logger logger = LoggerFactory.getLogger(DelegateBuilder.class);
    
    private static final String classTemplate;
    private static final String methodTemplate; 
    private static final String methodExTemplate;
    
    static {
        classTemplate = loadResourceAsString("classTemplate");
        methodTemplate = loadResourceAsString("MethodTemplate");
        methodExTemplate = loadResourceAsString("methodTemplateBeneEx");
    }
    
    private static String loadResourceAsString(String resourceName)
    {
        InputStream in = DelegateBuilder.class.getResourceAsStream(resourceName);
        try( Reader reader = new BufferedReader(new InputStreamReader(in, "UTF-8"))) 
        {
            CharBuffer buff = CharBuffer.allocate(1000);
            reader.read(buff);
            return buff.toString();
        } 
        catch(IOException e)
        {
            logger.error("Failed to load template resources.", e);
            throw new Error("Could not instantiate template resources.");
        }
    }
    
    private StringBuilder classStr = new StringBuilder(classTemplate);
    private SortedSet<MethodDeclaration> methods = new TreeSet<MethodDeclaration>(new Comparator<MethodDeclaration>() {

        @Override
        public int compare(MethodDeclaration o1, MethodDeclaration o2) {
            if(!o1.getName().equals(o2.getName()))
                return o1.getName().compareTo(o2.getName());
            else if(o1.getParameters().size() != o2.getParameters().size())
                return Integer.valueOf(o1.getParameters().size()).compareTo(Integer.valueOf(o2.getParameters().size()));
            else
                return 0; //We may want to do further sorting for when they have the same num of params but Im leaving it for now.
        }
    });
    private String serviceName = "";
    private String className = "";
    private String packageName = "";
    
    public static final DelegateBuilder start()
    {
        return new DelegateBuilder();
    }
    
    public final DelegateBuilder setServiceName(String serviceName)
    {
        this.serviceName = serviceName;
        
        if(!serviceName.endsWith("Service"))
            logger.warn("Attempting to generate delegate for file not marked as service.");
        else
            this.className = serviceName.replaceAll("Service$", "");
        
        return this;
    }
    
    public final DelegateBuilder setPackageName(String packageName)
    {
        this.packageName = packageName.startsWith(".") ? packageName.substring(1) : packageName;
    }
    
    public DelegateBuilder addMethod(MethodDeclaration m)
    {
        this.methods.add(m);
        return this;
    }
    
    private final String generateMethods()
    {
        StringBuilder str = new StringBuilder();
        for(MethodDeclaration m : this.methods)
        {
            str.append(generateMethod(m));
        }
        return str.toString();
    }
    
    private final String getMethodString(MethodDeclaration d)
    {
        for(NameExpr exp : d.getThrows())
        {
            if("BenecardExecption".equals(exp.getName()))
                return methodExTemplate;
            else if(!"Exception".equals(exp.getName()))
            {
                throw new Error("Unsupported Exception found on service method. Service: " 
                        + this.serviceName + " Method: " + d.getName() + " Exception: " + exp.getName());
            }
        }
        
        return methodTemplate;
    }
    
    private final String generateMethod(MethodDeclaration d)
    {
        return getMethodString()
                .replace("{returnType}", d.getType())
                .replace("{methodName}", d.getName())
                .replace("{}");
    }
    
    @Override
    public String toString()
    {
        return classStr.toString()
                .replace("{className}", this.className)
                .replace("{methods}", generateMethods())
                .replace("{serviceName}", this.serviceName)
                .replace("{packageName}", "." + this.packageName);
                
    }
}
