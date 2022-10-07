package jynxmacro;

import java.lang.invoke.MethodHandle;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.function.BinaryOperator;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

import static jynx2asm.ops.ExtendedOps.*;
import static jynx2asm.ops.JavaCallOps.*;
import static jynx2asm.ops.JvmOp.*;
import static jynx2asm.ops.LineOps.*;
import static jynx2asm.ops.SelectOps.*;
import static jynxmacro.StructuredMacroLib.StructuredOps.*;

import jynx2asm.ops.DynamicOp;
import jynx2asm.ops.JynxOp;
import jynx2asm.ops.MacroLib;
import jynx2asm.ops.MacroOp;
import jynx2asm.ops.MacroOption;
import jynx2asm.ops.SelectOps;

public class WasmMacroLib  extends MacroLib {

    private final static String NAME = "wasm32MVP";

    private final static String WASM_STORAGE = "wasmrun/Storage";
    private final static String WASM_STORAGE_L = nameL(WASM_STORAGE);
    private final static String WASM_HELPER = "wasmrun/Helper";
    private final static String WASM_TABLE = "wasmrun/Table";
    private final static String WASM_TABLE_L =  nameL(WASM_TABLE);
    private final static String MH_ARRAY_L = nameL(MethodHandle[].class);
    private final static String MH_L = nameL(MethodHandle.class);
    private final static String TABLE_PREFIX = "__Table__";
    private final static String MEMORY = "__Memory__0";
    private final static String GS_MEMORY = "GS:" + MEMORY+ "()" + WASM_STORAGE_L;

    private static String name(Class<?> klass) {
        assert !klass.isArray();
        return klass.getName().replace(".","/");
    }
    
    private static String nameL(String classname) {
        return 'L' + classname + ';'; 
    }

    private static String nameL(Class<?> klass) {
        if (klass.isArray()) {
            return klass.getName().replace(".","/");
        } else {
            assert !klass.isPrimitive();
            return nameL(name(klass));
        }
    }

    @Override
    public Stream<MacroOp> streamExternal() {
        return Arrays.stream(WasmOps.values())
            .filter(WasmOps::isExternal)
            .map(m->(MacroOp)m);
    }

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public UnaryOperator<String> parmTranslator() {
        return WasmMacroLib::translateParm;
    }

    @Override
    public BinaryOperator<String> ownerTranslator() {
        return WasmMacroLib::translateOwner;
    }

    @Override
    public EnumSet<MacroOption> getOptions() {
        return EnumSet.of(MacroOption.STRUCTURED_LABELS,MacroOption.UNSIGNED_LONG,MacroOption.INDENT);
    }

    public static String translateParm(String str) {
        if (!str.contains("->")) {
            return str;
        }
        assert !str.contains(";") && str.contains("->");
        str = str.replace("->V00", "V");
        str = str.replace("->()", "V");
        str = str.replace("->", "");
        str = str.replace(",","");
        str = str.replace("I32", "I");
        str = str.replace("I64", "J");
        str = str.replace("F32", "F");
        str = str.replace("F64", "D");
        return str;
    }
    
    public static String translateOwner(String classname, String str) {
        if (str == null || str.equals(".")) {
            return classname;
        }
        if (str.indexOf('/') == str.lastIndexOf('/') && str.startsWith("wasi")) {
            return "wasi/trampoline/" + str.substring(0,1).toUpperCase() + str.substring(1);
        }
        return str;
    }
    
    private static DynamicOp dynStorage(String method, String parms) {
        return DynamicOp.withBootParms(method, parms, WASM_STORAGE,
            "storageBootstrap",MH_L + "I",GS_MEMORY);
    }

    private enum WasmOps implements MacroOp {

