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

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

/**
 *
 * @author Kevin Smith
 */
public class DelegateBuilder {
    private static final String classTemplate;
    private static final String constantTemplate;
    private static final Logger logger = LoggerFactory.getLogger(DelegateBuilder.class);
    private static final String methodExTemplate;
    private static final String methodTemplate;

    static
    {
        try
        {
            classTemplate = BuilderUtil.loadResourceAsString( "/DelegateClassTemplate" );
            methodTemplate = BuilderUtil.loadResourceAsString( "/MethodTemplate" );
            methodExTemplate = BuilderUtil.loadResourceAsString( "/MethodTemplateBeneEx" );
            constantTemplate = BuilderUtil.loadResourceAsString( "/ConstantTemplate" );
        }
        catch( final IOException e )
        {
            DelegateBuilder.logger.error( "Failed to load Templates!", e );
            throw new Error( "Failed to load templates.", e );
        }
    }

    public static void parseFile( final Reader in, final Writer delegateClass, final Set<String> constants )
    throws IOException,
    ParseException
    {
        final CompilationUnit cu = JavaParser.parse( in );

        final DelegateBuilder builder = new DelegateBuilder( cu )
        .setPackageName( cu.getPackage().getName().getName() )
        .addMethods( BuilderUtil.getMethods( cu ) )
        .setServiceName( BuilderUtil.getClassName( cu ) );

        delegateClass.write( builder.generateClassDef() );
        delegateClass.flush();
        constants.addAll( builder.generateConstantDef() );
    }

    private String className = "";
    private final StringBuilder classStr = new StringBuilder(DelegateBuilder.classTemplate);
    private final CompilationUnit compUnit;
    private final Set<String> constants = Sets.newHashSet();
    private final SortedSet<MethodDeclaration> methods = new TreeSet<MethodDeclaration>(
    new BuilderUtil.ParameterComparator() );
    private String packageName = "";
    private String serviceName = "";

    public DelegateBuilder( final CompilationUnit cu )
    {
        this.compUnit = cu;
    }

    public DelegateBuilder addMethod(final MethodDeclaration m)
    {
        this.methods.add(m);
        return this;
    }

    public DelegateBuilder addMethods( final Collection<MethodDeclaration> m )
    {
        this.methods.addAll( m );
        return this;
    }

    private String constantize(final String value)
    {
        this.constants.add( value );
        return "ServiceConstants." + this.createConstant( value );
    }

    private String createConstant(final String str)
    {
        return ( this.serviceName.equals( str ) ? "" : this.serviceName )
        + Character.toUpperCase( str.charAt( 0 ) ) + str.substring( 1 );
    }

    private String generateClassDef()
    {
        if ( StringUtils.isBlank( this.className ) )
        {
            DelegateBuilder.logger.error( "The class name is blank." );
        }
        if ( StringUtils.isBlank( this.serviceName ) )
        {
            DelegateBuilder.logger.error( "The service name is blank." );
        }
        if ( StringUtils.isBlank( this.packageName ) )
        {
            DelegateBuilder.logger.error( "The package name is blank." );
        }
        return this.classStr.toString()
        .replace( "<className>", this.className )
        .replace( "<date>", new SimpleDateFormat( "MM/dd/yyyy" ).format( new Date() ) )
        .replace( "<methods>", Joiner.on( "\n" ).join( this.generateMethods() ) )
        .replace( "<serviceName>", this.serviceName )
        .replace( "<packageName>", this.packageName )
        .replace( "<imports>",
            Joiner.on( "\n" ).join( this.generateRequiredImports() ) );
    }

    private List<String> generateConstantDef()
    {
        return Arrays.asList( this.generateConstants( this.constants ) );
    }

