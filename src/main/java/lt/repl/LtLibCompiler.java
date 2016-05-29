/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2016 KuiGang Wang
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package lt.repl;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * compiles all LessTyping libraries
 */
public class LtLibCompiler {
        public static final List<String> LESSTYPING_LIBRARY_CLASSES = Arrays.asList(
                "lt/compiler/semantic/SFieldDef.class",
                "lt/compiler/semantic/SMember.class",
                "lt/compiler/semantic/SAnnotationPresentable.class",
                "lt/compiler/semantic/LeftValue.class",
                "lt/compiler/semantic/Value.class",
                "lt/compiler/semantic/STypeDef.class",
                "lt/compiler/semantic/SModifier.class",
                "lt/compiler/semantic/SAnno.class",
                "lt/compiler/semantic/SAnnoDef.class",
                "lt/compiler/semantic/SAnnoField.class",
                "lt/compiler/semantic/SMethodDef.class",
                "lt/compiler/semantic/SInvokable.class",
                "lt/compiler/semantic/SParameter.class",
                "lt/compiler/semantic/Instruction.class",
                "lt/compiler/semantic/ExceptionTable.class",
                "lt/compiler/semantic/builtin/StringConstantValue.class",
                "lt/compiler/semantic/builtin/ByteValue.class",
                "lt/compiler/semantic/builtin/EnumValue.class",
                "lt/compiler/semantic/builtin/DoubleValue.class",
                "lt/compiler/semantic/builtin/FloatTypeDef.class",
                "lt/compiler/semantic/builtin/IntValue.class",
                "lt/compiler/semantic/builtin/FloatValue.class",
                "lt/compiler/semantic/builtin/ShortTypeDef.class",
                "lt/compiler/semantic/builtin/CharValue.class",
                "lt/compiler/semantic/builtin/ByteTypeDef.class",
                "lt/compiler/semantic/builtin/DoubleTypeDef.class",
                "lt/compiler/semantic/builtin/IntTypeDef.class",
                "lt/compiler/semantic/builtin/NullTypeDef.class",
                "lt/compiler/semantic/builtin/LongValue.class",
                "lt/compiler/semantic/builtin/ClassValue.class",
                "lt/compiler/semantic/builtin/BoolValue.class",
                "lt/compiler/semantic/builtin/CharTypeDef.class",
                "lt/compiler/semantic/builtin/ShortValue.class",
                "lt/compiler/semantic/builtin/BoolTypeDef.class",
                "lt/compiler/semantic/builtin/NullValue.class",
                "lt/compiler/semantic/builtin/LongTypeDef.class",
                "lt/compiler/semantic/ConstantValue.class",
                "lt/compiler/semantic/SClassDef.class",
                "lt/compiler/semantic/SConstructorDef.class",
                "lt/compiler/semantic/SInterfaceDef.class",
                "lt/compiler/semantic/PrimitiveValue.class",
                "lt/compiler/semantic/Ins$ANewArray.class",
                "lt/compiler/semantic/Ins$ArrayLength.class",
                "lt/compiler/semantic/Ins$AThrow.class",
                "lt/compiler/semantic/Ins$Cast.class",
                "lt/compiler/semantic/Ins$CheckCast.class",
                "lt/compiler/semantic/Ins$ExStore.class",
                "lt/compiler/semantic/Ins$GetClass.class",
                "lt/compiler/semantic/Ins$GetField.class",
                "lt/compiler/semantic/Ins$GetStatic.class",
                "lt/compiler/semantic/Ins$Goto.class",
                "lt/compiler/semantic/Ins$IfEq.class",
                "lt/compiler/semantic/Ins$IfNe.class",
                "lt/compiler/semantic/Ins$Invoke.class",
                "lt/compiler/semantic/Ins$InvokeDynamic.class",
                "lt/compiler/semantic/Ins$InvokeInterface.class",
                "lt/compiler/semantic/Ins$InvokeSpecial.class",
                "lt/compiler/semantic/Ins$InvokeStatic.class",
                "lt/compiler/semantic/Ins$InvokeVirtual.class",
                "lt/compiler/semantic/Ins$InvokeWithTarget.class",
                "lt/compiler/semantic/Ins$LogicAnd.class",
                "lt/compiler/semantic/Ins$LogicOr.class",
                "lt/compiler/semantic/Ins$MonitorEnter.class",
                "lt/compiler/semantic/Ins$MonitorExit.class",
                "lt/compiler/semantic/Ins$New.class",
                "lt/compiler/semantic/Ins$NewArray.class",
                "lt/compiler/semantic/Ins$NewList.class",
                "lt/compiler/semantic/Ins$NewMap.class",
                "lt/compiler/semantic/Ins$Nop.class",
                "lt/compiler/semantic/Ins$OneVarOp.class",
                "lt/compiler/semantic/Ins$Pop.class",
                "lt/compiler/semantic/Ins$PutField.class",
                "lt/compiler/semantic/Ins$PutStatic.class",
                "lt/compiler/semantic/Ins$TALoad.class",
                "lt/compiler/semantic/Ins$TAStore.class",
                "lt/compiler/semantic/Ins$This.class",
                "lt/compiler/semantic/Ins$TLoad.class",
                "lt/compiler/semantic/Ins$TReturn.class",
                "lt/compiler/semantic/Ins$TStore.class",
                "lt/compiler/semantic/Ins$TwoVarOp.class",
                "lt/compiler/semantic/Ins.class",
                "lt/compiler/semantic/VoidType.class",
                "lt/compiler/semantic/PrimitiveTypeDef.class",
                "lt/compiler/semantic/ValuePack.class",
                "lt/compiler/semantic/SArrayTypeDef.class",
                "lt/compiler/semantic/MethodHandleValue.class",
                "lt/compiler/semantic/LocalVariable.class",
                "lt/compiler/semantic/ValueAnotherType.class",
                "lt/compiler/semantic/MethodTypeValue.class",
                "lt/compiler/semantic/SArrayValue.class",
                "lt/compiler/LineCol.class",
                "lt/compiler/SyntaxException.class",
                "lt/compiler/CompileException.class",
                "lt/compiler/UnexpectedEndException.class",
                "lt/compiler/syntactic/operation/OneVariableOperation.class",
                "lt/compiler/syntactic/operation/TwoVariableOperation.class",
                "lt/compiler/syntactic/operation/UnaryOneVariableOperation.class",
                "lt/compiler/syntactic/Operation.class",
                "lt/compiler/syntactic/Expression.class",
                "lt/compiler/syntactic/Statement.class",
                "lt/compiler/syntactic/literal/NumberLiteral.class",
                "lt/compiler/syntactic/literal/BoolLiteral.class",
                "lt/compiler/syntactic/literal/StringLiteral.class",
                "lt/compiler/syntactic/Literal.class",
                "lt/compiler/syntactic/Definition.class",
                "lt/compiler/syntactic/UnexpectedNewLayerException.class",
                "lt/compiler/syntactic/UnknownTokenException.class",
                "lt/compiler/syntactic/DuplicateVariableNameException.class",
                "lt/compiler/syntactic/pre/Modifier$Available.class",
                "lt/compiler/syntactic/pre/Modifier.class",
                "lt/compiler/syntactic/pre/Import.class",
                "lt/compiler/syntactic/pre/PackageDeclare.class",
                "lt/compiler/syntactic/Pre.class",
                "lt/compiler/syntactic/AST$Access.class",
                "lt/compiler/syntactic/AST$Anno.class",
                "lt/compiler/syntactic/AST$ArrayExp.class",
                "lt/compiler/syntactic/AST$Assignment.class",
                "lt/compiler/syntactic/AST$AsType.class",
                "lt/compiler/syntactic/AST$Procedure.class",
                "lt/compiler/syntactic/AST$For.class",
                "lt/compiler/syntactic/AST$If$IfPair.class",
                "lt/compiler/syntactic/AST$If.class",
                "lt/compiler/syntactic/AST$Index.class",
                "lt/compiler/syntactic/AST$Invocation.class",
                "lt/compiler/syntactic/AST$Lambda.class",
                "lt/compiler/syntactic/AST$MapExp.class",
                "lt/compiler/syntactic/AST$Null.class",
                "lt/compiler/syntactic/AST$PackageRef.class",
                "lt/compiler/syntactic/AST$Pass.class",
                "lt/compiler/syntactic/AST$Return.class",
                "lt/compiler/syntactic/AST$StaticScope.class",
                "lt/compiler/syntactic/AST$Synchronized.class",
                "lt/compiler/syntactic/AST$Throw.class",
                "lt/compiler/syntactic/AST$Try.class",
                "lt/compiler/syntactic/AST$TypeOf.class",
                "lt/compiler/syntactic/AST$UndefinedExp.class",
                "lt/compiler/syntactic/AST$While.class",
                "lt/compiler/syntactic/AST$Continue.class",
                "lt/compiler/syntactic/AST$Break.class",
                "lt/compiler/syntactic/AST.class",
                "lt/compiler/syntactic/def/InterfaceDef.class",
                "lt/compiler/syntactic/def/ClassDef.class",
                "lt/compiler/syntactic/def/VariableDef.class",
                "lt/compiler/syntactic/def/MethodDef.class",
                "lt/compiler/LtBug.class",
                "lt/compiler/ErrorManager$Out.class",
                "lt/compiler/ErrorManager$CompilingError.class",
                "lt/compiler/ErrorManager$1.class",
                "lt/compiler/ErrorManager.class",
                "lt/compiler/UnexpectedTokenException.class",
                "lt/compiler/lexical/IllegalIndentationException.class",
                "lt/compiler/lexical/EndingNode.class",
                "lt/compiler/lexical/Node.class",
                "lt/compiler/lexical/Args.class",
                "lt/compiler/lexical/TokenType.class",
                "lt/compiler/lexical/ElementStartNode.class",
                "lt/compiler/lexical/PairEntry.class",
                "lt/compiler/lexical/Element.class",
                "lt/compiler/SemanticScope$MethodRecorder.class",
                "lt/compiler/SemanticScope.class",
                "lt/compiler/CodeInfo$Size.class",
                "lt/compiler/CodeInfo$Container.class",
                "lt/compiler/CodeInfo.class",
                "lt/compiler/Scanner$Properties.class",
                "lt/compiler/Scanner$LineAndString.class",
                "lt/compiler/Scanner$1.class",
                "lt/compiler/Scanner.class",
                "lt/compiler/CodeGenerator.class",
                "lt/compiler/SemanticProcessor$1.class",
                "lt/compiler/SemanticProcessor.class",
                "lt/compiler/CompileUtil.class",
                "lt/compiler/Parser$ParseFail.class",
                "lt/compiler/Parser$1.class",
                "lt/compiler/Parser.class",
                "lt/dependencies/asm/Type.class",
                "lt/dependencies/asm/Handle.class",
                "lt/dependencies/asm/signature/SignatureWriter.class",
                "lt/dependencies/asm/signature/SignatureVisitor.class",
                "lt/dependencies/asm/signature/SignatureReader.class",
                "lt/dependencies/asm/TypePath.class",
                "lt/dependencies/asm/Opcodes.class",
                "lt/dependencies/asm/AnnotationWriter.class",
                "lt/dependencies/asm/AnnotationVisitor.class",
                "lt/dependencies/asm/ClassWriter.class",
                "lt/dependencies/asm/ClassVisitor.class",
                "lt/dependencies/asm/ByteVector.class",
                "lt/dependencies/asm/ClassReader.class",
                "lt/dependencies/asm/Item.class",
                "lt/dependencies/asm/Attribute.class",
                "lt/dependencies/asm/FieldWriter.class",
                "lt/dependencies/asm/FieldVisitor.class",
                "lt/dependencies/asm/MethodWriter.class",
                "lt/dependencies/asm/MethodVisitor.class",
                "lt/dependencies/asm/Context.class",
                "lt/dependencies/asm/Label.class",
                "lt/dependencies/asm/Handler.class",
                "lt/dependencies/asm/Frame.class",
                "lt/dependencies/asm/Edge.class",
                "lt/dependencies/asm/TypeReference.class",
                "lt/lang/function/Function20.class",
                "lt/lang/function/Function.class",
                "lt/lang/function/Function17.class",
                "lt/lang/function/Function21.class",
                "lt/lang/function/Function1.class",
                "lt/lang/function/Function19.class",
                "lt/lang/function/Function16.class",
                "lt/lang/function/Function22.class",
                "lt/lang/function/Function0.class",
                "lt/lang/function/Function3.class",
                "lt/lang/function/Function23.class",
                "lt/lang/function/Function14.class",
                "lt/lang/function/Function10.class",
                "lt/lang/function/Function8.class",
                "lt/lang/function/Function2.class",
                "lt/lang/function/Function5.class",
                "lt/lang/function/Function26.class",
                "lt/lang/function/Function12.class",
                "lt/lang/function/Function7.class",
                "lt/lang/function/Function25.class",
                "lt/lang/function/Function13.class",
                "lt/lang/function/Function11.class",
                "lt/lang/function/Function6.class",
                "lt/lang/function/Function9.class",
                "lt/lang/function/Function24.class",
                "lt/lang/function/Function4.class",
                "lt/lang/function/Function15.class",
                "lt/lang/function/Function18.class",
                "lt/lang/List.class",
                "lt/lang/Dynamic.class",
                "lt/lang/RangeList.class",
                "lt/lang/Wrapper.class",
                "lt/lang/FunctionalAbstractClass.class",
                "lt/lang/Utils.class",
                "lt/lang/Undefined.class",
                "lt/lang/LtIterator$ArrayIt.class",
                "lt/lang/LtIterator$It.class",
                "lt/lang/LtIterator$EnIt.class",
                "lt/lang/LtIterator.class",
                "lt/lang/LtRuntime.class",
                "lt/repl/REPL.class",
                "lt/repl/ScriptCompiler$Script.class",
                "lt/repl/ScriptCompiler.class",
                "lt/repl/Compiler$Config$Threads.class",
                "lt/repl/Compiler$Config$Code.class",
                "lt/repl/Compiler$Config$Result.class",
                "lt/repl/Compiler$Config.class",
                "lt/repl/Compiler$1.class",
                "lt/repl/Compiler$FileRoot.class",
                "lt/repl/Compiler$Scan.class",
                "lt/repl/Compiler$Parse.class",
                "lt/repl/Compiler.class",
                "lt/repl/ClassPathLoader.class",
                "lt/repl/Evaluator$CL.class",
                "lt/repl/Evaluator$Entry.class",
                "lt/repl/Evaluator$1.class",
                "lt/repl/Evaluator$2.class",
                "lt/repl/Evaluator$3.class",
                "lt/repl/Evaluator.class",
                "lt/repl/LtLibCompiler.class"
        );