        aux_ilt(asm_iconst_m1,asm_iushr), // shifts right 31 bits i.e. sign bit to one bit
        // boolean result; top of stack must be one of (-1, 0, 1)
        aux_ine_m101(asm_iconst_1,asm_iand),
        aux_ieq_m101(aux_ine_m101,asm_iconst_1,asm_ixor),
        aux_ilt_m101(aux_ilt),
        aux_ile_m101(asm_iconst_1,asm_isub,aux_ilt),
        aux_igt_m101(asm_iconst_1,asm_iadd,asm_iconst_1,asm_iushr),
        aux_ige_m101(asm_ineg,aux_ile_m101),
        // boolean result 
        aux_ine(ext_isignum,aux_ine_m101),
        aux_ieq(ext_isignum,aux_ieq_m101),
        aux_ile(asm_i2l,asm_lconst_1,asm_lcmp,aux_ilt),
        aux_igt(asm_i2l,asm_lneg,asm_iconst_m1,asm_lushr,asm_l2i),
        aux_ige(aux_ilt,asm_iconst_1,asm_ixor),

        aux_popn(SelectOps.of12(asm_pop,asm_pop2)),
        aux_dupn(SelectOps.of12(asm_dup, asm_dup2)),
        aux_dupn_xn(SelectOps.of12(asm_dup_x1,asm_dup2_x2)),
        aux_swapnn(aux_dupn_xn, aux_popn),

        aux_fstd_NaN(insert(WASM_HELPER,"arithmeticFloatNaN","(F)F"),asm_invokestatic),
        aux_dstd_NaN(insert(WASM_HELPER,"arithmeticDoubleNaN","(D)D"),asm_invokestatic),
        
        aux_newtable(insert(WASM_TABLE,"getInstance","()" + WASM_TABLE_L),asm_invokestatic),
        aux_newmem(insert(WASM_STORAGE,"getInstance","(II)" + WASM_STORAGE_L),asm_invokestatic),

        // control operators
        UNREACHABLE(insert(WASM_HELPER ,"unreachable","()Ljava/lang/AssertionError;"),asm_invokestatic,asm_athrow),
        BLOCK(ext_BLOCK),
        LOOP(ext_LOOP),
        IF(ext_IF_NEZ),
        ELSE(ext_ELSE),
        BR_IF(ext_BR_IFNEZ),
        BR_TABLE(asm_tableswitch),
        RETURN(ext_RETURN),
        END(ext_END),
        BR(ext_BR),
        CALL(asm_invokestatic),
        CALL_INDIRECT(
                prepend(TABLE_PREFIX),
                insert(WASM_TABLE_L),
                tok_swap,
                asm_getstatic,
                asm_swap,
                insert(WASM_TABLE, "getMH", "(I)" + MH_L),
                asm_invokevirtual,
                translateDesc(),
                replace("I)", MH_L + ")"),
                DynamicOp.of("invokeExact", null, WASM_TABLE,"callIndirectBootstrapMH")),
        // parametric operators
        NOP(asm_nop),
        DROP(aux_popn),
        SELECT(mac_label, asm_ifne, aux_swapnn, mac_label, xxx_label,  aux_popn),
        UNWIND(DynamicOp.of("unwind", null, WASM_HELPER, "unwindBootstrap")),
        // variable access
        LOCAL_GET(xxx_xload_rel),
        LOCAL_SET(xxx_xstore_rel),
        LOCAL_TEE(aux_dupn,xxx_xstore_rel), // TEE pops and pushes value on stack
        I32_GLOBAL_GET(insert("I"),tok_swap,asm_getstatic),
        I64_GLOBAL_GET(insert("J"),tok_swap,asm_getstatic),
        F32_GLOBAL_GET(insert("F"),tok_swap,asm_getstatic),
        F64_GLOBAL_GET(insert("D"),tok_swap,asm_getstatic),
        I32_GLOBAL_SET(insert("I"),tok_swap,asm_putstatic),
        I64_GLOBAL_SET(insert("J"),tok_swap,asm_putstatic),
        F32_GLOBAL_SET(insert("F"),tok_swap,asm_putstatic),
        F64_GLOBAL_SET(insert("D"),tok_swap,asm_putstatic),

