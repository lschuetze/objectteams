/*******************************************************************************
 * Copyright (c) 2000, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Fraunhofer FIRST - extended API and implementation
 *     Technical University Berlin - extended API and implementation
 *     Stephan Herrmann - Contribution for
 *								bug 383368 - [compiler][null] syntactic null analysis for field references
 *								Bug 392238 - [1.8][compiler][null] Detect semantically invalid null type annotations
 *								Bug 416307 - [1.8][compiler][null] subclass with type parameter substitution confuses null checking
 *								Bug 392099 - [1.8][compiler][null] Apply null annotation on types for null analysis
 *								Bug 417295 - [1.8[[null] Massage type annotated null analysis to gel well with deep encoded type bindings.
 *								Bug 392238 - [1.8][compiler][null] Detect semantically invalid null type annotations
 *								Bug 435570 - [1.8][null] @NonNullByDefault illegally tries to affect "throws E"
 *								Bug 466713 - Null Annotations: NullPointerException using <int @Nullable []> as Type Param
 *        Andy Clement - Contributions for
 *                          Bug 383624 - [1.8][compiler] Revive code generation support for type annotations (from Olivier's work)
 *******************************************************************************/
package org.eclipse.jdt.internal.compiler.ast;

import org.eclipse.jdt.internal.compiler.ASTVisitor;
import org.eclipse.jdt.internal.compiler.ast.TypeReference.AnnotationPosition;
import org.eclipse.jdt.internal.compiler.codegen.*;
import org.eclipse.jdt.internal.compiler.flow.*;
import org.eclipse.jdt.internal.compiler.impl.Constant;
import org.eclipse.jdt.internal.compiler.lookup.*;
import org.eclipse.objectteams.otdt.core.exceptions.InternalCompilerError;
import org.eclipse.objectteams.otdt.internal.core.compiler.lookup.DependentTypeBinding;
import org.eclipse.objectteams.otdt.internal.core.compiler.model.TeamModel;
import org.eclipse.objectteams.otdt.internal.core.compiler.statemachine.transformer.StandardElementGenerator;
import org.eclipse.objectteams.otdt.internal.core.compiler.util.RoleTypeCreator;

/**
 * OTDT changes:
 *
 * What: support special semantics for role types.
 * How:  If required create an alternate "roleCheckExpr" and delegate to that expression.
 * Participants:
 *	 		checkCastTypesCompatibility(): detects necessity and call createRoleCheck()
 * 			analyseCode(), generateCode() :  delegate
 * Why:  We don't use a special AST-type here, because necessity of role checks
 *       only arises during resolve, which is too late for TransformStatementsVisitor.
 *       Also InsertTypesAdjustmentsVisitor is not easy to use here, because detection
 *       of this case differs from those spotted by RoleTypeBinding.isCompatibleWith().
 *
 * @version $Id: InstanceOfExpression.java 23405 2010-02-03 17:02:18Z stephan $
 */
public class InstanceOfExpression extends OperatorExpression {

