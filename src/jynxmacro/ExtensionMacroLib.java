package jynxmacro;

import java.util.HashMap;
import java.util.Map;

import jynx2asm.ops.ExtendedOps;
import jynx2asm.ops.JavaCallOps;
import jynx2asm.ops.JynxOp;
import jynx2asm.ops.MacroLib;
import jynx2asm.ops.SelectOps;

public class ExtensionMacroLib extends MacroLib {
            
    private final static String NAME = "extension";
        
    @Override
    public Map<String,JynxOp> getMacros() {
        Map<String,JynxOp> map = new HashMap<>();
        map.putAll(JavaCallOps.getMacros());
        map.putAll(ExtendedOps.getMacros());
        map.putAll(SelectOps.getMacros());
        return map;
    }
    
    @Override
    public String name() {
        return NAME;
    }

}