    private String[] generateConstants( final Collection<String> coll )
    {
        final Object[] values = coll.toArray();
        final String[] constantValues = new String[values.length];
        for( int x = 0; x < values.length; x++ )
        {
            final String con = (String) values[x];

            constantValues[x] = DelegateBuilder.constantTemplate
            .replace( "<name>", this.createConstant( con ) )
            .replace("<value>", con);
        }

        Arrays.sort( constantValues );
        return constantValues;
    }

    private String generateMethod( final MethodDeclaration d )
    {
        return this.getMethodString( d )
        .replace( "<returnType>", d.getType().toString() )
        .replace( "<return>", d.getType().toString().equals( "void" ) ? "" : "return" )
        .replace( "<methodName>", d.getName() )
        .replace( "<methodNameConst>", this.constantize( d.getName() ) )
        .replace( "<serviceNameConst>", this.constantize( this.serviceName ) )
        .replace( "<javadoc>", this.generateMethodComment( d ) )
        .replace( "<params>", d.getParameters() != null ? Joiner.on( ", " ).join( d.getParameters() ) : "" )
        .replace( "<paramNames>", this.getParamNames( d.getParameters() ) );
    }

    private String generateMethodComment(final MethodDeclaration d)
    {
        if(d.getComment() == null || d.getComment().toString().isEmpty())
        {
            return "/**\n"
            + "\t * No comment exists for this service method.\n"
            + "\t */";
        }

        return d.getComment().toString().trim();
    }

    private List<String> generateMethods()
    {
        final List<String> genMethods = Lists.newArrayListWithCapacity( this.methods.size() );
        for(final MethodDeclaration m : this.methods)
        {
            genMethods.add( this.generateMethod( m ));
        }
        return genMethods;
    }

    private Set<String> generateRequiredImports()
    {
        //We use a tree set so we have non duplicated sorted results.
        final Set<String> reqImports = Sets.newTreeSet();
        for( final MethodDeclaration md : this.methods )
        {
            reqImports.addAll( this.getMethodRequiredImports( md ) );
        }

        return reqImports;
    }

    private Set<String> getMethodRequiredImports( final MethodDeclaration m )
    {
        if ( (m.getParameters() == null || m.getParameters().isEmpty()) && m.getType().toString().equals("void") )
        {
            return Sets.newHashSetWithExpectedSize( 0 );
        }

        final ImportResolver resolver = new ImportResolver( this.compUnit.getImports() );

        final Set<String> reqImports = Sets.newHashSet();
        if ( m.getParameters() != null && m.getParameters().size() > 0 )
        {
            for(final Parameter p : m.getParameters())
            {
                reqImports.addAll( resolver.resolve( p.getType() ) );
            }
        }

        if ( !m.getType().toString().equals( "void" ) )
        {
            reqImports.addAll( resolver.resolve( m.getType() ) );
        }

        return reqImports;
    }

    private String getMethodString(final MethodDeclaration d)
    {
        if ( d.getThrows() != null )
        {
            for( final NameExpr exp : d.getThrows() )
            {
                if ( "BenecardException".equals( exp.getName() ) )
                {
                    return DelegateBuilder.methodExTemplate;
                }
                else if ( !"Exception".equals( exp.getName() ) )
                {
                    throw new Error( "Unsupported Exception found on service method. Service: "
                    + this.serviceName + " Method: " + d.getName() + " Exception: " + exp.getName() );
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



    public final DelegateBuilder setPackageName(final String packageName)
    {
        this.packageName = packageName.startsWith(".") ? packageName.substring(1) : packageName;
        return this;
    }

    public final DelegateBuilder setServiceName(final String serviceName)
    {
        this.serviceName = serviceName;

        if(!serviceName.endsWith("Service"))
        {
            DelegateBuilder.logger.warn("Attempting to generate delegate for file not marked as service.");
        }
        else
        {
            this.className = serviceName.replaceAll( "Service$", "" ) + "Delegate";
        }

        return this;
    }

    @Override
    public String toString()
    {
        return "Class:\n" + this.generateClassDef() + "\nConstants:\n" + this.generateConstantDef();
    }
}
