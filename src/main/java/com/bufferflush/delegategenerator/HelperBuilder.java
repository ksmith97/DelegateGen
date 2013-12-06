/**
 * 
 */
package com.bufferflush.delegategenerator;

import japa.parser.JavaParser;
import japa.parser.ParseException;
import japa.parser.ast.CompilationUnit;
import japa.parser.ast.body.MethodDeclaration;
import japa.parser.ast.body.Parameter;
import japa.parser.ast.type.ClassOrInterfaceType;

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
            classTemplate = CodeGenUtil.loadResourceAsString( "/HelperClassTemplate" );
            callTemplate = CodeGenUtil.loadResourceAsString( "/CallTemplate" );
            constantTemplate = CodeGenUtil.loadResourceAsString( "/ConstantTemplate" );
        }
        catch( final IOException e )
        {
            HelperBuilder.logger.error( "Faild to load Templates!", e );
            throw new Error( "Failed to load templates.", e );
        }
    }

    private static String generateClassName( final String serviceName )
    {
        return (serviceName.endsWith( "Service" ) ? serviceName.replaceAll( "Service$", "" ) : serviceName) + "ServiceHelper";
    }

    private static String generateEjbLookup( final String packageName, final String serviceName )
    {
        return "ejblocal:" + packageName + "." + serviceName;
    }

    private static String generateLocalClassName( final String serviceName )
    {
        return "I" + serviceName + "Local";
    }

    public static void parseFile( final Reader in, final Writer delegateClass, final Set<String> constants )
    throws IOException,
    ParseException
    {
        final CompilationUnit cu = JavaParser.parse( in );

        final HelperBuilder builder = new HelperBuilder( cu );

        delegateClass.write( builder.generateClassDef() );
        delegateClass.flush();
        constants.addAll( builder.generateConstantDef() );
    }

    private final CompilationUnit compUnit;

    private final Map<String, String> constants = Maps.newHashMap();

    public HelperBuilder(final CompilationUnit cu)
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
        return scope + Character.toUpperCase( str.charAt( 0 ) ) + str.substring( 1 );
    }

    public String generateClassDef()
    {
        final String serviceName = CodeGenUtil.getClassName( this.compUnit );
        final String className = HelperBuilder.generateClassName(serviceName);
        final String localServiceName = HelperBuilder.generateLocalClassName( serviceName );
        final String ejbLookup = HelperBuilder.generateEjbLookup( this.compUnit.getPackage().getName().toString(),
            serviceName );
        final Collection<MethodDeclaration> methods = new TreeSet<MethodDeclaration>(
        new CodeGenUtil.ParameterComparator() );
        methods.addAll( CodeGenUtil.getMethods( this.compUnit ) );

        final ImportResolver resolver = new ImportResolver(this.compUnit.getImports());
        final Collection<String> imports = resolver.resolve( CodeGenUtil.getMethodTypes( methods ) );
        imports.addAll( resolver.resolve( new ClassOrInterfaceType( localServiceName ) ) );

        return HelperBuilder.classTemplate
        .replace( "<className>", className )
        .replace( "<date>", new SimpleDateFormat( "MM/dd/yyyy" ).format( new Date() ) )
        .replace( "<methodCalls>", Joiner.on( "" ).join( this.generateMethodCalls( methods, serviceName ) ) )
        .replace( "<serviceName>", serviceName )
        .replace( "<localClass>", localServiceName )
        .replace( "<localEjbName>", this.constantize( ejbLookup, "localEjbLookup", serviceName ) )
        .replace( "<imports>", Joiner.on( "\n" ).join( imports ) );
    }

    public List<String> generateConstantDef()
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

            constantValues[x] = HelperBuilder.constantTemplate
            .replace( "<name>", this.createConstant( con.getKey(), serviceName ) )
            .replace( "<value>", con.getValue() );
        }

        Arrays.sort( constantValues );
        return constantValues;
    }

    private String generateMethod( final MethodDeclaration d, final String serviceName )
    {
        return HelperBuilder.callTemplate
        .replace( "<method>", d.getName() )
        .replace( "<methodConst>", this.constantize( d.getName(), serviceName ) )
        .replace( "<params>", this.getCastedParamNames( d.getParameters() ) );
    }

    private List<String> generateMethodCalls( final Collection<MethodDeclaration> methods, final String serviceName )
    {
        final List<String> methodStrs = Lists.newArrayListWithCapacity( methods.size() );
        for( final MethodDeclaration m : methods )
        {
            String methodCall = this.generateMethod( m, serviceName );
            if ( methodStrs.size() > 0 )
            {
                methodCall = methodCall.replace( "if", "else if" );
            }
            methodStrs.add( methodCall );
        }
        return methodStrs;
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

    @Override
    public final String toString()
    {
        return "Class:\n" + this.generateClassDef() + "\nConstants:\n" + this.generateConstantDef();
    }
}