        // memory - args are alignment and offset(alignment currently ignored)
        I32_LOAD(tok_skip,WasmMacroLib.dynStorage("loadInt", "(I)I")),
        I64_LOAD(tok_skip,WasmMacroLib.dynStorage("loadLong", "(I)J")),
        F32_LOAD(tok_skip,WasmMacroLib.dynStorage("loadFloat", "(I)F")),
        F64_LOAD(tok_skip,WasmMacroLib.dynStorage("loadDouble", "(I)D")),

        I32_LOAD8_S(tok_skip,WasmMacroLib.dynStorage("loadByte", "(I)I")),
        I32_LOAD8_U(tok_skip,WasmMacroLib.dynStorage("loadUByte", "(I)I")),
        I32_LOAD16_S(tok_skip,WasmMacroLib.dynStorage("loadShort", "(I)I")),
        I32_LOAD16_U(tok_skip,WasmMacroLib.dynStorage("loadUShort", "(I)I")),

        I64_LOAD8_S(tok_skip,WasmMacroLib.dynStorage("loadByte2Long", "(I)J")),
        I64_LOAD8_U(tok_skip,WasmMacroLib.dynStorage("loadUByte2Long", "(I)J")),
        I64_LOAD16_S(tok_skip,WasmMacroLib.dynStorage("loadShort2Long", "(I)J")),
        I64_LOAD16_U(tok_skip,WasmMacroLib.dynStorage("loadUShort2Long", "(I)J")),
        I64_LOAD32_S(tok_skip,WasmMacroLib.dynStorage("loadInt2Long", "(I)J")),
        I64_LOAD32_U(tok_skip,WasmMacroLib.dynStorage("loadUInt2Long", "(I)J")),

        I32_STORE(tok_skip,WasmMacroLib.dynStorage("storeInt", "(II)V")),
        I64_STORE(tok_skip,WasmMacroLib.dynStorage("storeLong", "(IJ)V")),
        F32_STORE(tok_skip,WasmMacroLib.dynStorage("storeFloat", "(IF)V")),
        F64_STORE(tok_skip,WasmMacroLib.dynStorage("storeDouble", "(ID)V")),

        I32_STORE8(tok_skip,WasmMacroLib.dynStorage("storeByte", "(II)V")),
        I32_STORE16(tok_skip,WasmMacroLib.dynStorage("storeShort", "(II)V")),

        I64_STORE8(tok_skip,WasmMacroLib.dynStorage("storeLong2Byte", "(IJ)V")),
        I64_STORE16(tok_skip,WasmMacroLib.dynStorage("storeLong2Short", "(IJ)V")),
        I64_STORE32(tok_skip,WasmMacroLib.dynStorage("storeLong2Int", "(IJ)V")),


        // memory number is passed as 'disp' parameter
        MEMORY_SIZE(WasmMacroLib.dynStorage("currentPages", "()I")),
        MEMORY_GROW(WasmMacroLib.dynStorage("grow", "(I)I")),

        // constants
        I32_CONST(opc_ildc),
        I64_CONST(opc_lldc),
        F32_CONST(opc_fldc),
        F64_CONST(opc_dldc),

        // comparison operators
            // call static method would be length 3
            // jump version may be shorter or equal in some cases
        I32_EQZ(asm_i2l,asm_lconst_0, asm_lcmp, aux_ieq_m101),
        I32_EQ(inv_icompare, aux_ieq),
        I32_NE(inv_icompare, aux_ine),
        I32_LT_S(inv_icompare, aux_ilt),
        I32_LT_U(inv_iucompare, aux_ilt),
        I32_GT_S(inv_icompare, aux_igt),
        I32_GT_U(inv_iucompare, aux_igt),
        I32_LE_S(inv_icompare, aux_ile),
        I32_LE_U(inv_iucompare, aux_ile),
        I32_GE_S(inv_icompare, aux_ige),
        I32_GE_U(inv_iucompare, aux_ige),

