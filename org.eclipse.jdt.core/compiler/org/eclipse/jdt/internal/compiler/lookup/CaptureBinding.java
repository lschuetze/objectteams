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
 *     Technical University Berlin - extended API and implementation
 *     Stephan Herrmann - Contribution for
 *								Bug 400874 - [1.8][compiler] Inference infrastructure should evolve to meet JLS8 18.x (Part G of JSR335 spec)
 *******************************************************************************/
package org.eclipse.jdt.internal.compiler.lookup;

import org.eclipse.jdt.core.compiler.CharOperation;
import org.eclipse.jdt.internal.compiler.ast.ASTNode;
import org.eclipse.jdt.internal.compiler.ast.Expression;
import org.eclipse.jdt.internal.compiler.ast.Wildcard;
import org.eclipse.jdt.internal.compiler.classfmt.ClassFileConstants;
import org.eclipse.objectteams.otdt.internal.core.compiler.util.RoleTypeCreator;

public class CaptureBinding extends TypeVariableBinding {

	public TypeBinding lowerBound;
	public WildcardBinding wildcard;
	public int captureID;

	/* information to compute unique binding key */
	public ReferenceBinding sourceType;
	public int position;

	public CaptureBinding(WildcardBinding wildcard, ReferenceBinding sourceType, int position, int captureID) {
		super(TypeConstants.WILDCARD_CAPTURE_NAME_PREFIX, null, 0, wildcard.environment);
		this.wildcard = wildcard;
		this.modifiers = ClassFileConstants.AccPublic | ExtraCompilerModifiers.AccGenericSignature; // treat capture as public
		this.fPackage = wildcard.fPackage;
		this.sourceType = sourceType;
		this.position = position;
		this.captureID = captureID;
		if (wildcard.hasTypeAnnotations()) {
			setTypeAnnotations(wildcard.getTypeAnnotations(), wildcard.environment.globalOptions.isAnnotationBasedNullAnalysisEnabled);
		}
	}
	
	// for subclass CaptureBinding18
	protected CaptureBinding(ReferenceBinding sourceType, char[] sourceName, int position, int captureID, LookupEnvironment environment) {
		super(sourceName, null, 0, environment);
		this.modifiers = ClassFileConstants.AccPublic | ExtraCompilerModifiers.AccGenericSignature; // treat capture as public
		this.sourceType = sourceType;
		this.position = position;
		this.captureID = captureID;
	}

	public CaptureBinding(CaptureBinding prototype) {
		super(prototype);
		this.wildcard = prototype.wildcard;
		this.sourceType = prototype.sourceType;
		this.position = prototype.position;
		this.captureID = prototype.captureID;
		this.lowerBound = prototype.lowerBound;
	}
	
	// Captures may get cloned and annotated during type inference.
	public TypeBinding clone(TypeBinding enclosingType) {
		return new CaptureBinding(this);
	}

	/*
	 * sourceTypeKey ! wildcardKey position semi-colon
	 * p.X { capture of ? } --> !*123; (Lp/X; in declaring type except if leaf)
	 * p.X { capture of ? extends p.Y } --> !+Lp/Y;123; (Lp/X; in declaring type except if leaf)
	 */
	public char[] computeUniqueKey(boolean isLeaf) {
		StringBuffer buffer = new StringBuffer();
		if (isLeaf) {
			buffer.append(this.sourceType.computeUniqueKey(false/*not a leaf*/));
			buffer.append('&');
		}
		buffer.append(TypeConstants.WILDCARD_CAPTURE);
		buffer.append(this.wildcard.computeUniqueKey(false/*not a leaf*/));
		buffer.append(this.position);
		buffer.append(';');
		int length = buffer.length();
		char[] uniqueKey = new char[length];
		buffer.getChars(0, length, uniqueKey, 0);
		return uniqueKey;
	}

