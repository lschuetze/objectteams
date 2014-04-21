/**********************************************************************
 * This file is part of "Object Teams Development Tooling"-Software
 *
 * Copyright 2009 Stephan Herrmann
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * $Id$
 *
 * Please visit http://www.eclipse.org/objectteams for updates and contact.
 *
 * Contributors:
 * Stephan Herrmann
 **********************************************************************/
package org.eclipse.objectteams.otdt.internal.core.compiler.lookup;

import org.eclipse.jdt.core.compiler.CharOperation;
import org.eclipse.jdt.internal.compiler.classfmt.ClassFileConstants;
import org.eclipse.jdt.internal.compiler.codegen.CodeStream;
import org.eclipse.jdt.internal.compiler.codegen.Opcodes;
import org.eclipse.jdt.internal.compiler.impl.CompilerOptions.WeavingScheme;
import org.eclipse.jdt.internal.compiler.lookup.ExtraCompilerModifiers;
import org.eclipse.jdt.internal.compiler.lookup.LookupEnvironment;
import org.eclipse.jdt.internal.compiler.lookup.MethodBinding;
import org.eclipse.jdt.internal.compiler.lookup.ReferenceBinding;
import org.eclipse.jdt.internal.compiler.lookup.Scope;
import org.eclipse.jdt.internal.compiler.lookup.SourceTypeBinding;
import org.eclipse.jdt.internal.compiler.lookup.SyntheticMethodBinding;
import org.eclipse.jdt.internal.compiler.lookup.TypeBinding;
import org.eclipse.objectteams.otdt.core.compiler.IOTConstants;
import org.eclipse.objectteams.otdt.internal.core.compiler.model.MethodModel;
import org.eclipse.objectteams.otdt.internal.core.compiler.model.RoleModel;
import org.eclipse.objectteams.otdt.internal.core.compiler.statemachine.transformer.MethodSignatureEnhancer;
import org.eclipse.objectteams.otdt.internal.core.compiler.util.RoleTypeCreator;
import org.eclipse.objectteams.otdt.internal.core.compiler.util.TSuperHelper;

/**
 * This class represents placeholder base-call surrogates which only throw
 * an OTREInternalError  -> no need to have these generated by the OTRE.
 * 
 * @author stephan
 * @since 1.3.0
 */
public class SyntheticBaseCallSurrogate extends SyntheticOTMethodBinding
{
	// this corresponds to IOTConstants.OT_DOLLAR_NAME without the trailing '$':
	private static final char[] _OT = "_OT".toCharArray(); //$NON-NLS-1$

	private TypeBinding errorType;
	private ReferenceBinding stringType;
	
	/**
	 * Create a binding for a synthetic base-call surrogate.
	 * 
	 * @param callinMethod   the callin method potentially holding base-calls 
	 * @param declaringClass the class that will hold the surrogate implementation: normally the role, for static callin methods: the team.
	 */
	public SyntheticBaseCallSurrogate(MethodBinding callinMethod, SourceTypeBinding declaringClass) {
		super(declaringClass, ClassFileConstants.AccProtected|ClassFileConstants.AccSynthetic, callinMethod.selector, callinMethod.parameters, callinMethod.returnType);
		this.selector = SyntheticBaseCallSurrogate.genSurrogateName(callinMethod.selector, callinMethod.declaringClass.sourceName(), callinMethod.isStatic());
		if (!callinMethod.isStatic() && callinMethod.isCallin()) { // don't change paramaters if enhancement is absent
			// additional arg "boolean isSuperAccess":
			this.parameters = addIsSuperAccessArg(this.parameters, declaringClass.scope.compilerOptions().weavingScheme);
		}
		this.purpose = MethodAccess;
		this.targetMethod = callinMethod;
		TypeBinding origReturnType = MethodModel.getReturnType(callinMethod);
		MethodModel.saveReturnType(this, origReturnType);
		// fetch type bindings while we have a scope:
		Scope scope = declaringClass.scope;
		this.errorType = scope.getType(IOTConstants.OTRE_INTERNAL_ERROR, 3);
		this.stringType = scope.getJavaLangString();
		// the synthetic methods of a class will be sorted according to a per-class index, find the index now: 
		SyntheticMethodBinding[] knownAccessMethods = declaringClass.syntheticMethods();
		this.index = knownAccessMethods == null ? 0 : knownAccessMethods.length;
		this.sourceStart = declaringClass.scope.referenceContext.sourceStart;
		retrieveLineNumber(declaringClass);
	}

