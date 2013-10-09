/**
 * 
 */
package com.bufferflush.delegategenerator;

import japa.parser.ast.CompilationUnit;
import japa.parser.ast.body.MethodDeclaration;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Comparator;
import java.util.List;

import com.bufferflush.delegategenerator.visitor.ClassNameRetriever;
import com.bufferflush.delegategenerator.visitor.MethodAggregator;
import com.google.common.collect.Lists;

/**
 * @author ksmith_cntr
 * 
 */
public class BuilderUtil
{
    public static class ParameterComparator implements Comparator<MethodDeclaration>
    {
        @Override
        public int compare(final MethodDeclaration o1, final MethodDeclaration o2) {
            if(!o1.getName().equals(o2.getName()))
            {
                return o1.getName().compareTo(o2.getName());
            }
            else if(o1.getParameters().size() != o2.getParameters().size())
            {
                return Integer.valueOf(o1.getParameters().size()).compareTo(Integer.valueOf(o2.getParameters().size()));
            }
            else
            {
                return 0; //We may want to do further sorting for when they have the same num of params but I'm leaving it alone for now.
            }
        }
    };

    public static String getClassName(final CompilationUnit cu)
    {
        final String arg = "";
        new ClassNameRetriever().visit( cu, arg );
        return arg;
    }

    public static List<MethodDeclaration> getMethods( final CompilationUnit cu )
    {
        final List<MethodDeclaration> methods = Lists.newArrayList();
        new MethodAggregator().visit( cu, methods );
        return methods;
    }

    public static String loadResourceAsString( final String resourceName ) throws IOException
    {
        final InputStream in = DelegateBuilder.class.getResourceAsStream( resourceName );
        final BufferedReader reader = new BufferedReader( new InputStreamReader( in ) );
        try
        {
            final StringBuilder builder = new StringBuilder();
            String line = null;
            while( ( line = reader.readLine() ) != null )
            {
                builder.append( line );
                builder.append( "\n" );
            }
            return builder.toString();
        }
        finally
        {
            reader.close();
        }
    }
}
