module com.github.david32768.jynxmacro {
	requires com.github.david32768.JynxFor;
	provides com.github.david32768.jynxfor.ops.MacroLib with
		jynxmacro.WasmMacroLib,
		jynxmacro.StructuredMacroLib,
		jynxmacro.ASMTextMacroLib,
		jynxmacro.ExtensionMacroLib;
}
