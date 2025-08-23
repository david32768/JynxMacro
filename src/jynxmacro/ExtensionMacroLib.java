package jynxmacro;

import java.util.HashMap;
import java.util.Map;

import com.github.david32768.jynxfor.ops.ExtendedOps;
import com.github.david32768.jynxfor.ops.JavaCallOps;
import com.github.david32768.jynxfor.ops.JynxOp;
import com.github.david32768.jynxfor.ops.MacroLib;
import com.github.david32768.jynxfor.ops.SelectOps;

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
