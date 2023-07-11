module com.github.david32768.jynxmacro {
	requires com.github.david32768.jynx;
	provides jynx2asm.ops.MacroLib with
		jynxmacro.WasmMacroLib,
		jynxmacro.StructuredMacroLib,
		jynxmacro.ASMTextMacroLib,
		jynxmacro.ExtensionMacroLib;
}
/*
.version V11
.source module-info.java
.define_module
.module com.github.david32768.jynxmacro 0.1
.requires mandated java.base 11
.requires com.github.david32768.jynx 0.20
.provides jynx2asm/ops/MacroLib with .array
  jynxmacro/WasmMacroLib
  jynxmacro/StructuredMacroLib
  jynxmacro/ASMTextMacroLib
  jynxmacro/ExtensionMacroLib
.end_array
.packages .array
  jynxmacro
.end_array
; */
