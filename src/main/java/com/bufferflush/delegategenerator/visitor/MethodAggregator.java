/**
 * 
 */
package com.bufferflush.delegategenerator.visitor;

import japa.parser.ast.body.MethodDeclaration;
import japa.parser.ast.body.ModifierSet;
import japa.parser.ast.visitor.VoidVisitorAdapter;

import java.util.List;

/**
 * @author ksmith_cntr
 *
 */
public class MethodAggregator extends VoidVisitorAdapter<List<MethodDeclaration>>
{
    @Override
    public void visit( final MethodDeclaration d, final List<MethodDeclaration> arg )
    {
        if ( ModifierSet.hasModifier( d.getModifiers(), ModifierSet.PUBLIC ) )
        {
            arg.add( d );
        }
    }
}
