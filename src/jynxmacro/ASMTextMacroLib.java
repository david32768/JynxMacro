package jynxmacro;

import java.util.function.Predicate;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import static com.github.david32768.jynxfor.ops.JvmOp.*;

import static com.github.david32768.jynxfor.ops.AdjustToken.join;
import static com.github.david32768.jynxfor.ops.AdjustToken.LC;
import static com.github.david32768.jynxfor.ops.AdjustToken.removePrefix;
import static com.github.david32768.jynxfor.ops.LineOps.tok_skip;
import static com.github.david32768.jynxfor.ops.LineOps.tok_skipall;
import static com.github.david32768.jynxfor.ops.LineOps.tok_swap;
import static com.github.david32768.jynxfor.ops.MessageOp.ignoreMacro;
import static com.github.david32768.jynxfor.ops.MessageOp.unsupportedMacro;
import static com.github.david32768.jynxfor.ops.TestToken.check;
import static com.github.david32768.jynxfor.ops.TestToken.checkNot;

import com.github.david32768.jynxfor.ops.JvmOp;
import com.github.david32768.jynxfor.ops.JynxOp;
import com.github.david32768.jynxfor.ops.MacroLib;
import com.github.david32768.jynxfor.ops.MacroOp;

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
        LDC2_W(unsupportedMacro("Jynx ldc2_w used instead but different format for dynamic constant"),opc_ldc2_w),
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
