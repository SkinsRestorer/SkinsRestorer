package skinsrestorer.shared.interfaces;

import co.aikar.commands.CommandHelp;

/**
 * Created by McLive on 18.08.2019.
 *
 *  @param <C> Platform specific CommandSender
 *  @param <P> Platform specific Player
 *  @param <OP> Platform specific OnlinePlayer
 */
public interface ISkinCommand<C, P, OP> {
    void onDefault(C sender);
    void onSkinSetShort(P p, String skin);

    void onSkinClear(P p);
    void onSkinClearOther(C sender, OP target);

    void onSkinUpdate(P p);
    void onSkinUpdateOther(C sender, OP target);

    void onSkinSet(P p, String skin);
    void onSkinSetOther(C sender, OP target, String skin);

    void onHelp(C sender, CommandHelp help);
}
