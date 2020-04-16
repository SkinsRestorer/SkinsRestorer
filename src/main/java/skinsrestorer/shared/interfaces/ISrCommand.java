package skinsrestorer.shared.interfaces;

import co.aikar.commands.CommandHelp;

/**
 * Created by McLive on 18.08.2019.
 *
 *  @param <C> Platform specific CommandSender
 *  @param <OP> Platform specific OnlinePlayer
 */
public interface ISrCommand<C, OP> {
    void onReload(C sender);
    void onStatus(C sender);
    void onDrop(C sender, OP target);
    void onProps(C sender, OP target);

    void onHelp(C sender, CommandHelp help);
}
