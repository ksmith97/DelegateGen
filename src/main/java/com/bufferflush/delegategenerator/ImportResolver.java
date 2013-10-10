/**
 * 
 */
package com.bufferflush.delegategenerator;

import japa.parser.ParseException;
import japa.parser.ast.ImportDeclaration;
import japa.parser.ast.type.ClassOrInterfaceType;
import japa.parser.ast.type.ReferenceType;
import japa.parser.ast.type.Type;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

/**
 * @author ksmith_cntr
 *
 */
public class ImportResolver
{
    private static final Logger logger = LoggerFactory.getLogger( ImportResolver.class );
    private final Map<Type, Collection<String>> cache;
    private final Collection<ImportDeclaration> imports;

    public ImportResolver( final Collection<ImportDeclaration> d )
    {
        this.imports = d;
        this.cache = Maps.newHashMap();
    }

    private String getImportForType( final ClassOrInterfaceType classType )
    throws ParseException
    {
        final String typeStr = classType.toString().trim();
        if ( this.isFullyQualifiedType( typeStr ) )
        {
            return classType.toString().trim();
        }
        else
        {
            for( final ImportDeclaration dec : this.imports )
            {
                if ( dec.getName().getName().equals( typeStr ) )
                {
                    return dec.toString().trim();
                }
            }
        }

        throw new ParseException( "Could not find import for type: " + typeStr );
    }

    private Set<String> getImportsForType( final ReferenceType type )
    {
        final Set<String> imports = Sets.newHashSet();

        for( final ClassOrInterfaceType classType : this.resolveTypes( type ) )
        {
            try
            {
                imports.add( this.getImportForType( classType ) );
            }
            catch( final ParseException e )
            {
                ImportResolver.logger.warn( "Failed to retrieve import for type this may because it is in java.lang.",
                    e );
            }
        }

        return imports;
    }


    /**
     * Predicate to determine if a type is fully qualified.
     * Its currently a hack as it does not distinguish between fully and partial qualification.
     * 
     * @param type
     * @return
     */
    private boolean isFullyQualifiedType( final String type )
    {
        return type.contains( "." );
    }

    public final Collection<String> resolve( final Type t )
    {
        if ( ! ( t instanceof ReferenceType ) )
        {
            return Sets.newHashSetWithExpectedSize( 0 );
        }

        if ( this.cache.containsKey( t ) )
        {
            return this.cache.get( t );
        }

        return this.getImportsForType( (ReferenceType) t );
    }

    /**
     * This function explodes the type with possible generic arguments into a list of all the ClassOrInterfaceTypes
     * found.
     * We only care about ClassOrInterfaceTypes since they are the only ones that may need importing.
     * 
     * @param type
     * @return
     */
    private List<ClassOrInterfaceType> resolveTypes( final ReferenceType type )
    {
        final List<ClassOrInterfaceType> types = Lists.newArrayList();
        if ( type instanceof ReferenceType )
        {
            final ReferenceType refType = type;

            if ( refType.getType() instanceof ClassOrInterfaceType )
            {
                final ClassOrInterfaceType coit = (ClassOrInterfaceType) refType.getType();

                types.add( coit );

                if ( coit.getTypeArgs() != null )
                {
                    for( final Type t : coit.getTypeArgs() )
                    {
                        types.addAll( this.resolveTypes( t ) );
                    }
                }
            }
        }

        return types;
    }
}
