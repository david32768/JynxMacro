package jynxmacro;

import java.util.Arrays;
import java.util.stream.Stream;

import jynx2asm.ops.JynxOp;
import jynx2asm.ops.MacroOp;

public class WasiMacroLib extends WasmMacroLib {

    private final static String NAME = "wasi";

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public Stream<MacroOp> streamExternal() {
        Stream<MacroOp> wasm = super.streamExternal();
        Stream<MacroOp> wasi = Arrays.stream(WasiMacroLib.WasiOps.values())
            .filter(WasiMacroLib.WasiOps::isExternal)
            .map(m->(MacroOp)m);
        return Stream.concat(wasm, wasi);
    }

    private enum WasiOps implements MacroOp {

        STRING_STORE_SIZED(WasmMacroLib.dynLoadStore("putSizedString", "(ILjava/lang/String;)V")),
        STRING_STORE_C(WasmMacroLib.dynLoadStore("putCString", "(ILjava/lang/String;)V")),
        STRING_STORE(WasmMacroLib.dynLoadStore("putString", "(ILjava/lang/String;)V")),
        
        ;
            
        private final JynxOp[] jynxOps;

        private WasiOps(JynxOp... jops) {
            this.jynxOps = jops;
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
