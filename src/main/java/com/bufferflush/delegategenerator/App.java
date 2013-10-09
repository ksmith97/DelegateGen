package com.bufferflush.delegategenerator;

import java.io.File;
import java.util.Collection;
import com.google.common.collect.Lists;
import japa.parser.JavaParser;
import japa.parser.ParseException;
import japa.parser.ast.CompilationUnit;
import japa.parser.ast.body.MethodDeclaration;
import japa.parser.ast.visitor.VoidVisitorAdapter;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class App {

    private static final Logger logger = LoggerFactory.getLogger(App.class);

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        logger.info("Started");
        for (String s : checkFiles(args)) {
            parseFile(s);
        }
    }

    private static Collection<String> checkFiles(String[] paths) {
        Collection<String> files = Lists.newArrayList();
        for (String path : paths) {
            try {
                File f = new File(path);
                //TODO Explode directory into files here eventually
                if (f.isDirectory()) {
                    throw new IllegalArgumentException("Directories currently unsupported.");
                }

                files.add(path);
            } catch (Exception e) {
                logger.error("Failed to add file at path: " + path, e);
            }
        }

        return files;
    }

    private static void parseFile(String filePath) {
        logger.info("Parsing file " + filePath);
        try (InputStream in = new FileInputStream(filePath)) {
            CompilationUnit cu = JavaParser.parse(in);
            
            List<MethodDeclaration> methods = Lists.newArrayList();
            new MethodVisitor().visit(cu, methods);
            
        } catch (ParseException e) {
            logger.error("Failed to parse file.", e);
        } catch (IOException e) {
            logger.error("Could not open file for parsing.", e);
        }
    }

    private static final class MethodVisitor extends VoidVisitorAdapter<List<MethodDeclaration>> {

        @Override
        public void visit(MethodDeclaration d, List<MethodDeclaration> arg) {
            arg.add(d);
        }
    }
}