	public String debugName() {

		if (this.wildcard != null) {
			StringBuffer buffer = new StringBuffer(10);
			AnnotationBinding [] annotations = getTypeAnnotations();
			for (int i = 0, length = annotations == null ? 0 : annotations.length; i < length; i++) {
				buffer.append(annotations[i]);
				buffer.append(' ');
			}
			buffer
				.append(TypeConstants.WILDCARD_CAPTURE_NAME_PREFIX)
				.append(this.captureID)
				.append(TypeConstants.WILDCARD_CAPTURE_NAME_SUFFIX)
				.append(this.wildcard.debugName());
			return buffer.toString();
		}
		return super.debugName();
	}

	public char[] genericTypeSignature() {
		if (this.genericTypeSignature == null) {
			this.genericTypeSignature = CharOperation.concat(TypeConstants.WILDCARD_CAPTURE, this.wildcard.genericTypeSignature());
		}
		return this.genericTypeSignature;
	}

	/**
	 * Initialize capture bounds using substituted supertypes
	 * e.g. given X<U, V extends X<U, V>>,     capture(X<E,?>) = X<E,capture>, where capture extends X<E,capture>
	 */
	public void initializeBounds(Scope scope, ParameterizedTypeBinding capturedParameterizedType) {
		TypeVariableBinding wildcardVariable = this.wildcard.typeVariable();
		if (wildcardVariable == null) {
			// error resilience when capturing Zork<?>
			// no substitution for wildcard bound (only formal bounds from type variables are to be substituted: 104082)
			TypeBinding originalWildcardBound = this.wildcard.bound;
			switch (this.wildcard.boundKind) {
				case Wildcard.EXTENDS :
					// still need to capture bound supertype as well so as not to expose wildcards to the outside (111208)
					TypeBinding capturedWildcardBound = originalWildcardBound.capture(scope, this.position);
					if (originalWildcardBound.isInterface()) {
						this.setSuperClass(scope.getJavaLangObject());
						this.setSuperInterfaces(new ReferenceBinding[] { (ReferenceBinding) capturedWildcardBound });
					} else {
						// the wildcard bound should be a subtype of variable superclass
						// it may occur that the bound is less specific, then consider glb (202404)
						if (capturedWildcardBound.isArrayType() || TypeBinding.equalsEquals(capturedWildcardBound, this)) {
							this.setSuperClass(scope.getJavaLangObject());
						} else {
							this.setSuperClass((ReferenceBinding) capturedWildcardBound);
						}
						this.setSuperInterfaces(Binding.NO_SUPERINTERFACES);
					}
					this.setFirstBound(capturedWildcardBound);
					if ((capturedWildcardBound.tagBits & TagBits.HasTypeVariable) == 0)
						this.tagBits &= ~TagBits.HasTypeVariable;
					break;
				case Wildcard.UNBOUND :
					this.setSuperClass(scope.getJavaLangObject());
					this.setSuperInterfaces(Binding.NO_SUPERINTERFACES);
					this.tagBits &= ~TagBits.HasTypeVariable;
					break;
				case Wildcard.SUPER :
					this.setSuperClass(scope.getJavaLangObject());
					this.setSuperInterfaces(Binding.NO_SUPERINTERFACES);
					this.lowerBound = this.wildcard.bound;
					if ((originalWildcardBound.tagBits & TagBits.HasTypeVariable) == 0)
						this.tagBits &= ~TagBits.HasTypeVariable;
					break;
			}
			return;
		}
		ReferenceBinding originalVariableSuperclass = wildcardVariable.superclass;
		ReferenceBinding substitutedVariableSuperclass = (ReferenceBinding) Scope.substitute(capturedParameterizedType, originalVariableSuperclass);
		// prevent cyclic capture: given X<T>, capture(X<? extends T> could yield a circular type
		if (TypeBinding.equalsEquals(substitutedVariableSuperclass, this)) substitutedVariableSuperclass = originalVariableSuperclass;

		ReferenceBinding[] originalVariableInterfaces = wildcardVariable.superInterfaces();
		ReferenceBinding[] substitutedVariableInterfaces = Scope.substitute(capturedParameterizedType, originalVariableInterfaces);
		if (substitutedVariableInterfaces != originalVariableInterfaces) {
			// prevent cyclic capture: given X<T>, capture(X<? extends T> could yield a circular type
			for (int i = 0, length = substitutedVariableInterfaces.length; i < length; i++) {
				if (TypeBinding.equalsEquals(substitutedVariableInterfaces[i], this)) substitutedVariableInterfaces[i] = originalVariableInterfaces[i];
			}
		}
		// no substitution for wildcard bound (only formal bounds from type variables are to be substituted: 104082)
		TypeBinding originalWildcardBound = this.wildcard.bound;

		switch (this.wildcard.boundKind) {
			case Wildcard.EXTENDS :
				// still need to capture bound supertype as well so as not to expose wildcards to the outside (111208)
				TypeBinding capturedWildcardBound = originalWildcardBound.capture(scope, this.position);
//{ObjectTeams: is the bound a role type requiring wrapping?
				if (capturedWildcardBound.isRole())
					capturedWildcardBound = RoleTypeCreator.maybeWrapUnqualifiedRoleType(scope, capturedWildcardBound.enclosingType(), capturedWildcardBound, scope.methodScope().referenceMethod(), scope.problemReporter());
// SH}
				if (originalWildcardBound.isInterface()) {
					this.setSuperClass(substitutedVariableSuperclass);
					// merge wildcard bound into variable superinterfaces using glb
					if (substitutedVariableInterfaces == Binding.NO_SUPERINTERFACES) {
						this.setSuperInterfaces(new ReferenceBinding[] { (ReferenceBinding) capturedWildcardBound });
					} else {
						int length = substitutedVariableInterfaces.length;
						System.arraycopy(substitutedVariableInterfaces, 0, substitutedVariableInterfaces = new ReferenceBinding[length+1], 1, length);
						substitutedVariableInterfaces[0] =  (ReferenceBinding) capturedWildcardBound;
						this.setSuperInterfaces(Scope.greaterLowerBound(substitutedVariableInterfaces));
					}
				} else {
					// the wildcard bound should be a subtype of variable superclass
					// it may occur that the bound is less specific, then consider glb (202404)
					if (capturedWildcardBound.isArrayType() || TypeBinding.equalsEquals(capturedWildcardBound, this)) {
						this.setSuperClass(substitutedVariableSuperclass);
					} else {
						this.setSuperClass((ReferenceBinding) capturedWildcardBound);
						if (this.superclass.isSuperclassOf(substitutedVariableSuperclass)) {
							this.setSuperClass(substitutedVariableSuperclass);
						}
					}
					this.setSuperInterfaces(substitutedVariableInterfaces);
				}
				this.setFirstBound(capturedWildcardBound);
				if ((capturedWildcardBound.tagBits & TagBits.HasTypeVariable) == 0)
					this.tagBits &= ~TagBits.HasTypeVariable;
				break;
			case Wildcard.UNBOUND :
				this.setSuperClass(substitutedVariableSuperclass);
				this.setSuperInterfaces(substitutedVariableInterfaces);
				this.tagBits &= ~TagBits.HasTypeVariable;
				break;
			case Wildcard.SUPER :
				this.setSuperClass(substitutedVariableSuperclass);
				if (TypeBinding.equalsEquals(wildcardVariable.firstBound, substitutedVariableSuperclass) || TypeBinding.equalsEquals(originalWildcardBound, substitutedVariableSuperclass)) {
					this.setFirstBound(substitutedVariableSuperclass);
				}
				this.setSuperInterfaces(substitutedVariableInterfaces);
				this.lowerBound = originalWildcardBound;
				if ((originalWildcardBound.tagBits & TagBits.HasTypeVariable) == 0)
					this.tagBits &= ~TagBits.HasTypeVariable;
				break;
		}
	}

