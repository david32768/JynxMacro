package jynxmacro;

import java.lang.invoke.MethodHandle;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import static jynx2asm.ops.AdjustToken.*;
import static jynx2asm.ops.ExtendedOps.*;
import static jynx2asm.ops.JavaCallOps.*;
import static jynx2asm.ops.JvmOp.*;
import static jynx2asm.ops.LineOps.*;
import static jynx2asm.ops.SelectOps.*;
import static jynxmacro.StructuredMacroLib.StructuredOps.*;

import jynx2asm.ops.AdjustToken;
import jynx2asm.ops.CallOp;
import jynx2asm.ops.DynamicOp;
import jynx2asm.ops.IndentType;
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
    private final static String MH_ARRAY_L = CallOp.parmName(MethodHandle[].class);
    private final static String MH_L = CallOp.parmName(MethodHandle.class);
    private final static String TABLE_PREFIX = "__Table__";
    private final static String MEMORY_PREFIX = "__Memory__";
    private final static String GS_MEMORY_PREFIX = "GS:" + MEMORY_PREFIX;
    private final static String GS_MEMORY_POSTFIX = "()" + WASM_STORAGE_L;

    private static String nameL(String classname) {
        return 'L' + classname + ';'; 
    }

    @Override
    public Map<String, JynxOp> getMacros() {
        Map<String,JynxOp> map = new HashMap<>();
        Stream.of(WasmOps.values())
                .filter(m -> Character.isUpperCase(m.name().codePointAt(0)))
                .forEach(m -> map.put(m.toString(),m));
        return map;
    }
        
    @Override
    public String name() {
        return NAME;
    }

    @Override
    public EnumSet<MacroOption> getOptions() {
        return EnumSet.of(MacroOption.STRUCTURED_LABELS, MacroOption.INDENT, MacroOption.UNSIGNED_LONG);
    }

    private static final Map<String, String> PARM_MAP;
    
    static {
        PARM_MAP = new HashMap<>();
        PARM_MAP.put("I32", "I");
        PARM_MAP.put("I64", "J");
        PARM_MAP.put("F32", "F");
        PARM_MAP.put("F64", "D");
    }

    @Override
    public Map<String, String> parmTranslations() {
        return PARM_MAP;
    }
    
    private static final Map<String, String> OWNER_MAP;
    private static final String WASI = "Wasi_snapshot_preview1"; 
    private static final String WASI_PACKAGE = "wasi/trampoline";
    private static final String WASI_OWNER = WASI_PACKAGE + "/" + WASI;
    private static final String JYNX_WASI = "JYNX_WASI";
    
    static {
        OWNER_MAP = new HashMap<>();
        OWNER_MAP.put(WASI, WASI_OWNER);
        OWNER_MAP.put(JYNX_WASI, WASI_OWNER);
    }

    @Override
    public Map<String, String> ownerTranslations() {
        return OWNER_MAP;
    }
    
    private static JynxOp callHelper(String methodname, String desc) {
        return CallOp.of(WASM_HELPER, methodname, desc);
    }
    
    private static DynamicOp dynStorage(String method, String parms) {
        return DynamicOp.withBootParms(method, parms, WASM_STORAGE,
            "storageBootstrap",MH_L);
    }

    protected static DynamicOp dynLoadStore(String method, String parms) {
        return DynamicOp.withBootParms(method, parms, WASM_STORAGE,
            "loadStoreBootstrap",MH_L + "I");
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

        aux_fstd_NaN(callHelper("arithmeticFloatNaN","(F)F")),
        aux_dstd_NaN(callHelper("arithmeticDoubleNaN","(D)D")),
        
        aux_newtable(CallOp.of(WASM_TABLE,"getInstance","()" + WASM_TABLE_L)),
        aux_newmem(CallOp.of(WASM_STORAGE,"getInstance","(II)" + WASM_STORAGE_L)),
        aux_mem(AdjustToken.surround(GS_MEMORY_PREFIX, GS_MEMORY_POSTFIX)),
        aux_addbase0(insert("+0"),tok_swap),

        // init functions for initialising memory
        MEMORY_NEW(asm_ldc,asm_ldc,aux_newmem),
        MEMORY_GLOBAL_GET(insert(WASM_STORAGE_L),tok_swap,asm_getstatic),
        MEMORY_GLOBAL_SET(insert(WASM_STORAGE_L),tok_swap,asm_putstatic),
        MEMORY_CHECK(asm_ldc,asm_ldc,aux_mem,WasmMacroLib.dynStorage("checkInstance", "(II)V")),
        
        STRING_CONST(asm_ldc),
        BASE64_STORE(aux_mem,WasmMacroLib.dynLoadStore("putBase64String",
                CallOp.descFrom(void.class, int.class,String.class))),
        
        // init functions for initialising table
        TABLE_NEW(aux_newtable),
        TABLE_GLOBAL_GET(insert(WASM_TABLE_L),tok_swap,asm_getstatic),
        TABLE_GLOBAL_SET(insert(WASM_TABLE_L),tok_swap,asm_putstatic),
        ADD_ENTRY(
                opc_ildc,
                asm_iadd,
                DynamicOp.withBootParms("mharray", "()" + MH_ARRAY_L,
                    WASM_TABLE, "constantArrayBootstrap",MH_ARRAY_L),
                insertMethod(WASM_TABLE,"add","(I" + MH_ARRAY_L + ")" + WASM_TABLE_L),
                asm_invokevirtual
        ),
        // init local
        I32_LOCAL_INIT(ext_izero),
        I64_LOCAL_INIT(ext_lzero),
        F32_LOCAL_INIT(ext_fzero),
        F64_LOCAL_INIT(ext_dzero),
        // debug functions
        LOG(SelectOps.stackILFDA(inv_ibox, inv_lbox, inv_fbox, inv_dbox, asm_nop),
                asm_ldc,
                callHelper("log",CallOp.descFrom(void.class, Number.class, String.class))),
        
        
        // control operators
        UNREACHABLE(callHelper("unreachable",CallOp.descFrom(AssertionError.class)), asm_athrow),
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
                insertMethod(WASM_TABLE, "getMH", "(I)" + MH_L),
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
        LOCAL_GET(xxx_xload),
        LOCAL_SET(xxx_xstore),
        LOCAL_TEE(aux_dupn,xxx_xstore), // TEE pops and pushes value on stack
        I32_GLOBAL_GET(insert("I"),tok_swap,asm_getstatic),
        I64_GLOBAL_GET(insert("J"),tok_swap,asm_getstatic),
        F32_GLOBAL_GET(insert("F"),tok_swap,asm_getstatic),
        F64_GLOBAL_GET(insert("D"),tok_swap,asm_getstatic),
        I32_GLOBAL_SET(insert("I"),tok_swap,asm_putstatic),
        I64_GLOBAL_SET(insert("J"),tok_swap,asm_putstatic),
        F32_GLOBAL_SET(insert("F"),tok_swap,asm_putstatic),
        F64_GLOBAL_SET(insert("D"),tok_swap,asm_putstatic),

        // memory - boot args are alignment and offset
        I32_LOAD(aux_mem,WasmMacroLib.dynLoadStore("loadInt", "(I)I")),
        I64_LOAD(aux_mem,WasmMacroLib.dynLoadStore("loadLong", "(I)J")),
        F32_LOAD(aux_mem,WasmMacroLib.dynLoadStore("loadFloat", "(I)F")),
        F64_LOAD(aux_mem,WasmMacroLib.dynLoadStore("loadDouble", "(I)D")),

        I32_LOAD8_S(aux_mem,WasmMacroLib.dynLoadStore("loadByte", "(I)I")),
        I32_LOAD8_U(aux_mem,WasmMacroLib.dynLoadStore("loadUByte", "(I)I")),
        I32_LOAD16_S(aux_mem,WasmMacroLib.dynLoadStore("loadShort", "(I)I")),
        I32_LOAD16_U(aux_mem,WasmMacroLib.dynLoadStore("loadUShort", "(I)I")),

        I64_LOAD8_S(aux_mem,WasmMacroLib.dynLoadStore("loadByte2Long", "(I)J")),
        I64_LOAD8_U(aux_mem,WasmMacroLib.dynLoadStore("loadUByte2Long", "(I)J")),
        I64_LOAD16_S(aux_mem,WasmMacroLib.dynLoadStore("loadShort2Long", "(I)J")),
        I64_LOAD16_U(aux_mem,WasmMacroLib.dynLoadStore("loadUShort2Long", "(I)J")),
        I64_LOAD32_S(aux_mem,WasmMacroLib.dynLoadStore("loadInt2Long", "(I)J")),
        I64_LOAD32_U(aux_mem,WasmMacroLib.dynLoadStore("loadUInt2Long", "(I)J")),

        I32_STORE(aux_mem,WasmMacroLib.dynLoadStore("storeInt", "(II)V")),
        I64_STORE(aux_mem,WasmMacroLib.dynLoadStore("storeLong", "(IJ)V")),
        F32_STORE(aux_mem,WasmMacroLib.dynLoadStore("storeFloat", "(IF)V")),
        F64_STORE(aux_mem,WasmMacroLib.dynLoadStore("storeDouble", "(ID)V")),

        I32_STORE8(aux_mem,WasmMacroLib.dynLoadStore("storeByte", "(II)V")),
        I32_STORE16(aux_mem,WasmMacroLib.dynLoadStore("storeShort", "(II)V")),

        I64_STORE8(aux_mem,WasmMacroLib.dynLoadStore("storeLong2Byte", "(IJ)V")),
        I64_STORE16(aux_mem,WasmMacroLib.dynLoadStore("storeLong2Short", "(IJ)V")),
        I64_STORE32(aux_mem,WasmMacroLib.dynLoadStore("storeLong2Int", "(IJ)V")),


        MEMORY_SIZE(aux_mem,WasmMacroLib.dynStorage("currentPages", "()I")),
        MEMORY_GROW(aux_mem,WasmMacroLib.dynStorage("grow", "(I)I")),
        // some bulk memory ops
        MEMORY_FILL(aux_mem,aux_addbase0,WasmMacroLib.dynLoadStore("fill", "(III)V")),
        MEMORY_COPY(tok_swap, // dest src -> src dest ; NB not specified which order: dest src assumed
                    aux_mem,aux_addbase0,WasmMacroLib.dynLoadStore("getByteArray", "(II)[B"),
                    aux_mem,aux_addbase0,WasmMacroLib.dynLoadStore("putByteArray", "(I[B)V")),

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
        I32_DIV_S(callHelper("intDiv","(II)I")),
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
        I64_DIV_S(callHelper("longDiv","(JJ)J")),
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
        F32_TRUNC(callHelper("truncFloat","(F)F")),
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
        F64_TRUNC(callHelper("truncDouble","(D)D")),
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
        I32_TRUNC_S_F32(callHelper("floatToInt","(F)I")),
        I32_TRUNC_U_F32(callHelper("floatToUnsignedInt","(F)I")),
        I32_TRUNC_S_F64(callHelper("doubleToInt","(D)I")),
        I32_TRUNC_U_F64(callHelper("doubleToUnsignedInt","(D)I")),
        I32_TRUNC_SAT_S_F32(asm_f2i),
        I32_TRUNC_SAT_U_F32(callHelper("floatToSatUnsignedInt","(F)I")),
        I32_TRUNC_SAT_S_F64(asm_d2i),
        I32_TRUNC_SAT_U_F64(callHelper("doubleToSatUnsignedInt","(D)I")),

        I64_EXTEND_S_I32(asm_i2l),
        I64_EXTEND_U_I32(inv_iu2l),
        I64_TRUNC_S_F32(callHelper("floatToLong","(F)J")),
        I64_TRUNC_U_F32(callHelper("floatToUnsignedLong","(F)J")),
        I64_TRUNC_S_F64(callHelper("doubleToLong","(D)J")),
        I64_TRUNC_U_F64(callHelper("doubleToUnsignedLong","(D)J")),
        I64_TRUNC_SAT_S_F32(asm_f2l),
        I64_TRUNC_SAT_U_F32(callHelper("floatToSatUnsignedLong","(F)J")),
        I64_TRUNC_SAT_S_F64(asm_d2l),
        I64_TRUNC_SAT_U_F64(callHelper("doubleToSatUnsignedLong","(D)J")),

        F32_CONVERT_S_I32(asm_i2f),
        F32_CONVERT_U_I32(inv_iu2l,asm_l2f),
        F32_CONVERT_S_I64(asm_l2f),
        F32_CONVERT_U_I64(callHelper("unsignedLongToFloat","(J)F")),
        F32_DEMOTE_F64(asm_d2f),

        F64_CONVERT_S_I32(asm_i2d),
        F64_CONVERT_U_I32(inv_iu2l,asm_l2d),
        F64_CONVERT_S_I64(asm_l2d),
        F64_CONVERT_U_I64(callHelper("unsignedLongToDouble","(J)D")),
        F64_PROMOTE_F32(asm_f2d),
        // reinterpret
        I32_REINTERPRET_F32(inv_fasi),
        I64_REINTERPRET_F64(inv_dasl),
        F32_REINTERPRET_I32(inv_iasf),
        F64_REINTERPRET_I64(inv_lasd),
        // sign extension 2.0
        I32_EXTEND8_S(asm_i2b),
        I32_EXTEND16_S(asm_i2s),
        I64_EXTEND8_S(asm_l2i,asm_i2b,asm_i2l),
        I64_EXTEND16_S(asm_l2i,asm_i2s,asm_i2l),
        I64_EXTEND32_S(asm_l2i,asm_i2l),

    
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

        ;

        private final JynxOp[] jynxOps;

        private WasmOps(JynxOp... jops) {
            this.jynxOps = jops;
        }

        @Override
        public IndentType indentType() {
            switch (this) {
                case BLOCK:
                case LOOP:
                    return IndentType.BEGIN;
                case ELSE:
                    return IndentType.ELSE;
                case END:
                    return IndentType.END;
                case BR_IF:
                    return IndentType.NONE;
            }
            int index = name().indexOf('_');
            if (name().substring(index + 1).startsWith("IF")) {
                return IndentType.BEGIN;
            }
            return IndentType.NONE;
        }

        @Override
        public JynxOp[] getJynxOps() {
            return jynxOps;
        }

    }
}
