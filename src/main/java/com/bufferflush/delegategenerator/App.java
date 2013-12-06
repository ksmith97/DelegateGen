package com.bufferflush.delegategenerator;

import japa.parser.ParseException;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.Collection;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Sets;

public class App
{

    private static final Logger logger = LoggerFactory.getLogger( App.class );

    /**
     * @param args
     *            the command line arguments
     */
    public static void main( final String[] args ) throws Exception
    {
        App.logger.info( "Started" );
        if ( args.length != 4 )
        {
            System.err
                .println( "There are four required arguments.\n-The root dir for the services.\n-An empty dir to put the delegates.\n-An empty dir to put the heleprs.\nThe name of a file to write constants to." );
            System.exit( -1 );
        }

        final File targetServiceProject = new File( args[0] );
        if ( !targetServiceProject.isDirectory() )
        {
            System.err.println( "Expected a dir as the first param." );
            System.exit( -1 );
        }

        final File targetDelDir = new File( args[1] );
        if ( !targetDelDir.isDirectory() || targetDelDir.listFiles().length > 0 )
        {
            System.err.println( "Expected an empty dir as the second param." );
            System.exit( -1 );
        }

        final File targetHelperDir = new File( args[2] );
        if ( !targetHelperDir.isDirectory() || targetHelperDir.listFiles().length > 0 )
        {
            System.err.println( "Expected an empty dir as the third param." );
            System.exit( -1 );
        }

        final File constantsFile = new File( args[3] );
        if ( constantsFile.isDirectory() )
        {
            System.err.println( "Constants file name cannot be a directory." );
            System.exit( -1 );
        }


        new App( targetServiceProject, targetDelDir, targetHelperDir, constantsFile ).generate();
    }

    private final File constantsFile;
    private final File delegateDest;
    private final File helperDest;
    private final File serviceTarget;

    public App( final File a, final File b, final File c, final File d )
    {
        this.serviceTarget = a;
        this.delegateDest = b;
        this.helperDest = c;
        this.constantsFile = d;
    }

    public void generate() throws IOException, ParseException
    {
        App.logger.info( "Target: " + this.serviceTarget + " Delegate Destination: " + this.delegateDest
                         + " Helper Destination: " + this.helperDest );

        final Set<String> constants = Sets.newHashSet();

        final Collection<File> files = FileUtils.listFiles( this.serviceTarget, new String[] { "java" }, true );
        for( final File f : files )
        {
            if ( f.getName().matches( ".+Service\\.java$" ) )
            {
                //DelegateFile
                final File newDelFile = new File( this.delegateDest + File.separator
                                                  + f.getName().replace( "Service", "Delegate" ) );
                newDelFile.getParentFile().mkdirs();
                newDelFile.createNewFile();

                {
                    final Reader in = new BufferedReader( new InputStreamReader( new FileInputStream( f ) ) );
                    try
                    {
                        final Writer out = new BufferedWriter( new OutputStreamWriter(
                            new FileOutputStream( newDelFile ) ) );
                        try
                        {
                            DelegateBuilder.parseFile( in, out, constants );
                        }
                        finally
                        {
                            out.close();
                        }
                    }
                    finally
                    {
                        in.close();
                    }
                }

                //HelperFile
                final File newHelperFile = new File( this.helperDest + File.separator
                                                     + f.getName().replace( "Service", "ServiceHelper" ) );
                newHelperFile.getParentFile().mkdirs();
                newHelperFile.createNewFile();

                {
                    final Reader in = new BufferedReader( new InputStreamReader( new FileInputStream( f ) ) );
                    try
                    {
                        final Writer out = new BufferedWriter(
                            new OutputStreamWriter( new FileOutputStream( newHelperFile ) ) );
                        try
                        {
                            HelperBuilder.parseFile( in, out, constants );
                        }
                        finally
                        {
                            out.close();
                        }
                    }
                    finally
                    {
                        in.close();
                    }
                }
            }
        }


        final File constantsFile = this.constantsFile;
        if ( !constantsFile.exists() )
        {
            constantsFile.getParentFile().mkdirs();
            constantsFile.createNewFile();
        }

        final BufferedWriter out = new BufferedWriter( new OutputStreamWriter( new FileOutputStream( constantsFile ) ) );
        try
        {
            for( final String constant : constants )
            {
                out.write( constant + "\n" );
            }
        }
        finally
        {
            out.close();
        }

    }
}