	/** Directly generate the instruction for this method's body (no AST available). */
	public void generateInstructions(CodeStream codeStream) {
		codeStream.new_(this.errorType);
		codeStream.dup();
		codeStream.ldc("Binding error: base-call impossible!"); //$NON-NLS-1$
		MethodBinding ctorBinding = ((ReferenceBinding)this.errorType).getExactConstructor(new TypeBinding[]{this.stringType});
		codeStream.invoke(Opcodes.OPC_invokespecial, ctorBinding, this.errorType);
		codeStream.athrow();
		codeStream.aconst_null(); // always generalized to Object
		codeStream.areturn();
	}

	/** Is the callin method bound in the given role class? */
	public static boolean isCallinMethodBoundIn(MethodBinding callinMethod, ReferenceBinding roleClass) {
		if (TypeBinding.notEquals(callinMethod.declaringClass, roleClass)) {
			callinMethod = roleClass.getExactMethod(callinMethod.selector, callinMethod.parameters, null);
			if (callinMethod == null)
				return false;
		}
		if (roleClass.callinCallouts != null)
			for (CallinCalloutBinding mapping : roleClass.callinCallouts)
				if (mapping._roleMethodBinding == callinMethod)
					return true;
		return false;
	}

	/** Does the current role (callinMethod.declaringClass) inherit a callin binding for this method? */
	public static boolean isBindingForCallinMethodInherited(MethodBinding callinMethod) {
		ReferenceBinding declaringClass = callinMethod.declaringClass;
		if (isCallinMethodBoundIn(callinMethod, declaringClass))
			return false;
		while ((declaringClass = declaringClass.superclass()) != null) {
			if (isCallinMethodBoundIn(callinMethod, declaringClass))
				return true;
		}
		return false;
	}

	/**
	 * Add any required base-call surrogates to the given type.
	 * @param type must be a role class. 
	 */
	public static void addFakedBaseCallSurrogates(SourceTypeBinding type, LookupEnvironment environment) {
		if (type.methods() == null)
			return;
		if (environment.globalOptions.weavingScheme == WeavingScheme.OTDRE) // no surrogates for the dynamic weaver.
			return;
		for (MethodBinding method : type.methods())
			if (method.isCallin() && (method.returnType != null))
				SyntheticBaseCallSurrogate.getBaseCallSurrogate(method, type.roleModel, environment);
	}