	public Expression expression;
	public TypeReference type;

//{ObjectTeams alternate expression:
	/** when comparing role types this expression actually replaces "this": */
	private Expression roleCheckExpr = null;
// SH}

public InstanceOfExpression(Expression expression, TypeReference type) {
	this.expression = expression;
	this.type = type;
	type.bits |= IgnoreRawTypeCheck; // https://bugs.eclipse.org/bugs/show_bug.cgi?id=282141
	this.bits |= INSTANCEOF << OperatorSHIFT;
	this.sourceStart = expression.sourceStart;
	this.sourceEnd = type.sourceEnd;
}

public FlowInfo analyseCode(BlockScope currentScope, FlowContext flowContext, FlowInfo flowInfo) {
//{ObjectTeams: is it a role type check?
		if (this.roleCheckExpr != null)
			return this.roleCheckExpr.analyseCode(currentScope, flowContext, flowInfo);
// SH}
	LocalVariableBinding local = this.expression.localVariableBinding();
	if (local != null && (local.type.tagBits & TagBits.IsBaseType) == 0) {
		flowInfo = this.expression.analyseCode(currentScope, flowContext, flowInfo).
			unconditionalInits();
		FlowInfo initsWhenTrue = flowInfo.copy();
		initsWhenTrue.markAsComparedEqualToNonNull(local);
		flowContext.recordUsingNullReference(currentScope, local,
				this.expression, FlowContext.CAN_ONLY_NULL | FlowContext.IN_INSTANCEOF, flowInfo);
		// no impact upon enclosing try context
		return FlowInfo.conditional(initsWhenTrue, flowInfo.copy());
	}
	if (this.expression instanceof Reference && currentScope.compilerOptions().enableSyntacticNullAnalysisForFields) {
		FieldBinding field = ((Reference)this.expression).lastFieldBinding();
		if (field != null && (field.type.tagBits & TagBits.IsBaseType) == 0) {
			flowContext.recordNullCheckedFieldReference((Reference) this.expression, 1);
		}
	}
	return this.expression.analyseCode(currentScope, flowContext, flowInfo).
			unconditionalInits();
}

//{ObjectTeams: check alternative realization
	@Override
	boolean handledByGeneratedMethod(Scope scope, TypeBinding castType, TypeBinding expressionType)
	{
		if (castType.isRole() && expressionType instanceof ReferenceBinding)
		{
			if (TeamModel.isComparableToRole((ReferenceBinding)expressionType, (ReferenceBinding)castType))
			{
				if (!(castType instanceof DependentTypeBinding))
					castType = RoleTypeCreator.maybeWrapUnqualifiedRoleType(scope, castType, this);

				DependentTypeBinding roleCastType = (DependentTypeBinding)castType;
				if (roleCastType.hasEquivalentAnchorTo(expressionType))
					return false;
				if (! (scope instanceof BlockScope))
					throw new InternalCompilerError("can't create roleCheck without BlockScope"); //$NON-NLS-1$
				// FIXME(SH) can we do better? (see TypeBinding.isCastCompatible() as client of this method)
				this.roleCheckExpr = StandardElementGenerator.createRoleInstanceOfCheck(
						(BlockScope)scope, this, (ReferenceBinding)expressionType, roleCastType);
				return true;
			}
		}
		return false;
	}
// SH}

/**
 * Code generation for instanceOfExpression
 *
 * @param currentScope org.eclipse.jdt.internal.compiler.lookup.BlockScope
 * @param codeStream org.eclipse.jdt.internal.compiler.codegen.CodeStream
 * @param valueRequired boolean
*/
public void generateCode(BlockScope currentScope, CodeStream codeStream, boolean valueRequired) {
//{ObjectTeams: is it a role type check?
	if (this.roleCheckExpr != null) {
		this.roleCheckExpr.generateCode(currentScope, codeStream, valueRequired);
		return;
	}
// SH}
	int pc = codeStream.position;
	this.expression.generateCode(currentScope, codeStream, true);
	codeStream.instance_of(this.type, this.type.resolvedType);
	if (valueRequired) {
		codeStream.generateImplicitConversion(this.implicitConversion);
	} else {
		codeStream.pop();
	}
	codeStream.recordPositionsFrom(pc, this.sourceStart);
}

public StringBuffer printExpressionNoParenthesis(int indent, StringBuffer output) {
	this.expression.printExpression(indent, output).append(" instanceof "); //$NON-NLS-1$
	return this.type.print(0, output);
}

public TypeBinding resolveType(BlockScope scope) {
	this.constant = Constant.NotAConstant;
	TypeBinding expressionType = this.expression.resolveType(scope);
	TypeBinding checkedType = this.type.resolveType(scope, true /* check bounds*/);
	if (expressionType != null && checkedType != null && this.type.hasNullTypeAnnotation(AnnotationPosition.ANY)) {
		// don't complain if the entire operation is redundant anyway
		if (!expressionType.isCompatibleWith(checkedType) || NullAnnotationMatching.analyse(checkedType, expressionType, -1).isAnyMismatch())
			scope.problemReporter().nullAnnotationUnsupportedLocation(this.type);
	}
	if (expressionType == null || checkedType == null)
		return null;

	if (!checkedType.isReifiable()) {
		scope.problemReporter().illegalInstanceOfGenericType(checkedType, this);
	} else if (checkedType.isValidBinding()) {
		// if not a valid binding, an error has already been reported for unresolved type
		if ((expressionType != TypeBinding.NULL && expressionType.isBaseType()) // disallow autoboxing
				|| !checkCastTypesCompatibility(scope, checkedType, expressionType, null)) {
			scope.problemReporter().notCompatibleTypesError(this, expressionType, checkedType);
		}
	}
	return this.resolvedType = TypeBinding.BOOLEAN;
}

/**
 * @see org.eclipse.jdt.internal.compiler.ast.Expression#tagAsUnnecessaryCast(Scope,TypeBinding)
 */
public void tagAsUnnecessaryCast(Scope scope, TypeBinding castType) {
	// null is not instanceof Type, recognize direct scenario
	if (this.expression.resolvedType != TypeBinding.NULL)
		scope.problemReporter().unnecessaryInstanceof(this, castType);
}

public void traverse(ASTVisitor visitor, BlockScope scope) {
	if (visitor.visit(this, scope)) {
		this.expression.traverse(visitor, scope);
		this.type.traverse(visitor, scope);
	}
	visitor.endVisit(this, scope);
}
}
