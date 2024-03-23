/*
 * SkinsRestorer
 * Copyright (C) 2024  SkinsRestorer Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package net.skinsrestorer.shared.commands.library;

import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.tree.ArgumentCommandNode;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.mojang.brigadier.tree.RootCommandNode;

import java.lang.reflect.Field;
import java.util.Map;

public class RecursiveCustomMerger {
    @SuppressWarnings("unchecked")
    public static <T> void mergeThen(ArgumentBuilder<T, ?> builder, CommandNode<T> other) {
        try {
            Field argumentsField = ArgumentBuilder.class.getDeclaredField("arguments");
            argumentsField.setAccessible(true);
            RootCommandNode<T> arguments = (RootCommandNode<T>) argumentsField.get(builder);

            addChild(arguments, other);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> void addChild(CommandNode<T> base, CommandNode<T> other) throws ReflectiveOperationException {
        if (other instanceof RootCommandNode) {
            throw new UnsupportedOperationException("Cannot add a RootCommandNode as a child to any other CommandNode");
        }

        Field childrenField = CommandNode.class.getDeclaredField("children");
        childrenField.setAccessible(true);
        Map<String, CommandNode<T>> children = (Map<String, CommandNode<T>>) childrenField.get(base);

        final CommandNode<T> child = children.get(other.getName());
        if (child != null) {
            // We've found something to merge onto
            if (other.getCommand() != null) {
                Field commandField = CommandNode.class.getDeclaredField("command");
                commandField.setAccessible(true);
                commandField.set(child, other.getCommand());
            }

            if (other.getRequirement() != null) {
                Field requirementField = CommandNode.class.getDeclaredField("requirement");
                requirementField.setAccessible(true);

                PermissionPredicate<?> requirement = (PermissionPredicate<?>) requirementField.get(child);
                if (requirement != null) {
                    requirement.setPermission(((PermissionPredicate<?>) other.getRequirement()).getPermission());
                }
            }

            for (final CommandNode<T> grandchild : other.getChildren()) {
                addChild(child, grandchild);
            }
        } else {
            Field literalsField = CommandNode.class.getDeclaredField("literals");
            literalsField.setAccessible(true);

            Field argumentsField = CommandNode.class.getDeclaredField("arguments");
            argumentsField.setAccessible(true);

            Map<String, LiteralCommandNode<T>> literals = (Map<String, LiteralCommandNode<T>>) literalsField.get(base);
            Map<String, ArgumentCommandNode<T, ?>> arguments = (Map<String, ArgumentCommandNode<T, ?>>) argumentsField.get(base);

            children.put(other.getName(), other);
            if (other instanceof LiteralCommandNode<T> literalCommandNode) {
                literals.put(other.getName(), literalCommandNode);
            } else if (other instanceof ArgumentCommandNode<T, ?> argumentCommandNode) {
                arguments.put(other.getName(), argumentCommandNode);
            }
        }
    }
}
