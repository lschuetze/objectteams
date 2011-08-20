/*******************************************************************************
 * Copyright (c) 2000, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Technical University Berlin - extended API and implementation
 *******************************************************************************/
package org.eclipse.objectteams.internal.jdt.nullity;

/**
 * IMPORTANT NOTE: These constants are dedicated to the internal Scanner implementation.
 * It is mirrored in org.eclipse.jdt.core.compiler public package where it is API.
 * The mirror implementation is using the backward compatible ITerminalSymbols constant
 * definitions (stable with 2.0), whereas the internal implementation uses TerminalTokens
 * which constant values reflect the latest parser generation state.
 */
/**
 * Maps each terminal symbol in the java-grammar into a unique integer.
 * This integer is used to represent the terminal when computing a parsing action.
 *
 * Disclaimer : These constant values are generated automatically using a Java
 * grammar, therefore their actual values are subject to change if new keywords
 * were added to the language (for instance, 'assert' is a keyword in 1.4).
 */
public interface TerminalTokens_OT21M1 {

	// special tokens not part of grammar - not autogenerated
	int
	    TokenNameWHITESPACE = 1000,
		TokenNameCOMMENT_LINE = 1001,
		TokenNameCOMMENT_BLOCK = 1002,
		TokenNameCOMMENT_JAVADOC = 1003;

	int
      TokenNameIdentifier = 22,
      TokenNameabstract = 60,
      TokenNameassert = 94,
      TokenNameboolean = 33,
      TokenNamebreak = 95,
      TokenNamebyte = 34,
      TokenNamecase = 114,
      TokenNamecatch = 111,
      TokenNamechar = 35,
      TokenNameclass = 78,
      TokenNamecontinue = 96,
      TokenNameconst = 127,
      TokenNamedefault = 108,
      TokenNamedo = 97,
      TokenNamedouble = 36,
      TokenNameelse = 117,
      TokenNameenum = 105,
      TokenNameextends = 107,
      TokenNamefalse = 47,
      TokenNamefinal = 61,
      TokenNamefinally = 115,
      TokenNamefloat = 37,
      TokenNamefor = 98,
      TokenNamegoto = 128,
      TokenNameif = 99,
      TokenNameimplements = 125,
      TokenNameimport = 112,
      TokenNameinstanceof = 14,
      TokenNameint = 38,
      TokenNameinterface = 80,
      TokenNamelong = 39,
      TokenNamenative = 62,
      TokenNamenew = 44,
      TokenNamenull = 48,
      TokenNamepackage = 113,
      TokenNameprivate = 63,
      TokenNameprotected = 64,
      TokenNamepublic = 65,
      TokenNamereturn = 100,
      TokenNameshort = 40,
      TokenNamestatic = 57,
      TokenNamestrictfp = 66,
      TokenNamesuper = 42,
      TokenNameswitch = 101,
      TokenNamesynchronized = 58,
      TokenNamethis = 43,
      TokenNamethrow = 102,
      TokenNamethrows = 118,
      TokenNametransient = 67,
      TokenNametrue = 49,
      TokenNametry = 103,
      TokenNamevoid = 41,
      TokenNamevolatile = 68,
      TokenNamewhile = 81,
      TokenNameas = 82,
      TokenNamebase = 31,
      TokenNamecallin = 69,
      TokenNameplayedBy = 126,
      TokenNameprecedence = 116,
      TokenNamereadonly = 70,
      TokenNameteam = 59,
      TokenNametsuper = 46,
      TokenNamewhen = 109,
      TokenNamewith = 110,
      TokenNamewithin = 104,
      TokenNamereplace = 120,
      TokenNameafter = 119,
      TokenNamebefore = 121,
      TokenNameget = 122,
      TokenNameset = 123,
      TokenNameIntegerLiteral = 50,
      TokenNameLongLiteral = 51,
      TokenNameFloatingPointLiteral = 52,
      TokenNameDoubleLiteral = 53,
      TokenNameCharacterLiteral = 54,
      TokenNameStringLiteral = 55,
      TokenNamePLUS_PLUS = 9,
      TokenNameMINUS_MINUS = 10,
      TokenNameEQUAL_EQUAL = 18,
      TokenNameLESS_EQUAL = 15,
      TokenNameGREATER_EQUAL = 16,
      TokenNameNOT_EQUAL = 19,
      TokenNameLEFT_SHIFT = 17,
      TokenNameRIGHT_SHIFT = 12,
      TokenNameUNSIGNED_RIGHT_SHIFT = 13,
      TokenNamePLUS_EQUAL = 83,
      TokenNameMINUS_EQUAL = 84,
      TokenNameMULTIPLY_EQUAL = 85,
      TokenNameDIVIDE_EQUAL = 86,
      TokenNameAND_EQUAL = 87,
      TokenNameOR_EQUAL = 88,
      TokenNameXOR_EQUAL = 89,
      TokenNameREMAINDER_EQUAL = 90,
      TokenNameLEFT_SHIFT_EQUAL = 91,
      TokenNameRIGHT_SHIFT_EQUAL = 92,
      TokenNameUNSIGNED_RIGHT_SHIFT_EQUAL = 93,
      TokenNameOR_OR = 27,
      TokenNameAND_AND = 25,
      TokenNamePLUS = 2,
      TokenNameMINUS = 3,
      TokenNameNOT = 71,
      TokenNameREMAINDER = 6,
      TokenNameXOR = 21,
      TokenNameAND = 20,
      TokenNameMULTIPLY = 5,
      TokenNameOR = 23,
      TokenNameTWIDDLE = 72,
      TokenNameDIVIDE = 7,
      TokenNameGREATER = 11,
      TokenNameLESS = 4,
      TokenNameLPAREN = 26,
      TokenNameRPAREN = 28,
      TokenNameLBRACE = 74,
      TokenNameRBRACE = 32,
      TokenNameLBRACKET = 8,
      TokenNameRBRACKET = 76,
      TokenNameSEMICOLON = 29,
      TokenNameQUESTION = 24,
      TokenNameCOLON = 56,
      TokenNameCOMMA = 30,
      TokenNameDOT = 1,
      TokenNameEQUAL = 77,
      TokenNameAT = 45,
      TokenNameELLIPSIS = 124,
      TokenNameBINDIN = 79,
      TokenNameBINDOUT = 75,
      TokenNameCALLOUT_OVERRIDE = 106,
      TokenNameEOF = 73,
      TokenNameERROR = 129;
}