        I64_EQZ(asm_lconst_0, asm_lcmp, aux_ieq_m101),
        I64_EQ(asm_lcmp, aux_ieq_m101),
        I64_NE(asm_lcmp, aux_ine_m101),
        I64_LT_S(asm_lcmp, aux_ilt_m101),
        I64_LT_U(inv_lucompare, aux_ilt),
        I64_GT_S(asm_lcmp, aux_igt_m101),
        I64_GT_U(inv_lucompare, aux_igt),
        I64_LE_S(asm_lcmp, aux_ile_m101),
        I64_LE_U(inv_lucompare, aux_ile),
        I64_GE_S(asm_lcmp, aux_ige_m101),
        I64_GE_U(inv_lucompare, aux_ige),

        F32_EQ(asm_fcmpl, aux_ieq_m101),
        F32_NE(asm_fcmpl, aux_ine_m101),
        F32_LT(asm_fcmpg, aux_ilt_m101),
        F32_GT(asm_fcmpl, aux_igt_m101),
        F32_LE(asm_fcmpg, aux_ile_m101),
        F32_GE(asm_fcmpl, aux_ige_m101),

        F64_EQ(asm_dcmpl, aux_ieq_m101),
        F64_NE(asm_dcmpl, aux_ine_m101),
        F64_LT(asm_dcmpg, aux_ilt_m101),
        F64_GT(asm_dcmpl, aux_igt_m101),
        F64_LE(asm_dcmpg, aux_ile_m101),
        F64_GE(asm_dcmpl, aux_ige_m101),

        // numeric operators
        I32_CLZ(inv_iclz),
        I32_CTZ(inv_ictz),
        I32_POPCNT(inv_ipopct),

        I32_ADD(asm_iadd),
        I32_SUB(asm_isub),
        I32_MUL(asm_imul),
        I32_DIV_S(insert(WASM_HELPER,"intDiv","(II)I"),asm_invokestatic),
        I32_DIV_U(inv_iudiv),
        I32_REM_S(asm_irem),
        I32_REM_U(inv_iurem),

        I32_AND(asm_iand),
        I32_OR(asm_ior),
        I32_XOR(asm_ixor),

        I32_SHL(asm_ishl),
        I32_SHR_S(asm_ishr),
        I32_SHR_U(asm_iushr),
        I32_ROTL(inv_irotl),
        I32_ROTR(inv_irotr),

        I64_CLZ(inv_lclz, asm_i2l),
        I64_CTZ(inv_lctz, asm_i2l),
        I64_POPCNT(inv_lpopct, asm_i2l),

        I64_ADD(asm_ladd),
        I64_SUB(asm_lsub),
        I64_MUL(asm_lmul),
        I64_DIV_S(insert(WASM_HELPER,"longDiv","(JJ)J"),asm_invokestatic),
        I64_DIV_U(inv_ludiv),
        I64_REM_S(asm_lrem),
        I64_REM_U(inv_lurem),

        I64_AND(asm_land),
        I64_OR(asm_lor),
        I64_XOR(asm_lxor),

        I64_SHL(asm_l2i, asm_lshl),
        I64_SHR_S(asm_l2i, asm_lshr),
        I64_SHR_U(asm_l2i, asm_lushr),
        I64_ROTL(asm_l2i, inv_lrotl),
        I64_ROTR(asm_l2i, inv_lrotr),

        F32_ABS(inv_fabs),
        F32_NEG(asm_fneg),
        F32_CEIL(asm_f2d, inv_dceil, asm_d2f),
        F32_FLOOR(asm_f2d, inv_dfloor, asm_d2f),
        F32_TRUNC(insert(WASM_HELPER ,"truncFloat","(F)F"), asm_invokestatic),
        F32_NEAREST(asm_f2d, inv_drint, asm_d2f),
        F32_SQRT(asm_f2d, inv_dsqrt, asm_d2f),

