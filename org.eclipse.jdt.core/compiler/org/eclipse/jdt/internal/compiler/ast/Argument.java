/*******************************************************************************
 * Copyright (c) 2000, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * This is an implementation of an early-draft specification developed under the Java
 * Community Process (JCP) and is made available for testing and evaluation purposes
 * only. The code is not compatible with any specification of the JCP.
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Fraunhofer FIRST - extended API and implementation
 *     Technical University Berlin - extended API and implementation
 *     Stephan Herrmann - Contributions for
 *								bug 186342 - [compiler][null] Using annotations for null checking
 *								bug 365519 - editorial cleanup after bug 186342 and bug 365387
 *								Bug 417295 - [1.8[[null] Massage type annotated null analysis to gel well with deep encoded type bindings.
 *        Andy Clement (GoPivotal, Inc) aclement@gopivotal.com - Contributions for
 *                          Bug 409246 - [1.8][compiler] Type annotations on catch parameters not handled properly
 *******************************************************************************/
package org.eclipse.jdt.internal.compiler.ast;

import org.eclipse.jdt.core.compiler.CharOperation;
import org.eclipse.jdt.internal.compiler.ASTVisitor;
import org.eclipse.jdt.internal.compiler.classfmt.ClassFileConstants;
import org.eclipse.jdt.internal.compiler.impl.Constant;
import org.eclipse.jdt.internal.compiler.lookup.*;

/**
 * OTDT changes:
 *
 * What: DISABLED: resolved declared lifting type (see LiftingTypeReference.resolveType()).
 *
 * What: avoid duplicate addition to the method scope.
 *
 * @version $Id: Argument.java 23137 2009-12-04 20:03:06Z stephan $
 */
public class Argument extends LocalDeclaration {

	// prefix for setter method (to recognize special hiding argument)
	private final static char[] SET = "set".toCharArray(); //$NON-NLS-1$

	public Argument(char[] name, long posNom, TypeReference tr, int modifiers) {

		super(name, (int) (posNom >>> 32), (int) posNom);
		this.declarationSourceEnd = (int) posNom;
		this.modifiers = modifiers;
		this.type = tr;
		if (tr != null) {
			this.bits |= (tr.bits & ASTNode.HasTypeAnnotations);
		}
		this.bits |= (IsLocalDeclarationReachable | IsArgument);
	}

	public Argument(char[] name, long posNom, TypeReference tr, int modifiers, boolean typeElided) {

		super(name, (int) (posNom >>> 32), (int) posNom);
		this.declarationSourceEnd = (int) posNom;
		this.modifiers = modifiers;
		this.type = tr;
		if (tr != null) {
			this.bits |= (tr.bits & ASTNode.HasTypeAnnotations);
		}
		this.bits |= (IsLocalDeclarationReachable | IsArgument | IsTypeElided);
	}

//{ObjectTeams: consistent updating of name
	public void updateName(char[] newName) {
		this.name = newName;
		if (this.binding != null)
			this.binding.name = newName;
	}
// SH}

	public TypeBinding createBinding(MethodScope scope, TypeBinding typeBinding) {
		if (this.binding == null) {
			// for default constructors and fake implementation of abstract methods 
			this.binding = new LocalVariableBinding(this, typeBinding, this.modifiers, scope);
//{ObjectTeams: NPE occurred in Java5.testA119_nestedValueParameter8() ff.
/* orig:
//	    When we are called from RoleTypeCreator.resolveTypeAnchoredToArgument(AbstractMethodDeclaration, int)
//		we reach here, before resolveTypesFor has had a chance to set the binding's type.
		} else if (!this.binding.type.isValidBinding()) {
  :giro */
		} else if (this.binding.type != null && !this.binding.type.isValidBinding()) {
// SH}
			AbstractMethodDeclaration methodDecl = scope.referenceMethod();
			if (methodDecl != null) {
				MethodBinding methodBinding = methodDecl.binding;
				if (methodBinding != null) {
					methodBinding.tagBits |= TagBits.HasUnresolvedArguments;
				}
			}
		}
		if ((this.binding.tagBits & TagBits.AnnotationResolved) == 0) {
			resolveAnnotations(scope, this.annotations, this.binding, true);
			if (scope.compilerOptions().sourceLevel >= ClassFileConstants.JDK1_8) {
				Annotation.isTypeUseCompatible(this.type, scope, this.annotations);
				scope.validateNullAnnotation(this.binding.tagBits, this.type, this.annotations);
			}
		}
		this.binding.declaration = this;
		return this.binding.type; // might have been updated during resolveAnnotations (for typeAnnotations)
	}

