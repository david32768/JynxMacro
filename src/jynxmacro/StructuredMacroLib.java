package jynxmacro;

import java.util.EnumSet;
import java.util.stream.Stream;

import static jynx2asm.ops.ExtendedOps.*;
import jynx2asm.ops.IndentType;
import static jynx2asm.ops.JvmOp.*;
import static jynx2asm.ops.LineOps.*;
import static jynx2asm.ops.SelectOps.xxx_xreturn;

import jynx2asm.ops.JynxOp;
import jynx2asm.ops.MacroLib;
import jynx2asm.ops.MacroOp;
import jynx2asm.ops.MacroOption;

public class StructuredMacroLib extends MacroLib {
            
    private final static String NAME = "structured";
        
    @Override
    public Stream<MacroOp> streamExternal() {
        return Stream.of(StructuredOps.values())
                .filter(StructuredOps::isExternal)
                .map(m->(MacroOp)m);
    }

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public EnumSet<MacroOption> getOptions() {
        return EnumSet.of(MacroOption.STRUCTURED_LABELS,MacroOption.INDENT);
    }

    public enum StructuredOps implements MacroOp {

        // structured ops
        ext_BLOCK(mac_label,lab_push),
        ext_LOOP(mac_label,tok_dup,lab_push,xxx_label),
        ext_RETURN(xxx_xreturn),

        ext_ELSE(lab_peek_if,asm_goto,lab_peek_else,xxx_label),
        ext_END(lab_peek_else,xxx_labelweak,lab_pop,xxx_labelweak),

        aux_iflabel(mac_label,lab_push_if,lab_peek_else),
        
        ext_IF_NEZ(aux_iflabel,asm_ifeq),
        ext_IF_EQZ(aux_iflabel,asm_ifne),
        ext_IF_LTZ(aux_iflabel,asm_ifge),
        ext_IF_LEZ(aux_iflabel,asm_ifgt),
        ext_IF_GTZ(aux_iflabel,asm_ifle),
        ext_IF_GEZ(aux_iflabel,asm_iflt),

        ext_IF_ICMPNE(aux_iflabel,asm_if_icmpeq),
        ext_IF_ICMPEQ(aux_iflabel,asm_if_icmpne),
        ext_IF_ICMPLT(aux_iflabel,asm_if_icmpge),
        ext_IF_ICMPLE(aux_iflabel,asm_if_icmpgt),
        ext_IF_ICMPGT(aux_iflabel,asm_if_icmple),
        ext_IF_ICMPGE(aux_iflabel,asm_if_icmplt),

        ext_IF_LCMPNE(aux_iflabel,ext_if_lcmpeq),
        ext_IF_LCMPEQ(aux_iflabel,ext_if_lcmpne),
        ext_IF_LCMPLT(aux_iflabel,ext_if_lcmpge),
        ext_IF_LCMPLE(aux_iflabel,ext_if_lcmpgt),
        ext_IF_LCMPGT(aux_iflabel,ext_if_lcmple),
        ext_IF_LCMPGE(aux_iflabel,ext_if_lcmplt),

        ext_IF_FCMPNE(aux_iflabel,ext_if_fcmpeq),
        ext_IF_FCMPEQ(aux_iflabel,ext_if_fcmpne),
        ext_IF_FCMPLT(aux_iflabel,ext_if_fcmpge),
        ext_IF_FCMPLE(aux_iflabel,ext_if_fcmpgt),
        ext_IF_FCMPGT(aux_iflabel,ext_if_fcmple),
        ext_IF_FCMPGE(aux_iflabel,ext_if_fcmplt),

        ext_IF_DCMPNE(aux_iflabel,ext_if_dcmpeq),
        ext_IF_DCMPEQ(aux_iflabel,ext_if_dcmpne),
        ext_IF_DCMPLT(aux_iflabel,ext_if_dcmpge),
        ext_IF_DCMPLE(aux_iflabel,ext_if_dcmpgt),
        ext_IF_DCMPGT(aux_iflabel,ext_if_dcmple),
        ext_IF_DCMPGE(aux_iflabel,ext_if_dcmplt),

