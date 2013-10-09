/**
 * 
 */
package com.bufferflush.delegategenerator.visitor;

import japa.parser.ast.body.ClassOrInterfaceDeclaration;
import japa.parser.ast.body.ModifierSet;
import japa.parser.ast.visitor.VoidVisitorAdapter;

/**
 * @author ksmith_cntr
 * 
 *         Retrieves the class name for the class. I believe this technically will
 *         iterate over inner classes as well but just so long as they are private it
 *         should all work out.
 */
public class ClassNameRetriever extends VoidVisitorAdapter<String>
{

    @Override
    public void visit( final ClassOrInterfaceDeclaration d, String arg )
    {
        if ( ModifierSet.hasModifier( d.getModifiers(), ModifierSet.PUBLIC ) )
        {
            arg = d.getName();
        }
    }
}
