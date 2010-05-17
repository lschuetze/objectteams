/*******************************************************************************
 * Copyright (c) 2000, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * $Id: SingleTypeReference.java 23405 2010-02-03 17:02:18Z stephan $
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Fraunhofer FIRST - extended API and implementation
 *     Technical University Berlin - extended API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.compiler.ast;

import org.eclipse.jdt.internal.compiler.ASTVisitor;
import org.eclipse.jdt.internal.compiler.impl.CompilerOptions;
import org.eclipse.jdt.internal.compiler.lookup.*;
import org.eclipse.jdt.internal.compiler.problem.ProblemSeverities;
import org.eclipse.objectteams.otdt.internal.core.compiler.lookup.OTClassScope;

/**
 * OTDT changes:
 *
 * What: Support decapsulation.
 *
 * What: Support resolve using base import scope.
 */
public class SingleTypeReference extends TypeReference {

	public char[] token;

	public SingleTypeReference(char[] source, long pos) {

			this.token = source;
			this.sourceStart = (int) (pos>>>32)  ;
			this.sourceEnd = (int) (pos & 0x00000000FFFFFFFFL) ;

	}

	public TypeReference copyDims(int dim){
		//return a type reference copy of me with some dimensions
		//warning : the new type ref has a null binding

		return new ArrayTypeReference(this.token, dim,(((long)this.sourceStart)<<32)+this.sourceEnd);
	}

	public char[] getLastToken() {
		return this.token;
	}
	protected TypeBinding getTypeBinding(Scope scope) {
		if (this.resolvedType != null)
			return this.resolvedType;

		this.resolvedType = scope.getType(this.token);

		if (scope.kind == Scope.CLASS_SCOPE && this.resolvedType.isValidBinding())
			if (((ClassScope) scope).detectHierarchyCycle(this.resolvedType, this))
				return null;
		return this.resolvedType;
	}

	public char [][] getTypeName() {
		return new char[][] { this.token };
	}

	public StringBuffer printExpression(int indent, StringBuffer output){

		return output.append(this.token);
	}

	public TypeBinding resolveTypeEnclosing(BlockScope scope, ReferenceBinding enclosingType) {
		TypeBinding memberType = this.resolvedType = scope.getMemberType(this.token, enclosingType);
//{ObjectTeams: decapsulation:
		if (memberType.problemId() ==  ProblemReasons.NotVisible) {
			switch(this.getBaseclassDecapsulation()) {
			case ALLOWED:
				scope.problemReporter().decapsulation(this);
				//$FALL-THROUGH$
			case REPORTED:
				memberType= ((ProblemReferenceBinding)memberType).closestMatch();
			}
		}
// SH}
		boolean hasError = false;
		if (!memberType.isValidBinding()) {
			hasError = true;
			scope.problemReporter().invalidEnclosingType(this, memberType, enclosingType);
			memberType = ((ReferenceBinding)memberType).closestMatch();
			if (memberType == null) {
				return null;
			}
		}
		if (isTypeUseDeprecated(memberType, scope))
			reportDeprecatedType(memberType, scope);
		memberType = scope.environment().convertToRawType(memberType, false /*do not force conversion of enclosing types*/);
		if (memberType.isRawType()
				&& (this.bits & IgnoreRawTypeCheck) == 0
				&& scope.compilerOptions().getSeverity(CompilerOptions.RawTypeReference) != ProblemSeverities.Ignore){
			scope.problemReporter().rawTypeReference(this, memberType);
		}
		if (hasError) {
			// do not store the computed type, keep the problem type instead
			return memberType;
		}
		return this.resolvedType = memberType;
	}

//{ObjectTeams: for base-imported types (only single is supported):
	@Override
	public TypeBinding checkResolveUsingBaseImportScope(Scope scope) {
		if (   this.getBaseclassDecapsulation().isAllowed()
			|| scope.isBaseGuard())
		{
			TypeBinding problem = this.resolvedType;
			this.resolvedType = null; // force re-computation in getTypeBinding()
			Scope currentScope = scope;
			while (currentScope != null) {
				if (currentScope instanceof OTClassScope) {
					Scope baseImportScope = ((OTClassScope)currentScope).getBaseImportScope();
					if (baseImportScope != null) {
						this.resolvedType = getTypeBinding(baseImportScope);
						if (this.resolvedType != null && this.resolvedType.isValidBinding())
							return this.resolvedType = checkResolvedType(this.resolvedType, baseImportScope, false);
					}
				}
				currentScope = currentScope.parent;
			}
			this.resolvedType = problem;
		}
		return null;
	}
// SH}

	public void traverse(ASTVisitor visitor, BlockScope scope) {
		visitor.visit(this, scope);
		visitor.endVisit(this, scope);
	}

	public void traverse(ASTVisitor visitor, ClassScope scope) {
		visitor.visit(this, scope);
		visitor.endVisit(this, scope);
	}
}
