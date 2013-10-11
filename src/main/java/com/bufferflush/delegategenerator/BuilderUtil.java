/**
 * 
 */
package com.bufferflush.delegategenerator;

import japa.parser.ast.CompilationUnit;
import japa.parser.ast.body.MethodDeclaration;
import japa.parser.ast.body.Parameter;
import japa.parser.ast.type.Type;
import japa.parser.ast.type.VoidType;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import com.bufferflush.delegategenerator.visitor.ClassNameRetriever;
import com.bufferflush.delegategenerator.visitor.MethodAggregator;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

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

    /**
     * Gets the Class Name for the compilation unit.
     * 
     * @param cu
     * @return
     */
    public static String getClassName(final CompilationUnit cu)
    {
        final List<String> arg = Lists.newArrayList();
        new ClassNameRetriever().visit( cu, arg );
        return arg.get( 0 );
    }

    /**
     * Gets all the methods for a Compilation Unit.
     * 
     * @param cu
     * @return
     */
    public static List<MethodDeclaration> getMethods( final CompilationUnit cu )
    {
        final List<MethodDeclaration> methods = Lists.newArrayList();
        new MethodAggregator().visit( cu, methods );
        return methods;
    }

    /**
     * Retrieves all the types that are a part of the method declarations.
     * This allows us to generate a set of needed imports.
     * 
     * @param d
     * @return
     */
    public static Collection<Type> getMethodTypes( final Iterable<MethodDeclaration> methods )
    {
        final Set<Type> types = Sets.newHashSet();
        for(final MethodDeclaration method : methods)
        {
            types.addAll( BuilderUtil.getMethodTypes(method) );
        }
        return types;
    }

    /**
     * Retrieves all the types that are a part of the method declaration.
     * This allows us to generate a set of needed imports.
     * 
     * @param d
     * @return
     */
    public static Collection<Type> getMethodTypes( final MethodDeclaration d )
    {
        final Collection<Type> types = Sets.newHashSet();
        if ( d.getParameters() != null && !d.getParameters().isEmpty() )
        {
            for( final Parameter p : d.getParameters() )
            {
                types.add( p.getType() );
            }
        }

        if ( ! ( d.getType() instanceof VoidType ) )
        {
            types.add( d.getType() );
        }

        return types;
    }

    /**
     * Used to load a resource as a String.
     * Handles the problem where, when jar'd, resources are loaded from a different place.
     * 
     * @param resourceName
     * @return
     * @throws IOException
     */
    public static String loadResourceAsString( final String resourceName ) throws IOException
    {
        InputStream in = BuilderUtil.class.getResourceAsStream( resourceName );
        //Hack! The resource name must start with a / to load when jar'd but must not start with a slash when loaded while testing.
        if ( in == null && resourceName.startsWith( "/" ) )
        {
            in = BuilderUtil.class.getResourceAsStream( resourceName.substring( 1 ) );
        }

        if ( in == null )
        {
            throw new RuntimeException( "Could not find resource " + resourceName );
        }
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
