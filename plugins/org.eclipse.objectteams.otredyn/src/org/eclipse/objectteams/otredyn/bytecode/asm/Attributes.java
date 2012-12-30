/**********************************************************************
 * This file is part of "Object Teams Dynamic Runtime Environment"
 * 
 * Copyright 2009, 2012 Oliver Frank and others.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Please visit http://www.eclipse.org/objectteams for updates and contact.
 * 
 * Contributors:
 *		Oliver Frank - Initial API and implementation
 *		Stephan Herrmann - Initial API and implementation
 **********************************************************************/
package org.eclipse.objectteams.otredyn.bytecode.asm;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.objectteams.otredyn.bytecode.AbstractBoundClass;
import org.eclipse.objectteams.otredyn.bytecode.Binding;
import org.eclipse.objectteams.otredyn.bytecode.ClassRepository;
import org.eclipse.objectteams.otredyn.runtime.ClassIdentifierProviderFactory;
import org.eclipse.objectteams.otredyn.runtime.IBinding;
import org.eclipse.objectteams.otredyn.runtime.IClassIdentifierProvider;
import org.objectweb.asm.Attribute;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Label;

/**
 * This class contains all classes representing OT/J class file attributes
 * @author Oliver Frank
 */
abstract class Attributes {
	protected final static String ATTRIBUTE_OT_DYN_CALLIN_BINDINGS="OTDynCallinBindings";
	protected final static String ATTRIBUTE_CALLIN_PRECEDENCE = "CallinPrecedence";
	protected final static String ATTRIBUTE_OT_CLASS_FLAGS = "OTClassFlags";
	protected final static String ATTRIBUTE_OT_SPECIAL_ACCESS = "OTSpecialAccess";

	protected final static Attribute[] attributes = { 
		new CallinBindingsAttribute(0),
		new CallinPrecedenceAttribute(0),
		new OTClassFlagsAttribute(0),
		new OTSpecialAccessAttribute()
	};
	
	protected static class CallinBindingsAttribute extends Attribute {
		static final short COVARIANT_BASE_RETURN = 8;

		/** Represents all base method bindings of one callin binding. */
		protected static class MultiBinding {
			private String   roleClassName;
			private String   callinLabel;
			private String   baseClassName;
			private String[] baseMethodNames;
			private String[] baseMethodSignatures;
			private String[] declaringBaseClassNames;
			private int      callinModifier;
			private int[]    callinIds;
			private boolean  isHandleCovariantReturn;
			MultiBinding(String roleName, String callinLabel,
						 String baseClassName, 
						 String[] baseMethodNames, String[] baseMethodSignatures, String[] declaringBaseClassNames,
						 int callinModifier, int[] callinIds, boolean handleCovariantReturn) 
			{
				this.roleClassName = roleName;
				this.callinLabel = callinLabel;
				this.baseClassName = baseClassName;
				this.baseMethodNames = baseMethodNames;
				this.baseMethodSignatures = baseMethodSignatures;
				this.declaringBaseClassNames = declaringBaseClassNames;
				this.callinModifier = callinModifier;
				this.callinIds = callinIds;
				this.isHandleCovariantReturn = handleCovariantReturn;
			}
			protected String getRoleClassName() {
				return roleClassName;
			}

			protected String getBaseClassName() {
				return baseClassName;
			}

			protected String[] getBaseMethodNames() {
				return baseMethodNames;
			}

			protected String[] getBaseMethodSignatures() {
				return baseMethodSignatures;
			}
			
			protected int getCallinModifier() {
				return this.callinModifier;
			}

			protected int[] getCallinIds() {
				return callinIds;
			}

			protected String getCallinLabel() {
				return callinLabel;
			}
			public boolean isHandleCovariantReturn() {
				return this. isHandleCovariantReturn;
			}
			public String[] getDeclaringBaseClassName() {
				return this.declaringBaseClassNames;
			}
		}
		
		private MultiBinding[] bindings;

		public CallinBindingsAttribute(int bindingsCount) {
			super(ATTRIBUTE_OT_DYN_CALLIN_BINDINGS);
			this.bindings = new MultiBinding[bindingsCount];
		}
		
		private void addBinding(int i, String roleName, String callinLabel,
				                String baseClassName, 
				                String[] baseMethodNames, String[] baseMethodSignatures, String[] declaringBaseClassNames,
				                String callinModifierName, int[] callinIds, boolean handleCovariantReturn) {
			int callinModifier = 0;
			if ("before".equals(callinModifierName))
				callinModifier = Binding.BEFORE;
			else if ("after".equals(callinModifierName))
				callinModifier = Binding.AFTER;
			else
				callinModifier = Binding.REPLACE;
			this.bindings[i] = new MultiBinding(roleName, callinLabel,
					                            baseClassName, 
					                            baseMethodNames, baseMethodSignatures, declaringBaseClassNames,
					                            callinModifier, callinIds, handleCovariantReturn);
		}
		