        public static List<String> LESSTYPING_LT_FILES = Arrays.asList(
                "/lt/html.lt"
        );

        public static List<String> REQUIRED_FILES = Arrays.asList(
                "classes.txt", "build.lts.template"
        );

        public static ClassLoader loadAllClasses(ClassLoader loader, String classLoc) throws Exception {
                Map<String, byte[]> bytes = new HashMap<>();

                final int length = ".class".length();
                boolean replaceSeparator = !File.separator.equals("/");
                for (String cls : LESSTYPING_LIBRARY_CLASSES) {
                        String className = cls.substring(0, cls.length() - length).replace("/", ".");
                        try {
                                Class.forName(className);
                        } catch (ClassNotFoundException e) {
                                // class not found
                                // try loader
                                try {
                                        loader.loadClass(className);
                                } catch (ClassNotFoundException e1) {
                                        System.out.println("loading " + className);
                                        // still not found
                                        // then get the bytecode
                                        File f = new File(classLoc + File.separator + (replaceSeparator
                                                ? cls.replace("/", File.separator)
                                                : cls));
                                        FileInputStream fis = new FileInputStream(f);
                                        ByteArrayOutputStream baos = new ByteArrayOutputStream(1024);
                                        byte[] b = new byte[1024];
                                        int n;
                                        while ((n = fis.read(b)) != -1) {
                                                baos.write(b, 0, n);
                                        }
                                        bytes.put(className, baos.toByteArray());
                                }
                        }
                }

                return new ClassLoader() {
                        @Override
                        protected Class<?> findClass(String name) throws ClassNotFoundException {
                                if (bytes.containsKey(name)) {
                                        byte[] b = bytes.get(name);
                                        return defineClass(name, b, 0, b.length);
                                } else throw new ClassNotFoundException(name);
                        }
                };
        }

