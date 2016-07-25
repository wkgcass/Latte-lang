package lt.compiler;

import lt.compiler.semantic.SClassDef;
import lt.compiler.semantic.SInterfaceDef;
import lt.compiler.semantic.STypeDef;
import lt.dependencies.asm.ClassWriter;

import java.util.Map;

/**
 * the ClassWriter used when generating class files.
 * Overrides the {@link lt.dependencies.asm.ClassWriter#getCommonSuperClass(String, String)} for the classes to be compiled.
 */
public class SClassWriter extends ClassWriter {
        private final Map<String, STypeDef> typeDefMap;

        public SClassWriter(int flags, Map<String, STypeDef> typeDefMap) {
                super(flags);
                this.typeDefMap = typeDefMap;
        }

        @Override
        protected String getCommonSuperClass(String type1, String type2) {
                // try to get class info from typeMap
                STypeDef sc = typeDefMap.get(type1.replace('/', '.'));
                STypeDef sd = typeDefMap.get(type2.replace('/', '.'));
                if (sc == null || sd == null)
                        throw new LtBug("class " + (sc == null ? type1 : type2) + " not found");

                if (sc.isAssignableFrom(sd)) {
                        return type1;
                } else if (sd.isAssignableFrom(sc)) {
                        return type2;
                } else if (sc instanceof SInterfaceDef || sd instanceof SInterfaceDef) {
                        return "java/lang/Object";
                } else {
                        do {
                                sc = ((SClassDef) sc).parent();
                        } while (!sc.isAssignableFrom(sd));
                        return sc.fullName().replace(".", "/");
                }
        }
}
