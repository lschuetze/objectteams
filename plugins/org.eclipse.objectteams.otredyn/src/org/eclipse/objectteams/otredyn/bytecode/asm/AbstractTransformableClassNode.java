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

import java.util.List;
import java.util.ListIterator;

import org.eclipse.objectteams.otredyn.bytecode.Method;
import org.eclipse.objectteams.otredyn.transformer.names.ClassNames;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.LookupSwitchInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;

/**
 * Every class, that wants to manipulate the bytecode of a class
 * with the ASM Trea API, have to inherit from this class and do
 * the transformations in the method transform().
 * Additionally the class provides util methods to
 * manipulate the bytecode 
 * @author Oliver Frank
 */
public abstract class AbstractTransformableClassNode extends ClassNode {
	
	/**
	 * Returns instructions, that are needed to pack all arguments of a method
	 * in an {@link Object} Array
	 * @param args The Types of the arguments
	 * @param isStatic is this method static or not
	 * @return
	 */
	protected InsnList getBoxingInstructions(Type[] args, boolean isStatic) {
		int firstArgIndex = 1;
		if (isStatic) {
			firstArgIndex = 0;
		}
		InsnList instructions = new InsnList();
		instructions.add(createLoadIntConstant(args.length));
		instructions.add(new TypeInsnNode(Opcodes.ANEWARRAY,
				ClassNames.OBJECT_SLASH));
		for (int i=0, slot=0; i < args.length; slot += args[i++].getSize()) {
			instructions.add(new InsnNode(Opcodes.DUP));
			instructions.add(createLoadIntConstant(i));
			instructions.add(new IntInsnNode(args[i].getOpcode(Opcodes.ILOAD),
					slot + firstArgIndex));
			if (args[i].getSort() != Type.OBJECT
					&& args[i].getSort() != Type.ARRAY) {
				instructions.add(AsmTypeHelper
						.getBoxingInstructionForType(args[i]));
			}
			instructions.add(new InsnNode(Opcodes.AASTORE));
		}

		return instructions;
	}

	/**
	 * Returns the instructions, that are needed to convert 
	 * a return value of the type {@link Object} to the real type
	 * @param returnType the real type
	 * @return
	 */
	protected InsnList getUnboxingInstructionsForReturnValue(Type returnType) {
		InsnList instructions = new InsnList();
		switch (returnType.getSort()) {
		case Type.VOID:
			instructions.add(new InsnNode(Opcodes.POP));
			instructions.add(new InsnNode(Opcodes.RETURN));
			break;
		case Type.ARRAY: // fallthrough
		case Type.OBJECT:
			instructions.add(new TypeInsnNode(Opcodes.CHECKCAST, returnType
					.getInternalName()));
			instructions.add(new InsnNode(Opcodes.ARETURN));
			break;
		default:
			String objectType = AsmTypeHelper.getObjectType(returnType);
			instructions.add(new TypeInsnNode(Opcodes.CHECKCAST, objectType));
			instructions.add(AsmTypeHelper.getUnboxingInstructionForType(
					returnType, objectType));
			instructions
					.add(new InsnNode(returnType.getOpcode(Opcodes.IRETURN)));
		}
		return instructions;
	}

	/**
	 * Adds a new Label to an existing switch statement
	 * @param instructions the instructions, in which the switch statement is defined
	 * @param newInstructions the instructions of the new label
	 * @param labelIndex the index of the label
	 */
	@SuppressWarnings("unchecked")
	protected void addNewLabelToSwitch(InsnList instructions,
			InsnList newInstructions, int labelIndex) {
		ListIterator<AbstractInsnNode> iter = instructions.iterator();
		LookupSwitchInsnNode lSwitch = null;
		while (iter.hasNext()) {
			AbstractInsnNode node = (AbstractInsnNode) iter.next();
			if (node.getType() == AbstractInsnNode.LOOKUPSWITCH_INSN) {
				lSwitch = (LookupSwitchInsnNode) node;
				LabelNode label = new LabelNode();
				boolean labelAdded = false;
				for (int i = 0; i < lSwitch.keys.size(); i++) {
					Integer key = (Integer) lSwitch.keys.get(i);
					if (key >= labelIndex) {
						lSwitch.keys.add(i, labelIndex);
						lSwitch.labels.add(i, label);
						labelAdded = true;
						break;
					}
				}
				if (!labelAdded) {
					lSwitch.labels.add(label);
					lSwitch.keys.add(labelIndex);
				}
				boolean foundDefLabel = false;
				AbstractInsnNode prevNode = node;
				while (iter.hasNext()) {
					node = (AbstractInsnNode) iter.next();
					if (node.getType() == AbstractInsnNode.LABEL) {
						if (!foundDefLabel) {
							foundDefLabel = true;
						} else {
							break;
						}
					}
					prevNode = node;
				}
				instructions.insert(prevNode, label);
				instructions.insert(label, newInstructions);
			}	
		}
		if (lSwitch == null) {
			throw new RuntimeException("No switch statement found.");
		}
	}

