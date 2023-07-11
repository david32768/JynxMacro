package jynxmacro;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static jynx2asm.ops.AdjustToken.join;
import static jynx2asm.ops.AdjustToken.LC;
import static jynx2asm.ops.AdjustToken.removePrefix;
import jynx2asm.ops.JvmOp;
import static jynx2asm.ops.JvmOp.*;
import static jynx2asm.ops.LineOps.tok_skip;
import static jynx2asm.ops.LineOps.tok_skipall;
import static jynx2asm.ops.LineOps.tok_swap;
import static jynx2asm.ops.MessageOp.ignoreMacro;
import static jynx2asm.ops.MessageOp.unsupportedMacro;
import static jynx2asm.ops.TestToken.check;
import static jynx2asm.ops.TestToken.checkNot;

import jynx2asm.ops.JynxOp;
import jynx2asm.ops.MacroLib;
import jynx2asm.ops.MacroOp;

public class ASMTextMacroLib extends MacroLib {
            
    private final static String NAME = "ASMTextOps";
        
    @Override
    public Map<String, JynxOp> getMacros() {
        Map<String,JynxOp> map = new HashMap<>();
        JvmOp.getASMOps()
                .forEach(m-> map.put(m.toString().toUpperCase(), m));
        Stream.of(ASMTextOps.values())
                .forEach(m -> map.put(m.toString(),m)); // override some ASM ops
        return map;
    }
        
    @Override
    public String name() {
        return NAME;
    }

    private static final String LABEL_REGEX = "L[0-9]+";
    
    @Override
    public Predicate<String> labelTester() {
        return s->s.matches(LABEL_REGEX);
    }

    private enum ASMTextOps implements MacroOp {
        
        // Unsupported
        LDC(unsupportedMacro("Jynx ldc used instead but different format if not int or double"),asm_ldc),
        INVOKEDYNAMIC(unsupportedMacro("use Jynx invokedynamic instead as different format")),
        LOOKUPSWITCH(unsupportedMacro("use Jynx lookupswitch instead as different format")),
        TABLESWITCH(unsupportedMacro("use Jynx tableswitch instead as different format")),
        // ignore
        FRAME(ignoreMacro("stack map can be calculated"),tok_skipall),
        MAXSTACK(ignoreMacro("maxstack can be calculated"),check("="),tok_skip),
        MAXLOCALS(ignoreMacro("maxlocal can be calculated"),check("="),tok_skip),
        // different parameters
        GETFIELD(tok_swap,check(":"),asm_getfield),
        GETSTATIC(tok_swap,check(":"),asm_getstatic),
        INVOKEINTERFACE(join(""),asm_invokeinterface),
        INVOKESPECIAL(join(""),asm_invokespecial),
        INVOKESTATIC(join(""),asm_invokestatic,checkNot("{itf}")), // {itf} not supported (precede ClassName with @ instead)
        INVOKEVIRTUAL(join(""),asm_invokevirtual),
        LINENUMBER(xxx_line,tok_skip),
        NEWARRAY(removePrefix("T_"),LC(),asm_newarray),
        PUTFIELD(tok_swap,check(":"),asm_putfield),
        PUTSTATIC(tok_swap,check(":"),asm_putstatic),
        ;

        private final JynxOp[] jynxOps;

        private ASMTextOps(JynxOp... jops) {
            this.jynxOps = jops;
        }

        @Override
        public JynxOp[] getJynxOps() {
            return jynxOps;
        }

    }
}
