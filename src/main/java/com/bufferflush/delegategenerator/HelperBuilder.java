/**
 * 
 */
package com.bufferflush.delegategenerator;

import japa.parser.JavaParser;
import japa.parser.ParseException;
import japa.parser.ast.CompilationUnit;
import japa.parser.ast.ImportDeclaration;
import japa.parser.ast.body.MethodDeclaration;
import japa.parser.ast.body.Parameter;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

/**
 * @author ksmith_cntr
 *
 */
public class HelperBuilder
{
    private static final String callTemplate;
    private static final String classTemplate;
    private static final String constantTemplate;
    private static final Logger logger = LoggerFactory.getLogger( HelperBuilder.class );

    static
    {
        try
        {
            classTemplate = BuilderUtil.loadResourceAsString( "/HelperClassTemplate" );
            callTemplate = BuilderUtil.loadResourceAsString( "/CallTemplate" );
            constantTemplate = BuilderUtil.loadResourceAsString( "/ConstantTemplate" );
        }
        catch( final IOException e )
        {
            HelperBuilder.logger.error( "Faild to load Templates!", e );
            throw new Error( "Failed to load templates.", e );
        }
    }

    public static void parseFile( final Reader in, final Writer delegateClass, final Set<String> constants )
    throws IOException,
    ParseException
    {
        final CompilationUnit cu = JavaParser.parse( in );

        final HelperBuilder builder = new HelperBuilder( cu )
        .addMethods( BuilderUtil.getMethods( cu ) )
            .setServiceName( BuilderUtil.getClassName( cu ) );

        delegateClass.write( builder.generateClassDef() );
        delegateClass.flush();
        constants.addAll( builder.generateConstantDef() );
    }

    private String className = "";

    private final StringBuilder classStr = new StringBuilder( HelperBuilder.classTemplate );
    private final CompilationUnit compUnit;
    private final Map<String, String> constants = Maps.newHashMap();
    private String localServiceName = "";
    private final SortedSet<MethodDeclaration> methods = new TreeSet<MethodDeclaration>(
    new BuilderUtil.ParameterComparator() );
    private String serviceName = "";

    public HelperBuilder(final CompilationUnit cu)
    {
        this.compUnit = cu;
    }

    public final HelperBuilder addMethod( final MethodDeclaration m )
    {
        this.methods.add( m );
        return this;
    }

    public final HelperBuilder addMethods( final Collection<MethodDeclaration> m )
    {
        this.methods.addAll( m );
        return this;
    }

    private String constantize( final String value )
    {
        this.constants.put( value, value );
        return "ServiceConstants." + this.createConstant( value );
    }

    private String constantize( final String value, final String key )
    {
        final String existingValue = this.constants.get( key );
        if ( existingValue == null )
        {
            this.constants.put( key, value );
        }
        else if ( !existingValue.equals( key ) )
        {
            throw new RuntimeException( "Attempted to create a new constant with an existing name. Name: " + key
                + " existing value: " + existingValue + " NewValue:" + value );
        }
        //We don't care if they are inserting the same value as a pre existing value.

        return "ServiceConstants." + this.createConstant( key );
    }

    private String createConstant( final String str )
    {
        return ( this.serviceName.equals( str ) ? "" : this.serviceName )
        + Character.toUpperCase( str.charAt( 0 ) ) + str.substring( 1 );
    }

    private String generateClassDef()
    {
        if ( StringUtils.isBlank( this.className ) )
        {
            HelperBuilder.logger.error( "The class name is blank." );
        }
        if ( StringUtils.isBlank( this.serviceName ) )
        {
            HelperBuilder.logger.error( "The service name is blank." );
        }
        if ( StringUtils.isBlank( this.localServiceName ) )
        {
            HelperBuilder.logger.error( "The local service name is blank." );
        }
        return this.classStr.toString()
        .replace( "<className>", this.className )
        .replace( "<date>", new SimpleDateFormat( "MM/dd/yyyy" ).format( new Date() ) )
        .replace( "<methodCalls>", Joiner.on( "" ).join( this.generateMethodCalls() ) )
        .replace( "<serviceName>", this.serviceName )
        .replace( "<localClass>", this.localServiceName )
        .replace( "<localEjbName>", this.constantize( "ejblocal:" + this.compUnit.getPackage().getName() + "."
        + this.localServiceName, "localEjbLookup" ) )
        .replace( "<imports>", Joiner.on( "\n" ).join( this.generateRequiredImports( this.compUnit.getImports() ) ) );
    }

    private List<String> generateConstantDef()
    {
        return Arrays.asList( this.generateConstants( this.constants ) );
    }

    private String[] generateConstants( final Map<String, String> constants )
    {
        final Object[] values = constants.entrySet().toArray();
        final String[] constantValues = new String[values.length];
        for( int x = 0; x < values.length; x++ )
        {
            @SuppressWarnings( "unchecked" )
            final Map.Entry<String, String> con = (Entry<String, String>) values[x];

            constantValues[x] = HelperBuilder.constantTemplate
            .replace( "<name>", this.createConstant( con.getKey() ) )
            .replace( "<value>", con.getValue() );
        }

        Arrays.sort( constantValues );
        return constantValues;
    }

    private String generateMethod( final MethodDeclaration d )
    {
        return HelperBuilder.callTemplate
        .replace( "<method>", d.getName() )
        .replace( "<methodConst>", this.constantize( d.getName() ) )
        .replace( "<params>", this.getCastedParamNames( d.getParameters() ) );
    }

    private List<String> generateMethodCalls()
    {
        final List<String> methods = Lists.newArrayListWithCapacity( this.methods.size() );
        for( final MethodDeclaration m : this.methods )
        {
            String methodCall = this.generateMethod( m );
            if ( methods.size() > 0 )
            {
                methodCall = methodCall.replace( "if", "else if" );
            }
            methods.add( methodCall );
        }
        return methods;
    }

    private Set<String> generateRequiredImports( final Collection<ImportDeclaration> d )
    {
        final Set<String> imports = Sets.newHashSet();
        imports.addAll( ImportResolver.resolveImports( this.compUnit.getImports(),
            BuilderUtil.getMethodTypes( this.methods ) ) );

        imports.add( "import " + this.compUnit.getPackage().getName() + "." + this.localServiceName + ";" );

        return imports;
    }

    private String getCastedParamNames( final List<Parameter> list )
    {
        if ( list == null || list.size() == 0 )
        {
            return "";
        }

        final String[] names = new String[list.size()];
        for( int x = 0; x < list.size(); x++ )
        {
            names[x] = "(" + list.get( x ).getType() + ") paramObjects[" + x + "]";
        }

        return " " + Joiner.on( ", " ).join( names ) + " ";
    }

    public final HelperBuilder setServiceName( final String serviceName )
    {
        this.serviceName = serviceName;

        if ( !serviceName.endsWith( "Service" ) )
        {
            HelperBuilder.logger.warn( "Attempting to generate delegate for file not marked as service." );
        }
        else
        {
            this.className = serviceName.replaceAll( "Service$", "" ) + "ServiceHelper";
            this.localServiceName = "I" + serviceName + "Local";
        }

        return this;
    }

    @Override
    public final String toString()
    {
        return "Class:\n" + this.generateClassDef() + "\nConstants:\n" + this.generateConstantDef();
    }
}
