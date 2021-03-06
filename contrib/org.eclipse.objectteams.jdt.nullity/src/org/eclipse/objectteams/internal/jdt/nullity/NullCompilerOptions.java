/*******************************************************************************
 * Copyright (c) 2011 GK Software AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Stephan Herrmann - initial API and implementation 
 *******************************************************************************/
package org.eclipse.objectteams.internal.jdt.nullity;

import org.eclipse.jdt.core.compiler.CharOperation;
import org.eclipse.jdt.internal.compiler.ast.ASTNode;
import org.eclipse.jdt.internal.compiler.impl.CompilerOptions;
import org.eclipse.jdt.internal.compiler.impl.IrritantSet;

/** Additional constants for {@link CompilerOptions}. */
@SuppressWarnings("restriction")
public interface NullCompilerOptions {
	// copies from orig:
	public static final String ENABLED = "enabled"; //$NON-NLS-1$
	public static final String DISABLED = "disabled"; //$NON-NLS-1$


	// new:
	public static final String OPTION_ReportNullSpecViolation = "org.eclipse.jdt.core.compiler.problem.nullSpecViolation";  //$NON-NLS-1$
	public static final String OPTION_ReportPotentialNullSpecViolation = "org.eclipse.jdt.core.compiler.problem.potentialNullSpecViolation";  //$NON-NLS-1$
	public static final String OPTION_ReportNullSpecInsufficientInfo = "org.eclipse.jdt.core.compiler.problem.nullSpecInsufficientInfo";  //$NON-NLS-1$
	public static final String OPTION_ReportRedundantNullAnnotation = "org.eclipse.jdt.core.compiler.problem.redundantNullAnnotation";  //$NON-NLS-1$

	public static final String OPTION_AnnotationBasedNullAnalysis = "org.eclipse.jdt.core.compiler.annotation.nullanalysis"; //$NON-NLS-1$
	public static final String OPTION_NullableAnnotationName = "org.eclipse.jdt.core.compiler.annotation.nullable"; //$NON-NLS-1$
	public static final String OPTION_NonNullAnnotationName = "org.eclipse.jdt.core.compiler.annotation.nonnull"; //$NON-NLS-1$
	public static final String OPTION_NonNullByDefaultAnnotationName = "org.eclipse.jdt.core.compiler.annotation.nonnullbydefault"; //$NON-NLS-1$

	public static final String OPTION_NonNullIsDefault = "org.eclipse.jdt.core.compiler.annotation.nonnullisdefault";  //$NON-NLS-1$
	

	public static final int NullSpecViolation = IrritantSet.GROUP2 | ASTNode.Bit8;
	public static final int PotentialNullSpecViolation = IrritantSet.GROUP2 | ASTNode.Bit9;
	public static final int NullSpecInsufficientInfo = IrritantSet.GROUP2 | ASTNode.Bit10;
	public static final int RedundantNullAnnotation = IrritantSet.GROUP2 | ASTNode.Bit14;
	
	static final char[][] DEFAULT_NONNULL_ANNOTATION_NAME = CharOperation.splitOn('.', "org.eclipse.jdt.annotation.NonNull".toCharArray()); //$NON-NLS-1$
	static final char[][] DEFAULT_NULLABLE_ANNOTATION_NAME = CharOperation.splitOn('.', "org.eclipse.jdt.annotation.Nullable".toCharArray()); //$NON-NLS-1$
	static final char[][] DEFAULT_NONNULLBYDEFAULT_ANNOTATION_NAME = CharOperation.splitOn('.', "org.eclipse.jdt.annotation.NonNullByDefault".toCharArray()); //$NON-NLS-1$
}