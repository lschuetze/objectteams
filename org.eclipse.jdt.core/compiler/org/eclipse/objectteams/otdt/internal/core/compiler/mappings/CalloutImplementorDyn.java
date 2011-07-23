/**********************************************************************
 * This file is part of "Object Teams Development Tooling"-Software
 * 
 * Copyright 2011 GK Software AG and others.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Please visit http://www.eclipse.org/objectteams for updates and contact.
 * 
 * Contributors:
 *		Stephan Herrmann - Initial API and implementation
 **********************************************************************/
package org.eclipse.objectteams.otdt.internal.core.compiler.mappings;

import org.eclipse.jdt.internal.compiler.ast.Expression;
import org.eclipse.jdt.internal.compiler.classfmt.ClassFileConstants;
import org.eclipse.jdt.internal.compiler.lookup.Binding;
import org.eclipse.jdt.internal.compiler.lookup.MethodBinding;
import org.eclipse.jdt.internal.compiler.lookup.ReferenceBinding;
import org.eclipse.jdt.internal.compiler.lookup.Scope;
import org.eclipse.jdt.internal.compiler.lookup.TypeBinding;
import org.eclipse.jdt.internal.compiler.lookup.TypeConstants;
import org.eclipse.jdt.internal.compiler.parser.TerminalTokens;
import org.eclipse.objectteams.otdt.internal.core.compiler.ast.FieldAccessSpec;
import org.eclipse.objectteams.otdt.internal.core.compiler.ast.MethodSpec;
import org.eclipse.objectteams.otdt.internal.core.compiler.lookup.RoleTypeBinding;
import org.eclipse.objectteams.otdt.internal.core.compiler.model.RoleModel;
import org.eclipse.objectteams.otdt.internal.core.compiler.model.TeamModel;
import org.eclipse.objectteams.otdt.internal.core.compiler.util.AstGenerator;

/**
 * This class only contains those parts of callout generation that for
 * dynamic weaving deviated from the normal strategy.
 * 
 * @author stephan
 */
public class CalloutImplementorDyn {

	// wrapper methods for decapsulating callout
	public static final char[] ACCESS = "_OT$access".toCharArray(); //$NON-NLS-1$
	public static final char[] ACCESS_STATIC = "_OT$accessStatic".toCharArray(); //$NON-NLS-1$
	

	public static Expression baseAccessExpression(Scope scope, RoleModel roleModel, ReferenceBinding baseType, 
												  Expression receiver, MethodSpec baseSpec, Expression[] arguments,
												  AstGenerator gen) 
	{
		baseSpec.createAccessAttribute(roleModel);

		char[] selector = ensureAccessor(scope, baseType, baseSpec.isStatic());
		TeamModel teamModel = roleModel.getTeamModel();
		Expression memberIdArg = gen.intLiteral(teamModel.getNewCallinId(baseSpec));
		int opKind = 0;
		if (baseSpec instanceof FieldAccessSpec)
			if (((FieldAccessSpec) baseSpec).calloutModifier == TerminalTokens.TokenNameset)
				opKind = 1;
		Expression opKindArg = gen.intLiteral(opKind);
		Expression packagedArgs = gen.arrayAllocation(gen.qualifiedTypeReference(TypeConstants.JAVA_LANG_OBJECT), 1, arguments);
		Expression callerArg = gen.qualifiedThisReference(gen.typeReference(teamModel.getBinding()));
		Expression messageSend = gen.messageSend(receiver, selector, new Expression[] {memberIdArg, opKindArg, packagedArgs, callerArg});
		if (baseSpec.resolvedType() == TypeBinding.VOID)
			return messageSend;
		else
			return gen.createCastOrUnboxing(messageSend, baseSpec.resolvedType());
	}

	private static char[] ensureAccessor(Scope scope, ReferenceBinding baseType, boolean isStatic) {
		if (baseType instanceof RoleTypeBinding)
			baseType = baseType.getRealClass();
		char[] selector = isStatic ? ACCESS_STATIC : ACCESS;
		MethodBinding[] methods = baseType.getMethods(selector);
		if (methods == null || methods.length != 1) {
			int modifiers = ClassFileConstants.AccPublic|ClassFileConstants.AccSynthetic;
			if (isStatic)
				modifiers |= ClassFileConstants.AccStatic;
			MethodBinding method = new MethodBinding(
						modifiers,
						selector,
						scope.getJavaLangObject(),
						new TypeBinding[] {
							TypeBinding.INT, TypeBinding.INT,
							scope.environment().createArrayType(scope.getJavaLangObject(), 1),
							scope.getOrgObjectteamsITeam()
						},
						Binding.NO_EXCEPTIONS,
						baseType);
			baseType.addMethod(method);
		}
		return selector;
	}
}