	/**
	 * Retrieve or create a method binding representing the base call surrogate for a callin method.
	 * The actual method will be created by the OTRE.
	 *
	 * PRE: callinMethod already has all signature enhancements
	 *
	 * @param callinMethod
	 * @param environment   needed for lookup of java/lang/Object (needed for return type generalization).
	 *                      and for wrapping role types.
	 * @return a MethodBinding (if it already exists or if role is binary) or a new SyntheticBaseCallSurrogate or null (if none is required)
	 */
	public static MethodBinding getBaseCallSurrogate(MethodBinding callinMethod, RoleModel clazz, LookupEnvironment environment)
	{
	
		try {
			if (TSuperHelper.isTSuper(callinMethod))
				return null;
			if (SyntheticBaseCallSurrogate.isBindingForCallinMethodInherited(callinMethod))
				return null;
			MethodBinding result = null;
			ReferenceBinding roleType = callinMethod.declaringClass;
			ReferenceBinding teamType = roleType.enclosingType();
			ReferenceBinding declaringClass =  callinMethod.isStatic() ? teamType : roleType;
	
			char[] roleName = roleType.sourceName();
			char[] selector =
				genSurrogateName(callinMethod.selector, roleName, callinMethod.isStatic());
			TypeBinding returnType = MethodSignatureEnhancer.getGeneralizedReturnType(callinMethod.returnType, environment);
			TypeBinding[] baseCallParameters = callinMethod.parameters;
			if (!callinMethod.isStatic())
				baseCallParameters = addIsSuperAccessArg(baseCallParameters, environment.globalOptions.weavingScheme);
	
			// search existing surrogate:
			candidates:
			for (MethodBinding candidate : declaringClass.getMethods(selector)) {
				if (candidate.parameters.length != baseCallParameters.length)
					continue;
				for (int i=0; i<baseCallParameters.length; i++)
					if (!areTypesEqual(baseCallParameters[i].erasure(), candidate.parameters[i]))
						continue candidates;
				result = candidate;
				break;
			}
			if (result == null) {
				if (declaringClass.isBinaryBinding())
					result = new MethodBinding(
								ClassFileConstants.AccPublic,
								selector,
								returnType,
								baseCallParameters,
								null /*exceptions*/,
								declaringClass);
				else 
					result = ((SourceTypeBinding)declaringClass).addSyntheticBaseCallSurrogate(callinMethod);
				MethodModel.getModel(result)._fakeKind = MethodModel.FakeKind.BASECALL_SURROGATE;
				RoleTypeCreator.wrapTypesInMethodBindingSignature(result, environment);
				result.modifiers &= ~ExtraCompilerModifiers.AccUnresolved;
				declaringClass.addMethod(result);
			}
			MethodModel.getModel(callinMethod).setBaseCallSurrogate(result);
			return result;
		} catch (RuntimeException ex) {
			// check if failure is due to incompatible class file version
//			if (clazz != null && clazz.isIncompatibleCompilerVersion()) {
//				String version;
//				version = WordValueAttribute.getBytecodeVersionString(clazz._compilerVersion);
//				// note: we have no source to report this error against,
//				// so use whatever the current problem reporter is configured for.
//				String errorMessage = "Byte code for class "+String.valueOf(clazz.getBinding().readableName())
//									  +" has incompatible version "+version+", original error was: "+ex.toString();
//				try {
//					Config.getLookupEnvironment().problemReporter.abortDueToInternalError(
//							errorMessage);
//				} catch (NotConfiguredException e) {
//					throw new AbortCompilation(false, e);
//				}
//				return null;
//			}
			throw ex; // want to see all other exceptions
		}
	}

	/** For role types ignore weakening and anchors. */
	static boolean areTypesEqual(TypeBinding one, TypeBinding two) {
		if (TypeBinding.equalsEquals(one, two)) return true;
		ReferenceBinding enclosingOne = one.enclosingType();
		if (enclosingOne == null) return false;
		ReferenceBinding enclosingTwo = two.enclosingType();
		if (enclosingTwo == null) return false;
		if (!areTypesEqual(enclosingOne, enclosingTwo)) return false;
		return CharOperation.equals(one.sourceName(), two.sourceName());
	}
	
	static TypeBinding[] addIsSuperAccessArg(TypeBinding[] baseCallParameters, WeavingScheme weavingScheme) 
	{
		int len = baseCallParameters.length;
		TypeBinding[] newParams = new TypeBinding[len+1];
		int enhancingArgLen = MethodSignatureEnhancer.getEnhancingArgLen(weavingScheme);
		System.arraycopy(baseCallParameters, 0, newParams, 0, enhancingArgLen);
		newParams[enhancingArgLen] = TypeBinding.BOOLEAN;
		System.arraycopy(baseCallParameters, enhancingArgLen,
						 newParams, 		 enhancingArgLen+1,
						 len-enhancingArgLen);
		return newParams;
	}

	public static char[] genSurrogateName(char[] methodName, char[] roleName, boolean isStatic)
	{
		if (isStatic)
			return CharOperation.concatWith(
					new char[][] {_OT, roleName, methodName, "base".toCharArray()}, //$NON-NLS-1$
					'$');
		return CharOperation.concatWith(
				new char[][] {_OT, methodName, "base".toCharArray()}, //$NON-NLS-1$
				'$');
	}

	public static boolean isBaseCallSurrogateName(char[] name) {
		char[][] split = CharOperation.splitOn('$', name);
		if (split.length < 3)
			return false;
		return CharOperation.equals(split[0], _OT)
			&& CharOperation.equals(split[split.length-1], IOTConstants.BASE);
	}

	public static char[] stripBaseCallName(char[] selector) {
		if (CharOperation.occurencesOf('$', selector) == 2) {
			char[][] tokens = CharOperation.splitOn('$', selector);
			selector = tokens[1]; // pick m from _OT$m$base
		}
		return selector;
	}

	
}