        F32_ADD(asm_fadd),
        F32_SUB(asm_fsub),
        F32_MUL(asm_fmul),
        F32_DIV(asm_fdiv),
        F32_MIN(inv_fmin,aux_fstd_NaN),
        F32_MAX(inv_fmax,aux_fstd_NaN),
        F32_COPYSIGN(inv_fcopysign),

        F64_ABS(inv_dabs),
        F64_NEG(asm_dneg),
        F64_CEIL(inv_dceil,aux_dstd_NaN),
        F64_FLOOR(inv_dfloor,aux_dstd_NaN),
        F64_TRUNC(insert(WASM_HELPER ,"truncDouble","(D)D"),asm_invokestatic),
        F64_NEAREST(inv_drint),
        F64_SQRT(inv_dsqrt),

        F64_ADD(asm_dadd),
        F64_SUB(asm_dsub),
        F64_MUL(asm_dmul),
        F64_DIV(asm_ddiv),
        F64_MIN(inv_dmin,aux_dstd_NaN),
        F64_MAX(inv_dmax,aux_dstd_NaN),
        F64_COPYSIGN(inv_dcopysign),

        // conversions
        I32_WRAP_I64(asm_l2i),
        I32_TRUNC_S_F32(insert(WASM_HELPER,"float2int","(F)I"),asm_invokestatic),
        I32_TRUNC_U_F32(insert(WASM_HELPER ,"float2unsignedInt","(F)I"),asm_invokestatic),
        I32_TRUNC_S_F64(insert(WASM_HELPER,"double2int","(D)I"),asm_invokestatic),
        I32_TRUNC_U_F64(insert(WASM_HELPER ,"double2unsignedInt","(D)I"),asm_invokestatic),

        I64_EXTEND_S_I32(asm_i2l),
        I64_EXTEND_U_I32(inv_iu2l),
        I64_TRUNC_S_F32(insert(WASM_HELPER,"float2long","(F)J"),asm_invokestatic),
        I64_TRUNC_U_F32(insert(WASM_HELPER ,"float2unsignedLong","(F)J"),asm_invokestatic),
        I64_TRUNC_S_F64(insert(WASM_HELPER,"double2long","(D)J"),asm_invokestatic),
        I64_TRUNC_U_F64(insert(WASM_HELPER ,"double2unsignedLong","(D)J"),asm_invokestatic),

        F32_CONVERT_S_I32(asm_i2f),
        F32_CONVERT_U_I32(inv_iu2l,asm_l2f),
        F32_CONVERT_S_I64(asm_l2f),
        F32_CONVERT_U_I64(insert(WASM_HELPER ,"unsignedLong2float","(J)F"),asm_invokestatic),
        F32_DEMOTE_F64(asm_d2f),

        F64_CONVERT_S_I32(asm_i2d),
        F64_CONVERT_U_I32(inv_iu2l,asm_l2d),
        F64_CONVERT_S_I64(asm_l2d),
        F64_CONVERT_U_I64(insert(WASM_HELPER ,"unsignedLong2double","(J)D"),asm_invokestatic),
        F64_PROMOTE_F32(asm_f2d),
        // reinterpret
        I32_REINTERPRET_F32(inv_fasi),
        I64_REINTERPRET_F64(inv_dasl),
        F32_REINTERPRET_I32(inv_iasf),
        F64_REINTERPRET_I64(inv_lasd),

        // optimizations
        I32_IFEQZ(ext_IF_EQZ),
        
