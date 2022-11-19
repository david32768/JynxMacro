package jynxmacro;

import java.util.function.Predicate;
import java.util.stream.Stream;

import static jynx2asm.ops.JvmOp.*;
import static jynx2asm.ops.MessageOp.ignoreMacro;
import static jynx2asm.ops.AdjustToken.join;
import static jynx2asm.ops.AdjustToken.LC;
import static jynx2asm.ops.AdjustToken.replace;
import static jynx2asm.ops.LineOps.tok_skip;
import static jynx2asm.ops.LineOps.tok_skipall;
import static jynx2asm.ops.LineOps.tok_swap;
import static jynx2asm.ops.MessageOp.unsupportedMacro;
import static jynx2asm.ops.TestToken.check;
import static jynx2asm.ops.TestToken.checkNot;

import jynx2asm.ops.JynxOp;
import jynx2asm.ops.MacroLib;
import jynx2asm.ops.MacroOp;

public class ASMTextifier extends MacroLib {
            
    private final static String NAME = "ASMTextierOps";
        
    @Override
    public Stream<MacroOp> streamExternal() {
        return Stream.of(ASMTextOps.values())
                .filter(ASMTextOps::isExternal)
                .map(m->(MacroOp)m);
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
        NEWARRAY(replace("T_",""),LC(),asm_newarray),
        PUTFIELD(tok_swap,check(":"),asm_putfield),
        PUTSTATIC(tok_swap,check(":"),asm_putstatic),
        // Upper case
        AALOAD(asm_aaload),
        AASTORE(asm_aastore),
        ACONST_NULL(asm_aconst_null),
        ALOAD(asm_aload),
        ANEWARRAY(asm_anewarray),
        ARETURN(asm_areturn),
        ARRAYLENGTH(asm_arraylength),
        ASTORE(asm_astore),
        ATHROW(asm_athrow),
        BALOAD(asm_baload),
        BASTORE(asm_bastore),
        BIPUSH(asm_bipush),
        CALOAD(asm_caload),
        CASTORE(asm_castore),
        CHECKCAST(asm_checkcast),
        D2F(asm_d2f),
        D2I(asm_d2i),
        D2L(asm_d2l),
        DADD(asm_dadd),
        DALOAD(asm_daload),
        DASTORE(asm_dastore),
        DCMPG(asm_dcmpg),
        DCMPL(asm_dcmpl),
        DCONST_0(asm_dconst_0),
        DCONST_1(asm_dconst_1),
        DDIV(asm_ddiv),
        DLOAD(asm_dload),
        DMUL(asm_dmul),
        DNEG(asm_dneg),
        DREM(asm_drem),
        DRETURN(asm_dreturn),
        DSTORE(asm_dstore),
        DSUB(asm_dsub),
        DUP(asm_dup),
        DUP2(asm_dup2),
        DUP2_X1(asm_dup2_x1),
        DUP2_X2(asm_dup2_x2),
        DUP_X1(asm_dup_x1),
        DUP_X2(asm_dup_x2),
        F2D(asm_f2d),
        F2I(asm_f2i),
        F2L(asm_f2l),
        FADD(asm_fadd),
        FALOAD(asm_faload),
        FASTORE(asm_fastore),
        FCMPG(asm_fcmpg),
        FCMPL(asm_fcmpl),
        FCONST_0(asm_fconst_0),
        FCONST_1(asm_fconst_1),
        FCONST_2(asm_fconst_2),
        FDIV(asm_fdiv),
        FLOAD(asm_fload),
        FMUL(asm_fmul),
        FNEG(asm_fneg),
        FREM(asm_frem),
        FRETURN(asm_freturn),
        FSTORE(asm_fstore),
        FSUB(asm_fsub),
        GOTO(asm_goto),
        I2B(asm_i2b),
        I2C(asm_i2c),
        I2D(asm_i2d),
        I2F(asm_i2f),
        I2L(asm_i2l),
        I2S(asm_i2s),
        IADD(asm_iadd),
        IALOAD(asm_iaload),
        IAND(asm_iand),
        IASTORE(asm_iastore),
        ICONST_0(asm_iconst_0),
        ICONST_1(asm_iconst_1),
        ICONST_2(asm_iconst_2),
        ICONST_3(asm_iconst_3),
        ICONST_4(asm_iconst_4),
        ICONST_5(asm_iconst_5),
        ICONST_M1(asm_iconst_m1),
        IDIV(asm_idiv),
        IF_ACMPEQ(asm_if_acmpeq),
        IF_ACMPNE(asm_if_acmpne),
        IF_ICMPEQ(asm_if_icmpeq),
        IF_ICMPGE(asm_if_icmpge),
        IF_ICMPGT(asm_if_icmpgt),
        IF_ICMPLE(asm_if_icmple),
        IF_ICMPLT(asm_if_icmplt),
        IF_ICMPNE(asm_if_icmpne),
        IFEQ(asm_ifeq),
        IFGE(asm_ifge),
        IFGT(asm_ifgt),
        IFLE(asm_ifle),
        IFLT(asm_iflt),
        IFNE(asm_ifne),
        IFNONNULL(asm_ifnonnull),
        IFNULL(asm_ifnull),
        IINC(asm_iinc),
        ILOAD(asm_iload),
        IMUL(asm_imul),
        INEG(asm_ineg),
        INSTANCEOF(asm_instanceof),
        IOR(asm_ior),
        IREM(asm_irem),
        IRETURN(asm_ireturn),
        ISHL(asm_ishl),
        ISHR(asm_ishr),
        ISTORE(asm_istore),
        ISUB(asm_isub),
        IUSHR(asm_iushr),
        IXOR(asm_ixor),
        JSR(asm_jsr),
        L2D(asm_l2d),
        L2F(asm_l2f),
        L2I(asm_l2i),
        LADD(asm_ladd),
        LALOAD(asm_laload),
        LAND(asm_land),
        LASTORE(asm_lastore),
        LCMP(asm_lcmp),
        LCONST_0(asm_lconst_0),
        LCONST_1(asm_lconst_1),
        LDIV(asm_ldiv),
        LLOAD(asm_lload),
        LMUL(asm_lmul),
        LNEG(asm_lneg),
        LOR(asm_lor),
        LREM(asm_lrem),
        LRETURN(asm_lreturn),
        LSHL(asm_lshl),
        LSHR(asm_lshr),
        LSTORE(asm_lstore),
        LSUB(asm_lsub),
        LUSHR(asm_lushr),
        LXOR(asm_lxor),
        MONITORENTER(asm_monitorenter),
        MONITOREXIT(asm_monitorexit),
        MULTIANEWARRAY(asm_multianewarray),
        NEW(asm_new),
        NOP(asm_nop),
        POP(asm_pop),
        POP2(asm_pop2),
        RET(asm_ret),
        RETURN(asm_return),
        SALOAD(asm_saload),
        SASTORE(asm_sastore),
        SIPUSH(asm_sipush),
        SWAP(asm_swap),
        
        // Not output by ASM
        ALOAD_0(opc_aload_0),
        ALOAD_1(opc_aload_1),
        ALOAD_2(opc_aload_2),
        ALOAD_3(opc_aload_3),
        ALOAD_W(opc_aload_w),
        ASTORE_0(opc_astore_0),
        ASTORE_1(opc_astore_1),
        ASTORE_2(opc_astore_2),
        ASTORE_3(opc_astore_3),
        ASTORE_W(opc_astore_w),
        DLOAD_0(opc_dload_0),
        DLOAD_1(opc_dload_1),
        DLOAD_2(opc_dload_2),
        DLOAD_3(opc_dload_3),
        DLOAD_W(opc_dload_w),
        DSTORE_0(opc_dstore_0),
        DSTORE_1(opc_dstore_1),
        DSTORE_2(opc_dstore_2),
        DSTORE_3(opc_dstore_3),
        DSTORE_W(opc_dstore_w),
        FLOAD_0(opc_fload_0),
        FLOAD_1(opc_fload_1),
        FLOAD_2(opc_fload_2),
        FLOAD_3(opc_fload_3),
        FLOAD_W(opc_fload_w),
        FSTORE_0(opc_fstore_0),
        FSTORE_1(opc_fstore_1),
        FSTORE_2(opc_fstore_2),
        FSTORE_3(opc_fstore_3),
        FSTORE_W(opc_fstore_w),
        GOTO_W(opc_goto_w),
        IINC_W(opc_iinc_w),
        ILOAD_0(opc_iload_0),
        ILOAD_1(opc_iload_1),
        ILOAD_2(opc_iload_2),
        ILOAD_3(opc_iload_3),
        ILOAD_W(opc_iload_w),
        INVOKENONVIRTUAL(opc_invokenonvirtual),
        ISTORE_0(opc_istore_0),
        ISTORE_1(opc_istore_1),
        ISTORE_2(opc_istore_2),
        ISTORE_3(opc_istore_3),
        ISTORE_W(opc_istore_w),
        JSR_W(opc_jsr_w),
        LDC2_W(opc_ldc2_w),
        LDC_W(opc_ldc_w),
        LLOAD_0(opc_lload_0),
        LLOAD_1(opc_lload_1),
        LLOAD_2(opc_lload_2),
        LLOAD_3(opc_lload_3),
        LLOAD_W(opc_lload_w),
        LSTORE_0(opc_lstore_0),
        LSTORE_1(opc_lstore_1),
        LSTORE_2(opc_lstore_2),
        LSTORE_3(opc_lstore_3),
        LSTORE_W(opc_lstore_w),
        RET_W(opc_ret_w),
        WIDE(opc_wide),
        LABEL(xxx_label),
        LABELWEAK(xxx_labelweak),

        ;

        private final JynxOp[] jynxOps;

        private ASMTextOps(JynxOp... jops) {
            this.jynxOps = jops;
        }

        @Override
        public JynxOp[] getJynxOps() {
            return jynxOps;
        }

        @Override
        public boolean isExternal() {
            return true;
        }
        
    }
}
