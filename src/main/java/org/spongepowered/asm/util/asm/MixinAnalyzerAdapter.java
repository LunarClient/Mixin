/*
 * This file is part of Mixin, licensed under the MIT License (MIT).
 *
 * Copyright (c) SpongePowered <https://www.spongepowered.org>
 * Copyright (c) contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.spongepowered.asm.util.asm;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.AnalyzerAdapter;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MixinAnalyzerAdapter extends AnalyzerAdapter {
    private final MethodNode method;

    private int lastNumLocal;
    private Object[] lastLocals;

    public MixinAnalyzerAdapter(ClassNode classNode, MethodNode method) {
        super(Opcodes.ASM9, classNode.name, method.access, method.name, method.desc, null);

        this.method = method;

        List<Object> locals = new ArrayList<>();

        if ((method.access & Opcodes.ACC_STATIC) == 0) {
            if ("<init>".equals(method.name)) {
                locals.add(Opcodes.UNINITIALIZED_THIS);
            } else {
                locals.add(classNode.name);
            }
        }

        for (Type argumentType : Type.getArgumentTypes(method.desc)) {
            switch (argumentType.getSort()) {
                case Type.BOOLEAN:
                case Type.CHAR:
                case Type.BYTE:
                case Type.SHORT:
                case Type.INT:
                    locals.add(Opcodes.INTEGER);
                    break;
                case Type.FLOAT:
                    locals.add(Opcodes.FLOAT);
                    break;
                case Type.LONG:
                    locals.add(Opcodes.LONG);
                    locals.add(Opcodes.TOP);
                    break;
                case Type.DOUBLE:
                    locals.add(Opcodes.DOUBLE);
                    locals.add(Opcodes.TOP);
                    break;
                case Type.ARRAY:
                    locals.add(argumentType.getDescriptor());
                    break;
                case Type.OBJECT:
                    locals.add(argumentType.getInternalName());
                    break;
                default:
                    throw new AssertionError();
            }
        }

        this.lastNumLocal = locals.size();
        this.lastLocals = locals.toArray();
    }

    public void analyze(AbstractInsnNode upTo) {
        InsnList instructions = this.method.instructions;
        this.method.instructions = MixinAnalyzerAdapter.cloneInsnListUpTo(instructions, upTo);
        this.method.accept(this);
        this.method.instructions = instructions;
    }

    public Type getLocal(int localIndex) {
        if (this.lastLocals == null || localIndex >= this.lastLocals.length) {
            return null;
        }

        Object object = this.locals.get(localIndex);

        if (object instanceof Integer) {
            switch ((Integer) object) {
                case Type.BOOLEAN:
                    return Type.BOOLEAN_TYPE;
                case Type.CHAR:
                    return Type.CHAR_TYPE;
                case Type.BYTE:
                    return Type.BYTE_TYPE;
                case Type.SHORT:
                    return Type.SHORT_TYPE;
                case Type.INT:
                    return Type.INT_TYPE;
                case Type.FLOAT:
                    return Type.FLOAT_TYPE;
                case Type.LONG:
                    return Type.LONG_TYPE;
                case Type.DOUBLE:
                    return Type.DOUBLE_TYPE;
                default:
                    break;
            }
        } else if (object instanceof String) {
            return Type.getObjectType((String) object);
        }

        return null;
    }

    @Override
    public void visitFrame(int type, int numLocal, Object[] local, int numStack, Object[] stack) {
        switch (type) {
            case Opcodes.F_NEW:
            case Opcodes.F_FULL: {
                super.visitFrame(Opcodes.F_NEW, numLocal, local, numStack, stack);
                break;
            }
            case Opcodes.F_APPEND: {
                Object[] newLocals = new Object[this.lastNumLocal + numLocal];
                System.arraycopy(this.lastLocals, 0, newLocals, 0, this.lastNumLocal);
                System.arraycopy(local, 0, newLocals, this.lastNumLocal, numLocal);

                super.visitFrame(Opcodes.F_NEW, newLocals.length, newLocals, 0, new Object[0]);
                break;
            }
            case Opcodes.F_CHOP: {
                Object[] newLocals = new Object[this.lastNumLocal - numLocal];
                System.arraycopy(this.lastLocals, 0, newLocals, 0, newLocals.length);

                super.visitFrame(Opcodes.F_NEW, newLocals.length, newLocals, 0, new Object[0]);
                break;
            }
            case Opcodes.F_SAME: {
                super.visitFrame(Opcodes.F_NEW, this.lastNumLocal, this.lastLocals, 0, new Object[0]);
                break;
            }
            case Opcodes.F_SAME1: {
                super.visitFrame(Opcodes.F_NEW, this.lastNumLocal, this.lastLocals, 1, stack);
                break;
            }
            default:
                throw new IllegalArgumentException("Invalid frame type: " + type);
        }

        this.lastNumLocal = this.locals.size();
        this.lastLocals = this.locals.toArray();
    }

    public static InsnList cloneInsnListUpTo(InsnList src, AbstractInsnNode stop) {
        InsnList copy = new InsnList();
        final Map<LabelNode, LabelNode> labelMap = new HashMap<>();

        for (AbstractInsnNode n = src.getFirst(); n != null; n = n.getNext()) {
            if (n instanceof LabelNode) {
                labelMap.put((LabelNode) n, new LabelNode());
            }
        }

        for (AbstractInsnNode n = src.getFirst(); n != null; n = n.getNext()) {
            if (n == stop) {
                break;
            }

            AbstractInsnNode cloned = n.clone(labelMap);
            copy.add(cloned);
        }

        return copy;
    }
}
