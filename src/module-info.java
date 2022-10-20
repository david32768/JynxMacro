module com.github.david32768.jynxmacro {
	requires com.github.david32768.jynx;
	provides jynx2asm.ops.MacroLib with jynxmacro.WasmMacroLib, jynxmacro.StructuredMacroLib;
}
