package com.github.minecraftschurlimods.functionalarmortrims;

import net.minecraft.core.RegistryAccess;
import net.minecraftforge.server.ServerLifecycleHooks;

public class RegistryAccessServerProxy {
    static RegistryAccess getRegistryAccessServer() {
        return ServerLifecycleHooks.getCurrentServer().registryAccess();
    }
}