	public TypeBinding bind(MethodScope scope, TypeBinding typeBinding, boolean used) {
		TypeBinding newTypeBinding = createBinding(scope, typeBinding); // basically a no-op if createBinding() was called before

		// record the resolved type into the type reference
//{ObjectTeams:
		// might have been processed before via RoleTypeBinding.getArgumentType()
		// Note(SH): cannot use this.binding to detect this because
		// SourceTypeBinding.resolveTypesFor already sets the binding,
		// without entering it into the method scope.
		if ((this.bits & ASTNode.HasBeenResolved) != 0)
			return this.binding.type;
		this.bits |= ASTNode.HasBeenResolved;

//		// lifting type needs some further analysis now:
//        if (this.type != null && this.type.isDeclaredLifting())
//            this.type.resolveType(scope);
// SH}

		Binding existingVariable = scope.getBinding(this.name, Binding.VARIABLE, this, false /*do not resolve hidden field*/);
		if (existingVariable != null && existingVariable.isValidBinding()){
			final boolean localExists = existingVariable instanceof LocalVariableBinding;
			if (localExists && (this.bits & ASTNode.ShadowsOuterLocal) != 0 && scope.isLambdaSubscope()) {
				scope.problemReporter().lambdaRedeclaresArgument(this);
			} else if (localExists && this.hiddenVariableDepth == 0) {
				scope.problemReporter().redefineArgument(this);
			} else {
				boolean isSpecialArgument = false;
				if (existingVariable instanceof FieldBinding) {
					if (scope.isInsideConstructor()) {
						isSpecialArgument = true; // constructor argument
					} else {
						AbstractMethodDeclaration methodDecl = scope.referenceMethod();
						if (methodDecl != null && CharOperation.prefixEquals(SET, methodDecl.selector)) {
							isSpecialArgument = true; // setter argument
						}
//{ObjectTeams: more special arguments (in generated methods):
						if (methodDecl != null && methodDecl.isGenerated)
							isSpecialArgument = true;
// SH}
					}
				}
				scope.problemReporter().localVariableHiding(this, existingVariable, isSpecialArgument);
			}
		}
		scope.addLocalVariable(this.binding);
		this.binding.useFlag = used ? LocalVariableBinding.USED : LocalVariableBinding.UNUSED;
		return newTypeBinding;
	}

	/**
	 * @see org.eclipse.jdt.internal.compiler.ast.AbstractVariableDeclaration#getKind()
	 */
	public int getKind() {
		return (this.bits & ASTNode.IsArgument) != 0 ? PARAMETER : LOCAL_VARIABLE;
	}

	public boolean isArgument() {
		return true;
	}

	public boolean isVarArgs() {
		return this.type != null &&  (this.type.bits & IsVarArgs) != 0;
	}
	
	public boolean hasElidedType() {
		return (this.bits & IsTypeElided) != 0;
	}

	public StringBuffer print(int indent, StringBuffer output) {

		printIndent(indent, output);
		printModifiers(this.modifiers, output);
		if (this.annotations != null) {
			printAnnotations(this.annotations, output);
			output.append(' ');
		}

		if (this.type == null) {
			output.append("<no type> "); //$NON-NLS-1$
		} else {
			this.type.print(0, output).append(' ');
		}
		return output.append(this.name);
	}

	public StringBuffer printStatement(int indent, StringBuffer output) {

		return print(indent, output).append(';');
	}

	public TypeBinding resolveForCatch(BlockScope scope) {
		// resolution on an argument of a catch clause
		// provide the scope with a side effect : insertion of a LOCAL
		// that represents the argument. The type must be from JavaThrowable

		TypeBinding exceptionType = this.type.resolveType(scope, true /* check bounds*/);
		boolean hasError;
		if (exceptionType == null) {
			hasError = true;
		} else {
			hasError = false;
			switch(exceptionType.kind()) {
				case Binding.PARAMETERIZED_TYPE :
					if (exceptionType.isBoundParameterizedType()) {
						hasError = true;
						scope.problemReporter().invalidParameterizedExceptionType(exceptionType, this);
						// fall thru to create the variable - avoids additional errors because the variable is missing
					}
					break;
				case Binding.TYPE_PARAMETER :
					scope.problemReporter().invalidTypeVariableAsException(exceptionType, this);
					hasError = true;
					// fall thru to create the variable - avoids additional errors because the variable is missing
					break;
			}
			if (exceptionType.findSuperTypeOriginatingFrom(TypeIds.T_JavaLangThrowable, true) == null && exceptionType.isValidBinding()) {
				scope.problemReporter().cannotThrowType(this.type, exceptionType);
				hasError = true;
				// fall thru to create the variable - avoids additional errors because the variable is missing
			}
		}
		Binding existingVariable = scope.getBinding(this.name, Binding.VARIABLE, this, false /*do not resolve hidden field*/);
		if (existingVariable != null && existingVariable.isValidBinding()){
			if (existingVariable instanceof LocalVariableBinding && this.hiddenVariableDepth == 0) {
				scope.problemReporter().redefineArgument(this);
			} else {
				scope.problemReporter().localVariableHiding(this, existingVariable, false);
			}
		}
		
		if ((this.type.bits & ASTNode.IsUnionType) != 0) {
			this.binding = new CatchParameterBinding(this, exceptionType, this.modifiers | ClassFileConstants.AccFinal, false); // argument decl, but local var  (where isArgument = false)
			this.binding.tagBits |= TagBits.MultiCatchParameter;
		} else {
			this.binding = new CatchParameterBinding(this, exceptionType, this.modifiers, false); // argument decl, but local var  (where isArgument = false)
		}
		resolveAnnotations(scope, this.annotations, this.binding, true);
		Annotation.isTypeUseCompatible(this.type, scope, this.annotations);

		scope.addLocalVariable(this.binding);
		this.binding.setConstant(Constant.NotAConstant);
		if (hasError) return null;
		return exceptionType;
	}

	public void traverse(ASTVisitor visitor, BlockScope scope) {

		if (visitor.visit(this, scope)) {
			if (this.annotations != null) {
				int annotationsLength = this.annotations.length;
				for (int i = 0; i < annotationsLength; i++)
					this.annotations[i].traverse(visitor, scope);
			}
			if (this.type != null)
				this.type.traverse(visitor, scope);
		}
		visitor.endVisit(this, scope);
	}
	public void traverse(ASTVisitor visitor, ClassScope scope) {

		if (visitor.visit(this, scope)) {
			if (this.annotations != null) {
				int annotationsLength = this.annotations.length;
				for (int i = 0; i < annotationsLength; i++)
					this.annotations[i].traverse(visitor, scope);
			}
			if (this.type != null)
				this.type.traverse(visitor, scope);
		}
		visitor.endVisit(this, scope);
	}
}