	/**
	 * @see org.eclipse.jdt.internal.compiler.lookup.TypeBinding#isCapture()
	 */
	public boolean isCapture() {
		return true;
	}

	/**
	 * @see TypeBinding#isEquivalentTo(TypeBinding)
	 */
	public boolean isEquivalentTo(TypeBinding otherType) {
	    if (equalsEquals(this, otherType)) return true;
	    if (otherType == null) return false;
		// capture of ? extends X[]
		if (this.firstBound != null && this.firstBound.isArrayType()) {
			if (this.firstBound.isCompatibleWith(otherType))
				return true;
		}
		switch (otherType.kind()) {
			case Binding.WILDCARD_TYPE :
			case Binding.INTERSECTION_TYPE :
				return ((WildcardBinding) otherType).boundCheck(this);
		}
		return false;
	}

	public char[] readableName() {
		if (this.wildcard != null) {
			StringBuffer buffer = new StringBuffer(10);
			buffer
				.append(TypeConstants.WILDCARD_CAPTURE_NAME_PREFIX)
				.append(this.captureID)
				.append(TypeConstants.WILDCARD_CAPTURE_NAME_SUFFIX)
				.append(this.wildcard.readableName());
			int length = buffer.length();
			char[] name = new char[length];
			buffer.getChars(0, length, name, 0);
			return name;
		}
		return super.readableName();
	}