        I32_IFEQ(ext_IF_ICMPEQ),
        I32_IFNE(ext_IF_ICMPNE),
        I32_IFLT_S(ext_IF_ICMPLT),
        I32_IFLT_U(ext_IF_IUCMPLT),
        I32_IFGT_S(ext_IF_ICMPGT),
        I32_IFGT_U(ext_IF_IUCMPGT),
        I32_IFLE_S(ext_IF_ICMPLE),
        I32_IFLE_U(ext_IF_IUCMPLE),
        I32_IFGE_S(ext_IF_ICMPGE),
        I32_IFGE_U(ext_IF_IUCMPGE),

        I64_IFEQZ(asm_lconst_0, ext_IF_LCMPEQ),
        
        I64_IFEQ(ext_IF_LCMPEQ),
        I64_IFNE(ext_IF_LCMPNE),
        I64_IFLT_S(ext_IF_LCMPLT),
        I64_IFLT_U(ext_IF_LUCMPLT),
        I64_IFGT_S(ext_IF_LCMPGT),
        I64_IFGT_U(ext_IF_LUCMPGT),
        I64_IFLE_S(ext_IF_LCMPLE),
        I64_IFLE_U(ext_IF_LUCMPLE),
        I64_IFGE_S(ext_IF_LCMPGE),
        I64_IFGE_U(ext_IF_LUCMPGE),

        F32_IFEQ(ext_IF_FCMPEQ),
        F32_IFNE(ext_IF_FCMPNE),
        F32_IFLT(ext_IF_FCMPLT),
        F32_IFGT(ext_IF_FCMPGT),
        F32_IFLE(ext_IF_FCMPLE),
        F32_IFGE(ext_IF_FCMPGE),

        F64_IFEQ(ext_IF_DCMPEQ),
        F64_IFNE(ext_IF_DCMPNE),
        F64_IFLT(ext_IF_DCMPLT),
        F64_IFGT(ext_IF_DCMPGT),
        F64_IFLE(ext_IF_DCMPLE),
        F64_IFGE(ext_IF_DCMPGE),

        I32_BR_IFEQZ(ext_BR_IFEQZ),
        
        I32_BR_IFEQ(ext_BR_IF_ICMPEQ),
        I32_BR_IFNE(ext_BR_IF_ICMPNE),
        I32_BR_IFLT_S(ext_BR_IF_ICMPLT),
        I32_BR_IFLT_U(ext_BR_IF_IUCMPLT),
        I32_BR_IFGT_S(ext_BR_IF_ICMPGT),
        I32_BR_IFGT_U(ext_BR_IF_IUCMPGT),
        I32_BR_IFLE_S(ext_BR_IF_ICMPLE),
        I32_BR_IFLE_U(ext_BR_IF_IUCMPLE),
        I32_BR_IFGE_S(ext_BR_IF_ICMPGE),
        I32_BR_IFGE_U(ext_BR_IF_IUCMPGE),

        I64_BR_IFEQZ(asm_lconst_0, ext_BR_IF_LCMPEQ),
        
        I64_BR_IFEQ(ext_BR_IF_LCMPEQ),
        I64_BR_IFNE(ext_BR_IF_LCMPNE),
        I64_BR_IFLT_S(ext_BR_IF_LCMPLT),
        I64_BR_IFLT_U(ext_BR_IF_LUCMPLT),
        I64_BR_IFGT_S(ext_BR_IF_LCMPGT),
        I64_BR_IFGT_U(ext_BR_IF_LUCMPGT),
        I64_BR_IFLE_S(ext_BR_IF_LCMPLE),
        I64_BR_IFLE_U(ext_BR_IF_LUCMPLE),
        I64_BR_IFGE_S(ext_BR_IF_LCMPGE),
        I64_BR_IFGE_U(ext_BR_IF_LUCMPGE),

        F32_BR_IFEQ(ext_BR_IF_FCMPEQ),
        F32_BR_IFNE(ext_BR_IF_FCMPNE),
        F32_BR_IFLT(ext_BR_IF_FCMPLT),
        F32_BR_IFGT(ext_BR_IF_FCMPGT),
        F32_BR_IFLE(ext_BR_IF_FCMPLE),
        F32_BR_IFGE(ext_BR_IF_FCMPGE),

