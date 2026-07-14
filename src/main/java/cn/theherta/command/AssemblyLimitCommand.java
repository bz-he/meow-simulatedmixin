package cn.theherta.command;

import cn.theherta.Config;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

public class AssemblyLimitCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("meow")
                        .requires(src -> src.hasPermission(2)) // 需要OP权限
                        .then(Commands.literal("query")
                                .executes(ctx -> {
                                    int maxSize = Config.MAX_STRUCTURE_SIZE.get();
                                    ctx.getSource().sendSuccess(
                                            () -> Component.literal("当前最大结构方块数限制: " + maxSize), false
                                    );
                                    return 1;
                                })
                        )
                        .then(Commands.literal("set")
                                .then(Commands.argument("size", IntegerArgumentType.integer(1))
                                        .executes(ctx -> {
                                            int newSize = IntegerArgumentType.getInteger(ctx, "size");
                                            Config.MAX_STRUCTURE_SIZE.set(newSize);
                                            // 保存到配置文件
                                            Config.SPEC.save();
                                            ctx.getSource().sendSuccess(
                                                    () -> Component.literal("已将最大结构方块数限制设置为: " + newSize), true
                                            );
                                            return 1;
                                        })
                                )
                        )
        );
    }
}