		@Override
		protected Attribute read(ClassReader cr, int off, int len,
				char[] buf, int codeOff, Label[] labels) 
		{
			int bindingsCount = cr.readShort(off);							off += 2;
			CallinBindingsAttribute attr = new CallinBindingsAttribute(bindingsCount);
			for (int i = 0; i < bindingsCount; i++) {
				String roleName					= cr.readUTF8(off, buf);	off += 2;
				String callinLabel				= cr.readUTF8(off, buf);	off += 2;
				/* skip roleSelector, roleSignature */						off += 4;
				String callinModifier			= cr.readUTF8(off, buf);	off += 2;
				int flags						= cr.readByte(off);			off += 1;
				String baseClassName 			= cr.readUTF8(off, buf);	off += 2;
				/* skip filename & lineNumber & lineOffset */				off += 6;
				int baseMethodsCount 			= cr.readShort(off);		off += 2;
				String[] baseMethodNames 		= new String[baseMethodsCount];
				String[] baseMethodSignatures 	= new String[baseMethodsCount];
				String[] declaringBaseClassNames 	= new String[baseMethodsCount];
				int[] callinIds = new int[baseMethodsCount];
				for (int m = 0; m < baseMethodsCount; m++) {
					String baseMethodName 		= cr.readUTF8(off, buf);	off += 2;
					String baseMethodSignature 	= cr.readUTF8(off, buf);	off += 2;
					String declaringBaseClass 	= cr.readUTF8(off, buf);	off += 2;
					int callinId				= cr.readInt(off);			off += 4;
					baseMethodNames[m] 			= baseMethodName;
					baseMethodSignatures[m]		= baseMethodSignature;
					declaringBaseClassNames[m]  = declaringBaseClass;
					callinIds[m] 				= callinId;
					/* skip baseFlags & translationFlags */					off += 3;
				}
				attr.addBinding(i, roleName, callinLabel,
								baseClassName,
								baseMethodNames, baseMethodSignatures, declaringBaseClassNames,
								callinModifier, callinIds, ((flags & COVARIANT_BASE_RETURN) != 0));
			}
			return attr;
		}
		
		public MultiBinding[] getBindings() {
			return this.bindings;
		}
		
		@Override
		public String toString() {
			StringBuffer buf = new StringBuffer();
			for (MultiBinding binding : this.bindings) {
				buf.append(binding.getBaseClassName());
				int[] callinIds = binding.getCallinIds();
				String[] baseMethodNames = binding.getBaseMethodNames();
				String[] baseMethodSignatures = binding.getBaseMethodSignatures();
				for (int i=0; i<callinIds.length; i++) {
					buf.append("\n\t{");
					buf.append(callinIds[i]);
					buf.append("} ");
					buf.append(baseMethodNames[i]);
					buf.append(baseMethodSignatures[i]);
				}				
			}
			return buf.toString();
		}
	}
	protected static class CallinPrecedenceAttribute extends Attribute {
		String[] labels;
		public CallinPrecedenceAttribute(int elementCount) {
			super(ATTRIBUTE_CALLIN_PRECEDENCE);
			this.labels = new String[elementCount];
		}
		@Override
		protected Attribute read(ClassReader cr, int off, int len,
				char[] buf, int codeOff, Label[] labels) 
		{
			int elementCount = cr.readShort(off);		off += 2;
			CallinPrecedenceAttribute attr = new CallinPrecedenceAttribute(elementCount);
			for (int i = 0; i < elementCount; i++) {
				attr.labels[i] = cr.readUTF8(off, buf);	off += 2;
			}
			return attr;
		}
	}
	protected static class OTClassFlagsAttribute extends Attribute {
		int flags;
		protected OTClassFlagsAttribute(int flags) {
			super(ATTRIBUTE_OT_CLASS_FLAGS);
			this.flags = flags;
		}
		@Override
		protected Attribute read(ClassReader cr, int off, int len, char[] buf,
				int codeOff, Label[] labels) 
		{
			return new OTClassFlagsAttribute(cr.readUnsignedShort(off));			
		}
	}
	protected static class OTSpecialAccessAttribute extends Attribute {
		class DecapsField {
			String accessMode;
			boolean isStatic;
			String baseclass, name, desc;
			public int accessId;
			public DecapsField(String baseclass, String name, String desc, int accessId, String accessMode, boolean isStatic) {
				this.baseclass = baseclass;
				this.name = name;
				this.desc = desc;
				this.accessId = accessId;
				this.accessMode = accessMode;
				this.isStatic = isStatic;
			}
		}
		class DecapsMethod {
			String baseclass, name, desc;
			int id;
			boolean isStatic;
			DecapsMethod(String baseclass, String name, String desc, int id, boolean isStatic) {
				this.baseclass = baseclass;
				this.name = name;
				this.desc = desc;
				this.id = id;
				this.isStatic = isStatic;
			}
		}
		private static final int DECAPSULATION_METHOD_ACCESS= 4; // kinds are disjoint from those used by the old OTRE
		private static final int CALLOUT_FIELD_ACCESS = 5;
		private static final int SUPER_METHOD_ACCESS = 6;