        F64_BR_IFEQ(ext_BR_IF_DCMPEQ),
        F64_BR_IFNE(ext_BR_IF_DCMPNE),
        F64_BR_IFLT(ext_BR_IF_DCMPLT),
        F64_BR_IFGT(ext_BR_IF_DCMPGT),
        F64_BR_IFLE(ext_BR_IF_DCMPLE),
        F64_BR_IFGE(ext_BR_IF_DCMPGE),

        I32_SELECTEQZ(mac_label, asm_ifeq, aux_swapnn, mac_label, xxx_label,  aux_popn),
        
        I32_SELECTEQ(mac_label, asm_if_icmpeq, aux_swapnn, mac_label, xxx_label,  aux_popn),
        I32_SELECTNE(mac_label, asm_if_icmpne, aux_swapnn, mac_label, xxx_label,  aux_popn),
        I32_SELECTLT_S(mac_label, asm_if_icmplt, aux_swapnn, mac_label, xxx_label,  aux_popn),
        I32_SELECTLT_U(mac_label, ext_if_iucmplt, aux_swapnn, mac_label, xxx_label,  aux_popn),
        I32_SELECTGT_S(mac_label, asm_if_icmpgt, aux_swapnn, mac_label, xxx_label,  aux_popn),
        I32_SELECTGT_U(mac_label, ext_if_iucmpgt, aux_swapnn, mac_label, xxx_label,  aux_popn),
        I32_SELECTLE_S(mac_label, asm_if_icmple, aux_swapnn, mac_label, xxx_label,  aux_popn),
        I32_SELECTLE_U(mac_label, ext_if_iucmple, aux_swapnn, mac_label, xxx_label,  aux_popn),
        I32_SELECTGE_S(mac_label, asm_if_icmpge, aux_swapnn, mac_label, xxx_label,  aux_popn),
        I32_SELECTGE_U(mac_label, ext_if_iucmpge, aux_swapnn, mac_label, xxx_label,  aux_popn),

        I64_SELECTEQZ(asm_lconst_0,mac_label, ext_if_lcmpeq, aux_swapnn, mac_label, xxx_label,  aux_popn),
        
        I64_SELECTEQ(mac_label, ext_if_lcmpeq, aux_swapnn, mac_label, xxx_label,  aux_popn),
        I64_SELECTNE(mac_label, ext_if_lcmpne, aux_swapnn, mac_label, xxx_label,  aux_popn),
        I64_SELECTLT_S(mac_label, ext_if_lcmplt, aux_swapnn, mac_label, xxx_label,  aux_popn),
        I64_SELECTLT_U(mac_label, ext_if_lucmplt, aux_swapnn, mac_label, xxx_label,  aux_popn),
        I64_SELECTGT_S(mac_label, ext_if_lcmpgt, aux_swapnn, mac_label, xxx_label,  aux_popn),
        I64_SELECTGT_U(mac_label, ext_if_lucmpgt, aux_swapnn, mac_label, xxx_label,  aux_popn),
        I64_SELECTLE_S(mac_label, ext_if_lcmple, aux_swapnn, mac_label, xxx_label,  aux_popn),
        I64_SELECTLE_U(mac_label, ext_if_lucmple, aux_swapnn, mac_label, xxx_label,  aux_popn),
        I64_SELECTGE_S(mac_label, ext_if_lcmpge, aux_swapnn, mac_label, xxx_label,  aux_popn),
        I64_SELECTGE_U(mac_label, ext_if_lucmpge, aux_swapnn, mac_label, xxx_label,  aux_popn),

