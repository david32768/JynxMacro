# JynxMacro

this contains some "macro" libraries for Jynx.

These are:

*	.macrolib extension ; (ExtensionMacroLib)
*	.macrolib ASMTextOps ; (ASMTextMacroLib)
*	.macrolib structured ; (StructuredMacroLib)
*	.macrolib wasm32MVP ; (WasmMacroLib)

## extension

Some "useful" extensions

*	alias ops
```
; e.g.
ildc 3 : alias for iconst_3
ildc 240 ; alias for bipush 240
ildc -32767 ; alias for sipush -32767
ildc 32768 ; alias for ldc 32768
; also lldc, fldc and dldc
```
*	extended ops
```
; e.g.
if_fcmpge label
	; fcmpl
	; ifge labal
swap2
	; dup2_x2
	; pop2
```
*	call common java methods
```
; e.g.
iabs ; invokestatic java/lang/Math/iabs(I)I
```

## .macrolib ASMTextOps

Ops for Jynxifier

## .macrolib structured

structured "wasm" ops
e.g BEGIN, END

## .macrolib wasm32MVP

ops for Wasm MVP
