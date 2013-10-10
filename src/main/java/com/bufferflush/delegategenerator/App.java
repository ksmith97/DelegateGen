package com.bufferflush.delegategenerator;

import japa.parser.ParseException;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Writer;
import java.util.Collection;
import java.util.Set;
import java.util.TreeSet;

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
        final File f = new File( filePath );
        Reader in = null;
        try {
            //TODO read this into a resettable buffer so it doesn't need to be reread every time its used.
            in = new InputStreamReader( new FileInputStream( f ) );
            try
            {
                final Set<String> constants = new TreeSet<String>();

                App.logger.info( "Generating Delegate." );
                final String delegatePath = filePath.contains( "Service" ) ? filePath.replace( "Service", "Delegate" )
                                                                           : filePath.replace( ".java", "Delegate.java" );
                final Writer delegateWriter = new BufferedWriter( new FileWriter( new File( delegatePath ) ) );
                try
                {

                    DelegateBuilder.parseFile( in, delegateWriter, constants );
                }
                finally
                {
                    delegateWriter.close();
                }

                //Close the input stream.
                in.close();

                App.logger.info( "Delegate Generated." );
                App.logger.info( "Generating Service Helper." );

                in = new InputStreamReader( new FileInputStream( f ) );

                final String helperPath = filePath.contains( "Service" ) ? filePath.replace( "Service", "ServiceHelper" )
                                                                         : filePath.replace( ".java", "ServiceHelper.java" );
                final Writer helperWriter = new BufferedWriter( new FileWriter( new File( helperPath ) ) );
                try
                {

                    HelperBuilder.parseFile( in, helperWriter, constants );
                    App.logger.info( "Service Helper Generated." );
                }
                finally
                {
                    helperWriter.close();
                }

                final Writer constantsWriter = new BufferedWriter( new FileWriter( new File( f.getParent()
                    + File.separator
                    + "Constants.java" ) ) );

                try
                {
                    for( final String c : constants )
                    {
                        constantsWriter.write( c );
                    }
                }
                finally
                {
                    constantsWriter.close();
                }
            }
            finally
            {
                in.close();
            }
        } catch (final ParseException e) {
            App.logger.error("Failed to parse file.", e);
        } catch (final IOException e) {
            App.logger.error("Could not open file for parsing.", e);
        }
    }
}