        F32_SELECTEQ(mac_label, ext_if_fcmpeq, aux_swapnn, mac_label, xxx_label,  aux_popn),
        F32_SELECTNE(mac_label, ext_if_fcmpne, aux_swapnn, mac_label, xxx_label,  aux_popn),
        F32_SELECTLT(mac_label, ext_if_fcmplt, aux_swapnn, mac_label, xxx_label,  aux_popn),
        F32_SELECTGT(mac_label, ext_if_fcmpgt, aux_swapnn, mac_label, xxx_label,  aux_popn),
        F32_SELECTLE(mac_label, ext_if_fcmple, aux_swapnn, mac_label, xxx_label,  aux_popn),
        F32_SELECTGE(mac_label, ext_if_fcmpge, aux_swapnn, mac_label, xxx_label,  aux_popn),

        F64_SELECTEQ(mac_label, ext_if_dcmpeq, aux_swapnn, mac_label, xxx_label,  aux_popn),
        F64_SELECTNE(mac_label, ext_if_dcmpne, aux_swapnn, mac_label, xxx_label,  aux_popn),
        F64_SELECTLT(mac_label, ext_if_dcmplt, aux_swapnn, mac_label, xxx_label,  aux_popn),
        F64_SELECTGT(mac_label, ext_if_dcmpgt, aux_swapnn, mac_label, xxx_label,  aux_popn),
        F64_SELECTLE(mac_label, ext_if_dcmple, aux_swapnn, mac_label, xxx_label,  aux_popn),
        F64_SELECTGE(mac_label, ext_if_dcmpge, aux_swapnn, mac_label, xxx_label,  aux_popn),

        // init functions
        MEMORY_NEW(asm_ldc,asm_ldc,aux_newmem),
        MEMORY_CHECK(asm_ldc,asm_ldc,WasmMacroLib.dynStorage("checkInstance", "(II)" + WASM_STORAGE_L)),
        ADD_SEGMENT(asm_ldc,tok_swap,asm_ldc,WasmMacroLib.dynStorage("putBase64String", "(ILjava/lang/String;)V")),
        COPY_MEMORY(insert(WASM_STORAGE_L),
                tok_swap,
                asm_getstatic,
                insert(WASM_STORAGE_L),
                tok_swap,
                asm_putstatic),
        MEMORY_GLOBAL_GET(insert(WASM_STORAGE_L),tok_swap,asm_getstatic),
        MEMORY_GLOBAL_SET(insert(WASM_STORAGE_L),tok_swap,asm_putstatic),

        TABLE_NEW(aux_newtable),
        COPY_TABLE(insert(WASM_TABLE_L),
                tok_swap,
                asm_getstatic,
                insert(WASM_TABLE_L),
                tok_swap,
                asm_putstatic),
        ADD_ENTRY(
                opc_ildc,
                DynamicOp.withBootParms("mharray", "()" + MH_ARRAY_L,
                    WASM_TABLE, "constantArrayBootstrap",MH_ARRAY_L),
                insert(WASM_TABLE,"add","(I" + MH_ARRAY_L + ")" + WASM_TABLE_L),
                asm_invokevirtual
        ),
        TABLE_TEE(asm_dup,prepend(TABLE_PREFIX),insert(WASM_TABLE_L),tok_swap,asm_putstatic),

            ;

        private final JynxOp[] jynxOps;

        private WasmOps(JynxOp... jops) {
            this.jynxOps = jops;
        }

        @Override
        public boolean reduceIndentBefore() {
            switch(this) {
                case ELSE:
                case END:
                    return true;
                default:
                    return false;
            }
        }

        @Override
        public boolean increaseIndentAfter() {
            switch(this) {
                case BLOCK:
                case LOOP:
                case ELSE:
                    return true;
                case BR_IF:
                    return false;
                default:
                    int index = name().indexOf('_');
                    return  name().substring(index + 1).startsWith("IF");
            }
        }

        @Override
        public JynxOp[] getJynxOps() {
            return jynxOps;
        }

        @Override
        public boolean isExternal() {
            return Character.isUpperCase(name().codePointAt(0));
        }

    }
}