        ext_IF_IUCMPLT(aux_iflabel,ext_if_iucmpge),
        ext_IF_IUCMPLE(aux_iflabel,ext_if_iucmpgt),
        ext_IF_IUCMPGT(aux_iflabel,ext_if_iucmple),
        ext_IF_IUCMPGE(aux_iflabel,ext_if_iucmplt),

        ext_IF_LUCMPLT(aux_iflabel,ext_if_lucmpge),
        ext_IF_LUCMPLE(aux_iflabel,ext_if_lucmpgt),
        ext_IF_LUCMPGT(aux_iflabel,ext_if_lucmple),
        ext_IF_LUCMPGE(aux_iflabel,ext_if_lucmplt),

        ext_BR(asm_goto),
        ext_BR_IFEQZ(asm_ifeq),
        ext_BR_IFNEZ(asm_ifne),
        ext_BR_IFLTZ(asm_iflt),
        ext_BR_IFLEZ(asm_ifle),
        ext_BR_IFGTZ(asm_ifgt),
        ext_BR_IFGEZ(asm_ifge),

        ext_BR_IF_ICMPEQ(asm_if_icmpeq),
        ext_BR_IF_ICMPNE(asm_if_icmpne),
        ext_BR_IF_ICMPLT(asm_if_icmplt),
        ext_BR_IF_ICMPLE(asm_if_icmple),
        ext_BR_IF_ICMPGT(asm_if_icmpgt),
        ext_BR_IF_ICMPGE(asm_if_icmpge),

        ext_BR_IF_LCMPEQ(ext_if_lcmpeq),
        ext_BR_IF_LCMPNE(ext_if_lcmpne),
        ext_BR_IF_LCMPLT(ext_if_lcmplt),
        ext_BR_IF_LCMPLE(ext_if_lcmple),
        ext_BR_IF_LCMPGT(ext_if_lcmpgt),
        ext_BR_IF_LCMPGE(ext_if_lcmpge),

        ext_BR_IF_FCMPEQ(ext_if_fcmpeq),
        ext_BR_IF_FCMPNE(ext_if_fcmpne),
        ext_BR_IF_FCMPLT(ext_if_fcmplt),
        ext_BR_IF_FCMPLE(ext_if_fcmple),
        ext_BR_IF_FCMPGT(ext_if_fcmpgt),
        ext_BR_IF_FCMPGE(ext_if_fcmpge),

        ext_BR_IF_DCMPEQ(ext_if_dcmpeq),
        ext_BR_IF_DCMPNE(ext_if_dcmpne),
        ext_BR_IF_DCMPLT(ext_if_dcmplt),
        ext_BR_IF_DCMPLE(ext_if_dcmple),
        ext_BR_IF_DCMPGT(ext_if_dcmpgt),
        ext_BR_IF_DCMPGE(ext_if_dcmpge),

        ext_BR_IF_IUCMPLT(ext_if_iucmplt),
        ext_BR_IF_IUCMPLE(ext_if_iucmple),
        ext_BR_IF_IUCMPGT(ext_if_iucmpgt),
        ext_BR_IF_IUCMPGE(ext_if_iucmpge),

        ext_BR_IF_LUCMPLT(ext_if_lucmplt),
        ext_BR_IF_LUCMPLE(ext_if_lucmple),
        ext_BR_IF_LUCMPGT(ext_if_lucmpgt),
        ext_BR_IF_LUCMPGE(ext_if_lucmpge),

        ;

        private final JynxOp[] jynxOps;

        private StructuredOps(JynxOp... jops) {
            this.jynxOps = jops;
        }

        @Override
        public JynxOp[] getJynxOps() {
            return jynxOps;
        }

        @Override
        public String toString() {
            return name().substring(4);
        }

        @Override
        public IndentType indentType() {
            switch (this) {
                case ext_BLOCK:
                case ext_LOOP:
                    return IndentType.BEGIN;
                case ext_ELSE:
                    return IndentType.END;
                case ext_END:
                    return IndentType.END;
            }
            if (name().startsWith("ext_IF_")) {
                return IndentType.BEGIN;
            }
            return IndentType.NONE;
        }

        @Override
        public boolean isExternal() {
            return name().startsWith("ext_");
        }
        
    }
}
