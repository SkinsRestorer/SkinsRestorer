/*
 * Copyright 2016 inventivetalent. All rights reserved.
 *
 *  Redistribution and use in source and binary forms, with or without modification, are
 *  permitted provided that the following conditions are met:
 *
 *     1. Redistributions of source code must retain the above copyright notice, this list of
 *        conditions and the following disclaimer.
 *
 *     2. Redistributions in binary form must reproduce the above copyright notice, this list
 *        of conditions and the following disclaimer in the documentation and/or other materials
 *        provided with the distribution.
 *
 *  THIS SOFTWARE IS PROVIDED BY THE AUTHOR ''AS IS'' AND ANY EXPRESS OR IMPLIED
 *  WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 *  FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE AUTHOR OR
 *  CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 *  CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 *  SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 *  ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 *  NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 *  ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 *  The views and conclusions contained in the software and documentation are those of the
 *  authors and contributors and should not be interpreted as representing official policies,
 *  either expressed or implied, of anybody else.
 */

package skinsrestorer.shared.utils.updater.bungee;

import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.plugin.Plugin;
import skinsrestorer.shared.utils.updater.core.SpigetUpdateAbstract;
import skinsrestorer.shared.utils.updater.core.VersionComparator;

public class SpigetUpdate extends SpigetUpdateAbstract {

	protected final Plugin plugin;

	public SpigetUpdate(Plugin plugin, int resourceId) {
		super(resourceId, plugin.getDescription().getVersion(), plugin.getLogger());
		this.plugin = plugin;
		setUserAgent("SpigetResourceUpdater/Bungee");
	}

	@Override
	public SpigetUpdate setUserAgent(String userAgent) {
		super.setUserAgent(userAgent);
		return this;
	}

	@Override
	public SpigetUpdate setVersionComparator(VersionComparator comparator) {
		super.setVersionComparator(comparator);
		return this;
	}

	@Override
	protected void dispatch(Runnable runnable) {
		ProxyServer.getInstance().getScheduler().runAsync(plugin, runnable);
	}
}
