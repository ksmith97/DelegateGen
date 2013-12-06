/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.bufferflush.delegategenerator;

import japa.parser.JavaParser;
import japa.parser.ParseException;
import japa.parser.ast.CompilationUnit;
import japa.parser.ast.body.MethodDeclaration;
import japa.parser.ast.body.Parameter;
import japa.parser.ast.expr.NameExpr;
import japa.parser.ast.type.VoidType;

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
import java.util.TreeSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.benecard.pbm.codegen.CodeGenUtil;
import com.benecard.pbm.codegen.ImportResolver;
import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

/**
 * 
 * @author Kevin Smith
 */
public class DelegateBuilder
{
    private static final String classTemplate;
    private static final String constantTemplate;
    private static final Logger logger = LoggerFactory.getLogger( DelegateBuilder.class );
    private static final String methodExTemplate;
    private static final String methodTemplate;

    static
    {
        try
        {
            classTemplate = CodeGenUtil.loadResourceAsString( "/DelegateClassTemplate" );
            methodTemplate = CodeGenUtil.loadResourceAsString( "/MethodTemplate" );
            methodExTemplate = CodeGenUtil.loadResourceAsString( "/MethodTemplateBeneEx" );
            constantTemplate = CodeGenUtil.loadResourceAsString( "/ConstantTemplate" );
        }
        catch( final IOException e )
        {
            DelegateBuilder.logger.error( "Failed to load Templates!", e );
            throw new Error( "Failed to load templates.", e );
        }
    }

    private static String generateClassName( final String serviceName )
    {
        return ( serviceName.endsWith( "Service" ) ? serviceName.replaceAll( "Service$", "" ) : serviceName )
        + "Delegate";
    }

    public static void parseFile( final Reader in, final Writer out, final Set<String> constants )
    throws IOException,
    ParseException
    {
        final CompilationUnit cu = JavaParser.parse( in );

        final DelegateBuilder builder = new DelegateBuilder( cu );

        out.write( builder.generateClassDef() );
        out.flush();
        constants.addAll( builder.generateConstantDef() );
    }

    private final CompilationUnit compUnit;
    private final Map<String, String> constants = Maps.newHashMap();

    public DelegateBuilder( final CompilationUnit cu )
    {
        this.compUnit = cu;
    }

    private String constantize( final String value, final String scope )
    {
        return this.constantize( value, value, scope );
    }

    private String constantize( final String value, final String key, final String scope )
    {
        final String existingValue = this.constants.get( key );
        if ( existingValue == null )
        {
            this.constants.put( key, value );
        }
        else if ( !existingValue.equals( value ) )
        {
            throw new RuntimeException( "Attempted to create a new constant with an existing name. Name: " + key
                + " existing value: " + existingValue + " NewValue:" + value );
        }
        //We don't care if they are inserting the same value as a pre existing value.

        return "ServiceConstants." + this.createConstant( key, scope );
    }

    private String createConstant( final String str, final String scope )
    {
        return ( str.equals( scope ) ? "" : scope ) + Character.toUpperCase( str.charAt( 0 ) ) + str.substring( 1 );
    }

    private String generateClassDef()
    {
        final String serviceName = CodeGenUtil.getClassName( this.compUnit );
        final String className = DelegateBuilder.generateClassName( serviceName );
        final String packageName = this.compUnit.getPackage().getName().getName().toString();

        final Collection<MethodDeclaration> methods = new TreeSet<MethodDeclaration>( new CodeGenUtil.ParameterComparator() );
        methods.addAll( CodeGenUtil.getMethods( this.compUnit ) );

        final Collection<String> imports = ImportResolver.resolveImports( this.compUnit.getImports(), CodeGenUtil.getMethodTypes( methods ) );

        return DelegateBuilder.classTemplate
        .replace( "<className>", className )
        .replace( "<date>", new SimpleDateFormat( "MM/dd/yyyy" ).format( new Date() ) )
        .replace( "<methods>", Joiner.on( "\n" ).join( this.generateMethods( methods, serviceName ) ) )
        .replace( "<serviceName>", serviceName )
        .replace( "<packageName>", packageName )
        .replace( "<imports>", Joiner.on( "\n" ).join( imports ) );
    }

    private List<String> generateConstantDef()
    {
        return Arrays.asList( this.generateConstants( this.constants, CodeGenUtil.getClassName( this.compUnit ) ) );
    }

    private String[] generateConstants( final Map<String, String> constants, final String serviceName )
    {
        final Object[] values = constants.entrySet().toArray();
        final String[] constantValues = new String[values.length];
        for( int x = 0; x < values.length; x++ )
        {
            @SuppressWarnings( "unchecked" )
            final Map.Entry<String, String> con = (Entry<String, String>) values[x];

            constantValues[x] = DelegateBuilder.constantTemplate
            .replace( "<name>", this.createConstant( con.getKey(), serviceName ) )
            .replace( "<value>", con.getValue() );
        }

        Arrays.sort( constantValues );
        return constantValues;
    }

    private String generateMethod( final MethodDeclaration d, final String serviceName )
    {
        try
        {
            return this.getMethodString( d )
            .replace( "<returnType>", d.getType().toString() )
            .replace( "<return>", ( d.getType() instanceof VoidType ) ? "" : "return" )
            .replace( "<methodName>", d.getName() )
            .replace( "<methodNameConst>", this.constantize( d.getName(), serviceName ) )
            .replace( "<serviceNameConst>", this.constantize( serviceName, "" ) )
            .replace( "<javadoc>", this.generateMethodComment( d ) )
            .replace( "<params>", d.getParameters() != null ? Joiner.on( ", " ).join( d.getParameters() ) : "" )
            .replace( "<paramNames>", this.getParamNames( d.getParameters() ) );
        }
        catch( final ParseException e )
        {
            DelegateBuilder.logger.error( "An exception occured when parsing method declaration in service "
            + serviceName, e );
            throw new RuntimeException( "Failed to parse service." );
        }
    }

    private String generateMethodComment( final MethodDeclaration d )
    {
        if ( d.getComment() == null || d.getComment().toString().isEmpty() )
        {
            return "/**\n"
            + "\t * No comment exists for this service method.\n"
            + "\t */";
        }

        return d.getComment().toString().trim();
    }

    private Collection<String> generateMethods( final Collection<MethodDeclaration> methods, final String serviceName )
    {
        final Collection<String> genMethods = Lists.newArrayListWithCapacity( methods.size() );
        for( final MethodDeclaration m : methods )
        {
            genMethods.add( this.generateMethod( m, serviceName ) );
        }
        return genMethods;
    }

    private String getMethodString( final MethodDeclaration d ) throws ParseException
    {
        if ( d.getThrows() != null )
        {
            for( final NameExpr exp : d.getThrows() )
            {
                if ( "BenecardException".equals( exp.getName() ) )
                {
                    return DelegateBuilder.methodExTemplate;
                }
                else
                {
                    return DelegateBuilder.methodTemplate;
                }
            }
        }

        return DelegateBuilder.methodTemplate;
    }

    private String getParamNames( final List<Parameter> list )
    {
        if ( list == null || list.isEmpty() )
        {
            return "";
        }

        final String[] names = new String[list.size()];
        for( int x = 0; x < list.size(); x++ )
        {
            names[x] = list.get( x ).getId().getName();
        }

        return ", " + Joiner.on( ", " ).join( names );
    }

    @Override
    public String toString()
    {
        return "Class:\n" + this.generateClassDef() + "\nConstants:\n" + this.generateConstantDef();
    }
}
