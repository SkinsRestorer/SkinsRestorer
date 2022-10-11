SkinsRestorer is running in "Proxy Mode"
If you are NOT using a proxy (Bungeecord, waterfall or velocity) in your network, set spigot.yml -> bungeecord: false

Using a proxy? --> you MUST use proxy installation! (or use override files)

What is "Proxy mode"
The proxy will handle SkinsRestorer's command's, API, permission, database and config in a "Centralised" manner.
This way, each backend does not need to apply the skin when switching servers.

How to install on proxy?
Download the latest version from https://www.spigotmc.org/resources/skinsrestorer.2124/
Place the SkinsRestorer.jar in ./plugins/ folders of every Spigot server.
Place the plugin in ./plugins/ folder of every proxy server.
Check & set on every backend server spigot.yml -> bungeecord: true
Restart (/restart or /stop) all servers [Plugman or /reload are NOT supported, use /stop or /end]

Your proxy will now tell all your backend servers what to do!
You may now configure SkinsRestorer on your proxy (<proxy>/plugins/SkinsRestorer)

!== override files ==!
BackendStorage (api & commands):
You can enable the backend storage by create a file called enableSkinStorageAPI.txt in SkinsRestorer folder (backend server -> ./plugins/SkinsRestorer/enableSkinStorageAPI.txt)
This is useful when using plugins like bedwards, dynmap, etc. that need to get the skin through api from the backend server.
keep in mind that connecting mysql to the same database as the proxy is required for this to work!

ProxyMode:
Warning: Disable ProxyMode will force the backend to handle everything including applying skin on join. This is not recommended as the proxy would only need to apply the skin once.
How to urn off proxy mode: create a file called disableProxyMode.txt in SkinsRestorer folder (backend server -> ./plugins/SkinsRestorer/disableProxyMode.txt)
This will force disable proxy mode on next restart.