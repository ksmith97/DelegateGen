package com.bufferflush.delegategenerator;

import japa.parser.ParseException;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.Collection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;

public class App {

    private static final Logger logger = LoggerFactory.getLogger(App.class);

    private static Collection<String> checkFiles(final String[] paths) {
        final Collection<String> files = Lists.newArrayList();
        for (final String path : paths) {
            try {
                final File f = new File(path);
                //TODO Explode directory into files here eventually
                if (f.isDirectory()) {
                    throw new IllegalArgumentException("Directories currently unsupported.");
                }

                files.add(path);
            } catch (final Exception e) {
                App.logger.error("Failed to add file at path: " + path, e);
            }
        }

        return files;
    }

    /**
     * @param args the command line arguments
     */
    public static void main(final String[] args) {
        App.logger.info("Started");
        for (final String s : App.checkFiles(args)) {
            App.parseFile(s);
        }
    }

    private static void parseFile(final String filePath) {
        App.logger.info("Parsing file " + filePath);
        Reader in = null;
        Writer writer = null;
        try {
            in = new InputStreamReader( new FileInputStream( filePath ) );
            writer = new PrintWriter( System.out );

            final String delegatePath = filePath.contains( "Service" ) ? filePath.replace( "Service", "Delegate" )
                                                                       : filePath.replace( ".java", "Delegate.java" );
            final Writer classWriter = new BufferedWriter( new OutputStreamWriter( new FileOutputStream( new File(
                delegatePath ) ) ) );

            final Writer constantsWriter = new BufferedWriter( new OutputStreamWriter( new FileOutputStream( new File(
            "Constants.java" ) ) ) );


            DelegateBuilder.parseFile( in, classWriter, constantsWriter );
        } catch (final ParseException e) {
            App.logger.error("Failed to parse file.", e);
        } catch (final IOException e) {
            App.logger.error("Could not open file for parsing.", e);
        } finally {

            try
            {
                if ( in != null )
                {
                    in.close();
                }
            }
            catch( final IOException e )
            {
                App.logger.error( "Failed to close Reader.", e );
            }

            try
            {
                if ( writer != null )
                {
                    writer.close();
                }
            }
            catch( final IOException e )
            {
                App.logger.error( "Failed to close Writer.", e );
            }
        }
    }
}
