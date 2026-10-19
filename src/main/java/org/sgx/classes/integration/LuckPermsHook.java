package org.sgx.classes.integration;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.node.NodeType;
import net.luckperms.api.node.types.InheritanceNode;

import java.util.Set;
import java.util.UUID;

/**
 * Весь код, который использует API LuckPerms. Вызывается ТОЛЬКО из LuckPermsIntegration
 * после проверки FabricLoader.isModLoaded("luckperms"). Без LuckPerms этот класс не загружается.
 */
final class LuckPermsHook {

    private LuckPermsHook() {}

    static void sync(UUID playerId, String targetGroup, Set<String> allClassGroups) {
        LuckPerms api = LuckPermsProvider.get();

        if (targetGroup != null) {
            api.getGroupManager().createAndLoadGroup(targetGroup);
        }

        api.getUserManager().modifyUser(playerId, user -> {
            user.data().clear(n -> n.getType() == NodeType.INHERITANCE
                    && allClassGroups.contains(((InheritanceNode) n).getGroupName()));

            if (targetGroup != null) {
                user.data().add(InheritanceNode.builder(targetGroup).build());
            }
        });
    }
}