	public char[] shortReadableName() {
		if (this.wildcard != null) {
			StringBuffer buffer = new StringBuffer(10);
			buffer
				.append(TypeConstants.WILDCARD_CAPTURE_NAME_PREFIX)
				.append(this.captureID)
				.append(TypeConstants.WILDCARD_CAPTURE_NAME_SUFFIX)
				.append(this.wildcard.shortReadableName());
			int length = buffer.length();
			char[] name = new char[length];
			buffer.getChars(0, length, name, 0);
			return name;
		}
		return super.shortReadableName();
	}

	public String toString() {
		if (this.wildcard != null) {
			StringBuffer buffer = new StringBuffer(10);
			AnnotationBinding [] annotations = getTypeAnnotations();
			for (int i = 0, length = annotations == null ? 0 : annotations.length; i < length; i++) {
				buffer.append(annotations[i]);
				buffer.append(' ');
			}
			buffer
				.append(TypeConstants.WILDCARD_CAPTURE_NAME_PREFIX)
				.append(this.captureID)
				.append(TypeConstants.WILDCARD_CAPTURE_NAME_SUFFIX)
				.append(this.wildcard);
			return buffer.toString();
		}
		return super.toString();
	}

//{ObjectTeams: wrapping of role types in various fields of this capture:
	public TypeBinding maybeWrapQualifiedRoleType(Scope scope, Expression anchorExpr, ASTNode typedNode, TypeBinding originalBinding) {
		boolean hasWrapped = false;
		if (this.inRecursiveFunction)
			return originalBinding;

		this.inRecursiveFunction = true;
		try {
	    	TypeBinding wrappedBound = RoleTypeCreator.maybeWrapQualifiedRoleType(scope, anchorExpr, this.firstBound, typedNode);
	    	if (TypeBinding.notEquals(wrappedBound, this.firstBound))
	        	hasWrapped = true;
	        
	    	TypeBinding newSuper = RoleTypeCreator.maybeWrapQualifiedRoleType(scope, anchorExpr, this.superclass, typedNode);
	        if (TypeBinding.notEquals(newSuper, this.superclass))
				hasWrapped = true;
	        
	        ReferenceBinding[] newSuperIfcs = new ReferenceBinding[this.superInterfaces.length]; 
	    	for (int i=0; i<this.superInterfaces.length; i++) {
	            newSuperIfcs[i] = (ReferenceBinding) RoleTypeCreator.maybeWrapQualifiedRoleType(scope, anchorExpr, this.superInterfaces[i], typedNode);
	            if (TypeBinding.notEquals(newSuperIfcs[i], this.superInterfaces[i]))
	            	hasWrapped = true;
	    	}
	    	if (hasWrapped) {
	    		CaptureBinding newCapture = new CaptureBinding(this.wildcard, this.sourceType, this.position, scope.compilationUnitScope().nextCaptureID());
	    		newCapture.firstBound = wrappedBound;
	    		newCapture.superclass = (ReferenceBinding)newSuper;
	    		newCapture.superInterfaces = newSuperIfcs;
	    		int dimensions = originalBinding.dimensions();
	    		if (dimensions > 0)
					return this.environment.createArrayType(newCapture, dimensions);
	    		return newCapture;
	    	}
	    	return originalBinding; // may be an ArrayBinding with this as leafComponentType
		} finally {
			this.inRecursiveFunction = false;
		}
	}
// SH}
}