        public static ClassLoader compileAllLtFiles(ClassLoader loader, String ltDir, String outputDir) {
                try {
                        boolean replaceSeparator = !File.separator.equals("/");

                        Compiler compiler = new Compiler(loader);
                        Map<String, File> map = new HashMap<>();
                        for (String lt : LESSTYPING_LT_FILES) {
                                map.put(lt, new File(ltDir +
                                        (replaceSeparator ? lt.replace("/", File.separator) : lt)
                                ));
                        }
                        compiler.config.result.outputDir = new File(outputDir);
                        return compiler.compile(map);
                } catch (Throwable ignore) {
                        // ignore.printStackTrace();
                        return null;
                }
        }

        public static void scanPath(String scanPath, String outputFile) throws Exception {
                StringBuilder sb = new StringBuilder();
                scanPathRecursive(new File(scanPath), sb, scanPath.length() + 1);
                File f = new File(outputFile);
                if (!f.exists()) f.createNewFile();
                FileOutputStream fos = new FileOutputStream(f);
                fos.write(sb.toString().getBytes());
        }

        public static void scanPathRecursive(File dir, final StringBuilder sb, final int l) {
                File[] files = dir.listFiles();
                if (files == null) return;
                for (File f : files) {
                        if (f.isDirectory()) {
                                scanPathRecursive(f, sb, l);
                        } else if (f.getName().endsWith(".class")) {
                                sb.append(f.getAbsolutePath().substring(l)).append("\n");
                        }
                }
        }

        public static void main(String[] args) throws Exception {
                String classLoc = args[0];
                String ltDir = args[1];
                String outputDir = args[0];
                String scanPath = args[0];
                String scanResult = outputDir + File.separator + "classes.txt";

                compileAllLtFiles(loadAllClasses(ClassLoader.getSystemClassLoader(), classLoc), ltDir, outputDir);
                scanPath(scanPath, scanResult);
        }
}