	/**
	 * Returns a {@link MethodNode} for a given {@link Method} instance
	 * @param method
	 * @return the {@link MethodNode} or null if there is no such method
	 */
	@SuppressWarnings("unchecked")
	protected MethodNode getMethod(Method method) {
		List<MethodNode> methodList = methods;
		for (MethodNode methodNode : methodList) {
			if (methodNode.name.compareTo(method.getName()) == 0
					&& methodNode.desc.compareTo(method.getSignature()) == 0) {
				return methodNode;
			}
		}

		return null;
	}

	/**
	 * This method could be used to generate debug outputs in the generated code in the form: <br>
	 * <code>
	 * Sytsem.out.println(message);
	 * </code>
	 * @param message
	 * @return
	 */
	protected InsnList getInstructionsForDebugOutput(String message) {
		InsnList instructions = new InsnList();
		instructions.add(new FieldInsnNode(Opcodes.GETSTATIC,
				"java/lang/System", "out", "Ljava/io/PrintStream;"));
		instructions.add(new LdcInsnNode(message));
		instructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL,
				"java/io/PrintStream", "println", "(Ljava/lang/String;)V"));
		return instructions;
	}
	
	/**
	 * Adds instructions to put all arguments of a method on the stack.
	 * @param instructions
	 * @param args
	 * @param isStatic
	 */
	protected void addInstructionsForLoadArguments(InsnList instructions, Type[] args, boolean isStatic) {
		int firstArgIndex = 1;
		if (isStatic) {
			firstArgIndex = 0;
		}
		// put "this" on the stack for an non-static method
		if (!isStatic) {
			instructions.add(new IntInsnNode(Opcodes.ALOAD, 0));
		}
		for (int i=0, slot=firstArgIndex; i < args.length; slot+=args[i++].getSize()) {
			instructions.add(new IntInsnNode(args[i].getOpcode(Opcodes.ILOAD),
					slot));
		}
	}
	
	/**
	 * Replace all return statements in the given instructions with new 
	 * statements that convert the real return value to {@link Object}
	 * and return this new {@link Object}
	 * 
	 * @param instructions
	 * @param returnType
	 */
	@SuppressWarnings("unchecked")
	protected void replaceReturn(InsnList instructions, Type returnType) {
		if (returnType.getSort() != Type.OBJECT &&
				returnType.getSort() != Type.ARRAY &&
				returnType.getSort() != Type.VOID) {
			ListIterator<AbstractInsnNode> orgMethodIter = instructions.iterator();
			while (orgMethodIter.hasNext()) {
				AbstractInsnNode orgMethodNode = orgMethodIter.next();
				if (orgMethodNode.getOpcode() == returnType.getOpcode(Opcodes.IRETURN)) {
					instructions.remove(orgMethodNode);
					instructions.add(AsmTypeHelper.getBoxingInstructionForType(returnType));
					instructions.add(new InsnNode(Opcodes.ARETURN));
				}
			}
		} else if (returnType.getSort() == Type.VOID) {
			ListIterator<AbstractInsnNode> orgMethodIter = instructions.iterator();
			while (orgMethodIter.hasNext()) {
				AbstractInsnNode orgMethodNode = orgMethodIter.next();
				if (orgMethodNode.getOpcode() == Opcodes.RETURN) {
					instructions.remove(orgMethodNode);
				}
			}
			instructions.add(new InsnNode(Opcodes.ACONST_NULL));
			instructions.add(new InsnNode(Opcodes.ARETURN));
		}
	}

	/**
	 * Create an instruction for loading an integer constant,
	 * using the most compact possible format. 
	 */
	protected AbstractInsnNode createLoadIntConstant(int constant) {
		if (constant <= 5)
			return new InsnNode(Opcodes.ICONST_0+constant);
		else if (constant < Byte.MAX_VALUE)
			return new IntInsnNode(Opcodes.BIPUSH, constant);
		else
			return new LdcInsnNode(constant);
	}
	/**
	 * In this method, concrete Implementations of this class
	 * can manipulate the bytecode
	 */
	protected abstract void transform();
}