		List<DecapsMethod> methods = new ArrayList<DecapsMethod>();
		List<DecapsField> fields = new ArrayList<DecapsField>();
		
		protected OTSpecialAccessAttribute() {
			super(ATTRIBUTE_OT_SPECIAL_ACCESS);
		}
		@Override
		protected Attribute read(ClassReader cr, int off, int len, char[] buf,
				int codeOff, Label[] labels) 
		{
			OTSpecialAccessAttribute attr = new OTSpecialAccessAttribute();
			int size = cr.readUnsignedShort(off); 			off+=2;
			for (int i=0; i<size; i++) {
				int kind = cr.readByte(off++);
				switch (kind) {
				case DECAPSULATION_METHOD_ACCESS:
					attr.readMethodAccess(cr, off, buf);	off+=6;
					break;
				case CALLOUT_FIELD_ACCESS:
					attr.readFieldAccess(cr, off, buf); 	off+=8;
				default:
					// FIXME(SH): handle!
				}
			}
			return attr;
		}
		private void readMethodAccess(ClassReader cr, int off, char[] buf) {
			String className   = cr.readUTF8(off, buf);
			String encodedName = cr.readUTF8(off+2, buf);
			String methodDesc  = cr.readUTF8(off+4, buf);
			int id = cr.readUnsignedShort(off+6);
			boolean isStatic = false;
			String baseClass;
			String methodName;
			int pos = encodedName.indexOf('?');
			if (pos == -1) {
				pos = encodedName.indexOf('!');
				isStatic = true;
			}
			baseClass = encodedName.substring(0, pos);
			methodName = encodedName.substring(pos+1);
			this.methods.add(new DecapsMethod(baseClass, methodName, methodDesc, id, isStatic));
		}
		private void readFieldAccess(ClassReader cr, int off, char[] buf) {
			int accessId = cr.readByte(off);
			int flags = cr.readByte(off+1);
			String className   = cr.readUTF8(off+2, buf);
			String fieldName = cr.readUTF8(off+4, buf);
			String fieldDesc  = cr.readUTF8(off+6, buf);
			boolean isStatic = (flags & 2) != 0;
			String accessMode = (flags & 1) == 1 ? "set" : "get";
			this.fields.add(new DecapsField(className, fieldName, fieldDesc, accessId, accessMode, isStatic));
		}
		public void registerAt(AsmBoundClass clazz) {
			ClassRepository repo = ClassRepository.getInstance();
			IClassIdentifierProvider provider = ClassIdentifierProviderFactory.getClassIdentifierProvider();

			for (DecapsMethod dMethod : this.methods) {
				// FIXME(SH): the following may need adaptation for OT/Equinox or other multi-classloader settings:
				// bypassing the identifier provider (we don't have a Class<?> yet):
				// String boundClassIdentifier = provider.getBoundClassIdentifier(clazz, dMethod.baseclass);
				AbstractBoundClass baseclass = repo.getBoundClass(dMethod.baseclass, dMethod.baseclass.replace('.', '/'), clazz.getClassLoader());
				// register the target method:
				baseclass.getMethod(dMethod.name, dMethod.desc, false/*covariantReturn*/, dMethod.isStatic);
				clazz.recordAccessId(dMethod.id);
				clazz.addBinding(new Binding(clazz, dMethod.baseclass, dMethod.name, dMethod.desc, dMethod.id, IBinding.BindingType.METHOD_ACCESS));
			}

			for (DecapsField dField: this.fields) {
				// FIXME(SH): the following may need adaptation for OT/Equinox or other multi-classloader settings:
				// bypassing the identifier provider (we don't have a Class<?> yet):
				// String boundClassIdentifier = provider.getBoundClassIdentifier(clazz, dMethod.baseclass);
				AbstractBoundClass baseclass = repo.getBoundClass(dField.baseclass, dField.baseclass.replace('.', '/'), clazz.getClassLoader());
				// register the target field:
				baseclass.getField(dField.name, dField.desc);
				clazz.recordAccessId(dField.accessId);
				clazz.addBinding(new Binding(clazz, dField.baseclass, dField.name, dField.desc, dField.accessId, IBinding.BindingType.FIELD_ACCESS));
			}
		}
	}
}